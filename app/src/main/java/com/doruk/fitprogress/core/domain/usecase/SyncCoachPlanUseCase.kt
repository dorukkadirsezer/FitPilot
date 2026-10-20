package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
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
                    name = plan.workoutName,
                    dayCode = "AI_COACH",
                    dayFocus = "Custom",
                    defaultCardioType = null,
                    defaultCardioMinutes = null
                )
            } else {
                template = template.copy(name = plan.workoutName)
                // Clear old template exercises
                dao.deleteTemplateExercisesByTemplateId(template.id)
            }
            dao.insertTemplates(listOf(template))

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

            // Create Template Exercises
            val templateExercises = plan.exercises.mapIndexed { index, planEx ->
                WorkoutTemplateExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    templateId = template.id,
                    exerciseId = planEx.id,
                    orderIndex = index + 1,
                    isSuperset = false,
                    supersetGroupId = null,
                    supersetRoundCount = null,
                    targetSets = planEx.targetSets,
                    targetRepsMin = planEx.targetReps ?: (planEx.targetRepsMin ?: 10),
                    targetRepsMax = planEx.targetReps ?: (planEx.targetRepsMax ?: 10),
                    targetWeightMin = planEx.targetLoad,
                    targetWeightMax = planEx.targetLoad,
                    weightUnit = "kg",
                    restSeconds = planEx.restTimerSeconds ?: 90,
                    rpeMin = planEx.targetRpe,
                    rpeMax = planEx.targetRpe
                )
            }
            if (templateExercises.isNotEmpty()) {
                dao.insertTemplateExercises(templateExercises)
            }

                Result.success("Imported: ${plan.workoutName} (${plan.exercises.size} exercises)")
            } catch (e: kotlinx.serialization.SerializationException) {
                Result.failure(Exception("Invalid JSON format"))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
