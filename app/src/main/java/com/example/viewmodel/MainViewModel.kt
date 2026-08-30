package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiApiClient
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.sensor.LocationTracker
import com.example.data.sensor.StepSensorManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val mealDao = db.mealDao()
    private val userProfileDao = db.userProfileDao()
    private val walkDao = db.walkDao()

    val stepSensorManager = StepSensorManager(application)
    val locationTracker = LocationTracker(application)

    // User Profile State
    val userProfile: StateFlow<UserProfileEntity> = userProfileDao.getUserProfile()
        .map { it ?: UserProfileEntity() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfileEntity()
        )

    // Today's Date Boundaries
    private val startOfDay: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

    private val endOfDay: Long
        get() {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            return cal.timeInMillis
        }

    // Today's logged meals
    val todayMeals: StateFlow<List<MealEntity>> = mealDao.getMealsForDay(startOfDay, endOfDay)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Food Scanner State
    private val _scannedBitmap = MutableStateFlow<Bitmap?>(null)
    val scannedBitmap: StateFlow<Bitmap?> = _scannedBitmap.asStateFlow()

    private val _isAnalyzingFood = MutableStateFlow(false)
    val isAnalyzingFood: StateFlow<Boolean> = _isAnalyzingFood.asStateFlow()

    private val _foodAnalysisResult = MutableStateFlow<FoodAnalysisResult?>(null)
    val foodAnalysisResult: StateFlow<FoodAnalysisResult?> = _foodAnalysisResult.asStateFlow()

    private val _scannerErrorMessage = MutableStateFlow<String?>(null)
    val scannerErrorMessage: StateFlow<String?> = _scannerErrorMessage.asStateFlow()

    // Step Tracking & Active Workout State
    val liveSteps: StateFlow<Int> = stepSensorManager.liveSteps

    private val _isWorkoutActive = MutableStateFlow(false)
    val isWorkoutActive: StateFlow<Boolean> = _isWorkoutActive.asStateFlow()

    private val _workoutDurationSeconds = MutableStateFlow(0L)
    val workoutDurationSeconds: StateFlow<Long> = _workoutDurationSeconds.asStateFlow()

    private var timerJob: Job? = null

    val workoutDistanceMeters: StateFlow<Float> = locationTracker.workoutDistanceMeters
    val routePoints: StateFlow<List<LatLngPoint>> = locationTracker.routePoints
    val currentLocation: StateFlow<LatLngPoint?> = locationTracker.currentLocation

    // AI Diet Coach & Audit State
    private val _instantDietAudit = MutableStateFlow<String>("")
    val instantDietAudit: StateFlow<String> = _instantDietAudit.asStateFlow()

    private val _isAuditLoading = MutableStateFlow(false)
    val isAuditLoading: StateFlow<Boolean> = _isAuditLoading.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = MessageSender.AI_COACH,
                text = "Привет! Я твой персональный ИИ-диетолог и фитнес-наставник. Задай любой вопрос о питании, дефиците калорий или тренировках!"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // Nearby Maps Spots
    private val _nearbyPlaces = MutableStateFlow<List<MapsPlace>>(emptyList())
    val nearbyPlaces: StateFlow<List<MapsPlace>> = _nearbyPlaces.asStateFlow()

    init {
        // Initialize default user profile if none exists
        viewModelScope.launch {
            userProfileDao.getUserProfile().firstOrNull()?.let {
                // profile exists
            } ?: run {
                userProfileDao.insertOrUpdateProfile(UserProfileEntity())
            }
            // Seed initial sample meal for rich initial experience if empty
            if (mealDao.getAllMeals().first().isEmpty()) {
                mealDao.insertMeal(
                    MealEntity(
                        timestamp = System.currentTimeMillis() - 3600_000 * 4,
                        mealType = MealType.BREAKFAST.name,
                        dishName = "Овсяная каша с ягодами и миндалем",
                        calories = 340,
                        proteinGrams = 12.5f,
                        fatGrams = 8.0f,
                        carbsGrams = 54.0f,
                        fiberGrams = 6.0f,
                        portionGrams = 280,
                        healthRating = 9,
                        advice = "Прекрасный сложный углевод на утро, дает долгую сытость."
                    )
                )
                mealDao.insertMeal(
                    MealEntity(
                        timestamp = System.currentTimeMillis() - 3600_000 * 1,
                        mealType = MealType.LUNCH.name,
                        dishName = "Куриная грудка на гриле с брокколи и гречкой",
                        calories = 460,
                        proteinGrams = 42.0f,
                        fatGrams = 11.0f,
                        carbsGrams = 48.0f,
                        fiberGrams = 7.5f,
                        portionGrams = 350,
                        healthRating = 10,
                        advice = "Эталонное блюдо для снижения процента жира!"
                    )
                )
            }
        }

        stepSensorManager.startListening()
        // Set initial test steps for rich preview
        stepSensorManager.setSteps(4280)
        fetchNearbyPlaces()
    }

    // ----------------------------------------------------
    // Food Scanner Operations
    // ----------------------------------------------------
    fun setScannedBitmap(bitmap: Bitmap) {
        _scannedBitmap.value = bitmap
        analyzeScannedFood(bitmap)
    }

    fun analyzeScannedFood(bitmap: Bitmap) {
        viewModelScope.launch {
            _isAnalyzingFood.value = true
            _scannerErrorMessage.value = null
            try {
                val result = GeminiApiClient.analyzeFoodPlate(bitmap)
                if (result.isSuccess) {
                    _foodAnalysisResult.value = result.getOrNull()
                } else {
                    _scannerErrorMessage.value = result.exceptionOrNull()?.message ?: "Не удалось распознать блюдо"
                }
            } catch (e: Exception) {
                _scannerErrorMessage.value = e.message
            } finally {
                _isAnalyzingFood.value = false
            }
        }
    }

    fun logAnalyzedMeal(mealType: MealType) {
        val analysis = _foodAnalysisResult.value ?: return
        viewModelScope.launch {
            val meal = MealEntity(
                timestamp = System.currentTimeMillis(),
                mealType = mealType.name,
                dishName = analysis.dishName,
                calories = analysis.calories,
                proteinGrams = analysis.proteinGrams,
                fatGrams = analysis.fatGrams,
                carbsGrams = analysis.carbsGrams,
                fiberGrams = analysis.fiberGrams,
                portionGrams = analysis.portionGrams,
                healthRating = analysis.healthScore,
                advice = analysis.weightLossAdvice,
                ingredients = analysis.ingredients.joinToString(", ")
            )
            mealDao.insertMeal(meal)
            _foodAnalysisResult.value = null
            _scannedBitmap.value = null
            // Trigger automatic quick audit refresh
            refreshDietAudit()
        }
    }

    fun addManualMeal(
        dishName: String,
        calories: Int,
        protein: Float,
        fat: Float,
        carbs: Float,
        mealType: MealType
    ) {
        viewModelScope.launch {
            val meal = MealEntity(
                timestamp = System.currentTimeMillis(),
                mealType = mealType.name,
                dishName = dishName,
                calories = calories,
                proteinGrams = protein,
                fatGrams = fat,
                carbsGrams = carbs,
                portionGrams = 250,
                healthRating = 8,
                advice = "Добавлено вручную."
            )
            mealDao.insertMeal(meal)
            refreshDietAudit()
        }
    }

    fun deleteMeal(meal: MealEntity) {
        viewModelScope.launch {
            mealDao.deleteMeal(meal)
            refreshDietAudit()
        }
    }

    fun clearScannedFood() {
        _scannedBitmap.value = null
        _foodAnalysisResult.value = null
        _scannerErrorMessage.value = null
    }

    // ----------------------------------------------------
    // Step & Workout Operations
    // ----------------------------------------------------
    fun startWorkout() {
        if (_isWorkoutActive.value) return
        _isWorkoutActive.value = true
        locationTracker.startTracking()

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isWorkoutActive.value) {
                delay(1000L)
                _workoutDurationSeconds.value += 1
            }
        }
    }

    fun pauseWorkout() {
        _isWorkoutActive.value = false
        timerJob?.cancel()
        locationTracker.stopTracking()
    }

    fun finishAndSaveWorkout() {
        pauseWorkout()
        val duration = _workoutDurationSeconds.value
        val distance = locationTracker.workoutDistanceMeters.value
        val weight = userProfile.value.currentWeightKg
        val stepsInWorkout = ((distance / 0.75f).toInt()).coerceAtLeast((duration / 1.5).toInt())
        val burned = StepSensorManager.calculateBurnedCalories(stepsInWorkout, weight)

        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val session = WalkSessionEntity(
                dateString = sdf.format(Date()),
                steps = stepsInWorkout,
                distanceMeters = distance,
                burnedKcal = burned,
                durationSeconds = duration
            )
            walkDao.insertWalkSession(session)
            // Add steps to daily total
            stepSensorManager.addManualSteps(stepsInWorkout)
            // Reset active workout state
            _workoutDurationSeconds.value = 0
            locationTracker.resetWorkout()
            refreshDietAudit()
        }
    }

    fun simulateWalkStep() {
        stepSensorManager.addManualSteps(120)
        locationTracker.simulateNextStep()
    }

    // ----------------------------------------------------
    // AI Nutritionist & Diet Coach Operations
    // ----------------------------------------------------
    fun refreshDietAudit() {
        viewModelScope.launch {
            _isAuditLoading.value = true
            val profile = userProfile.value
            val meals = todayMeals.value
            val steps = liveSteps.value
            val burnedKcal = StepSensorManager.calculateBurnedCalories(steps, profile.currentWeightKg)

            val audit = GeminiApiClient.getInstantDietAnalysis(profile, meals, steps, burnedKcal)
            _instantDietAudit.value = audit
            _isAuditLoading.value = false
        }
    }

    fun sendChatMessage(userText: String) {
        if (userText.isBlank()) return
        val userMsg = ChatMessage(sender = MessageSender.USER, text = userText.trim())
        _chatMessages.value = _chatMessages.value + userMsg

        viewModelScope.launch {
            _isChatLoading.value = true
            val profile = userProfile.value
            val consumed = todayMeals.value.sumOf { it.calories }
            val burned = StepSensorManager.calculateBurnedCalories(liveSteps.value, profile.currentWeightKg)

            val history = _chatMessages.value.map { (if (it.sender == MessageSender.USER) "USER" else "MODEL") to it.text }
            val reply = GeminiApiClient.askDietCoach(userText, profile, consumed, burned, history)

            val coachMsg = ChatMessage(sender = MessageSender.AI_COACH, text = reply)
            _chatMessages.value = _chatMessages.value + coachMsg
            _isChatLoading.value = false
        }
    }

    fun fetchNearbyPlaces() {
        viewModelScope.launch {
            val loc = locationTracker.currentLocation.value ?: LatLngPoint(55.7558, 37.6173)
            val spots = GeminiApiClient.getNearbyWalkingSpots(loc.latitude, loc.longitude)
            _nearbyPlaces.value = spots
        }
    }

    // ----------------------------------------------------
    // Profile Updates
    // ----------------------------------------------------
    fun updateProfile(
        currentWeightKg: Float,
        targetWeightKg: Float,
        heightCm: Float,
        age: Int,
        gender: Gender,
        activityLevel: ActivityLevel,
        deficitTargetKcal: Int,
        dailyStepGoal: Int
    ) {
        viewModelScope.launch {
            val updated = userProfile.value.copy(
                currentWeightKg = currentWeightKg,
                targetWeightKg = targetWeightKg,
                heightCm = heightCm,
                age = age,
                gender = gender.name,
                activityLevel = activityLevel.name,
                deficitTargetKcal = deficitTargetKcal,
                dailyStepGoal = dailyStepGoal
            )
            userProfileDao.insertOrUpdateProfile(updated)
            refreshDietAudit()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stepSensorManager.stopListening()
        locationTracker.stopTracking()
        timerJob?.cancel()
    }
}
