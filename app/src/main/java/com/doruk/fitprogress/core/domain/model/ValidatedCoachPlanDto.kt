package com.doruk.fitprogress.core.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ValidatedCoachPlanDto(
    @SerialName("programVersion")
    @JsonNames("program_version", "programVersion", "version")
    val programVersion: String = "2.2-gemini",

    @SerialName("athlete")
    @JsonNames("athlete", "athlete_name", "athleteName")
    val athlete: String = "FitPilot Athlete",

    @SerialName("coachRationale")
    @JsonNames("coach_rationale", "coachRationale", "rationale", "summary")
    val coachRationale: String? = null,

    @SerialName("dailyNutritionTarget")
    @JsonNames("daily_nutrition_target", "dailyNutritionTarget", "nutritionTarget")
    val dailyNutritionTarget: ValidatedNutritionTargetDto = ValidatedNutritionTargetDto(),

    @SerialName("workouts")
    @JsonNames("workouts", "workout_days", "workoutDays")
    val workouts: List<ValidatedWorkoutDayDto> = emptyList()
) {
    /**
     * Validates structural and physiological constraints before modifying Room DB.
     * Throws IllegalArgumentException if schema constraints are violated.
     */
    fun validate(): ValidatedCoachPlanDto {
        require(workouts.isNotEmpty()) {
            "Returned macrocycle contains 0 workout days."
        }
        workouts.forEachIndexed { dayIdx, day ->
            require(day.workoutName.isNotBlank()) {
                "Workout day #${dayIdx + 1} has a blank workoutName."
            }
            require(day.exercises.isNotEmpty()) {
                "Workout '${day.workoutName}' contains 0 exercises."
            }
            day.exercises.forEach { ex ->
                require(ex.id.isNotBlank() && ex.name.isNotBlank()) {
                    "Exercise in '${day.workoutName}' is missing id or name."
                }
                require(ex.targetSets in 1..20) {
                    "Exercise '${ex.name}' has invalid targetSets (${ex.targetSets}). Must be 1..20."
                }
                val repsMin = ex.targetReps ?: ex.targetRepsMin ?: 8
                require(repsMin in 1..100) {
                    "Exercise '${ex.name}' has invalid targetRepsMin ($repsMin)."
                }
            }
        }
        return this
    }

    fun toProgramPlanDto(): ProgramPlanDto {
        return ProgramPlanDto(
            programVersion = programVersion,
            athlete = athlete,
            dailyNutritionTarget = NutritionTargetDto(
                calories = dailyNutritionTarget.calories.coerceIn(1000, 8000),
                proteinGrams = dailyNutritionTarget.proteinGrams.coerceIn(40, 450),
                fatGrams = dailyNutritionTarget.fatGrams.coerceIn(20, 300),
                carbsGrams = dailyNutritionTarget.carbsGrams.coerceIn(20, 1000),
                dailyStepsTarget = dailyNutritionTarget.dailyStepsTarget.coerceIn(1000, 40000),
                sleepHoursTarget = dailyNutritionTarget.sleepHoursTarget.coerceIn(4.0f, 12.0f)
            ),
            workouts = workouts.mapIndexed { index, day ->
                WorkoutDayDto(
                    dayNumber = if (day.dayNumber > 0) day.dayNumber else (index + 1),
                    workoutName = day.workoutName.trim(),
                    exercises = day.exercises.map { ex ->
                        val minReps = (ex.targetRepsMin ?: ex.targetReps ?: 8).coerceIn(1, 100)
                        val maxReps = (ex.targetRepsMax ?: ex.targetReps ?: minReps).coerceAtLeast(minReps)
                        ExerciseTargetDto(
                            id = ex.id.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_').ifBlank {
                                ex.name.trim().lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
                            },
                            name = ex.name.trim(),
                            targetSets = ex.targetSets.coerceIn(1, 15),
                            targetReps = ex.targetReps,
                            targetRepsMin = minReps,
                            targetRepsMax = maxReps,
                            targetLoad = ex.targetLoad?.takeIf { it >= 0f },
                            targetRpe = ex.targetRpe?.coerceIn(5.0f, 10.0f),
                            restTimerSeconds = (ex.restTimerSeconds ?: 90).coerceIn(15, 600),
                            notes = ex.notes
                        )
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ValidatedNutritionTargetDto(
    @SerialName("calories")
    @JsonNames("calories", "daily_calories")
    val calories: Int = 2500,

    @SerialName("proteinGrams")
    @JsonNames("protein_grams", "proteinGrams", "protein")
    val proteinGrams: Int = 160,

    @SerialName("fatGrams")
    @JsonNames("fat_grams", "fatGrams", "fat")
    val fatGrams: Int = 75,

    @SerialName("carbsGrams")
    @JsonNames("carbs_grams", "carbsGrams", "carbs")
    val carbsGrams: Int = 260,

    @SerialName("dailyStepsTarget")
    @JsonNames("daily_steps_target", "dailyStepsTarget", "steps")
    val dailyStepsTarget: Int = 8500,

    @SerialName("sleepHoursTarget")
    @JsonNames("sleep_hours_target", "sleepHoursTarget", "sleep")
    val sleepHoursTarget: Float = 8.0f
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ValidatedWorkoutDayDto(
    @SerialName("dayNumber")
    @JsonNames("day_number", "dayNumber", "day")
    val dayNumber: Int = 1,

    @SerialName("workoutName")
    @JsonNames("workout_name", "workoutName", "name", "title")
    val workoutName: String,

    @SerialName("exercises")
    @JsonNames("exercises", "movements")
    val exercises: List<ValidatedExerciseTargetDto> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ValidatedExerciseTargetDto(
    @SerialName("id")
    @JsonNames("id", "exercise_id", "exerciseId")
    val id: String,

    @SerialName("name")
    @JsonNames("name", "exercise_name", "exerciseName")
    val name: String,

    @SerialName("targetSets")
    @JsonNames("target_sets", "targetSets", "sets")
    val targetSets: Int = 3,

    @SerialName("targetReps")
    @JsonNames("target_reps", "targetReps", "reps")
    val targetReps: Int? = null,

    @SerialName("targetRepsMin")
    @JsonNames("target_reps_min", "targetRepsMin", "repsMin")
    val targetRepsMin: Int? = null,

    @SerialName("targetRepsMax")
    @JsonNames("target_reps_max", "targetRepsMax", "repsMax")
    val targetRepsMax: Int? = null,

    @SerialName("targetLoad")
    @JsonNames("target_load", "targetLoad", "weight", "loadKg")
    val targetLoad: Float? = null,

    @SerialName("targetRpe")
    @JsonNames("target_rpe", "targetRpe", "rpe")
    val targetRpe: Float? = null,

    @SerialName("restTimerSeconds")
    @JsonNames("rest_timer_seconds", "restTimerSeconds", "restSeconds")
    val restTimerSeconds: Int? = 90,

    @SerialName("notes")
    @JsonNames("notes", "coach_note", "coachNote")
    val notes: String? = null
)

// ---------------------------------------------------------------------------
// Telemetry Payload DTOs exported by ExportUserProgressionStatsUseCase
// ---------------------------------------------------------------------------

@Serializable
data class UserTelemetryPayloadDto(
    val exportedAtIso: String,
    val athleteName: String,
    val currentNutritionTargets: ValidatedNutritionTargetDto,
    val adherenceMetrics: AdherenceMetricsDto,
    val currentProgramOverview: List<CurrentTemplateSummaryDto>,
    val recentCompletedSessions: List<RecentSessionTelemetryDto>,
    val catalogExercises: List<CatalogExerciseRefDto>
)

@Serializable
data class AdherenceMetricsDto(
    val totalCompletedWorkouts: Int,
    val workoutsLast14Days: Int,
    val workoutsLast30Days: Int,
    val overallSetCompletionRate: Float,
    val averageSessionRpe: Float?,
    val averageSessionVolumeKg: Float
)

@Serializable
data class CurrentTemplateSummaryDto(
    val dayCode: String,
    val workoutName: String,
    val isAiProgram: Boolean,
    val exercises: List<ValidatedExerciseTargetDto>
)

@Serializable
data class RecentSessionTelemetryDto(
    val sessionId: String,
    val dateIso: String,
    val workoutName: String,
    val durationMinutes: Int,
    val totalWorkingVolumeKg: Float,
    val averageRpe: Float?,
    val completedWorkingSets: Int,
    val skippedSets: Int,
    val exerciseSnapshots: List<ExercisePerformanceSnapshotDto>
)

@Serializable
data class ExercisePerformanceSnapshotDto(
    val exerciseId: String,
    val exerciseName: String,
    val workingSetsCompleted: Int,
    val bestSetWeightKg: Float?,
    val bestSetReps: Int?,
    val averageRpe: Float?
)

@Serializable
data class CatalogExerciseRefDto(
    val id: String,
    val name: String,
    val muscleGroup: String,
    val equipment: String
)
