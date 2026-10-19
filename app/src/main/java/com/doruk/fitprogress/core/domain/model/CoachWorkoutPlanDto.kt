package com.doruk.fitprogress.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CoachWorkoutPlanDto(
    val workoutName: String,
    val dayCode: String? = null,
    val dayFocus: String? = null,
    val exercises: List<CoachExerciseDto>,
    val notes: String? = null
)

@Serializable
data class CoachExerciseDto(
    val id: String, // E.g., ex_squat
    val name: String,
    val targetSets: Int,
    val targetReps: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetLoad: Float? = null,
    val targetRpe: Float? = null,
    val restTimerSeconds: Int? = null,
    val notes: String? = null,

    // For export (actual performance)
    val completedSets: List<CoachSetLogDto>? = null
)

@Serializable
data class CoachSetLogDto(
    val setNumber: Int,
    val targetReps: Int,
    val actualReps: Int? = null,
    val targetWeight: Float? = null,
    val actualWeight: Float? = null,
    val weightUnit: String,
    val rpe: Float? = null,
    val completed: Boolean,
    val isSkipped: Boolean = false,
    val note: String? = null
)
