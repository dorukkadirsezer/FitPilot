package com.doruk.fitprogress.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.*

@Database(
    entities = [
        UserProfileEntity::class,
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        WorkoutTemplateExerciseEntity::class,
        TemplateWarmupInstructionEntity::class,
        TemplateWarmupSetEntity::class,
        WorkoutSessionEntity::class,
        WorkoutExerciseSessionEntity::class,
        SetLogEntity::class,
        RestTimerLogEntity::class,
        ReadinessCheckInEntity::class,
        SmartDayRecommendationEntity::class,
        TimeLimitedWorkoutPlanEntity::class,
        TimeLimitedWorkoutPlanExerciseEntity::class,
        SetAdviceEntity::class,
        ProgressionReadinessScoreEntity::class,
        AdaptiveCardioRecommendationEntity::class,
        ProgressionSuggestionEntity::class,
        NutritionDayEntity::class,
        FoodItemEntity::class,
        MealLogEntity::class,
        BodyMetricEntity::class,
        CardioSessionEntity::class,
        AiInteractionLogEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class GymFlowDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun nutritionDao(): com.doruk.fitprogress.core.database.dao.NutritionDao
    abstract fun analyticsDao(): com.doruk.fitprogress.core.database.dao.AnalyticsDao
    abstract fun aiDao(): com.doruk.fitprogress.core.database.dao.AiDao
}
