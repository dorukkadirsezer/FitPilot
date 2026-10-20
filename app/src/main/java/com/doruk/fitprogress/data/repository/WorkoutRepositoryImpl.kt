package com.doruk.fitprogress.data.repository

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutDetailDto
import com.doruk.fitprogress.core.database.entity.WorkoutHistoryListItemDto
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.TemplateWithDetails
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.SmartDayRecommendationEntity
import com.doruk.fitprogress.core.database.entity.TimeLimitedWorkoutPlanExerciseEntity
import com.doruk.fitprogress.core.database.entity.RestTimerLogEntity
import com.doruk.fitprogress.core.database.entity.SetAdviceEntity
import com.doruk.fitprogress.core.database.entity.AdaptiveCardioRecommendationEntity
import com.doruk.fitprogress.core.database.entity.CardioSessionEntity
import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import com.doruk.fitprogress.core.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class WorkoutRepositoryImpl @Inject constructor(
    private val workoutDao: WorkoutDao
) : WorkoutRepository {
    override fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>> = workoutDao.getAllTemplates()
    override suspend fun getAllTemplatesSync(): List<WorkoutTemplateEntity> = workoutDao.getAllTemplatesSync()
    override suspend fun getTemplateByDayCode(code: String): WorkoutTemplateEntity? = workoutDao.getTemplateByDayCode(code)
    override fun getAllExercises(): Flow<List<ExerciseEntity>> = workoutDao.getAllExercises()
    override suspend fun getAllExercisesSync(): List<ExerciseEntity> = workoutDao.getAllExercisesSync()
    override suspend fun getWorkoutDetail(sessionId: String): WorkoutDetailDto? = workoutDao.getWorkoutDetail(sessionId)
    override fun getWorkoutHistoryList(): Flow<List<WorkoutHistoryListItemDto>> = workoutDao.getWorkoutHistoryList()

    override suspend fun getTemplateWithDetails(templateId: String): TemplateWithDetails? = workoutDao.getTemplateWithDetails(templateId)
    override suspend fun getUnfinishedSessionForTemplate(templateId: String): WorkoutSessionEntity? = workoutDao.getUnfinishedSessionForTemplate(templateId)
    override suspend fun getExerciseSessionsForWorkout(sessionId: String): List<WorkoutExerciseSessionEntity> = workoutDao.getExerciseSessionsForWorkout(sessionId)
    override suspend fun getSetLogsForExerciseSessions(exerciseSessionIds: List<String>): List<SetLogEntity> = workoutDao.getSetLogsForExerciseSessions(exerciseSessionIds)
    override suspend fun getSmartDayRecommendation(id: String): SmartDayRecommendationEntity? = workoutDao.getSmartDayRecommendation(id)
    override suspend fun getTimeLimitedPlanExercises(planId: String): List<TimeLimitedWorkoutPlanExerciseEntity> = workoutDao.getTimeLimitedPlanExercises(planId)
    override suspend fun initializeWorkoutSession(session: WorkoutSessionEntity?, exercises: List<WorkoutExerciseSessionEntity>) {
        if (session != null) {
            workoutDao.initializeWorkoutSession(session, exercises)
        }
    }

    override suspend fun updateWorkoutSessionNote(sessionId: String, note: String?) = workoutDao.updateWorkoutSessionNote(sessionId, note)
    override suspend fun updateExerciseSessionNote(exerciseSessionId: String, note: String?) = workoutDao.updateExerciseSessionNote(exerciseSessionId, note)
    override suspend fun updateSetLogNote(setId: String, note: String?) = workoutDao.updateSetLogNote(setId, note)

    override suspend fun insertSetLog(setLog: SetLogEntity) = workoutDao.insertSetLog(setLog)
    override suspend fun insertSetAdvice(advice: SetAdviceEntity) = workoutDao.insertSetAdvice(advice)
    override suspend fun insertRestTimerLog(log: RestTimerLogEntity) = workoutDao.insertRestTimerLog(log)

    override suspend fun updateWorkoutSession(session: WorkoutSessionEntity) = workoutDao.updateWorkoutSession(session)
    override suspend fun getReadinessCheckIn(id: String): ReadinessCheckInEntity? = workoutDao.getReadinessCheckIn(id)
    override suspend fun insertAdaptiveCardioRecommendation(recommendation: AdaptiveCardioRecommendationEntity) = workoutDao.insertAdaptiveCardioRecommendation(recommendation)
    override suspend fun insertCardioSession(session: CardioSessionEntity) = workoutDao.insertCardioSession(session)
}
