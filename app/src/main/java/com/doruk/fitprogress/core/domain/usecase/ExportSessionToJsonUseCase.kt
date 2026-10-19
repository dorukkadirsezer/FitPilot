package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.domain.model.CoachExerciseDto
import com.doruk.fitprogress.core.domain.model.CoachSetLogDto
import com.doruk.fitprogress.core.domain.model.CoachWorkoutPlanDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class ExportSessionToJsonUseCase @Inject constructor(
    private val dao: WorkoutDao,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        val resultPair: Pair<Result<Unit>, String?> = withContext(Dispatchers.IO) {
            try {
                val detail = dao.getWorkoutDetail(sessionId) ?: return@withContext Pair(Result.failure(Exception("Session not found")), null)

                val exercises = detail.exercises.map { exDto ->
                val workingSets = exDto.sets.filter { it.setLog.setType == "WORKING" }
                val completedSetDtos = workingSets.map { setLogWithDetails ->
                    val log = setLogWithDetails.setLog
                    CoachSetLogDto(
                        setNumber = log.setNumber,
                        targetReps = log.targetReps,
                        actualReps = log.actualReps,
                        targetWeight = log.targetWeight,
                        actualWeight = log.actualWeight,
                        weightUnit = log.weightUnit,
                        rpe = log.rpe,
                        completed = log.completed,
                        isSkipped = log.isSkipped,
                        note = log.note
                    )
                }

                CoachExerciseDto(
                    id = exDto.exerciseSession.exerciseId,
                    name = exDto.exercise?.name ?: "Unknown",
                    targetSets = workingSets.size,
                    restTimerSeconds = exDto.exerciseSession.restTimerSeconds ?: exDto.exercise?.defaultRestSeconds,
                    notes = exDto.exerciseSession.note,
                    completedSets = completedSetDtos
                )
            }

                val plan = CoachWorkoutPlanDto(
                    workoutName = detail.workoutName ?: "Exported Workout",
                    dayCode = detail.dayCode,
                    dayFocus = detail.session.note, // Or anything else
                    exercises = exercises,
                    notes = detail.session.note
                )

                val format = Json { prettyPrint = true }
                val jsonStr = format.encodeToString(plan)
                Pair(Result.success(Unit), jsonStr)
            } catch (e: Exception) {
                Pair(Result.failure(e), null)
            }
        }

        val res = resultPair.first
        val jsonStr = resultPair.second

        if (res.isSuccess && jsonStr != null) {
            withContext(Dispatchers.Main) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Coach Plan JSON", jsonStr)
                clipboard.setPrimaryClip(clip)
            }
        }

        return res
    }
}
