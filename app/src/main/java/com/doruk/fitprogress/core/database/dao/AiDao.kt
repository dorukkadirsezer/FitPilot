package com.doruk.fitprogress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.doruk.fitprogress.core.database.entity.AiInteractionLogEntity

@Dao
interface AiDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteractionLog(log: AiInteractionLogEntity)

    @Query("SELECT * FROM ai_interaction_logs ORDER BY createdAt DESC")
    suspend fun getAllInteractionLogs(): List<AiInteractionLogEntity>
    
    @Query("SELECT * FROM ai_interaction_logs WHERE interactionType = :type ORDER BY createdAt DESC")
    suspend fun getInteractionLogsByType(type: String): List<AiInteractionLogEntity>
}
