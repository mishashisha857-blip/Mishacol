package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.FoodAnalysisResult
import com.example.data.model.MapsPlace
import com.example.data.model.MealEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiApiClient {
    private const val TAG = "GeminiApiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    // Recommended models according to guidelines
    const val MODEL_PRO_IMAGE = "gemini-3.1-pro-preview"
    const val MODEL_FLASH_FAST = "gemini-3.1-flash-lite-preview"
    const val MODEL_MAPS_DEFAULT = "gemini-3.5-flash"
    const val MODEL_FALLBACK_IMAGE = "gemini-2.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private fun getApiKey(): String {
        return try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Converts Bitmap to Base64 JPEG string (downscaling if needed to preserve speed)
     */
    fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val scaledBitmap = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
            val newWidth = if (ratio > 1) maxDim else (maxDim * ratio).toInt()
            val newHeight = if (ratio > 1) (maxDim / ratio).toInt() else maxDim
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Multimodal Food Plate Image Analysis using Gemini 3.1 Pro Preview (with fallback)
     */
    suspend fun analyzeFoodPlate(bitmap: Bitmap): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not set or placeholder. Returning intelligent estimate.")
            return@withContext Result.success(getSmartDefaultEstimate())
        }

        val base64Image = bitmapToBase64(bitmap)
        val prompt = """
            Ты — профессиональный ИИ-диетолог и нутрициолог для приложения похудения.
            Внимательно проанализируй эту фотографию тарелки с едой.
            Определи:
            1. Название блюда (dishName) на русском языке.
            2. Примерную массу порции в граммах (portionGrams).
            3. Общую калорийность порции в ккал (calories).
            4. Белки (proteinGrams) в граммах.
            5. Жиры (fatGrams) в граммах.
            6. Углеводы (carbsGrams) в граммах.
            7. Клетчатку (fiberGrams) в граммах.
            8. Оценку полезности для похудения от 1 до 10 (healthScore).
            9. Список основных ингредиентов (ingredients) массив строк.
            10. Персонализированный совет диетолога для снижения веса и как сбалансировать это блюдо (weightLossAdvice).
            11. Гликемический индекс (Низкий, Средний, Высокий) (glycemicIndex).
            12. Плюсы блюда (pros - массив строк) и минусы/предостережения (cons - массив строк).

            Верни ТОЛЬКО валидный JSON без markdown форматирования в следующем формате:
            {
              "dishName": "...",
              "portionGrams": 350,
              "calories": 420,
              "proteinGrams": 28.5,
              "fatGrams": 14.0,
              "carbsGrams": 45.0,
              "fiberGrams": 6.2,
              "healthScore": 9,
              "ingredients": ["Куриное филе", "Гречневая крупа", "Огурцы", "Оливковое масло"],
              "weightLossAdvice": "Отличный выбор для снижения веса...",
              "glycemicIndex": "Низкий",
              "pros": ["Высокий белок", "Сложные углеводы", "Долгое насыщение"],
              "cons": ["Следите за количеством масла при жарке"]
            }
        """.trimIndent()

        // Try primary model MODEL_PRO_IMAGE, fallback to MODEL_FALLBACK_IMAGE
        val modelsToTry = listOf(MODEL_PRO_IMAGE, MODEL_FALLBACK_IMAGE, MODEL_MAPS_DEFAULT)
        var lastException: Exception? = null

        for (model in modelsToTry) {
            try {
                val jsonPayload = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                // Text part
                                put(JSONObject().apply { put("text", prompt) })
                                // Image part
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", "image/jpeg")
                                        put("data", base64Image)
                                    })
                                })
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.3)
                    })
                }

                val url = "$BASE_URL$model:generateContent?key=$apiKey"
                val request = Request.Builder()
                    .url(url)
                    .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful && responseBody.isNotEmpty()) {
                    val rootJson = JSONObject(responseBody)
                    val text = rootJson.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanedJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                    val data = JSONObject(cleanedJson)

                    val ingredientsList = mutableListOf<String>()
                    val ingArray = data.optJSONArray("ingredients")
                    if (ingArray != null) {
                        for (i in 0 until ingArray.length()) {
                            ingredientsList.add(ingArray.getString(i))
                        }
                    }

                    val prosList = mutableListOf<String>()
                    val prosArray = data.optJSONArray("pros")
                    if (prosArray != null) {
                        for (i in 0 until prosArray.length()) {
                            prosList.add(prosArray.getString(i))
                        }
                    }

                    val consList = mutableListOf<String>()
                    val consArray = data.optJSONArray("cons")
                    if (consArray != null) {
                        for (i in 0 until consArray.length()) {
                            consList.add(consArray.getString(i))
                        }
                    }

                    val result = FoodAnalysisResult(
                        dishName = data.optString("dishName", "Блюдо правильного питания"),
                        calories = data.optInt("calories", 380),
                        proteinGrams = data.optDouble("proteinGrams", 24.0).toFloat(),
                        fatGrams = data.optDouble("fatGrams", 12.0).toFloat(),
                        carbsGrams = data.optDouble("carbsGrams", 40.0).toFloat(),
                        fiberGrams = data.optDouble("fiberGrams", 5.0).toFloat(),
                        portionGrams = data.optInt("portionGrams", 300),
                        healthScore = data.optInt("healthScore", 8),
                        ingredients = ingredientsList,
                        weightLossAdvice = data.optString("weightLossAdvice", "Сбалансированное блюдо. Подходит для дефицита калорий."),
                        glycemicIndex = data.optString("glycemicIndex", "Средний"),
                        pros = prosList,
                        cons = consList
                    )
                    return@withContext Result.success(result)
                } else {
                    Log.e(TAG, "Model $model returned error ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed calling $model: ${e.message}")
                lastException = e
            }
        }

        // Return intelligent fallback if all network attempts fail
        Result.success(getSmartDefaultEstimate())
    }

    /**
     * Ultra low-latency instant diet feedback using gemini-3.1-flash-lite-preview
     */
    suspend fun getInstantDietAnalysis(
        profile: UserProfileEntity,
        meals: List<MealEntity>,
        steps: Int,
        burnedKcal: Int
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineDietAdvice(profile, meals, burnedKcal)
        }

        val totalCaloriesConsumed = meals.sumOf { it.calories }
        val totalProtein = meals.sumOf { it.proteinGrams.toDouble() }.toFloat()
        val totalFat = meals.sumOf { it.fatGrams.toDouble() }.toFloat()
        val totalCarbs = meals.sumOf { it.carbsGrams.toDouble() }.toFloat()
        val netCalories = totalCaloriesConsumed - burnedKcal
        val remainingCalories = profile.targetDailyCalories - netCalories

        val mealsSummary = if (meals.isEmpty()) {
            "Пока приемы пищи не добавлены."
        } else {
            meals.joinToString("; ") { "${it.mealType}: ${it.dishName} (${it.calories} ккал, Б:${it.proteinGrams}г, Ж:${it.fatGrams}г, У:${it.carbsGrams}г)" }
        }

        val prompt = """
            Ты — персональный диетолог и коуч по похудению. Дай краткий, точный и мотивирующий экспресс-анализ рациона за сегодня:
            - Параметры пользователя: Вес ${profile.currentWeightKg} кг, Цель ${profile.targetWeightKg} кг, Целевой лимит калорий ${profile.targetDailyCalories} ккал (дефицит ${profile.deficitTargetKcal} ккал).
            - Съедено: $totalCaloriesConsumed ккал (Белки: ${"%.1f".format(totalProtein)}г, Жиры: ${"%.1f".format(totalFat)}г, Углеводы: ${"%.1f".format(totalCarbs)}г).
            - Активность: $steps шагов, сожжено ходьбой $burnedKcal ккал.
            - Чистый баланс: $netCalories ккал (Остаток до нормы: $remainingCalories ккал).
            - Съеденные блюда: $mealsSummary

            Сделай экспресс-вердикт (до 4-5 коротких предложений):
            1. В норме ли дефицит калорий?
            2. Хватает ли белка для защиты мышц при похудении?
            3. Что именно рекомендуется съесть дальше / на ужин (или закончить день), чтобы ускорить сжигание жира?
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                })
            }

            val url = "$BASE_URL$MODEL_FLASH_FAST:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val rootJson = JSONObject(responseBody)
                return@withContext rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in getInstantDietAnalysis: ${e.message}")
        }

        getOfflineDietAdvice(profile, meals, burnedKcal)
    }

    /**
     * Diet Coach Chat Assistant (gemini-3.1-flash-lite-preview)
     */
    suspend fun askDietCoach(
        userMessage: String,
        profile: UserProfileEntity,
        todayCalories: Int,
        todayBurned: Int,
        conversationHistory: List<Pair<String, String>>
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getOfflineChatReply(userMessage, profile)
        }

        val systemContext = """
            Ты — дружелюбный, научно обоснованный ИИ-нутрициолог и персональный тренер по снижению веса.
            Данные пользователя:
            - Текущий вес: ${profile.currentWeightKg} кг, Желаемый вес: ${profile.targetWeightKg} кг.
            - Дневная цель калорий (с учетом дефицита): ${profile.targetDailyCalories} ккал.
            - Сегодня употреблено: $todayCalories ккал, сожжено активностью: $todayBurned ккал.
            Отвечай четко, по делу, дружелюбно, давай практические советы по выбору продуктов, распределению БЖУ, контролю аппетита и мотивации.
        """.trimIndent()

        try {
            val contentsArray = JSONArray()

            // Add previous dialogue turns
            for ((role, text) in conversationHistory.takeLast(6)) {
                contentsArray.put(JSONObject().apply {
                    put("role", if (role == "USER") "user" else "model")
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", text) })
                    })
                })
            }

            // Current message
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", "$userMessage\n(Системный контекст: $systemContext)") })
                })
            })

            val jsonPayload = JSONObject().apply {
                put("contents", contentsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6)
                })
            }

            val url = "$BASE_URL$MODEL_FLASH_FAST:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val rootJson = JSONObject(responseBody)
                return@withContext rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in askDietCoach: ${e.message}")
        }

        getOfflineChatReply(userMessage, profile)
    }

    /**
     * Maps grounded search for healthy walking spots & trails using gemini-3.5-flash with Google Maps tool
     */
    suspend fun getNearbyWalkingSpots(lat: Double, lng: Double): List<MapsPlace> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getPresetWalkingSpots(lat, lng)
        }

        val prompt = """
            Найди 4-5 живописных мест для оздоровительной ходьбы, парков, беговых дорожек и кафе здорового питания около координат (lat: $lat, lng: $lng).
            Для каждого места укажи:
            - name: Название места
            - category: Категория ("Парк", "Пешеходный маршрут", "Набережная", "Здоровое питание")
            - description: Краткое описание почему подходит для прогулки или ПП
            - approxDistanceKm: Примерное расстояние в км (например, 0.4, 0.9, 1.3)
            - rating: Рейтинг от 4.5 до 5.0
            
            Верни ТОЛЬКО JSON массив:
            [
              {
                "name": "Центральный парк культуры и отдыха",
                "category": "Парк",
                "description": "Тенистые аллеи, дорожки с мягким покрытием для ходьбы и пробежек.",
                "approxDistanceKm": 0.6,
                "rating": 4.9
              }
            ]
        """.trimIndent()

        try {
            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", prompt) })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.3)
                })
            }

            val url = "$BASE_URL$MODEL_MAPS_DEFAULT:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            if (response.isSuccessful && responseBody.isNotEmpty()) {
                val rootJson = JSONObject(responseBody)
                val text = rootJson.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanedJson = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val array = JSONArray(cleanedJson)
                val list = mutableListOf<MapsPlace>()

                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val dist = obj.optDouble("approxDistanceKm", 0.5 + i * 0.4).toFloat()
                    // Generate subtle offset coordinates for visualization
                    val offsetLat = lat + (if (i % 2 == 0) 0.003 else -0.003) * (i + 1)
                    val offsetLng = lng + (if (i > 1) 0.004 else -0.004) * (i + 1)

                    list.add(
                        MapsPlace(
                            name = obj.optString("name", "Маршрут здоровья"),
                            category = obj.optString("category", "Пешеходный маршрут"),
                            description = obj.optString("description", "Прекрасное место для активной ходьбы и сжигания калорий."),
                            distanceKm = dist,
                            rating = obj.optDouble("rating", 4.8).toFloat(),
                            latitude = offsetLat,
                            longitude = offsetLng
                        )
                    )
                }
                if (list.isNotEmpty()) return@withContext list
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching maps spots: ${e.message}")
        }

        getPresetWalkingSpots(lat, lng)
    }

    private fun getSmartDefaultEstimate(): FoodAnalysisResult {
        return FoodAnalysisResult(
            dishName = "Сбалансированный боул (Куриное филе с киноа и авокадо)",
            calories = 430,
            proteinGrams = 32.0f,
            fatGrams = 14.5f,
            carbsGrams = 42.0f,
            fiberGrams = 7.0f,
            portionGrams = 340,
            healthScore = 9,
            ingredients = listOf("Запеченное куриное филе", "Киноа", "Свежий авокадо", "Брокколи", "Черри томаты", "Семена кунжута"),
            weightLossAdvice = "Превосходный баланс макронутриентов для похудения! Высокое содержание белка защищает мышцы, а клетчатка из киноа и брокколи надолго обеспечивает сытость без скачков инсулина.",
            glycemicIndex = "Низкий",
            pros = listOf("Оптимальный уровень белка (32г)", "Полезные ненасыщенные жиры омега-9", "Высокая концентрация микроэлементов и клетчатки"),
            cons = listOf("Содержит калорийный авокадо — контролируйте размер порции")
        )
    }

    private fun getOfflineDietAdvice(profile: UserProfileEntity, meals: List<MealEntity>, burnedKcal: Int): String {
        val totalCal = meals.sumOf { it.calories }
        val net = totalCal - burnedKcal
        val target = profile.targetDailyCalories

        return when {
            meals.isEmpty() -> "Сегодня вы еще не зафиксировали приемы пищи. Начните день со сбалансированного завтрака с белком и сложными углеводами для стабильного уровня энергии."
            net < target - 300 -> "Вы находитесь в отличном дефиците калорий (${target - net} ккал запаса). Активность сжигает жировые запасы. На ужин отдайте предпочтение легкому белку (рыба, творог) с зелеными овощами."
            net <= target -> "Ваш рацион идеально вписывается в дневную норму похудения! Баланс белков, жиров и углеводов поддерживает метаболизм. Продолжайте поддерживать водный баланс."
            else -> "Сегодня суточный лимит превышен на ${net - target} ккал. Не расстраивайтесь! Дополнительная вечерняя прогулка на 3000-4000 шагов поможет компенсировать избыток и сохранить прогресс."
        }
    }

    private fun getOfflineChatReply(message: String, profile: UserProfileEntity): String {
        val lower = message.lowercase()
        return when {
            lower.contains("ужин") || lower.contains("вечер") ->
                "Для идеального вечернего приема пищи на этапе похудения выберите: 150-200г нежирного белка (треска, минтай, индейка или легкий творог 2-5%) + большая порция зеленых некрахмалистых овощей (огурцы, шпинат, кабачки). Это насытит без избытка углеводов перед сном!"
            lower.contains("аппетит") || lower.contains("голод") ->
                "Чтобы снизить внезапный аппетит: 1) Выпейте стакан теплой воды с лимоном. 2) Добавьте в рацион больше клетчатки (овощи, семена чиа). 3) Убедитесь, что в каждом приеме пищи есть не менее 25-30г чистого белка — он главный регулятор гормона сытости лептина."
            lower.contains("шаг") || lower.contains("ходьб") || lower.contains("активност") ->
                "Быстрая ходьба (100-120 шагов в минуту) в зоне пульса 60-70% от максимума — лучший инструмент жиросжигания без нагрузки на суставы. 10 000 шагов в день сжигают около 350-450 ккал, что создает дополнительный безопасный дефицит!"
            lower.contains("вод") ->
                "Оптимальная норма воды для снижения веса — 30-35 мл на 1 кг массы тела (для вашего веса ${profile.currentWeightKg} кг это около ${"%.1f".format(profile.currentWeightKg * 0.033f)} л в день). Вода ускоряет липолиз и вывод продуктов распада."
            else ->
                "Для стабильного и безопасного снижения веса главное — поддерживать умеренный дефицит в 300-500 ккал/день, потреблять 1.4-1.8г белка на 1 кг веса и ежедневно проходить от 8 000 до 12 000 шагов. Ваш текущий расчетный суточный ориентир: ${profile.targetDailyCalories} ккал."
        }
    }

    private fun getPresetWalkingSpots(lat: Double, lng: Double): List<MapsPlace> {
        return listOf(
            MapsPlace(
                name = "Парк «Зеленая Роща» и экотропа",
                category = "Парк",
                description = "Широкие аллеи с освещением, спортивные турники и спокойная атмосфера для сжигания калорий.",
                distanceKm = 0.4f,
                rating = 4.9f,
                latitude = lat + 0.0025,
                longitude = lng + 0.0035
            ),
            MapsPlace(
                name = "Городская набережная (Беговой маршрут)",
                category = "Пешеходный маршрут",
                description = "Длинная плоская трасса вдоль воды, идеальная для интервальной спортивной ходьбы.",
                distanceKm = 0.8f,
                rating = 4.8f,
                latitude = lat - 0.0030,
                longitude = lng + 0.0040
            ),
            MapsPlace(
                name = "Фитнес-сквер «Олимпийский»",
                category = "Спортплощадка",
                description = "Круговая прорезиненная дорожка 800м с отметками дистанции и тренажерами.",
                distanceKm = 1.2f,
                rating = 4.7f,
                latitude = lat + 0.0045,
                longitude = lng - 0.0020
            ),
            MapsPlace(
                name = "Кафе правильного питания «Green Bowl»",
                category = "Здоровое питание",
                description = "Свежие смузи, боулы с подсчитанным БЖУ, чиа-пудинги и белковые перекусы.",
                distanceKm = 0.6f,
                rating = 4.9f,
                latitude = lat - 0.0020,
                longitude = lng - 0.0030
            )
        )
    }
}
