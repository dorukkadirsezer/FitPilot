package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "workout_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("templateId")]
)
data class WorkoutSessionEntity(
    @PrimaryKey val id: String,
    val templateId: String?,
    val startedAt: Long,
    val finishedAt: Long?,
    val durationSeconds: Long?,
    val totalWorkingVolumeKg: Float?,
    val averageRpe: Float?,
    val cardioCompleted: Boolean,
    val note: String? = null
)

@Entity(
    tableName = "workout_exercise_sessions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutSessionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutSessionId"), Index("exerciseId")]
)
data class WorkoutExerciseSessionEntity(
    @PrimaryKey val id: String,
    val workoutSessionId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val supersetGroupId: String?,
    val note: String? = null
)

@Entity(
    tableName = "set_logs",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutExerciseSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutExerciseSessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutExerciseSessionId")]
)
data class SetLogEntity(
    @PrimaryKey val id: String,
    val workoutExerciseSessionId: String,
    val setNumber: Int,
    val setType: String, // "WARMUP" or "WORKING"
    val targetWeight: Float?,
    val actualWeight: Float?,
    val weightUnit: String,
    val targetReps: Int,
    val actualReps: Int?,
    val rpe: Float?,
    val completed: Boolean,
    val isSkipped: Boolean = false,
    val note: String? = null
)

@Entity(
    tableName = "rest_timer_logs",
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
data class RestTimerLogEntity(
    @PrimaryKey val id: String,
    val setLogId: String?, // Nullable if attached to a superset group instead of a specific set
    val supersetGroupId: String?,
    val roundNumber: Int?,
    val plannedRestSeconds: Int,
    val actualRestSeconds: Int,
    val skipped: Boolean
)
