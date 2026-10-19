package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.ProgressionSuggestionEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import java.util.UUID
import javax.inject.Inject

class CalculateProgressionUseCase @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    suspend operator fun invoke(
        exercise: ExerciseEntity,
        recentSessions: List<WorkoutExerciseSessionEntity>,
        setLogs: List<SetLogEntity>,
        latestSmartMode: String?
    ): ProgressionSuggestionEntity {
        val exerciseId = exercise.id
        val latestSessionId = recentSessions.maxByOrNull { it.id }?.id // In real usage, the most recent session ID
        
        if (recentSessions.isEmpty()) {
            return saveAndReturn(exerciseId, null, null, exercise.defaultUnit, null, exercise.defaultUnit, "INSUFFICIENT_DATA", "Not enough data.", "DETERMINISTIC")
        }

        if (latestSmartMode == "light") {
            return saveAndReturn(exerciseId, latestSessionId, null, exercise.defaultUnit, null, exercise.defaultUnit, "SUPPRESSED_BY_LIGHT_MODE", "Progression suppressed because the last session was Light Mode.", "DETERMINISTIC")
        }

        if (latestSmartMode == "controlled") {
            return saveAndReturn(exerciseId, latestSessionId, null, exercise.defaultUnit, null, exercise.defaultUnit, "SUPPRESSED_BY_CONTROLLED_MODE", "Progression suppressed to prioritize form in Controlled Mode. Repeat weight.", "DETERMINISTIC")
        }

        val workingSets = setLogs.filter { it.setType == "WORKING" && !it.isSkipped && it.completed }
        if (workingSets.isEmpty()) {
            return saveAndReturn(exerciseId, latestSessionId, null, exercise.defaultUnit, null, exercise.defaultUnit, "INSUFFICIENT_DATA", "No valid working sets found.", "DETERMINISTIC")
        }

        val lastTwoSessions = recentSessions.take(2).reversed()
        if (lastTwoSessions.size < 2) {
            val sessionToUse = lastTwoSessions.lastOrNull()?.id ?: latestSessionId
            val latestSets = workingSets.filter { it.workoutExerciseSessionId == sessionToUse }
            val currentWeight = latestSets.firstOrNull()?.actualWeight
            val currentUnit = latestSets.firstOrNull()?.weightUnit ?: exercise.defaultUnit
            return saveAndReturn(exerciseId, sessionToUse, currentWeight, currentUnit, currentWeight, currentUnit, "INSUFFICIENT_DATA", "Only one session logged. Keep building data.", "DETERMINISTIC")
        }

        val previousSessionId = lastTwoSessions.first().id
        val latestSessionIdToUse = lastTwoSessions.last().id

        val prevSets = workingSets.filter { it.workoutExerciseSessionId == previousSessionId }
        val latestSets = workingSets.filter { it.workoutExerciseSessionId == latestSessionIdToUse }

        if (prevSets.isEmpty() || latestSets.isEmpty()) {
            return saveAndReturn(exerciseId, latestSessionIdToUse, null, exercise.defaultUnit, null, exercise.defaultUnit, "INSUFFICIENT_DATA", "Missing working set data in recent sessions.", "DETERMINISTIC")
        }

        val prevWeight = prevSets.first().actualWeight
        val prevUnit = prevSets.first().weightUnit
        val latestWeight = latestSets.first().actualWeight
        val latestUnit = latestSets.first().weightUnit

        if (latestWeight == null) {
            return saveAndReturn(exerciseId, latestSessionIdToUse, null, latestUnit, null, latestUnit, "INSUFFICIENT_DATA", "Target weight was unspecified. Log a real weight first.", "DETERMINISTIC")
        }

        // Determine native increment
        val increment = when {
            latestUnit == "lbs" && (exercise.equipment == "barbell" || exercise.equipment == "smith machine" || exercise.equipment == "cable" || exercise.equipment == "machine") -> 5f
            latestUnit == "kg" && exercise.equipment == "dumbbells" -> 2f // Usually +2kg (1kg per db) or 2.5kg
            latestUnit == "kg" && (exercise.equipment == "machine" || exercise.equipment == "cable") -> 2.5f
            else -> if (latestUnit == "lbs") 5f else 2.5f
        }

        // Evaluate latest session
        val latestAllTargetReps = latestSets.all { (it.actualReps ?: 0) >= it.targetReps }
        val latestTopReps = latestSets.all { (it.actualReps ?: 0) >= it.targetReps + 2 } // proxy for top of range
        val latestRpeHigh = latestSets.any { (it.rpe ?: 0f) > 9.0f } // High RPE prevents increase
        val latestRpeTooHigh = latestSets.any { (it.rpe ?: 0f) > 9.5f }

        // Evaluate previous session
        val prevAllTargetReps = prevSets.all { (it.actualReps ?: 0) >= it.targetReps }
        val prevTopReps = prevSets.all { (it.actualReps ?: 0) >= it.targetReps + 2 }
        val prevRpeHigh = prevSets.any { (it.rpe ?: 0f) > 9.0f }

        val avgLatestReps = latestSets.map { it.actualReps ?: 0 }.average()
        val avgPrevReps = prevSets.map { it.actualReps ?: 0 }.average()

        if (prevWeight != latestWeight || prevUnit != latestUnit) {
            // Different weights across sessions
            if (latestTopReps && !latestRpeHigh && prevTopReps) {
                // Latest session was a breeze, prev was also good. We could increase, but user already changed weights.
                // It's safer to repeat and confirm if they just increased.
                return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "REPEAT_WEIGHT", "You recently changed weights. Repeat $latestWeight $latestUnit to solidify performance.", "DETERMINISTIC")
            }
            if (avgLatestReps < avgPrevReps - 2 || latestRpeTooHigh) {
                return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "REDUCE_LOAD", "Performance dropped. Consider reducing slightly.", "DETERMINISTIC")
            }
            return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "REPEAT_WEIGHT", "Repeat weight. Build consistency at $latestWeight $latestUnit.", "DETERMINISTIC")
        }

        // Same weight in both sessions (Double Progression core)
        if (latestTopReps && prevTopReps && !latestRpeHigh && !prevRpeHigh) {
            val nextWeight = latestWeight + increment
            return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, nextWeight, latestUnit, "INCREASE_WEIGHT", "Next time, try $nextWeight $latestUnit.", "DETERMINISTIC")
        }

        if (latestRpeTooHigh || (avgLatestReps <= avgPrevReps - 2 && !latestAllTargetReps)) {
            return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "REDUCE_LOAD", "Reduce slightly or focus on recovery.", "DETERMINISTIC")
        }

        if (!latestTopReps) {
            return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "BUILD_REPS", "Keep the same load; focus on building reps.", "DETERMINISTIC")
        }

        return saveAndReturn(exerciseId, latestSessionIdToUse, latestWeight, latestUnit, latestWeight, latestUnit, "REPEAT_WEIGHT", "Repeat $latestWeight $latestUnit once more.", "DETERMINISTIC")
    }

    private suspend fun saveAndReturn(
        exerciseId: String,
        workoutExerciseSessionId: String?,
        currentWeight: Float?,
        currentUnit: String,
        suggestedWeight: Float?,
        suggestedUnit: String,
        type: String,
        reason: String,
        source: String
    ): ProgressionSuggestionEntity {
        // Prevent duplicates
        if (workoutExerciseSessionId != null) {
            val existing = workoutDao.getProgressionSuggestionForExerciseSession(workoutExerciseSessionId)
            if (existing != null) return existing
        }

        val entity = ProgressionSuggestionEntity(
            id = UUID.randomUUID().toString(),
            exerciseId = exerciseId,
            workoutExerciseSessionId = workoutExerciseSessionId,
            currentWeight = currentWeight,
            currentUnit = currentUnit,
            suggestedWeight = suggestedWeight,
            suggestedUnit = suggestedUnit,
            suggestionType = type,
            reason = reason,
            source = source,
            createdAt = System.currentTimeMillis(),
            accepted = false
        )
        workoutDao.insertProgressionSuggestion(entity)
        return entity
    }
}
