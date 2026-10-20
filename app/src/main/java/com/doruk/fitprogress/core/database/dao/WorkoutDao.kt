package com.doruk.fitprogress.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.doruk.fitprogress.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<WorkoutTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplateExercises(exercises: List<WorkoutTemplateExerciseEntity>)

    @Query("DELETE FROM workout_template_exercises WHERE templateId = :templateId")
    suspend fun deleteTemplateExercisesByTemplateId(templateId: String)

    @Query("DELETE FROM workout_templates WHERE isAiProgram = 1")
    suspend fun deleteAiProgramTemplates()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarmupInstructions(instructions: List<TemplateWarmupInstructionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWarmupSets(warmups: List<TemplateWarmupSetEntity>)

    @Query("SELECT * FROM workout_templates")
    fun getAllTemplates(): Flow<List<WorkoutTemplateEntity>>

    @Query("SELECT * FROM workout_templates")
    suspend fun getAllTemplatesSync(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_templates WHERE isAiProgram = 1")
    suspend fun getAiProgramTemplatesSync(): List<WorkoutTemplateEntity>

    @Query("SELECT * FROM workout_templates WHERE dayCode = :code LIMIT 1")
    suspend fun getTemplateByDayCode(code: String): WorkoutTemplateEntity?

    @Query("SELECT * FROM workout_template_exercises WHERE templateId = :templateId ORDER BY orderIndex ASC")
    suspend fun getTemplateExercisesSync(templateId: String): List<WorkoutTemplateExerciseEntity>

    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun getAllExercisesSync(): List<ExerciseEntity>

    // Phase 4 additions
    @Transaction
    @Query("SELECT * FROM workout_templates WHERE id = :templateId LIMIT 1")
    suspend fun getTemplateWithDetails(templateId: String): TemplateWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutSession(session: WorkoutSessionEntity)

    @Update
    suspend fun updateWorkoutSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExerciseSession(session: WorkoutExerciseSessionEntity)

    @Transaction
    suspend fun initializeWorkoutSession(
        session: WorkoutSessionEntity?,
        exerciseSessions: List<WorkoutExerciseSessionEntity>
    ) {
        if (session != null) {
            insertWorkoutSession(session)
        }
        exerciseSessions.forEach { insertWorkoutExerciseSession(it) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetLog(setLog: SetLogEntity)

    @Update
    suspend fun updateSetLog(setLog: SetLogEntity)

    @Query("UPDATE workout_sessions SET note = :note WHERE id = :sessionId")
    suspend fun updateWorkoutSessionNote(sessionId: String, note: String?)

    @Query("UPDATE workout_exercise_sessions SET note = :note WHERE id = :exerciseSessionId")
    suspend fun updateExerciseSessionNote(exerciseSessionId: String, note: String?)

    @Query("UPDATE set_logs SET note = :note WHERE id = :setId")
    suspend fun updateSetLogNote(setId: String, note: String?)

    // Phase 4.5 additions
    @Query("SELECT * FROM workout_sessions WHERE templateId = :templateId AND finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getUnfinishedSessionForTemplate(templateId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM workout_exercise_sessions WHERE workoutSessionId = :sessionId")
    suspend fun getExerciseSessionsForWorkout(sessionId: String): List<WorkoutExerciseSessionEntity>



    @Query("SELECT * FROM set_logs WHERE workoutExerciseSessionId IN (:exerciseSessionIds)")
    suspend fun getSetLogsForExerciseSessions(exerciseSessionIds: List<String>): List<SetLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRestTimerLog(log: RestTimerLogEntity)

    // Phase 5 Additions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadinessCheckIn(checkIn: ReadinessCheckInEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSmartDayRecommendation(recommendation: SmartDayRecommendationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLimitedWorkoutPlan(plan: TimeLimitedWorkoutPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimeLimitedWorkoutPlanExercises(exercises: List<TimeLimitedWorkoutPlanExerciseEntity>)
    
    @Query("SELECT * FROM readiness_check_ins WHERE id = :id LIMIT 1")
    suspend fun getReadinessCheckIn(id: String): ReadinessCheckInEntity?

    @Query("SELECT * FROM smart_day_recommendations WHERE id = :id LIMIT 1")
    suspend fun getSmartDayRecommendation(id: String): SmartDayRecommendationEntity?

    @Query("SELECT * FROM time_limited_workout_plans WHERE originalTemplateId = :templateId LIMIT 1")
    suspend fun getTimeLimitedPlanForTemplate(templateId: String): TimeLimitedWorkoutPlanEntity?

    @Query("SELECT * FROM time_limited_workout_plan_exercises WHERE planId = :planId")
    suspend fun getTimeLimitedPlanExercises(planId: String): List<TimeLimitedWorkoutPlanExerciseEntity>

    // Phase 6 Additions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetAdvice(advice: SetAdviceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdaptiveCardioRecommendation(recommendation: AdaptiveCardioRecommendationEntity)

    @Query("SELECT * FROM set_advices WHERE setLogId = :setLogId LIMIT 1")
    suspend fun getSetAdviceForSetLog(setLogId: String): SetAdviceEntity?

    @Query("SELECT * FROM set_advices WHERE supersetGroupId = :supersetGroupId AND roundNumber = :roundNumber LIMIT 1")
    suspend fun getSetAdviceForSupersetRound(supersetGroupId: String, roundNumber: Int): SetAdviceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardioSession(session: CardioSessionEntity)

    // Phase 7A Additions
    @Query("SELECT * FROM workout_sessions WHERE finishedAt IS NOT NULL ORDER BY COALESCE(finishedAt, startedAt) DESC")
    fun getAllFinishedWorkoutSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getWorkoutSessionById(sessionId: String): WorkoutSessionEntity?

    @Query("SELECT * FROM cardio_sessions WHERE workoutSessionId = :sessionId LIMIT 1")
    suspend fun getCardioSessionForWorkout(sessionId: String): CardioSessionEntity?

    @Query("""
        SELECT sl.* FROM set_logs sl
        INNER JOIN workout_exercise_sessions wes ON sl.workoutExerciseSessionId = wes.id
        INNER JOIN workout_sessions ws ON wes.workoutSessionId = ws.id
        WHERE wes.exerciseId = :exerciseId AND ws.finishedAt IS NOT NULL
        ORDER BY COALESCE(ws.finishedAt, ws.startedAt) ASC, sl.setNumber ASC
    """)
    suspend fun getHistoricalSetLogsForExercise(exerciseId: String): List<SetLogEntity>

    @Query("""
        SELECT wes.* FROM workout_exercise_sessions wes
        INNER JOIN workout_sessions ws ON wes.workoutSessionId = ws.id
        WHERE wes.exerciseId = :exerciseId AND ws.finishedAt IS NOT NULL
        ORDER BY COALESCE(ws.finishedAt, ws.startedAt) DESC
        LIMIT :limit
    """)
    suspend fun getRecentExerciseSessionsForExercise(exerciseId: String, limit: Int): List<WorkoutExerciseSessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressionSuggestion(suggestion: ProgressionSuggestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressionReadinessScore(score: ProgressionReadinessScoreEntity)

    @Query("SELECT * FROM progression_suggestions WHERE exerciseId = :exerciseId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestProgressionSuggestion(exerciseId: String): ProgressionSuggestionEntity?

    @Query("SELECT * FROM progression_suggestions WHERE workoutExerciseSessionId = :workoutExerciseSessionId LIMIT 1")
    suspend fun getProgressionSuggestionForExerciseSession(workoutExerciseSessionId: String): ProgressionSuggestionEntity?

    @Query("SELECT * FROM progression_readiness_scores WHERE exerciseId = :exerciseId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestProgressionReadinessScore(exerciseId: String): ProgressionReadinessScoreEntity?

    // Phase 7A.6 Additions - History DTO Queries
    @Transaction
    @Query("""
        SELECT ws.*, 
               wt.dayCode, wt.name AS workoutName,
               (SELECT COUNT(*) FROM set_logs sl INNER JOIN workout_exercise_sessions wes ON sl.workoutExerciseSessionId = wes.id WHERE wes.workoutSessionId = ws.id AND sl.isSkipped = 1) AS skippedSetCount,
               (SELECT COUNT(*) FROM set_logs sl INNER JOIN workout_exercise_sessions wes ON sl.workoutExerciseSessionId = wes.id WHERE wes.workoutSessionId = ws.id AND sl.completed = 1 AND sl.isSkipped = 0) AS completedSetCount,
               (CASE WHEN 
                  (ws.note IS NOT NULL AND trim(ws.note) != '') OR
                  EXISTS (SELECT 1 FROM workout_exercise_sessions wes WHERE wes.workoutSessionId = ws.id AND wes.note IS NOT NULL AND trim(wes.note) != '') OR
                  EXISTS (SELECT 1 FROM set_logs sl INNER JOIN workout_exercise_sessions wes ON sl.workoutExerciseSessionId = wes.id WHERE wes.workoutSessionId = ws.id AND sl.note IS NOT NULL AND trim(sl.note) != '')
                THEN 1 ELSE 0 END) AS hasNotes,
               (SELECT sdr.selectedMode FROM smart_day_recommendations sdr INNER JOIN readiness_check_ins rci ON sdr.checkInId = rci.id WHERE sdr.suggestedWorkoutTemplateId = ws.templateId ORDER BY rci.date DESC LIMIT 1) AS smartCoachingMode,
               (SELECT sdr.overriddenByUser FROM smart_day_recommendations sdr INNER JOIN readiness_check_ins rci ON sdr.checkInId = rci.id WHERE sdr.suggestedWorkoutTemplateId = ws.templateId ORDER BY rci.date DESC LIMIT 1) AS smartCoachingOverridden
        FROM workout_sessions ws
        LEFT JOIN workout_templates wt ON ws.templateId = wt.id
        WHERE ws.finishedAt IS NOT NULL
        ORDER BY COALESCE(ws.finishedAt, ws.startedAt) DESC
    """)
    fun getWorkoutHistoryList(): Flow<List<WorkoutHistoryListItemDto>>

    @Transaction
    @Query("""
        SELECT ws.*, 
               wt.dayCode, wt.name AS workoutName,
               (SELECT sdr.selectedMode FROM smart_day_recommendations sdr INNER JOIN readiness_check_ins rci ON sdr.checkInId = rci.id WHERE sdr.suggestedWorkoutTemplateId = ws.templateId ORDER BY rci.date DESC LIMIT 1) AS smartCoachingMode,
               (SELECT sdr.overriddenByUser FROM smart_day_recommendations sdr INNER JOIN readiness_check_ins rci ON sdr.checkInId = rci.id WHERE sdr.suggestedWorkoutTemplateId = ws.templateId ORDER BY rci.date DESC LIMIT 1) AS smartCoachingOverridden
        FROM workout_sessions ws
        LEFT JOIN workout_templates wt ON ws.templateId = wt.id
        WHERE ws.id = :sessionId
    """)
    suspend fun getWorkoutDetail(sessionId: String): WorkoutDetailDto?

    @Transaction
    @Query("""
        SELECT wes.*,
               ws.startedAt AS workoutStartedAt,
               ws.finishedAt AS workoutFinishedAt
        FROM workout_exercise_sessions wes
        INNER JOIN workout_sessions ws ON wes.workoutSessionId = ws.id
        WHERE wes.exerciseId = :exerciseId AND ws.finishedAt IS NOT NULL
        ORDER BY COALESCE(ws.finishedAt, ws.startedAt) DESC
        LIMIT :limit
    """)
    suspend fun getExerciseHistorySessions(exerciseId: String, limit: Int): List<ExerciseHistorySessionDto>

    // Phase 8A Analytics Additions
    @Query("SELECT * FROM workout_sessions WHERE finishedAt BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getWorkoutSessionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<WorkoutSessionEntity>

    @Query("SELECT sl.* FROM set_logs sl INNER JOIN workout_exercise_sessions wes ON sl.workoutExerciseSessionId = wes.id INNER JOIN workout_sessions ws ON wes.workoutSessionId = ws.id WHERE ws.finishedAt BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getSetLogsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<SetLogEntity>

    @Query("SELECT * FROM cardio_sessions WHERE date BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getCardioSessionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<CardioSessionEntity>
    
    @Query("SELECT * FROM progression_suggestions WHERE createdAt BETWEEN :startTimestamp AND :endTimestamp")
    suspend fun getProgressionSuggestionsBetweenDates(startTimestamp: Long, endTimestamp: Long): List<ProgressionSuggestionEntity>
}
