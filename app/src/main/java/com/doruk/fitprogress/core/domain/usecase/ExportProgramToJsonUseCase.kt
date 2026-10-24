package com.doruk.fitprogress.core.domain.usecase

import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.domain.model.ExerciseTargetDto
import com.doruk.fitprogress.core.domain.model.NutritionTargetDto
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import com.doruk.fitprogress.core.domain.model.WorkoutDayDto
import com.doruk.fitprogress.core.util.ClipboardSecurityUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ExportProgramToJsonUseCase @Inject constructor(
    private val dao: WorkoutDao,
    private val profileDataStore: ProfileDataStore,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Result<Unit> {
        val resultPair: Pair<Result<Unit>, String?> = withContext(Dispatchers.IO) {
            try {
                // M4 Privacy: Do not export personal athlete name (PII)
                val anonymizedAthlete = "FitPilot_Athlete"
                val calories = profileDataStore.targetCaloriesFlow.firstOrNull() ?: 2500
                val protein = profileDataStore.targetProteinFlow.firstOrNull() ?: 150
                val fat = profileDataStore.targetFatFlow.firstOrNull() ?: 80
                val carbs = profileDataStore.targetCarbsFlow.firstOrNull() ?: 250
                val steps = profileDataStore.dailyStepsTargetFlow.firstOrNull() ?: 8000
                val sleep = profileDataStore.sleepHoursTargetFlow.firstOrNull() ?: 8.0f

                val nutritionTarget = NutritionTargetDto(
                    calories = calories,
                    proteinGrams = protein,
                    fatGrams = fat,
                    carbsGrams = carbs,
                    dailyStepsTarget = steps,
                    sleepHoursTarget = sleep
                )

                // Fetch AI templates
                val templates = dao.getAiProgramTemplatesSync()
                val allExercises = dao.getAllExercisesSync().associateBy { it.id }

                val workoutDays = templates.mapIndexed { index, template ->
                    val templateExercises = dao.getTemplateExercisesSync(template.id)

                    val exerciseTargets = templateExercises.map { tex ->
                        val exercise = allExercises[tex.exerciseId]
                        ExerciseTargetDto(
                            id = tex.exerciseId,
                            name = exercise?.name ?: "Unknown Exercise",
                            targetSets = tex.targetSets,
                            targetReps = if (tex.targetRepsMin == tex.targetRepsMax) tex.targetRepsMin else null,
                            targetRepsMin = tex.targetRepsMin,
                            targetRepsMax = tex.targetRepsMax,
                            targetLoad = tex.targetWeightMin,
                            targetRpe = tex.rpeMin,
                            restTimerSeconds = tex.restSeconds,
                            notes = null
                        )
                    }

                    WorkoutDayDto(
                        dayNumber = index + 1,
                        workoutName = template.name,
                        exercises = exerciseTargets
                    )
                }

                val plan = ProgramPlanDto(
                    programVersion = "2.2",
                    athlete = anonymizedAthlete,
                    dailyNutritionTarget = nutritionTarget,
                    workouts = workoutDays
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
                // M5: Mark copied JSON payload with EXTRA_IS_SENSITIVE
                ClipboardSecurityUtils.copySensitiveText(
                    context = context,
                    label = "Program Plan JSON",
                    text = jsonStr
                )
            }
        }

        return res
    }
}
