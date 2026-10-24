package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipboardManager
import android.content.Context
import androidx.room.withTransaction
import com.doruk.fitprogress.core.database.GymFlowDatabase
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.di.AiCoachJson
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import com.doruk.fitprogress.core.domain.model.ValidatedCoachPlanDto
import com.doruk.fitprogress.core.domain.model.ValidatedWorkoutDayDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

class SyncCoachPlanUseCase @Inject constructor(
    private val database: GymFlowDatabase,
    private val dao: WorkoutDao,
    private val profileDataStore: ProfileDataStore,
    @AiCoachJson private val json: Json,
    @ApplicationContext private val context: Context
) {
    companion object {
        // M5 DoS protection: Maximum 256 KB characters allowed from clipboard
        private const val MAX_CLIPBOARD_JSON_LENGTH = 256_000
    }

    suspend operator fun invoke(): Result<String> {
        val rawClipboardText = withContext(Dispatchers.Main) {
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            } catch (e: Exception) {
                null
            }
        }

        if (rawClipboardText.isNullOrBlank()) {
            return Result.failure(Exception("Pano boş. Lütfen geçerli bir program JSON verisi kopyalayın."))
        }

        if (rawClipboardText.length > MAX_CLIPBOARD_JSON_LENGTH) {
            return Result.failure(Exception("Panodaki veri boyutu çok büyük (Maksimum 256 KB)."))
        }

        return withContext(Dispatchers.IO) {
            try {
                // M5: Strictly validate untrusted clipboard JSON through ValidatedCoachPlanDto.validate()
                val cleanJson = stripMarkdownJsonFence(rawClipboardText)
                val validatedDto = decodeCoachPlanCompat(cleanJson).validate()
                val plan: ProgramPlanDto = validatedDto.toProgramPlanDto()

                // M4: Only update bounded nutrition targets; preserve local athlete name (no PII overwrite)
                profileDataStore.setNutritionTargets(
                    calories = plan.dailyNutritionTarget.calories,
                    protein = plan.dailyNutritionTarget.proteinGrams,
                    fat = plan.dailyNutritionTarget.fatGrams,
                    carbs = plan.dailyNutritionTarget.carbsGrams,
                    steps = plan.dailyNutritionTarget.dailyStepsTarget,
                    sleep = plan.dailyNutritionTarget.sleepHoursTarget
                )

                val templatesToInsert = mutableListOf<WorkoutTemplateEntity>()
                val templateExercisesToInsert = mutableListOf<WorkoutTemplateExerciseEntity>()
                val newExercisesToInsert = mutableMapOf<String, ExerciseEntity>()

                // M5: Atomic Room transaction for deleting old AI templates and inserting validated ones
                database.withTransaction {
                    dao.deleteAiProgramTemplates()

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
                                    coachNote = planEx.notes,
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
                }

                Result.success("Program başarıyla içe aktarıldı! ${templatesToInsert.size} antrenman günü eklendi.")
            } catch (e: Exception) {
                Result.failure(Exception("Program doğrulanamadı: ${e.localizedMessage ?: "Geçersiz JSON şeması"}", e))
            }
        }
    }

    private fun decodeCoachPlanCompat(cleanJson: String): ValidatedCoachPlanDto {
        val trimmed = cleanJson.trim()
        return if (trimmed.startsWith("[")) {
            val days = json.decodeFromString<List<ValidatedWorkoutDayDto>>(trimmed)
            ValidatedCoachPlanDto(workouts = days)
        } else {
            json.decodeFromString<ValidatedCoachPlanDto>(trimmed)
        }
    }

    private fun stripMarkdownJsonFence(raw: String): String {
        var cleaned = raw.trim()
        if (cleaned.startsWith("```json", ignoreCase = true)) {
            cleaned = cleaned.substring(7).trimStart()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3).trimStart()
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.dropLast(3).trimEnd()
        }
        return cleaned.trim()
    }
}
