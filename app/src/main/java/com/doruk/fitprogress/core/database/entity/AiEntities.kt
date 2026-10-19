package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_interaction_logs")
data class AiInteractionLogEntity(
    @PrimaryKey val id: String,
    val interactionType: String, // E.g., FOOD_PARSER, WORKOUT_SUMMARY, PROGRESSION_EXPLANATION, WEEKLY_REVIEW
    val promptSummary: String, // Short summary, NOT the full raw prompt
    val response: String,
    val createdAt: Long,
    val savedByUser: Boolean
)
