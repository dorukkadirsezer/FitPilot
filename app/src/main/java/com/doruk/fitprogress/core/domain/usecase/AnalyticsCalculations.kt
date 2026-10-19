package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.AnalyticsDao
import com.doruk.fitprogress.core.database.dao.NutritionDao
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import com.doruk.fitprogress.core.database.entity.CardioSessionEntity
import com.doruk.fitprogress.core.database.entity.NutritionDayEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.database.entity.ProgressionSuggestionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

object AnalyticsCalculator {

    fun calculateWorkoutSummary(
        workouts: List<WorkoutSessionEntity>,
        setLogs: List<SetLogEntity>,
        progressionSuggestions: List<ProgressionSuggestionEntity>,
        sessionToDayCode: Map<String, String?>
    ): WorkoutWeeklySummary {
        var aCount = 0
        var bCount = 0
        var cCount = 0
        var dCount = 0

        for (session in workouts) {
            when (sessionToDayCode[session.id]) {
                "A" -> aCount++
                "B" -> bCount++
                "C" -> cCount++
                "D" -> dCount++
            }
        }

        var totalVolumeKg = 0f
        var rpeSum = 0f
        var rpeCount = 0
        var skippedSets = 0

        for (setLog in setLogs) {
            if (setLog.isSkipped) {
                skippedSets++
                continue
            }
            if (setLog.completed && setLog.setType == "WORKING") {
                val weight = setLog.actualWeight ?: 0f
                val reps = setLog.actualReps ?: 0
                val unit = setLog.weightUnit
                val weightInKg = if (unit == "lbs") weight * 0.453592f else weight

                totalVolumeKg += (weightInKg * reps)

                if (setLog.rpe != null) {
                    rpeSum += setLog.rpe
                    rpeCount++
                }
            }
        }

        val avgRpe = if (rpeCount > 0) rpeSum / rpeCount else null

        return WorkoutWeeklySummary(
            workoutsCompleted = workouts.size,
            aDays = aCount,
            bDays = bCount,
            cDays = cCount,
            dDays = dCount,
            totalVolumeKg = totalVolumeKg,
            averageRpe = avgRpe,
            skippedSets = skippedSets,
            progressionSuggestionsCount = progressionSuggestions.size
        )
    }

    fun calculateCardioSummary(cardios: List<CardioSessionEntity>): CardioWeeklySummary {
        return CardioWeeklySummary(
            sessionsCompleted = cardios.size,
            totalMinutes = cardios.sumOf { it.durationMinutes },
            skippedCardioCount = 0
        )
    }

    fun calculateNutritionSummary(
        nutritionDays: List<NutritionDayEntity>,
        mealLogs: List<MealLogEntity>
    ): NutritionWeeklySummary {
        var totalCalories = 0f
        var totalProtein = 0f
        var daysProteinHit = 0
        var daysCaloriesHit = 0

        for (day in nutritionDays) {
            val dayMeals = mealLogs.filter { it.nutritionDayId == day.id }
            val dayCals = dayMeals.sumOf { it.calories.toDouble() }.toFloat()
            val dayPro = dayMeals.sumOf { it.protein.toDouble() }.toFloat()

            totalCalories += dayCals
            totalProtein += dayPro

            if (day.targetProtein > 0 && dayPro >= day.targetProtein) {
                daysProteinHit++
            }
            if (day.targetCalories > 0 && dayCals >= day.targetCalories) {
                daysCaloriesHit++
            }
        }

        val avgCals = if (nutritionDays.isNotEmpty()) totalCalories / nutritionDays.size else 0f
        val avgPro = if (nutritionDays.isNotEmpty()) totalProtein / nutritionDays.size else 0f

        return NutritionWeeklySummary(
            averageDailyCalories = avgCals,
            averageDailyProtein = avgPro,
            daysLogged = nutritionDays.size,
            daysProteinTargetReached = daysProteinHit,
            daysCalorieTargetReachedOrExceeded = daysCaloriesHit
        )
    }

    fun calculateBodySummary(
        metrics: List<BodyMetricEntity>,
        todayIso: String
    ): BodySummary {
        val sortedMetrics = metrics.sortedByDescending { it.dateIso }
        val today = LocalDate.parse(todayIso, DateTimeFormatter.ISO_LOCAL_DATE)

        val latest = sortedMetrics.firstOrNull()
        val previous = if (sortedMetrics.size > 1) sortedMetrics[1] else null

        val latestWeight = latest?.bodyWeight
        val latestWaist = latest?.waist

        var weightChange7: Float? = null
        var weightChange14: Float? = null
        var weightChange30: Float? = null

        if (latestWeight != null) {
            val sevenDayMetric = sortedMetrics.firstOrNull {
                ChronoUnit.DAYS.between(LocalDate.parse(it.dateIso), today) in 6..8
            }
            if (sevenDayMetric?.bodyWeight != null) weightChange7 = latestWeight - sevenDayMetric.bodyWeight

            val fourteenDayMetric = sortedMetrics.firstOrNull {
                ChronoUnit.DAYS.between(LocalDate.parse(it.dateIso), today) in 13..15
            }
            if (fourteenDayMetric?.bodyWeight != null) weightChange14 = latestWeight - fourteenDayMetric.bodyWeight

            val thirtyDayMetric = sortedMetrics.firstOrNull {
                ChronoUnit.DAYS.between(LocalDate.parse(it.dateIso), today) in 28..32
            }
            if (thirtyDayMetric?.bodyWeight != null) weightChange30 = latestWeight - thirtyDayMetric.bodyWeight
        }

        val waistChange = if (latestWaist != null && previous?.waist != null) {
            latestWaist - previous.waist
        } else null

        return BodySummary(
            latestWeight = latestWeight,
            weightChange7Days = weightChange7,
            weightChange14Days = weightChange14,
            weightChange30Days = weightChange30,
            latestWaist = latestWaist,
            waistChange = waistChange
        )
    }
}

class CalculateAnalyticsUseCase @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val nutritionDao: NutritionDao,
    private val analyticsDao: AnalyticsDao
) {

    suspend fun getDashboardSummary(todayIso: String): AnalyticsDashboardSummary {
        val today = LocalDate.parse(todayIso, DateTimeFormatter.ISO_LOCAL_DATE)
        val sevenDaysAgo = today.minusDays(7)
        val startDateIso = sevenDaysAgo.format(DateTimeFormatter.ISO_LOCAL_DATE)

        val startTimestamp = sevenDaysAgo.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endTimestamp = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // 1. Workout Weekly Summary
        val workouts = workoutDao.getWorkoutSessionsBetweenDates(startTimestamp, endTimestamp)
        val setLogs = workoutDao.getSetLogsBetweenDates(startTimestamp, endTimestamp)
        val progressionSuggestions = workoutDao.getProgressionSuggestionsBetweenDates(startTimestamp, endTimestamp)

        val sessionToDayCode = mutableMapOf<String, String?>()
        for (session in workouts) {
            val template = workoutDao.getTemplateWithDetails(session.templateId ?: "")?.template
            sessionToDayCode[session.id] = template?.dayCode
        }

        val workoutSummary = AnalyticsCalculator.calculateWorkoutSummary(
            workouts = workouts,
            setLogs = setLogs,
            progressionSuggestions = progressionSuggestions,
            sessionToDayCode = sessionToDayCode
        )

        // 2. Cardio Weekly Summary
        val cardios = workoutDao.getCardioSessionsBetweenDates(startTimestamp, endTimestamp)
        val cardioSummary = AnalyticsCalculator.calculateCardioSummary(cardios)

        // 3. Nutrition Weekly Summary
        val nutritionDays = nutritionDao.getNutritionDaysBetweenDates(startDateIso, todayIso)
        val mealLogs = nutritionDao.getMealLogsBetweenDates(startDateIso, todayIso)
        
        val nutritionSummary = AnalyticsCalculator.calculateNutritionSummary(nutritionDays, mealLogs)

        // 4. Body Summary
        val thirtyDaysAgo = today.minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val metrics = analyticsDao.getBodyMetricsBetweenDates(thirtyDaysAgo, todayIso)
        val bodySummary = AnalyticsCalculator.calculateBodySummary(metrics, todayIso)

        return AnalyticsDashboardSummary(
            workoutSummary = workoutSummary,
            cardioSummary = cardioSummary,
            nutritionSummary = nutritionSummary,
            bodySummary = bodySummary
        )
    }
}
