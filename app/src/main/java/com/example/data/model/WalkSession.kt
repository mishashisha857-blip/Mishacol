package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "walk_sessions")
data class WalkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // e.g. "2026-08-30"
    val steps: Int,
    val distanceMeters: Float,
    val burnedKcal: Int,
    val durationSeconds: Long,
    val pathPointsJson: String = "[]",
    val timestamp: Long = System.currentTimeMillis()
)
