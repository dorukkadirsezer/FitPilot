package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "body_metrics",
    indices = [
        androidx.room.Index(value = ["dateIso"], unique = true)
    ]
)
data class BodyMetricEntity(
    @PrimaryKey val id: String,
    val dateIso: String,
    val bodyWeight: Float?,
    val weightUnit: String,
    val waist: Float?,
    val chest: Float?,
    val arm: Float?,
    val leg: Float?,
    val bodyFatPercentage: Float?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "cardio_sessions")
data class CardioSessionEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String?,
    val date: Long,
    val cardioType: String, // INCLINE_WALK, ELLIPTICAL, STATIONARY_BIKE, WALK
    val durationMinutes: Int,
    val perceivedExertion: Int?,
    val notes: String?
)
