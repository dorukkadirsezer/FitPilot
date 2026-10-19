package com.doruk.fitprogress.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TemplateExerciseWithDetails(
    @Embedded val templateExercise: WorkoutTemplateExerciseEntity,
    
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutTemplateExerciseId"
    )
    val warmupSets: List<TemplateWarmupSetEntity>
)

data class TemplateWithDetails(
    @Embedded val template: WorkoutTemplateEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val warmupInstructions: List<TemplateWarmupInstructionEntity>,
    
    @Relation(
        entity = WorkoutTemplateExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "templateId"
    )
    val exercises: List<TemplateExerciseWithDetails>
)
