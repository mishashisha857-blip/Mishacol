package com.example.data.model

data class FoodAnalysisResult(
    val dishName: String,
    val calories: Int,
    val proteinGrams: Float,
    val fatGrams: Float,
    val carbsGrams: Float,
    val fiberGrams: Float = 0f,
    val portionGrams: Int = 300,
    val healthScore: Int = 8, // 1-10
    val ingredients: List<String> = emptyList(),
    val weightLossAdvice: String = "",
    val glycemicIndex: String = "Средний",
    val pros: List<String> = emptyList(),
    val cons: List<String> = emptyList()
)
