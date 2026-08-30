package com.example.data.model

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class MessageSender {
    USER,
    AI_COACH
}

data class MapsPlace(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val category: String, // "Парк", "Маршрут", "Эко-кафе", "Спортплощадка"
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Float = 0.5f,
    val rating: Float = 4.8f,
    val description: String = "",
    val address: String = ""
)
