package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val trainingGoal: String,
    val defaultUnit: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String,
    val movementType: String,
    val defaultRestSeconds: Int,
    val defaultUnit: String,
    val isCompound: Boolean,
    val requiresStraps: Boolean,
    val coachNote: String?,
    val createdAt: Long,
    val updatedAt: Long
)
