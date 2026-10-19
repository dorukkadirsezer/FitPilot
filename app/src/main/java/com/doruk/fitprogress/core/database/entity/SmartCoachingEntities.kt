package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "readiness_check_ins")
data class ReadinessCheckInEntity(
    @PrimaryKey val id: String,
    val date: Long,
    val plannedDayCode: String?, // A/B/C/D
    val sleepQuality: Int, // 1-5
    val muscleSoreness: Int, // 1-5
    val energyLevel: Int, // 1-5
    val motivationLevel: Int, // 1-5
    val kneeSensitivity: String, // none, mild, moderate, high
    val backDiscomfort: String, // none, mild, moderate, high
    val previousWorkoutFeltHard: Boolean,
    val availableTimeMode: String, // full, 60, 45, 30
    val notes: String?
)

@Entity(
    tableName = "smart_day_recommendations",
    foreignKeys = [
        ForeignKey(
            entity = ReadinessCheckInEntity::class,
            parentColumns = ["id"],
            childColumns = ["checkInId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["suggestedWorkoutTemplateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("checkInId"), Index("suggestedWorkoutTemplateId")]
)
data class SmartDayRecommendationEntity(
    @PrimaryKey val id: String,
    val checkInId: String,
    val suggestedWorkoutTemplateId: String?,
    val recommendedMode: String, // normal, controlled, light, rest_or_walk
    val selectedMode: String?, // what the user actually chose
    val loadAdjustmentPercent: Int,
    val volumeAdjustment: Int,
    val cardioRecommendation: String?,
    val reason: String?,
    val accepted: Boolean,
    val overriddenByUser: Boolean
)

@Entity(
    tableName = "time_limited_workout_plans",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["originalTemplateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("originalTemplateId")]
)
data class TimeLimitedWorkoutPlanEntity(
    @PrimaryKey val id: String,
    val originalTemplateId: String,
    val maxDurationMinutes: Int
)

@Entity(
    tableName = "time_limited_workout_plan_exercises",
    foreignKeys = [
        ForeignKey(
            entity = TimeLimitedWorkoutPlanEntity::class,
            parentColumns = ["id"],
            childColumns = ["planId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("planId"), Index("exerciseId")]
)
data class TimeLimitedWorkoutPlanExerciseEntity(
    @PrimaryKey val id: String,
    val planId: String,
    val exerciseId: String,
    val status: String, // included, skipped, optional
    val reason: String?,
    val orderIndex: Int
)

@Entity(
    tableName = "set_advices",
    foreignKeys = [
        ForeignKey(
            entity = SetLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["setLogId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("setLogId")]
)
data class SetAdviceEntity(
    @PrimaryKey val id: String,
    val setLogId: String?,
    val supersetGroupId: String?,
    val roundNumber: Int?,
    val adviceText: String,
    val type: String, // decrease_weight, maintain, form_check
    val shownToUser: Boolean
)

@Entity(
    tableName = "progression_readiness_scores",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("exerciseId")]
)
data class ProgressionReadinessScoreEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val date: Long,
    val score: Int, // 0-100
    val factors: String
)

@Entity(
    tableName = "adaptive_cardio_recommendations",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutSessionId")]
)
data class AdaptiveCardioRecommendationEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val recommendedCardio: String?,
    val originalCardio: String?,
    val reason: String?,
    val accepted: Boolean
)

@Entity(
    tableName = "progression_suggestions",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WorkoutExerciseSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseSessionId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("exerciseId"), Index(value = ["workoutExerciseSessionId"], unique = true)]
)
data class ProgressionSuggestionEntity(
    @PrimaryKey val id: String,
    val exerciseId: String,
    val workoutExerciseSessionId: String?,
    val currentWeight: Float?,
    val currentUnit: String,
    val suggestedWeight: Float?,
    val suggestedUnit: String,
    val suggestionType: String, // INCREASE_WEIGHT, REPEAT_WEIGHT, BUILD_REPS, REDUCE_LOAD, INSUFFICIENT_DATA, SUPPRESSED_BY_CONTROLLED_MODE, SUPPRESSED_BY_LIGHT_MODE
    val reason: String,
    val source: String, // DETERMINISTIC, GEMINI
    val createdAt: Long,
    val accepted: Boolean
)
