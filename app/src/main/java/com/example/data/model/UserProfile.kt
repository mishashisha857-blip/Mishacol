package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Gender(val titleRu: String) {
    MALE("Мужской"),
    FEMALE("Женский")
}

enum class ActivityLevel(val titleRu: String, val multiplier: Float) {
    SEDENTARY("Сидячий образ жизни", 1.2f),
    LIGHT("Легкая активность (1-3 тренировки/нед)", 1.375f),
    MODERATE("Умеренная активность (3-5 тренировок/нед)", 1.55f),
    VERY_ACTIVE("Высокая активность (ежедневно)", 1.725f)
}

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val currentWeightKg: Float = 78.0f,
    val targetWeightKg: Float = 70.0f,
    val heightCm: Float = 175.0f,
    val age: Int = 28,
    val gender: String = Gender.MALE.name,
    val activityLevel: String = ActivityLevel.LIGHT.name,
    val deficitTargetKcal: Int = 400, // Deficit for steady weight loss
    val dailyStepGoal: Int = 10000,
    val waterGoalMl: Int = 2200
) {
    // Mifflin - St Jeor Formula for BMR
    val bmr: Float
        get() {
            return if (gender == Gender.MALE.name) {
                10f * currentWeightKg + 6.25f * heightCm - 5f * age + 5f
            } else {
                10f * currentWeightKg + 6.25f * heightCm - 5f * age - 161f
            }
        }

    val tdee: Float
        get() {
            val level = try {
                ActivityLevel.valueOf(activityLevel)
            } catch (e: Exception) {
                ActivityLevel.LIGHT
            }
            return bmr * level.multiplier
        }

    // Recommended daily calories to reach weight loss target
    val targetDailyCalories: Int
        get() = (tdee - deficitTargetKcal).toInt().coerceAtLeast(1200)

    // Recommended daily macros (Protein ~30%, Fat ~30%, Carbs ~40% for balanced weight loss)
    val targetProteinGrams: Float
        get() = (targetDailyCalories * 0.30f) / 4f // 4 kcal per gram of protein

    val targetFatGrams: Float
        get() = (targetDailyCalories * 0.30f) / 9f // 9 kcal per gram of fat

    val targetCarbsGrams: Float
        get() = (targetDailyCalories * 0.40f) / 4f // 4 kcal per gram of carbs
}
