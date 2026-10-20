package com.doruk.fitprogress.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ProgramPlanDto(
    val programVersion: String,
    val athlete: String,
    val dailyNutritionTarget: NutritionTargetDto,
    val workouts: List<WorkoutDayDto>
)

@Serializable
data class NutritionTargetDto(
    val calories: Int,
    val proteinGrams: Int,
    val fatGrams: Int,
    val carbsGrams: Int,
    val dailyStepsTarget: Int,
    val sleepHoursTarget: Float
)

@Serializable
data class WorkoutDayDto(
    val dayNumber: Int,
    val workoutName: String,
    val exercises: List<ExerciseTargetDto>
)

@Serializable
data class ExerciseTargetDto(
    val id: String,
    val name: String,
    val targetSets: Int,
    val targetReps: Int? = null,
    val targetRepsMin: Int? = null,
    val targetRepsMax: Int? = null,
    val targetLoad: Float? = null,
    val targetRpe: Float? = null,
    val restTimerSeconds: Int? = null,
    val notes: String? = null
)
