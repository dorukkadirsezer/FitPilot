package com.doruk.fitprogress.core.domain.repository

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
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>>
    suspend fun getAllTemplatesSync(): List<WorkoutTemplateEntity>
    suspend fun getTemplateByDayCode(code: String): WorkoutTemplateEntity?
    fun getAllExercises(): Flow<List<ExerciseEntity>>
    suspend fun getAllExercisesSync(): List<ExerciseEntity>
    suspend fun getWorkoutDetail(sessionId: String): WorkoutDetailDto?
    fun getWorkoutHistoryList(): Flow<List<WorkoutHistoryListItemDto>>

    suspend fun getTemplateWithDetails(templateId: String): TemplateWithDetails?
    suspend fun getUnfinishedSessionForTemplate(templateId: String): WorkoutSessionEntity?
    suspend fun getExerciseSessionsForWorkout(sessionId: String): List<WorkoutExerciseSessionEntity>
    suspend fun getSetLogsForExerciseSessions(exerciseSessionIds: List<String>): List<SetLogEntity>
    suspend fun getSmartDayRecommendation(id: String): SmartDayRecommendationEntity?
    suspend fun getTimeLimitedPlanExercises(planId: String): List<TimeLimitedWorkoutPlanExerciseEntity>
    suspend fun initializeWorkoutSession(session: WorkoutSessionEntity?, exercises: List<WorkoutExerciseSessionEntity>)

    suspend fun updateWorkoutSessionNote(sessionId: String, note: String?)
    suspend fun updateExerciseSessionNote(exerciseSessionId: String, note: String?)
    suspend fun updateSetLogNote(setId: String, note: String?)

    suspend fun insertSetLog(setLog: SetLogEntity)
    suspend fun insertSetAdvice(advice: SetAdviceEntity)
    suspend fun insertRestTimerLog(log: RestTimerLogEntity)

    suspend fun updateWorkoutSession(session: WorkoutSessionEntity)
    suspend fun getReadinessCheckIn(id: String): ReadinessCheckInEntity?
    suspend fun insertAdaptiveCardioRecommendation(recommendation: AdaptiveCardioRecommendationEntity)
    suspend fun insertCardioSession(session: CardioSessionEntity)
}
