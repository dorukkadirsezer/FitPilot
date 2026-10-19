package com.doruk.fitprogress.core.domain.usecase

data class WorkoutWeeklySummary(
    val workoutsCompleted: Int,
    val aDays: Int,
    val bDays: Int,
    val cDays: Int,
    val dDays: Int,
    val totalVolumeKg: Float,
    val averageRpe: Float?,
    val skippedSets: Int,
    val progressionSuggestionsCount: Int
)

data class CardioWeeklySummary(
    val sessionsCompleted: Int,
    val totalMinutes: Int,
    val skippedCardioCount: Int
)

data class NutritionWeeklySummary(
    val averageDailyCalories: Float,
    val averageDailyProtein: Float,
    val daysLogged: Int,
    val daysProteinTargetReached: Int,
    val daysCalorieTargetReachedOrExceeded: Int
)

data class BodySummary(
    val latestWeight: Float?,
    val weightChange7Days: Float?,
    val weightChange14Days: Float?,
    val weightChange30Days: Float?,
    val latestWaist: Float?,
    val waistChange: Float? // since previous entry
)

data class AnalyticsDashboardSummary(
    val workoutSummary: WorkoutWeeklySummary,
    val cardioSummary: CardioWeeklySummary,
    val nutritionSummary: NutritionWeeklySummary,
    val bodySummary: BodySummary
)
