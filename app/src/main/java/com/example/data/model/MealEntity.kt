package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MealType(val titleRu: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mealType: String = MealType.LUNCH.name,
    val dishName: String,
    val calories: Int,
    val proteinGrams: Float,
    val fatGrams: Float,
    val carbsGrams: Float,
    val fiberGrams: Float = 0f,
    val portionGrams: Int = 300,
    val healthRating: Int = 8, // 1 to 10
    val advice: String = "",
    val ingredients: String = "",
    val imageUri: String? = null
)
