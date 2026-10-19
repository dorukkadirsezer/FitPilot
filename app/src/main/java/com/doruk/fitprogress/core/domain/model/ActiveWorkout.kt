package com.doruk.fitprogress.core.domain.model

data class ActiveWorkoutSession(
    val sessionId: String,
    val templateId: String,
    val name: String,
    val dayFocus: String,
    val startedAt: Long,
    val warmups: List<ActiveWarmupInstruction>,
    val exercises: List<ActiveExercise>,
    val note: String? = null
)

data class ActiveWarmupInstruction(
    val id: String,
    val description: String,
    val isRequired: Boolean,
    val durationMinutes: Int?
)

data class ActiveExercise(
    val exerciseSessionId: String,
    val templateExerciseId: String,
    val exerciseId: String,
    val name: String,
    val muscleGroup: String,
    val coachNote: String?,
    val programRiskReminders: List<String>,
    val requiresStraps: Boolean,
    val orderIndex: Int,
    val isSuperset: Boolean,
    val supersetGroupId: String?,
    val supersetRoundCount: Int?,
    val supersetAdviceText: String?,
    val defaultRestSeconds: Int,
    val sets: List<ActiveSet>,
    val note: String? = null
)

data class ActiveSet(
    val id: String,
    val setNumber: Int,
    val isWarmup: Boolean,
    val warmupLabel: String?,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetWeightMin: Float?,
    val targetWeightMax: Float?,
    val targetRpeMin: Float?,
    val targetRpeMax: Float?,
    val unit: String,
    
    // User-editable state
    val loggedReps: Int?,
    val loggedWeight: Float?,
    val loggedRpe: Float?,
    val isCompleted: Boolean,
    val isSkipped: Boolean,

    // Smart Coaching
    val adviceText: String?,
    val adviceType: String?,
    val note: String? = null
)
