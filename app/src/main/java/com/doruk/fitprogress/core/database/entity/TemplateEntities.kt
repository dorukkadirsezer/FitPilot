package com.doruk.fitprogress.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(tableName = "workout_templates")
data class WorkoutTemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val dayCode: String,
    val dayFocus: String,
    val defaultCardioType: String?,
    val defaultCardioMinutes: Int?
)

@Entity(
    tableName = "workout_template_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId"), Index("exerciseId")]
)
data class WorkoutTemplateExerciseEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val exerciseId: String,
    val orderIndex: Int,
    val isSuperset: Boolean,
    val supersetGroupId: String?,
    val supersetRoundCount: Int?,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightMin: Float?,
    val targetWeightMax: Float?,
    val weightUnit: String,
    val restSeconds: Int,
    val rpeMin: Float?,
    val rpeMax: Float?
)

@Entity(
    tableName = "template_warmup_instructions",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateEntity::class,
            parentColumns = ["id"],
            childColumns = ["templateId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("templateId")]
)
data class TemplateWarmupInstructionEntity(
    @PrimaryKey val id: String,
    val templateId: String,
    val description: String,
    val orderIndex: Int,
    val durationMinutes: Int?,
    val isRequired: Boolean
)

@Entity(
    tableName = "template_warmup_sets",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutTemplateExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutTemplateExerciseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("workoutTemplateExerciseId")]
)
data class TemplateWarmupSetEntity(
    @PrimaryKey val id: String,
    val workoutTemplateExerciseId: String,
    val setNumber: Int,
    val targetReps: Int,
    val targetWeightValue: Float?,
    val targetWeightUnit: String?,
    val targetWeightLabel: String?,
    val restSeconds: Int
)
