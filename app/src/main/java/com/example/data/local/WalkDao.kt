package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WalkSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WalkDao {
    @Query("SELECT * FROM walk_sessions ORDER BY timestamp DESC")
    fun getAllWalkSessions(): Flow<List<WalkSessionEntity>>

    @Query("SELECT * FROM walk_sessions WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getWalkSessionsForDate(dateString: String): Flow<List<WalkSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWalkSession(session: WalkSessionEntity): Long

    @Query("DELETE FROM walk_sessions WHERE id = :id")
    suspend fun deleteWalkSessionById(id: Long)
}
