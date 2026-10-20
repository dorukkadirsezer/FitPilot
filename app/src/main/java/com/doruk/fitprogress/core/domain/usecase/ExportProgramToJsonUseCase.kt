package com.doruk.fitprogress.core.domain.usecase

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.domain.model.ExerciseTargetDto
import com.doruk.fitprogress.core.domain.model.NutritionTargetDto
import com.doruk.fitprogress.core.domain.model.ProgramPlanDto
import com.doruk.fitprogress.core.domain.model.WorkoutDayDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject

import dagger.hilt.android.qualifiers.ApplicationContext

class ExportProgramToJsonUseCase @Inject constructor(
    private val dao: WorkoutDao,
    private val profileDataStore: ProfileDataStore,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Result<Unit> {
        val resultPair: Pair<Result<Unit>, String?> = withContext(Dispatchers.IO) {
            try {
                // Fetch profile data
                val athlete = profileDataStore.athleteNameFlow.firstOrNull() ?: "Unknown Athlete"
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
                            notes = null // Note not currently mapped back
                        )
                    }

                    WorkoutDayDto(
                        dayNumber = index + 1,
                        workoutName = template.name,
                        exercises = exerciseTargets
                    )
                }

                val plan = ProgramPlanDto(
                    programVersion = "2.1",
                    athlete = athlete,
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
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Program Plan JSON", jsonStr)
                clipboard.setPrimaryClip(clip)
            }
        }

        return res
    }
}
