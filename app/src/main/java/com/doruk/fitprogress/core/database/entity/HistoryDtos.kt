package com.doruk.fitprogress.core.database.entity

import androidx.room.Embedded
import androidx.room.Relation

// ----------------------------------------------------
// Workout History Screen DTOs
// ----------------------------------------------------

data class WorkoutHistoryListItemDto(
    @Embedded val session: WorkoutSessionEntity,
    
    val dayCode: String?,
    val workoutName: String?,
    
    val skippedSetCount: Int,
    val completedSetCount: Int,
    val hasNotes: Boolean,
    
    val smartCoachingMode: String?,
    val smartCoachingOverridden: Boolean?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutSessionId"
    )
    val cardioSession: CardioSessionEntity?
)

// ----------------------------------------------------
// Workout Detail Screen DTOs
// ----------------------------------------------------

data class SetLogWithDetailsDto(
    @Embedded val setLog: SetLogEntity,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "setLogId"
    )
    val restTimerLog: RestTimerLogEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "setLogId"
    )
    val setAdvice: SetAdviceEntity?
)

data class WorkoutDetailExerciseDto(
    @Embedded val exerciseSession: WorkoutExerciseSessionEntity,
    
    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "id"
    )
    val exercise: ExerciseEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseSessionId",
        entity = SetLogEntity::class
    )
    val sets: List<SetLogWithDetailsDto>
)

data class WorkoutDetailDto(
    @Embedded val session: WorkoutSessionEntity,
    
    val dayCode: String?,
    val workoutName: String?,
    val smartCoachingMode: String?,
    val smartCoachingOverridden: Boolean?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutSessionId"
    )
    val cardioSession: CardioSessionEntity?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutSessionId",
        entity = WorkoutExerciseSessionEntity::class
    )
    val exercises: List<WorkoutDetailExerciseDto>
)

// ----------------------------------------------------
// Exercise History Screen DTOs
// ----------------------------------------------------

data class ExerciseHistorySessionDto(
    @Embedded val exerciseSession: WorkoutExerciseSessionEntity,
    
    val workoutStartedAt: Long,
    val workoutFinishedAt: Long?,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseSessionId",
        entity = SetLogEntity::class
    )
    val sets: List<SetLogWithDetailsDto>
)
