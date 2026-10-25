package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

open class FakeWorkoutDao : WorkoutDao {
    override suspend fun insertExercises(exercises: List<ExerciseEntity>) {}
    override suspend fun insertTemplates(templates: List<WorkoutTemplateEntity>) {}
    override suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>) {}
    override suspend fun deleteTemplateExercisesByTemplateId(templateId: String) {}
    override suspend fun deleteAiProgramTemplates() {}
    override suspend fun insertWarmupInstructions(instructions: List<TemplateWarmupInstructionEntity>) {}
    override suspend fun insertWarmupSets(warmups: List<TemplateWarmupSetEntity>) {}
    override fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>> = emptyFlow()
    override suspend fun getAllTemplatesSync(): List<WorkoutTemplateEntity> = emptyList()
    override suspend fun getAiProgramTemplatesSync(): List<WorkoutTemplateEntity> = emptyList()
    override suspend fun getTemplateByDayCode(code: String): WorkoutTemplateEntity? = null
    override suspend fun getTemplateExercisesSync(templateId: String): List<WorkoutTemplateExerciseEntity> = emptyList()
    override fun getAllExercises(): Flow<List<ExerciseEntity>> = emptyFlow()
    override suspend fun getAllExercisesSync(): List<ExerciseEntity> = emptyList()
    override suspend fun getTemplateWithDetails(templateId: String): TemplateWithDetails? = null
    override suspend fun insertWorkoutSession(session: WorkoutSessionEntity) {}
    override suspend fun updateWorkoutSession(session: WorkoutSessionEntity) {}
    override suspend fun insertWorkoutExerciseSession(session: WorkoutExerciseSessionEntity) {}
    override suspend fun insertSetLog(setLog: SetLogEntity) {}
    override suspend fun updateSetLog(setLog: SetLogEntity) {}
    override suspend fun getUnfinishedSessionForTemplate(templateId: String): WorkoutSessionEntity? = null
    override suspend fun getExerciseSessionsForWorkout(sessionId: String): List<WorkoutExerciseSessionEntity> = emptyList()
    override suspend fun getSetLogsForExerciseSessions(exerciseSessionIds: List<String>): List<SetLogEntity> = emptyList()
    override suspend fun insertRestTimerLog(log: RestTimerLogEntity) {}
    override suspend fun insertReadinessCheckIn(checkIn: ReadinessCheckInEntity) {}
    override suspend fun insertSmartDayRecommendation(recommendation: SmartDayRecommendationEntity) {}
    override suspend fun insertTimeLimitedWorkoutPlan(plan: TimeLimitedWorkoutPlanEntity) {}
    override suspend fun insertTimeLimitedWorkoutPlanExercises(exercises: List<TimeLimitedWorkoutPlanExerciseEntity>) {}
    override suspend fun getReadinessCheckIn(id: String): ReadinessCheckInEntity? = null
    override suspend fun getSmartDayRecommendation(id: String): SmartDayRecommendationEntity? = null
    override suspend fun getTimeLimitedPlanForTemplate(templateId: String): TimeLimitedWorkoutPlanEntity? = null
    override suspend fun getTimeLimitedPlanExercises(planId: String): List<TimeLimitedWorkoutPlanExerciseEntity> = emptyList()
    override suspend fun insertSetAdvice(advice: SetAdviceEntity) {}
    override suspend fun insertAdaptiveCardioRecommendation(recommendation: AdaptiveCardioRecommendationEntity) {}
    override suspend fun getSetAdviceForSetLog(setLogId: String): SetAdviceEntity? = null
    override suspend fun getSetAdviceForSupersetRound(supersetGroupId: String, roundNumber: Int): SetAdviceEntity? = null
    override suspend fun insertCardioSession(session: CardioSessionEntity) {}
    override fun getAllFinishedWorkoutSessions(): Flow<List<WorkoutSessionEntity>> = emptyFlow()
    override suspend fun getWorkoutSessionById(sessionId: String): WorkoutSessionEntity? = null
    override suspend fun getCardioSessionForWorkout(sessionId: String): CardioSessionEntity? = null
    override suspend fun getHistoricalSetLogsForExercise(exerciseId: String): List<SetLogEntity> = emptyList()
    override suspend fun getRecentExerciseSessionsForExercise(exerciseId: String, limit: Int): List<WorkoutExerciseSessionEntity> = emptyList()
    override suspend fun insertProgressionSuggestion(suggestion: ProgressionSuggestionEntity) {}
    override suspend fun insertProgressionReadinessScore(score: ProgressionReadinessScoreEntity) {}
    override suspend fun getLatestProgressionSuggestion(exerciseId: String): ProgressionSuggestionEntity? = null
    override suspend fun getProgressionSuggestionForExerciseSession(workoutExerciseSessionId: String): ProgressionSuggestionEntity? = null
    override suspend fun getLatestProgressionReadinessScore(exerciseId: String): ProgressionReadinessScoreEntity? = null
    override fun getWorkoutHistoryList(): Flow<List<WorkoutHistoryListItemDto>> = emptyFlow()
    override suspend fun getWorkoutDetail(sessionId: String): WorkoutDetailDto? = null
    override suspend fun getExerciseHistorySessions(exerciseId: String, limit: Int): List<ExerciseHistorySessionDto> = emptyList()
    
    override suspend fun getWorkoutSessionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<WorkoutSessionEntity> = emptyList()
    override suspend fun getSetLogsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<SetLogEntity> = emptyList()
    override suspend fun getCardioSessionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<CardioSessionEntity> = emptyList()
    override suspend fun getProgressionSuggestionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<ProgressionSuggestionEntity> = emptyList()
    override suspend fun updateWorkoutSessionNote(sessionId: String, note: String?) {}
    override suspend fun updateExerciseSessionNote(exerciseSessionId: String, note: String?) {}
    override suspend fun updateSetLogNote(setLogId: String, note: String?) {}
    override fun getCompletedWorkoutsCountFlow(): Flow<Int> = kotlinx.coroutines.flow.flowOf(0)
}
