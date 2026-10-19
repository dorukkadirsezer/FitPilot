package com.doruk.fitprogress.feature.analytics

import com.doruk.fitprogress.core.database.entity.*
import com.doruk.fitprogress.core.domain.usecase.AnalyticsCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsTrackingTest {

    @Test
    fun `Workout Summary - empty state handles correctly`() {
        val summary = AnalyticsCalculator.calculateWorkoutSummary(
            workouts = emptyList(),
            setLogs = emptyList(),
            progressionSuggestions = emptyList(),
            sessionToDayCode = emptyMap()
        )
        
        assertEquals(0, summary.workoutsCompleted)
        assertEquals(0f, summary.totalVolumeKg)
        assertEquals(null, summary.averageRpe)
        assertEquals(0, summary.skippedSets)
        assertEquals(0, summary.progressionSuggestionsCount)
        assertEquals(0, summary.aDays)
    }
    
    @Test
    fun `Workout Summary - calculates days, volume, rpe and skipped sets`() {
        val workouts = listOf(
            WorkoutSessionEntity("sess1", "templateA", 0L, 0L, 0L, null, null, false),
            WorkoutSessionEntity("sess2", "templateB", 0L, 0L, 0L, null, null, false),
            WorkoutSessionEntity("sess3", "templateA", 0L, 0L, 0L, null, null, false)
        )
        
        val setLogs = listOf(
            SetLogEntity("set1", "wExSess1", 1, "WORKING", 100f, 100f, "kg", 10, 10, 8f, true, false), // 1000 kg, RPE 8
            SetLogEntity("set2", "wExSess1", 2, "WORKING", 100f, 100f, "lbs", 10, 10, 9f, true, false), // 100 lbs -> 45.3592 kg * 10 = 453.592 kg, RPE 9
            SetLogEntity("set3", "wExSess2", 1, "WORKING", 50f, 50f, "kg", 10, 0, null, false, true), // Skipped
            SetLogEntity("set4", "wExSess3", 1, "WARMUP", 20f, 20f, "kg", 15, 15, null, true, false) // Warmup, ignored
        )
        
        val suggestions = listOf(
            ProgressionSuggestionEntity("sugg1", "ex1", "sess1", 100f, "kg", 105f, "kg", "INCREASE_WEIGHT", "reason", "DETERMINISTIC", 0L, false)
        )
        
        val sessionMap = mapOf(
            "sess1" to "A",
            "sess2" to "B",
            "sess3" to "A"
        )
        
        val summary = AnalyticsCalculator.calculateWorkoutSummary(workouts, setLogs, suggestions, sessionMap)
        
        assertEquals(3, summary.workoutsCompleted)
        assertEquals(2, summary.aDays)
        assertEquals(1, summary.bDays)
        assertEquals(0, summary.cDays)
        assertEquals(1, summary.skippedSets)
        assertEquals(1, summary.progressionSuggestionsCount)
        assertEquals(1453.592f, summary.totalVolumeKg, 0.01f)
        assertEquals(8.5f, summary.averageRpe!!, 0.01f)
    }

    @Test
    fun `Cardio Summary - empty state handles correctly`() {
        val summary = AnalyticsCalculator.calculateCardioSummary(emptyList())
        assertEquals(0, summary.sessionsCompleted)
        assertEquals(0, summary.totalMinutes)
    }

    @Test
    fun `Cardio Summary - aggregates total minutes`() {
        val cardios = listOf(
            CardioSessionEntity("c1", "sess1", 0L, "WALK", 20, 5, null),
            CardioSessionEntity("c2", null, 0L, "ELLIPTICAL", 30, 7, null)
        )
        val summary = AnalyticsCalculator.calculateCardioSummary(cardios)
        assertEquals(2, summary.sessionsCompleted)
        assertEquals(50, summary.totalMinutes)
    }

    @Test
    fun `Nutrition Summary - calculates daily averages and target hits`() {
        val nutritionDays = listOf(
            NutritionDayEntity("day1", "2023-10-10", 2000, 150, 200, 50, null),
            NutritionDayEntity("day2", "2023-10-11", 2000, 150, 200, 50, null)
        )
        
        val mealLogs = listOf(
            MealLogEntity("m1", "day1", null, "BREAKFAST", "Oats", "bowl", 1f, 500f, 20f, 80f, 10f, null, 0L),
            MealLogEntity("m2", "day1", null, "LUNCH", "Chicken", "breast", 1f, 1600f, 140f, 20f, 20f, null, 0L), // Total day1: 2100 cal, 160 pro (Hits both)
            
            MealLogEntity("m3", "day2", null, "DINNER", "Salad", "bowl", 1f, 1000f, 50f, 100f, 30f, null, 0L) // Total day2: 1000 cal, 50 pro (Misses both)
        )
        
        val summary = AnalyticsCalculator.calculateNutritionSummary(nutritionDays, mealLogs)
        
        assertEquals(2, summary.daysLogged)
        assertEquals(1550f, summary.averageDailyCalories, 0.01f)
        assertEquals(105f, summary.averageDailyProtein, 0.01f)
        assertEquals(1, summary.daysProteinTargetReached)
        assertEquals(1, summary.daysCalorieTargetReachedOrExceeded)
    }
    
    @Test
    fun `Body Summary - calculates changes based on time ranges`() {
        val todayIso = "2023-10-15"
        val todayMetric = BodyMetricEntity("1", "2023-10-15", 80f, "kg", 85f, null, null, null, null, null, 0L, 0L)
        val prevMetric = BodyMetricEntity("2", "2023-10-14", 81f, "kg", 86f, null, null, null, null, null, 0L, 0L)
        val sevenDaysAgo = BodyMetricEntity("3", "2023-10-08", 82f, "kg", 87f, null, null, null, null, null, 0L, 0L)
        val fourteenDaysAgo = BodyMetricEntity("4", "2023-10-01", 83f, "kg", 88f, null, null, null, null, null, 0L, 0L)
        
        val metrics = listOf(todayMetric, prevMetric, sevenDaysAgo, fourteenDaysAgo)
        
        val summary = AnalyticsCalculator.calculateBodySummary(metrics, todayIso)
        
        assertEquals(80f, summary.latestWeight)
        assertEquals(85f, summary.latestWaist)
        assertEquals(-2f, summary.weightChange7Days)
        assertEquals(-3f, summary.weightChange14Days)
        assertEquals(null, summary.weightChange30Days)
        assertEquals(-1f, summary.waistChange)
    }
}
