package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.datastore.ProfileDataStore
import com.doruk.fitprogress.core.domain.model.AdherenceMetricsDto
import com.doruk.fitprogress.core.domain.model.CatalogExerciseRefDto
import com.doruk.fitprogress.core.domain.model.CurrentTemplateSummaryDto
import com.doruk.fitprogress.core.domain.model.ExercisePerformanceSnapshotDto
import com.doruk.fitprogress.core.domain.model.RecentSessionTelemetryDto
import com.doruk.fitprogress.core.domain.model.UserTelemetryPayloadDto
import com.doruk.fitprogress.core.domain.model.ValidatedExerciseTargetDto
import com.doruk.fitprogress.core.domain.model.ValidatedNutritionTargetDto
import com.doruk.fitprogress.core.di.AiCoachJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Aggregates completed workout sessions (RPE, Working Volume, Set Adherence),
 * active templates, and exercise catalog references from Room into a lightweight
 * JSON telemetry payload for autonomous Gemini macrocycle generation.
 */
class ExportUserProgressionStatsUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val profileDataStore: ProfileDataStore,
    @AiCoachJson private val json: Json
) {
    suspend operator fun invoke(maxRecentSessions: Int = 10): UserTelemetryPayloadDto = withContext(Dispatchers.IO) {
        val nowMillis = System.currentTimeMillis()
        val fourteenDaysAgoMillis = nowMillis - TimeUnit.DAYS.toMillis(14)
        val thirtyDaysAgoMillis = nowMillis - TimeUnit.DAYS.toMillis(30)

        val athleteName = profileDataStore.athleteNameFlow.firstOrNull()?.takeIf { it.isNotBlank() } ?: "FitPilot Athlete"
        val targetCalories = profileDataStore.targetCaloriesFlow.firstOrNull() ?: 2500
        val targetProtein = profileDataStore.targetProteinFlow.firstOrNull() ?: 160
        val targetFat = profileDataStore.targetFatFlow.firstOrNull() ?: 75
        val targetCarbs = profileDataStore.targetCarbsFlow.firstOrNull() ?: 260
        val targetSteps = profileDataStore.dailyStepsTargetFlow.firstOrNull() ?: 8500
        val targetSleep = profileDataStore.sleepHoursTargetFlow.firstOrNull() ?: 8.0f

        val allExercises = workoutDao.getAllExercisesSync()
        val exerciseMap = allExercises.associateBy { it.id }

        val historyItems = workoutDao.getWorkoutHistoryList().firstOrNull().orEmpty()
        val totalCompletedCount = historyItems.size

        val workoutsLast14Days = historyItems.count { item ->
            val ts = item.session.finishedAt ?: item.session.startedAt
            ts >= fourteenDaysAgoMillis
        }
        val workoutsLast30Days = historyItems.count { item ->
            val ts = item.session.finishedAt ?: item.session.startedAt
            ts >= thirtyDaysAgoMillis
        }

        val totalCompletedSetsAll = historyItems.sumOf { it.completedSetCount }
        val totalSkippedSetsAll = historyItems.sumOf { it.skippedSetCount }
        val totalAttemptedSetsAll = totalCompletedSetsAll + totalSkippedSetsAll
        val adherenceRate = if (totalAttemptedSetsAll > 0) {
            ((totalCompletedSetsAll.toFloat() / totalAttemptedSetsAll.toFloat()) * 100f).roundTo1Decimal()
        } else {
            100.0f
        }

        val rpeValues = historyItems.mapNotNull { it.session.averageRpe?.toFloat() }.filter { it > 0f }
        val avgSessionRpe = if (rpeValues.isNotEmpty()) {
            rpeValues.average().toFloat().roundTo1Decimal()
        } else {
            null
        }

        val volumeValues = historyItems.mapNotNull { it.session.totalWorkingVolumeKg?.toFloat() }
        val avgSessionVolume = if (volumeValues.isNotEmpty()) {
            volumeValues.average().toFloat().roundTo1Decimal()
        } else {
            0f
        }

        // Detailed snapshots for the most recent N sessions
        val recentSessionsTelemetry = historyItems.take(maxRecentSessions).map { historyItem ->
            val detail = workoutDao.getWorkoutDetail(historyItem.session.id)
            val ts = historyItem.session.finishedAt ?: historyItem.session.startedAt
            val dateIso = Instant.ofEpochMilli(ts)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
                .format(DateTimeFormatter.ISO_LOCAL_DATE)

            val snapshots = detail?.exercises.orEmpty().mapNotNull { exDto ->
                val workingSets = exDto.sets.map { it.setLog }.filter { it.setType != "WARMUP" && it.completed && !it.isSkipped }
                if (workingSets.isEmpty() && exDto.sets.isEmpty()) return@mapNotNull null

                val bestSet = workingSets.maxByOrNull { set ->
                    val w = set.actualWeight?.toDouble() ?: 0.0
                    val r = (set.actualReps ?: 0).toDouble()
                    w * (1.0 + r / 30.0)
                }
                val exRpes = workingSets.mapNotNull { it.rpe?.toFloat() }
                val exAvgRpe = if (exRpes.isNotEmpty()) exRpes.average().toFloat().roundTo1Decimal() else null

                ExercisePerformanceSnapshotDto(
                    exerciseId = exDto.exerciseSession.exerciseId,
                    exerciseName = exDto.exercise?.name ?: exerciseMap[exDto.exerciseSession.exerciseId]?.name ?: exDto.exerciseSession.exerciseId,
                    workingSetsCompleted = workingSets.size,
                    bestSetWeightKg = bestSet?.actualWeight?.toFloat()?.roundTo1Decimal(),
                    bestSetReps = bestSet?.actualReps,
                    averageRpe = exAvgRpe
                )
            }

            RecentSessionTelemetryDto(
                sessionId = historyItem.session.id,
                dateIso = dateIso,
                workoutName = historyItem.workoutName ?: "Workout Session",
                durationMinutes = (((historyItem.session.durationSeconds ?: 0L).toLong() / 60L).toInt()).coerceAtLeast(1),
                totalWorkingVolumeKg = (historyItem.session.totalWorkingVolumeKg?.toFloat() ?: 0f).roundTo1Decimal(),
                averageRpe = historyItem.session.averageRpe?.toFloat()?.roundTo1Decimal(),
                completedWorkingSets = historyItem.completedSetCount,
                skippedSets = historyItem.skippedSetCount,
                exerciseSnapshots = snapshots
            )
        }

        // Current active templates overview
        val templates = workoutDao.getAllTemplatesSync()
        val currentProgramOverview = templates.take(7).map { template ->
            val templateExercises = workoutDao.getTemplateExercisesSync(template.id)
            CurrentTemplateSummaryDto(
                dayCode = template.dayCode,
                workoutName = template.name,
                isAiProgram = template.isAiProgram,
                exercises = templateExercises.map { tex ->
                    val exEntity = exerciseMap[tex.exerciseId]
                    ValidatedExerciseTargetDto(
                        id = tex.exerciseId,
                        name = exEntity?.name ?: tex.exerciseId,
                        targetSets = tex.targetSets,
                        targetReps = if (tex.targetRepsMin == tex.targetRepsMax) tex.targetRepsMin else null,
                        targetRepsMin = tex.targetRepsMin,
                        targetRepsMax = tex.targetRepsMax,
                        targetLoad = tex.targetWeightMin,
                        targetRpe = tex.rpeMin,
                        restTimerSeconds = tex.restSeconds
                    )
                }
            )
        }

        val catalogRefs = allExercises.take(45).map { ex ->
            CatalogExerciseRefDto(
                id = ex.id,
                name = ex.name,
                muscleGroup = ex.muscleGroup,
                equipment = ex.equipment
            )
        }

        val todayIso = Instant.ofEpochMilli(nowMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        UserTelemetryPayloadDto(
            exportedAtIso = todayIso,
            athleteName = athleteName,
            currentNutritionTargets = ValidatedNutritionTargetDto(
                calories = targetCalories,
                proteinGrams = targetProtein,
                fatGrams = targetFat,
                carbsGrams = targetCarbs,
                dailyStepsTarget = targetSteps,
                sleepHoursTarget = targetSleep
            ),
            adherenceMetrics = AdherenceMetricsDto(
                totalCompletedWorkouts = totalCompletedCount,
                workoutsLast14Days = workoutsLast14Days,
                workoutsLast30Days = workoutsLast30Days,
                overallSetCompletionRate = adherenceRate,
                averageSessionRpe = avgSessionRpe,
                averageSessionVolumeKg = avgSessionVolume
            ),
            currentProgramOverview = currentProgramOverview,
            recentCompletedSessions = recentSessionsTelemetry,
            catalogExercises = catalogRefs
        )
    }

    suspend fun exportAsJsonString(maxRecentSessions: Int = 10): String = withContext(Dispatchers.IO) {
        val payload = invoke(maxRecentSessions)
        json.encodeToString(payload)
    }

    private fun Float.roundTo1Decimal(): Float {
        return (this * 10f).roundToInt() / 10f
    }
}
