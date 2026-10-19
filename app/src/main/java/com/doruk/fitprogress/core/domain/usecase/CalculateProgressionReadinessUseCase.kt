package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ProgressionReadinessScoreEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import java.util.UUID
import javax.inject.Inject

class CalculateProgressionReadinessUseCase @Inject constructor(
    private val workoutDao: WorkoutDao
) {
    suspend operator fun invoke(
        exerciseId: String,
        recentSessions: List<WorkoutExerciseSessionEntity>,
        setLogs: List<SetLogEntity>,
        latestSmartMode: String?, // normal, controlled, light, rest_or_walk
        exerciseDiscomfortContext: Boolean
    ): ProgressionReadinessScoreEntity? {
        if (recentSessions.isEmpty()) return null

        val lastTwoSessions = recentSessions.take(2).reversed() // Chronological order
        var score = 50
        val factors = mutableListOf<String>()

        if (lastTwoSessions.isEmpty()) return null
        
        val latestSessionId = lastTwoSessions.last().id
        val previousSessionId = if (lastTwoSessions.size > 1) lastTwoSessions.first().id else null
        
        val latestSets = setLogs.filter { it.workoutExerciseSessionId == latestSessionId && it.setType == "WORKING" && !it.isSkipped && it.completed }
        val previousSets = previousSessionId?.let { pid -> setLogs.filter { it.workoutExerciseSessionId == pid && it.setType == "WORKING" && !it.isSkipped && it.completed } } ?: emptyList()

        if (latestSets.isEmpty()) return null

        val allTargetRepsCompleted = latestSets.all { (it.actualReps ?: 0) >= it.targetReps }
        if (allTargetRepsCompleted) {
            score += 20
            factors.add("Completed all target reps (+20)")
        }

        // Assuming targetReps is the minimum, and maximum is usually +2 or +5. For simplicity in MVP, we define "top of rep range" as actualReps >= targetReps + 2.
        val topOfRepRangeReached = latestSets.all { (it.actualReps ?: 0) >= it.targetReps + 2 }
        if (topOfRepRangeReached) {
            score += 15
            factors.add("Reached top of rep range (+15)")
        }

        val rpeWithinTarget = latestSets.all { set ->
            val actual = set.rpe ?: return@all false
            actual <= 8.5f
        }
        if (rpeWithinTarget) {
            score += 10
            factors.add("RPE within target range (+10)")
        }

        val rpeExceededTarget = latestSets.any { set ->
            val actual = set.rpe ?: return@any false
            actual > 9.0f // target + 1
        }
        if (rpeExceededTarget) {
            score -= 15
            factors.add("RPE exceeded target by > 1 (-15)")
        }

        if (previousSets.isNotEmpty()) {
            val avgLatestReps = latestSets.map { it.actualReps ?: 0 }.average()
            val avgPrevReps = previousSets.map { it.actualReps ?: 0 }.average()
            if (avgLatestReps >= avgPrevReps) {
                score += 10
                factors.add("Performance stable or improved (+10)")
            } else if (avgLatestReps <= avgPrevReps - 2) {
                score -= 15
                factors.add("Performance dropped significantly (-15)")
            }
        }

        val anySkipped = setLogs.filter { it.workoutExerciseSessionId == latestSessionId && it.setType == "WORKING" }.any { it.isSkipped }
        if (!anySkipped) {
            score += 5
            factors.add("No skipped sets (+5)")
        } else {
            score -= 20
            factors.add("Required working sets skipped (-20)")
        }

        if (latestSmartMode == "light" || latestSmartMode == "rest_or_walk") {
            score -= 20
            factors.add("Light mode session (-20)")
        } else if (latestSmartMode == "controlled") {
            score -= 10
            factors.add("Controlled mode session (-10)")
        }

        if (exerciseDiscomfortContext) {
            score -= 10
            factors.add("Relevant sensitivity context (-10)")
        }

        score = score.coerceIn(0, 100)

        val entity = ProgressionReadinessScoreEntity(
            id = UUID.randomUUID().toString(),
            exerciseId = exerciseId,
            date = System.currentTimeMillis(),
            score = score,
            factors = factors.joinToString(" | ")
        )

        workoutDao.insertProgressionReadinessScore(entity)
        return entity
    }
}
