package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.domain.model.CoachWorkoutPlanDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class SyncCoachPlanUseCase @Inject constructor(
    private val dao: WorkoutDao,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Result<String> {
        val jsonStr = withContext(Dispatchers.Main) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.primaryClip?.getItemAt(0)?.text?.toString()
        }

        if (jsonStr.isNullOrBlank()) {
            return Result.failure(Exception("Clipboard is empty"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val format = Json { ignoreUnknownKeys = true }
            val plan: CoachWorkoutPlanDto = format.decodeFromString(jsonStr)

            // Ensure "AI Coach" template exists
            val templates = dao.getAllTemplatesSync()
            var template = templates.find { it.dayCode == "AI_COACH" }
            if (template == null) {
                template = WorkoutTemplateEntity(
                    id = UUID.randomUUID().toString(),
                    name = "AI Coach Plan",
                    dayCode = "AI_COACH",
                    dayFocus = "Custom",
                    defaultCardioType = null,
                    defaultCardioMinutes = null
                )
                dao.insertTemplates(listOf(template))
            }

            // Ensure all exercises exist
            val allExercises = dao.getAllExercisesSync()
            val exercisesMap = allExercises.associateBy { it.id }

            val newExercisesToInsert = mutableListOf<ExerciseEntity>()
            plan.exercises.forEach { planEx ->
                if (!exercisesMap.containsKey(planEx.id)) {
                    // Try by name matching if ID is different? For now, insert if ID missing
                    val existingByName = allExercises.find { it.name.equals(planEx.name, ignoreCase = true) }
                    if (existingByName == null) {
                        newExercisesToInsert.add(
                            ExerciseEntity(
                                id = planEx.id,
                                name = planEx.name,
                                muscleGroup = "Other",
                                equipment = "Other",
                                movementType = "Other",
                                defaultRestSeconds = planEx.restTimerSeconds ?: 90,
                                defaultUnit = "kg",
                                isCompound = false,
                                requiresStraps = false,
                                coachNote = null,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        // If ID from JSON differs but name exists, should ideally map it.
                        // For simplicity, sticking to ID matching or new insert.
                    }
                }
            }
            if (newExercisesToInsert.isNotEmpty()) {
                dao.insertExercises(newExercisesToInsert)
            }

            // Create new WorkoutSessionEntity
            val sessionId = UUID.randomUUID().toString()
            val session = WorkoutSessionEntity(
                id = sessionId,
                templateId = template.id,
                startedAt = System.currentTimeMillis(),
                finishedAt = null,
                durationSeconds = null,
                totalWorkingVolumeKg = null,
                averageRpe = null,
                cardioCompleted = false,
                note = plan.notes
            )
            dao.insertWorkoutSession(session)

            // Create Exercise Sessions and Set Logs
            plan.exercises.forEachIndexed { index, planEx ->
                // Map ID correctly if existing by name was found? Just use planEx.id as we inserted if missing
                val exerciseSessionId = UUID.randomUUID().toString()
                val exSession = WorkoutExerciseSessionEntity(
                    id = exerciseSessionId,
                    workoutSessionId = sessionId,
                    exerciseId = planEx.id,
                    orderIndex = index,
                    supersetGroupId = null,
                    note = planEx.notes,
                    restTimerSeconds = planEx.restTimerSeconds
                )
                dao.insertWorkoutExerciseSession(exSession)

                // Add Set Logs
                val setsToCreate = planEx.targetSets
                for (i in 1..setsToCreate) {
                    val setLog = SetLogEntity(
                        id = UUID.randomUUID().toString(),
                        workoutExerciseSessionId = exerciseSessionId,
                        setNumber = i,
                        setType = "WORKING",
                        targetWeight = planEx.targetLoad,
                        actualWeight = null,
                        weightUnit = "kg", // Or deduce from user preference
                        targetReps = planEx.targetReps ?: (planEx.targetRepsMax ?: 10),
                        actualReps = null,
                        rpe = planEx.targetRpe,
                        completed = false,
                        isSkipped = false,
                        note = null
                    )
                    dao.insertSetLog(setLog)
                }
            }

                Result.success(sessionId)
            } catch (e: kotlinx.serialization.SerializationException) {
                Result.failure(Exception("Invalid JSON format"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
