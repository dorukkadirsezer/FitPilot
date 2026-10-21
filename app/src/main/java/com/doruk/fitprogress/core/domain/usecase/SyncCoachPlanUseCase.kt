package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class SyncCoachPlanUseCase @Inject constructor(
    private val dao: WorkoutDao,
    private val profileDataStore: ProfileDataStore,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Result<String> {
        val jsonStr = withContext(Dispatchers.Main) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            } catch (e: Exception) {
                null
            }
        }

        if (jsonStr.isNullOrBlank()) {
            return Result.failure(Exception("Clipboard is empty"))
        }

        return withContext(Dispatchers.IO) {
            try {
                val format = Json { ignoreUnknownKeys = true }
                val plan: ProgramPlanDto = format.decodeFromString(jsonStr)

                // Save profile data
                profileDataStore.setAthleteName(plan.athlete)
                profileDataStore.setNutritionTargets(
                    calories = plan.dailyNutritionTarget.calories,
                    protein = plan.dailyNutritionTarget.proteinGrams,
                    fat = plan.dailyNutritionTarget.fatGrams,
                    carbs = plan.dailyNutritionTarget.carbsGrams,
                    steps = plan.dailyNutritionTarget.dailyStepsTarget,
                    sleep = plan.dailyNutritionTarget.sleepHoursTarget
                )

                // Delete old AI programs
                dao.deleteAiProgramTemplates()

                // Insert new templates
                val templatesToInsert = mutableListOf<WorkoutTemplateEntity>()
                val templateExercisesToInsert = mutableListOf<WorkoutTemplateExerciseEntity>()
                val newExercisesToInsert = mutableMapOf<String, ExerciseEntity>()

                val allExercises = dao.getAllExercisesSync()
                val exercisesMap = allExercises.associateBy { it.id }

                for (workout in plan.workouts) {
                    val templateId = UUID.randomUUID().toString()
                    val template = WorkoutTemplateEntity(
                        id = templateId,
                        name = workout.workoutName,
                        dayCode = "PROGRAM_DAY_${workout.dayNumber}",
                        dayFocus = "AI Coach Program",
                        defaultCardioType = null,
                        defaultCardioMinutes = null,
                        isAiProgram = true
                    )
                    templatesToInsert.add(template)

                    workout.exercises.forEachIndexed { index, planEx ->
                        if (!exercisesMap.containsKey(planEx.id) && !newExercisesToInsert.containsKey(planEx.id)) {
                            newExercisesToInsert[planEx.id] = ExerciseEntity(
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
                        }

                        templateExercisesToInsert.add(
                            WorkoutTemplateExerciseEntity(
                                id = UUID.randomUUID().toString(),
                                templateId = templateId,
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
                        )
                    }
                }

                if (newExercisesToInsert.isNotEmpty()) {
                    dao.insertExercises(newExercisesToInsert.values.toList())
                }

                if (templatesToInsert.isNotEmpty()) {
                    dao.insertTemplates(templatesToInsert)
                }

                if (templateExercisesToInsert.isNotEmpty()) {
                    dao.insertTemplateExercises(templateExercisesToInsert)
                }

                Result.success("Program imported successfully! ${templatesToInsert.size} workouts added.")
            } catch (e: Exception) {
                Result.failure(Exception("Failed to parse program: ${e.message}", e))
            }
        }
    }
}
