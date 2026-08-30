package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.FoodAnalysisResult
import com.example.data.model.MealType
import com.example.ui.theme.*
import com.example.viewmodel.MainViewModel

data class PresetFoodPlate(
    val title: String,
    val description: String,
    val primaryColor: Color,
    val iconEmoji: String
)

@Composable
fun FoodScannerScreen(
    viewModel: MainViewModel,
    onMealLogged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scannedBitmap by viewModel.scannedBitmap.collectAsState()
    val isAnalyzing by viewModel.isAnalyzingFood.collectAsState()
    val analysisResult by viewModel.foodAnalysisResult.collectAsState()
    val errorMessage by viewModel.scannerErrorMessage.collectAsState()

    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var showSuccessSnackbar by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            viewModel.setScannedBitmap(bitmap)
        }
    }

    // Gallery picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    viewModel.setScannedBitmap(bitmap)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    val presetPlates = remember {
        listOf(
            PresetFoodPlate(
                "Куриная грудка с киноа и брокколи",
                "Идеальный баланс белка и клетчатки для сушки",
                Color(0xFF10B981),
                "🥗"
            ),
            PresetFoodPlate(
                "Стейк из лосося с авокадо и спаржей",
                "Богат омега-3 и полезными жирами",
                Color(0xFFF59E0B),
                "🥑"
            ),
            PresetFoodPlate(
                "Овсяная каша с черникой и семенами чиа",
                "Медленные углеводы для сытного завтрака",
                Color(0xFF3B82F6),
                "🥣"
            ),
            PresetFoodPlate(
                "Греческий салат с фетой и оливками",
                "Низкокалорийный легкий перекус",
                Color(0xFF8B5CF6),
                "🍅"
            )
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Нейросеть-Сканер Еды",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Распознает блюдо на фото, рассчитывает калории и БЖУ (белки, жиры, углеводы) с советами для похудения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Action Buttons: Take Photo / Choose Gallery
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("open_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Камера",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Камера", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("open_gallery_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Галерея",
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Галерея", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick Preset Plates Picker for instant testing
        item {
            Column {
                Text(
                    text = "Или выберите тестовое блюдо для проверки ИИ:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(presetPlates) { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier
                                .width(200.dp)
                                .clickable {
                                    val bmp = createSamplePlateBitmap(preset.title, preset.iconEmoji)
                                    viewModel.setScannedBitmap(bmp)
                                }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = preset.iconEmoji,
                                    fontSize = 28.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = preset.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2
                                )
                            }
                        }
                    }
                }
            }
        }

        // Image Preview & Scanning Indicator
        if (scannedBitmap != null) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            bitmap = scannedBitmap!!.asImageBitmap(),
                            contentDescription = "Фотография блюда",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        if (isAnalyzing) {
                            // Scanning animation overlay
                            val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
                            val scanProgress by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scanProgress"
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            ) {
                                // Animated glowing line
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(0.04f)
                                        .align(Alignment.TopCenter)
                                        .offset(y = (scanProgress * 220).dp)
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, EmeraldLight, Color.Transparent)
                                            )
                                        )
                                )

                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        color = EmeraldLight,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Нейросеть определяет калории и БЖУ...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Close/Clear button
                        IconButton(
                            onClick = { viewModel.clearScannedFood() },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Закрыть",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Error message if any
        if (errorMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }

        // Food Analysis Results Card
        if (analysisResult != null && !isAnalyzing) {
            item {
                FoodAnalysisResultCard(
                    result = analysisResult!!,
                    selectedMealType = selectedMealType,
                    onMealTypeChanged = { selectedMealType = it },
                    onLogMeal = {
                        viewModel.logAnalyzedMeal(selectedMealType)
                        showSuccessSnackbar = true
                        onMealLogged()
                    }
                )
            }
        }
    }
}

@Composable
fun FoodAnalysisResultCard(
    result: FoodAnalysisResult,
    selectedMealType: MealType,
    onMealTypeChanged: (MealType) -> Unit,
    onLogMeal: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("food_analysis_result_card"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Title & Health Score Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.dishName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Порция: ~${result.portionGrams} г • ГИ: ${result.glycemicIndex}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (result.healthScore >= 7) EmeraldContainer else AmberContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Оценка: ${result.healthScore}/10",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.healthScore >= 7) OnEmeraldContainer else OnAmberContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Calories Highlight Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Калорийность порции",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${result.calories} ккал",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroPill("Белки", "${"%.1f".format(result.proteinGrams)}г", ProteinColor)
                        MacroPill("Жиры", "${"%.1f".format(result.fatGrams)}г", FatColor)
                        MacroPill("Углеводы", "${"%.1f".format(result.carbsGrams)}г", CarbsColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nutritionist Weight Loss Advice Card
            if (result.weightLossAdvice.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Совет диетолога по похудению:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OnEmeraldContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = result.weightLossAdvice,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Ingredients
            if (result.ingredients.isNotEmpty()) {
                Text(
                    text = "Ингредиенты:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = result.ingredients.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Meal Type Selector
            Text(
                text = "Добавить в дневник как:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MealType.values().forEach { type ->
                    FilterChip(
                        selected = selectedMealType == type,
                        onClick = { onMealTypeChanged(type) },
                        label = { Text(type.titleRu, fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Log Button
            Button(
                onClick = onLogMeal,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("log_scanned_meal_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Записать блюдо в дневник", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun MacroPill(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = color
            )
        }
    }
}

/**
 * Creates a sample food plate bitmap for instant testing without camera
 */
private fun createSamplePlateBitmap(dishTitle: String, emoji: String): Bitmap {
    val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Background plate
    val paintBg = Paint().apply { color = android.graphics.Color.rgb(240, 245, 242) }
    canvas.drawRect(0f, 0f, 400f, 400f, paintBg)

    // Ceramic plate circle
    val paintPlate = Paint().apply {
        color = android.graphics.Color.rgb(255, 255, 255)
        isAntiAlias = true
    }
    canvas.drawCircle(200f, 200f, 170f, paintPlate)

    // Inner rim
    val paintRim = Paint().apply {
        color = android.graphics.Color.rgb(210, 230, 220)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    canvas.drawCircle(200f, 200f, 155f, paintRim)

    // Emoji icon
    val paintEmoji = Paint().apply {
        textSize = 90f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    canvas.drawText(emoji, 200f, 190f, paintEmoji)

    // Title label
    val paintText = Paint().apply {
        color = android.graphics.Color.rgb(30, 60, 45)
        textSize = 20f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }
    val shortTitle = if (dishTitle.length > 25) dishTitle.substring(0, 22) + "..." else dishTitle
    canvas.drawText(shortTitle, 200f, 250f, paintText)

    return bitmap
}
