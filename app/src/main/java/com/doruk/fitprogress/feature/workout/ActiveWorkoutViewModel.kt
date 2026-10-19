package com.doruk.fitprogress.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.RestTimerLogEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.domain.model.ActiveExercise
import com.doruk.fitprogress.core.domain.model.ActiveSet
import com.doruk.fitprogress.core.domain.model.ActiveWarmupInstruction
import com.doruk.fitprogress.core.domain.model.ActiveWorkoutSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

import com.doruk.fitprogress.core.domain.usecase.CalculateSetAdviceUseCase
import com.doruk.fitprogress.core.domain.usecase.CalculateAdaptiveCardioUseCase
import com.doruk.fitprogress.core.domain.usecase.GetProgramRiskRemindersUseCase
import com.doruk.fitprogress.core.database.entity.AdaptiveCardioRecommendationEntity
import com.doruk.fitprogress.core.database.entity.SetAdviceEntity
import com.doruk.fitprogress.core.domain.usecase.AdviceType

import com.doruk.fitprogress.core.ai.AiRepository
import com.doruk.fitprogress.core.datastore.WorkoutSettingsDataStore
import com.doruk.fitprogress.core.util.TimerAlertManager
import kotlinx.coroutines.flow.first

@HiltViewModel
class ActiveWorkoutViewModel @Inject constructor(
    private val dao: WorkoutDao,
    private val savedStateHandle: SavedStateHandle,
    private val getProgramRiskRemindersUseCase: GetProgramRiskRemindersUseCase,
    private val calculateSetAdviceUseCase: CalculateSetAdviceUseCase,
    private val calculateAdaptiveCardioUseCase: CalculateAdaptiveCardioUseCase,
    private val aiRepository: AiRepository,
    private val workoutSettingsDataStore: WorkoutSettingsDataStore,
    private val timerAlertManager: TimerAlertManager
) : ViewModel() {

    private val templateId: String = savedStateHandle.get<String>("templateId") ?: ""

    private val _workoutSession = MutableStateFlow<ActiveWorkoutSession?>(null)
    val workoutSession: StateFlow<ActiveWorkoutSession?> = _workoutSession.asStateFlow()

    private val _restTimerState = MutableStateFlow<RestTimerState?>(null)
    val restTimerState: StateFlow<RestTimerState?> = _restTimerState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _cardioRecommendation = MutableStateFlow<AdaptiveCardioRecommendationEntity?>(null)
    val cardioRecommendation: StateFlow<AdaptiveCardioRecommendationEntity?> = _cardioRecommendation.asStateFlow()
    
    val timerSoundEnabled: StateFlow<Boolean> = workoutSettingsDataStore.timerSoundEnabled.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
    val timerWarningEnabled: StateFlow<Boolean> = workoutSettingsDataStore.timerWarningEnabled.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
    val timerFinishedEnabled: StateFlow<Boolean> = workoutSettingsDataStore.timerFinishedEnabled.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
    val timerVibrationEnabled: StateFlow<Boolean> = workoutSettingsDataStore.timerVibrationEnabled.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), true)
    val timerSoundStyle: StateFlow<com.doruk.fitprogress.core.domain.model.TimerSoundStyle> = workoutSettingsDataStore.timerSoundStyle.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP)

    private var timerJob: Job? = null
    private var hasPlayedWarningAlert = false

    init {
        loadWorkout()
    }

    private fun loadWorkout() {
        if (templateId.isBlank()) {
            _errorMessage.value = "Workout template not provided."
            return
        }

        viewModelScope.launch {
            try {
                val templateDetails = dao.getTemplateWithDetails(templateId)
                if (templateDetails == null) {
                    _errorMessage.value = "Failed to load workout details."
                    return@launch
                }
                
                val unfinishedSession = dao.getUnfinishedSessionForTemplate(templateId)

                val sessionId = unfinishedSession?.id ?: UUID.randomUUID().toString()
                val sessionStartedAt = unfinishedSession?.startedAt ?: System.currentTimeMillis()

                // Collect entities for atomic initialization
                var sessionToInsert: WorkoutSessionEntity? = null
                val exercisesToInsert = mutableListOf<WorkoutExerciseSessionEntity>()

                if (unfinishedSession == null) {
                    sessionToInsert = WorkoutSessionEntity(
                        id = sessionId,
                        templateId = templateId,
                        startedAt = sessionStartedAt,
                        finishedAt = null,
                        durationSeconds = 0,
                        totalWorkingVolumeKg = 0f,
                        averageRpe = null,
                        cardioCompleted = false,
                        note = null
                    )
                }
                
                // 1. Map Warmups
                val warmups = templateDetails.warmupInstructions.sortedBy { it.orderIndex }.map {
                    ActiveWarmupInstruction(
                        id = it.id,
                        description = it.description,
                        isRequired = it.isRequired,
                        durationMinutes = it.durationMinutes
                    )
                }

                // 2. Fetch existing session data if resuming
                val existingExerciseSessions = if (unfinishedSession != null) {
                    dao.getExerciseSessionsForWorkout(sessionId).associateBy { it.exerciseId }
                } else emptyMap()

                val existingSets = if (existingExerciseSessions.isNotEmpty()) {
                    dao.getSetLogsForExerciseSessions(existingExerciseSessions.values.map { it.id })
                        .groupBy { it.workoutExerciseSessionId }
                } else emptyMap()

                // 3. Map Exercises and Sets
                val recId: String? = savedStateHandle.get("recId")
                val planId: String? = savedStateHandle.get("planId")

                val recommendation = if (recId != null) dao.getSmartDayRecommendation(recId) else null
                val timeLimitedExercises = if (planId != null) dao.getTimeLimitedPlanExercises(planId).associateBy { it.exerciseId } else null

                val isLightMode = recommendation?.recommendedMode == "LIGHT" && recommendation.overriddenByUser == false
                val isControlledMode = recommendation?.recommendedMode == "CONTROLLED" && recommendation.overriddenByUser == false

                val activeExercises = templateDetails.exercises
                    .filter { ex ->
                        // Time-limited filter
                        timeLimitedExercises?.get(ex.exercise.id)?.status != "skipped"
                    }
                    .sortedBy { it.templateExercise.orderIndex }
                    .map { exDetails ->
                    val existingExSession = existingExerciseSessions[exDetails.exercise.id]
                    val exerciseSessionId = existingExSession?.id ?: UUID.randomUUID().toString()
                    
                    // Queue Exercise Session to DB incrementally if it's new
                    if (existingExSession == null) {
                        exercisesToInsert.add(
                            WorkoutExerciseSessionEntity(
                                id = exerciseSessionId,
                                workoutSessionId = sessionId,
                                exerciseId = exDetails.exercise.id,
                                orderIndex = exDetails.templateExercise.orderIndex,
                                supersetGroupId = exDetails.templateExercise.supersetGroupId,
                                note = null
                            )
                        )
                    }

                    val activeSets = mutableListOf<ActiveSet>()
                    val existingSetsForEx = existingSets[exerciseSessionId]?.associateBy { it.setNumber } ?: emptyMap()
                    
                    // Map Warmup Sets
                    exDetails.warmupSets.sortedBy { it.setNumber }.forEach { warmupSet ->
                        val existingSet = existingSetsForEx[warmupSet.setNumber]
                        activeSets.add(
                            ActiveSet(
                                id = existingSet?.id ?: UUID.randomUUID().toString(),
                                setNumber = warmupSet.setNumber,
                                isWarmup = true,
                                warmupLabel = warmupSet.targetWeightLabel ?: "Warmup",
                                targetRepsMin = warmupSet.targetReps,
                                targetRepsMax = warmupSet.targetReps,
                                targetWeightMin = warmupSet.targetWeightValue,
                                targetWeightMax = warmupSet.targetWeightValue,
                                targetRpeMin = null,
                                targetRpeMax = null,
                                unit = warmupSet.targetWeightUnit ?: "lbs",
                                loggedReps = existingSet?.actualReps,
                                loggedWeight = existingSet?.actualWeight,
                                loggedRpe = existingSet?.rpe,
                                isCompleted = existingSet?.completed ?: false,
                                isSkipped = existingSet?.isSkipped ?: false,
                                adviceText = null,
                                adviceType = null,
                                note = existingSet?.note
                            )
                        )
                    }

                    // Map Working Sets
                    val startSetNum = if (activeSets.isEmpty()) 1 else activeSets.maxOf { it.setNumber } + 1
                    
                    // Light mode set reduction logic: reduce sets by 1 for accessories (orderIndex > 2)
                    val isAccessory = exDetails.templateExercise.orderIndex > 2
                    val targetSets = if (isLightMode && isAccessory && exDetails.templateExercise.targetSets > 1) {
                        exDetails.templateExercise.targetSets - 1
                    } else {
                        exDetails.templateExercise.targetSets
                    }

                    for (i in 0 until targetSets) {
                        val setNum = startSetNum + i
                        val existingSet = existingSetsForEx[setNum]
                        
                        // Light mode load reduction
                        val weightModifier = if (isLightMode) 0.90f else 1f
                        val targetWeightMin = exDetails.templateExercise.targetWeightMin?.let { it * weightModifier }
                        val targetWeightMax = exDetails.templateExercise.targetWeightMax?.let { it * weightModifier }

                        activeSets.add(
                            ActiveSet(
                                id = existingSet?.id ?: UUID.randomUUID().toString(),
                                setNumber = setNum,
                                isWarmup = false,
                                warmupLabel = null,
                                targetRepsMin = exDetails.templateExercise.targetRepsMin,
                                targetRepsMax = exDetails.templateExercise.targetRepsMax,
                                targetWeightMin = targetWeightMin,
                                targetWeightMax = targetWeightMax,
                                targetRpeMin = exDetails.templateExercise.rpeMin,
                                targetRpeMax = exDetails.templateExercise.rpeMax,
                                unit = exDetails.templateExercise.weightUnit,
                                loggedReps = existingSet?.actualReps,
                                loggedWeight = existingSet?.actualWeight,
                                loggedRpe = existingSet?.rpe,
                                isCompleted = existingSet?.completed ?: false,
                                isSkipped = existingSet?.isSkipped ?: false,
                                adviceText = null,
                                adviceType = null,
                                note = existingSet?.note
                            )
                        )
                    }

                      val reminders = getProgramRiskRemindersUseCase(
                          exercise = exDetails.exercise,
                          templateExercise = exDetails.templateExercise,
                          dayCode = templateDetails.template.dayCode
                      ).toMutableList()
                      
                      if (isControlledMode) {
                          reminders.add("REMINDER_CONTROLLED_MODE")
                      }

                    ActiveExercise(
                        exerciseSessionId = exerciseSessionId,
                        templateExerciseId = exDetails.templateExercise.id,
                        exerciseId = exDetails.exercise.id,
                        name = exDetails.exercise.name,
                        muscleGroup = exDetails.exercise.muscleGroup,
                        coachNote = exDetails.exercise.coachNote,
                        programRiskReminders = reminders,
                        requiresStraps = exDetails.exercise.requiresStraps,
                        orderIndex = exDetails.templateExercise.orderIndex,
                        isSuperset = exDetails.templateExercise.isSuperset,
                        supersetGroupId = exDetails.templateExercise.supersetGroupId,
                        supersetRoundCount = exDetails.templateExercise.supersetRoundCount,
                        supersetAdviceText = null,
                        defaultRestSeconds = exDetails.templateExercise.restSeconds,
                        sets = activeSets,
                        note = existingExSession?.note
                    )
                }

                // Execute atomic initialization
                dao.initializeWorkoutSession(sessionToInsert, exercisesToInsert)

                val session = ActiveWorkoutSession(
                    sessionId = sessionId,
                    templateId = templateId,
                    name = templateDetails.template.name,
                    dayFocus = templateDetails.template.dayFocus,
                    startedAt = sessionStartedAt,
                    warmups = warmups,
                    exercises = activeExercises,
                    note = unfinishedSession?.note
                )

                _workoutSession.value = session
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "An error occurred while loading the workout: ${e.message}"
            }
        }
    }

    fun updateSet(exerciseSessionId: String, setId: String, weight: Float?, reps: Int?, rpe: Float?) {
        val current = _workoutSession.value ?: return
        val updatedExercises = current.exercises.map { ex ->
            if (ex.exerciseSessionId == exerciseSessionId) {
                ex.copy(sets = ex.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(loggedWeight = weight, loggedReps = reps, loggedRpe = rpe)
                    } else set
                })
            } else ex
        }
        _workoutSession.value = current.copy(exercises = updatedExercises)
    }

    fun updateWorkoutNote(note: String?) {
        val current = _workoutSession.value ?: return
        _workoutSession.value = current.copy(note = note)
        viewModelScope.launch {
            dao.updateWorkoutSessionNote(current.sessionId, note)
        }
    }

    fun updateExerciseNote(exerciseSessionId: String, note: String?) {
        val current = _workoutSession.value ?: return
        val updatedExercises = current.exercises.map { ex ->
            if (ex.exerciseSessionId == exerciseSessionId) {
                ex.copy(note = note)
            } else ex
        }
        _workoutSession.value = current.copy(exercises = updatedExercises)
        viewModelScope.launch {
            dao.updateExerciseSessionNote(exerciseSessionId, note)
        }
    }

    fun updateSetNote(exerciseSessionId: String, setId: String, note: String?) {
        val current = _workoutSession.value ?: return
        val updatedExercises = current.exercises.map { ex ->
            if (ex.exerciseSessionId == exerciseSessionId) {
                ex.copy(sets = ex.sets.map { set ->
                    if (set.id == setId) {
                        set.copy(note = note)
                    } else set
                })
            } else ex
        }
        _workoutSession.value = current.copy(exercises = updatedExercises)
        viewModelScope.launch {
            dao.updateSetLogNote(setId, note)
        }
    }

    private val processingSets = mutableSetOf<String>()

    fun toggleSetCompletion(exerciseSessionId: String, setId: String) {
        if (processingSets.contains(setId)) return
        
        viewModelScope.launch {
            processingSets.add(setId)
            try {
                val current = _workoutSession.value ?: return@launch
                var completedSet: ActiveSet? = null
                var parentExercise: ActiveExercise? = null
                var finalAdviceType: String? = null
                var finalAdviceText: String? = null
                var updatedSupersetAdviceText: String? = null

                // Pre-calculate target sets to process
                val targetExIndex = current.exercises.indexOfFirst { it.exerciseSessionId == exerciseSessionId }
                if (targetExIndex == -1) return@launch
                val targetEx = current.exercises[targetExIndex]
                
                val targetSetIndex = targetEx.sets.indexOfFirst { it.id == setId }
                if (targetSetIndex == -1) return@launch
                val set = targetEx.sets[targetSetIndex]
                
                // If it's already completed, we just toggle it off (un-complete it)
                val newCompleted = !set.isCompleted
                val updated = set.copy(isCompleted = newCompleted, isSkipped = false)

                val setLog = SetLogEntity(
                    id = updated.id,
                    workoutExerciseSessionId = exerciseSessionId,
                    setNumber = updated.setNumber,
                    setType = if (updated.isWarmup) "WARMUP" else "WORKING",
                    targetWeight = updated.targetWeightMin,
                    actualWeight = updated.loggedWeight ?: updated.targetWeightMin,
                    weightUnit = updated.unit,
                    targetReps = updated.targetRepsMin,
                    actualReps = updated.loggedReps ?: updated.targetRepsMin,
                    rpe = updated.loggedRpe ?: updated.targetRpeMin,
                    completed = updated.isCompleted,
                    isSkipped = updated.isSkipped,
                    note = updated.note
                )

                var setAdviceToInsert: SetAdviceEntity? = null
                
                // Calculate Advice if completing a working set
                if (newCompleted) {
                    completedSet = updated
                    parentExercise = targetEx
                    
                    if (!updated.isWarmup) {
                        val previousSetNum = updated.setNumber - 1
                        val previousWorkingSet = if (previousSetNum > 0) {
                            dao.getSetLogsForExerciseSessions(listOf(exerciseSessionId))
                                .find { it.setNumber == previousSetNum && it.setType == "WORKING" }
                        } else null

                        val adviceResult = calculateSetAdviceUseCase(
                            currentSet = setLog,
                            previousWorkingSet = previousWorkingSet,
                            targetRepsMin = updated.targetRepsMin,
                            targetRepsMax = updated.targetRepsMax,
                            targetRpe = updated.targetRpeMin
                        )

                        if (adviceResult.type != AdviceType.NO_ADVICE) {
                            finalAdviceType = adviceResult.type.name
                            finalAdviceText = adviceResult.text
                            setAdviceToInsert = SetAdviceEntity(
                                id = UUID.randomUUID().toString(),
                                setLogId = setLog.id,
                                supersetGroupId = null,
                                roundNumber = null,
                                adviceText = adviceResult.text,
                                type = adviceResult.type.name,
                                shownToUser = false
                            )
                        }
                    }
                }
                
                // Persist to DB: SetLog is the critical source of truth
                dao.insertSetLog(setLog)
                
                // SetAdvice is non-critical. If it fails, SetLog should still be saved.
                if (setAdviceToInsert != null) {
                    try {
                        dao.insertSetAdvice(setAdviceToInsert)
                    } catch (e: Exception) {
                        // Ignore advice insertion failures so they don't block the workout flow
                    }
                }

                val fullyUpdatedSet = updated.copy(adviceType = finalAdviceType, adviceText = finalAdviceText)
                if (newCompleted) {
                    completedSet = fullyUpdatedSet
                }

                // Superset Advice logic (done after DB transaction for safety)
                updatedSupersetAdviceText = targetEx.supersetAdviceText
                if (targetEx.isSuperset && targetEx.supersetGroupId != null && completedSet != null && !completedSet.isWarmup) {
                    val roundNum = completedSet.setNumber
                    val supersetExercises = current.exercises.filter { it.supersetGroupId == targetEx.supersetGroupId }
                    
                    // We simulate the update to check if all in round are completed
                    val allCompleted = supersetExercises.all { supEx ->
                        if (supEx.exerciseSessionId == exerciseSessionId) {
                            val simulatedSets = supEx.sets.toMutableList()
                            simulatedSets[targetSetIndex] = fullyUpdatedSet
                            simulatedSets.find { it.setNumber == roundNum }?.isCompleted == true
                        } else {
                            supEx.sets.find { it.setNumber == roundNum }?.isCompleted == true
                        }
                    }
                    
                    if (allCompleted) {
                        val exerciseSessionIds = supersetExercises.map { it.exerciseSessionId }
                        val allSetLogs = dao.getSetLogsForExerciseSessions(exerciseSessionIds)
                        val setsInRound = allSetLogs.filter { it.setNumber == roundNum && it.setType == "WORKING" }
                        
                        val targetRepsMinList = supersetExercises.map { supEx -> 
                            if (supEx.exerciseSessionId == exerciseSessionId) {
                                fullyUpdatedSet.targetRepsMin
                            } else {
                                supEx.sets.find { it.setNumber == roundNum }?.targetRepsMin
                            }
                        }
                        val targetRpeList = supersetExercises.map { supEx -> 
                            if (supEx.exerciseSessionId == exerciseSessionId) {
                                fullyUpdatedSet.targetRpeMin
                            } else {
                                supEx.sets.find { it.setNumber == roundNum }?.targetRpeMin
                            }
                        }

                        val roundAdvice = calculateSetAdviceUseCase.calculateForSupersetRound(
                            setsInRound = setsInRound,
                            targetRepsMinList = targetRepsMinList,
                            targetRpeList = targetRpeList
                        )
                        
                        if (roundAdvice.type != AdviceType.NO_ADVICE) {
                            updatedSupersetAdviceText = roundAdvice.text
                            // Non-critical insert for superset advice
                            try {
                                dao.insertSetAdvice(
                                    SetAdviceEntity(
                                        id = UUID.randomUUID().toString(),
                                        setLogId = null,
                                        supersetGroupId = targetEx.supersetGroupId,
                                        roundNumber = roundNum,
                                        adviceText = roundAdvice.text,
                                        type = roundAdvice.type.name,
                                        shownToUser = false
                                    )
                                )
                            } catch (e: Exception) {
                                // Ignore failure on superset advice
                            }
                        }
                    }
                }

                // Update UI state
                val updatedExercises = current.exercises.toMutableList()
                val updatedSets = targetEx.sets.toMutableList()
                updatedSets[targetSetIndex] = fullyUpdatedSet
                updatedExercises[targetExIndex] = targetEx.copy(sets = updatedSets, supersetAdviceText = updatedSupersetAdviceText)

                _workoutSession.value = current.copy(exercises = updatedExercises)

                // Trigger Rest Timer if we just completed it
                completedSet?.let { set ->
                    parentExercise?.let { ex ->
                        triggerRestTimerIfNeeded(current, ex, set)
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save set. Please try again."
            } finally {
                processingSets.remove(setId)
            }
        }
    }

    private fun triggerRestTimerIfNeeded(session: ActiveWorkoutSession, completedExercise: ActiveExercise, completedSet: ActiveSet) {
        if (completedSet.isWarmup) {
            startTimer(
                durationSeconds = 60,
                setLogId = completedSet.id,
                supersetGroupId = null,
                roundNumber = null
            )
            return
        }

        if (completedExercise.isSuperset && completedExercise.supersetGroupId != null) {
            val supersetExercises = session.exercises.filter { it.supersetGroupId == completedExercise.supersetGroupId }
            val roundNum = completedSet.setNumber
            
            val allCompleted = supersetExercises.all { supEx ->
                supEx.sets.find { it.setNumber == roundNum }?.isCompleted == true
            }

            if (allCompleted) {
                startTimer(
                    durationSeconds = completedExercise.defaultRestSeconds,
                    setLogId = null,
                    supersetGroupId = completedExercise.supersetGroupId,
                    roundNumber = roundNum
                )
            }
        } else {
            startTimer(
                durationSeconds = completedExercise.defaultRestSeconds,
                setLogId = completedSet.id,
                supersetGroupId = null,
                roundNumber = null
            )
        }
    }

    private fun startTimer(durationSeconds: Int, setLogId: String?, supersetGroupId: String?, roundNumber: Int?) {
        timerJob?.cancel()
        
        // Log unfinished timer first if one was active
        _restTimerState.value?.let { currentTimer ->
            persistTimerLog(currentTimer, skipped = true)
        }

        val startTimeMillis = System.currentTimeMillis()
        val endTimeMillis = startTimeMillis + (durationSeconds * 1000L)
        val newState = RestTimerState(
            id = UUID.randomUUID().toString(),
            setLogId = setLogId,
            supersetGroupId = supersetGroupId,
            roundNumber = roundNumber,
            startTimeMillis = startTimeMillis,
            endTimeMillis = endTimeMillis,
            totalDurationSeconds = durationSeconds,
            remainingSeconds = durationSeconds
        )
        _restTimerState.value = newState

        timerJob = viewModelScope.launch {
            hasPlayedWarningAlert = false
            
            // Read settings once per timer
            val soundEnabled = workoutSettingsDataStore.timerSoundEnabled.first()
            val warningEnabled = workoutSettingsDataStore.timerWarningEnabled.first()
            val finishedEnabled = workoutSettingsDataStore.timerFinishedEnabled.first()
            val vibrationEnabled = workoutSettingsDataStore.timerVibrationEnabled.first()

            while (true) {
                val current = _restTimerState.value ?: break
                val remaining = ((current.endTimeMillis - System.currentTimeMillis()) / 1000).toInt()
                
                if (remaining <= 30 && !hasPlayedWarningAlert && warningEnabled) {
                    val style = timerSoundStyle.value
                    timerAlertManager.playWarningAlert(soundEnabled, vibrationEnabled, style)
                    hasPlayedWarningAlert = true
                }
                
                if (remaining <= 0) {
                    if (finishedEnabled) {
                        val style = timerSoundStyle.value
                        timerAlertManager.playFinishedAlert(soundEnabled, vibrationEnabled, style)
                    }
                    persistTimerLog(current, skipped = false)
                    _restTimerState.value = null
                    break
                }
                _restTimerState.value = current.copy(remainingSeconds = remaining)
                delay(1000)
            }
        }
    }

    private fun persistTimerLog(state: RestTimerState, skipped: Boolean) {
        viewModelScope.launch {
            val actualRestSeconds = ((System.currentTimeMillis() - state.startTimeMillis) / 1000).toInt()
            dao.insertRestTimerLog(
                RestTimerLogEntity(
                    id = state.id,
                    setLogId = state.setLogId,
                    supersetGroupId = state.supersetGroupId,
                    roundNumber = state.roundNumber,
                    plannedRestSeconds = state.totalDurationSeconds,
                    actualRestSeconds = actualRestSeconds,
                    skipped = skipped
                )
            )
        }
    }

    fun setTimerSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            workoutSettingsDataStore.setTimerSoundEnabled(enabled)
        }
    }

    fun setTimerWarningEnabled(enabled: Boolean) {
        viewModelScope.launch {
            workoutSettingsDataStore.setTimerWarningEnabled(enabled)
        }
    }

    fun setTimerFinishedEnabled(enabled: Boolean) {
        viewModelScope.launch { workoutSettingsDataStore.setTimerFinishedEnabled(enabled) }
    }

    fun setTimerVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { workoutSettingsDataStore.setTimerVibrationEnabled(enabled) }
    }

    fun setTimerSoundStyle(style: com.doruk.fitprogress.core.domain.model.TimerSoundStyle) {
        viewModelScope.launch { workoutSettingsDataStore.setTimerSoundStyle(style) }
    }

    fun testTimerSound() {
        viewModelScope.launch {
            val soundEnabled = timerSoundEnabled.value
            val vibrationEnabled = timerVibrationEnabled.value
            val style = timerSoundStyle.value
            timerAlertManager.playFinishedAlert(soundEnabled, vibrationEnabled, style)
        }
    }

    fun dismissTimer() {
        timerJob?.cancel()
        _restTimerState.value?.let {
            persistTimerLog(it, skipped = true)
        }
        _restTimerState.value = null
    }

    fun addTimerSeconds(secondsToAdd: Int) {
        val current = _restTimerState.value ?: return
        val newEndTime = current.endTimeMillis + (secondsToAdd * 1000L)
        _restTimerState.value = current.copy(
            endTimeMillis = newEndTime,
            totalDurationSeconds = current.totalDurationSeconds + secondsToAdd,
            remainingSeconds = current.remainingSeconds + secondsToAdd
        )
    }

    fun subtractTimerSeconds(secondsToSubtract: Int) {
        val current = _restTimerState.value ?: return
        val newEndTime = current.endTimeMillis - (secondsToSubtract * 1000L)
        val newRemaining = ((newEndTime - System.currentTimeMillis()) / 1000).toInt()
        
        if (newRemaining > 0) {
            _restTimerState.value = current.copy(
                endTimeMillis = newEndTime,
                totalDurationSeconds = (current.totalDurationSeconds - secondsToSubtract).coerceAtLeast(0),
                remainingSeconds = newRemaining
            )
        } else {
            dismissTimer()
        }
    }

    fun hasIncompleteSets(): Boolean {
        val current = _workoutSession.value ?: return false
        return current.exercises.any { ex ->
            ex.sets.any { !it.isCompleted && !it.isSkipped && !it.isWarmup }
        }
    }

    fun finishWorkout(skipIncomplete: Boolean = false) {
        dismissTimer()
        viewModelScope.launch {
            val current = _workoutSession.value ?: return@launch
            
            var totalVolumeKg = 0f
            var totalRpe = 0f
            var workingSetCount = 0

            val finalExercises = current.exercises.map { ex ->
                val finalSets = ex.sets.map { set ->
                    if (!set.isCompleted && !set.isWarmup) {
                        if (skipIncomplete) {
                            val skippedSet = set.copy(isSkipped = true)
                            dao.insertSetLog(
                                SetLogEntity(
                                    id = skippedSet.id,
                                    workoutExerciseSessionId = ex.exerciseSessionId,
                                    setNumber = skippedSet.setNumber,
                                    setType = "WORKING",
                                    targetWeight = skippedSet.targetWeightMin,
                                    actualWeight = skippedSet.loggedWeight,
                                    weightUnit = skippedSet.unit,
                                    targetReps = skippedSet.targetRepsMin,
                                    actualReps = skippedSet.loggedReps,
                                    rpe = skippedSet.loggedRpe,
                                    completed = false,
                                    isSkipped = true,
                                    note = skippedSet.note
                                )
                            )
                            skippedSet
                        } else set
                    } else {
                        if (!set.isWarmup && set.isCompleted && !set.isSkipped) {
                            val weight = set.loggedWeight ?: set.targetWeightMin ?: 0f
                            val reps = set.loggedReps ?: set.targetRepsMin
                            val unit = set.unit.lowercase()
                            
                            val weightInKg = if (unit == "lbs") weight * 0.453592f else weight
                            totalVolumeKg += (weightInKg * reps)
                            
                            val rpe = set.loggedRpe ?: set.targetRpeMin
                            if (rpe != null) {
                                totalRpe += rpe
                                workingSetCount++
                            }
                        }
                        set
                    }
                }
                ex.copy(sets = finalSets)
            }

            val avgRpe = if (workingSetCount > 0) totalRpe / workingSetCount else null
            val finishedAt = System.currentTimeMillis()
            val duration = (finishedAt - current.startedAt) / 1000

            val sessionEntity = WorkoutSessionEntity(
                id = current.sessionId,
                templateId = current.templateId,
                startedAt = current.startedAt,
                finishedAt = finishedAt,
                durationSeconds = duration,
                totalWorkingVolumeKg = totalVolumeKg,
                averageRpe = avgRpe,
                cardioCompleted = false,
                note = current.note
            )
            dao.updateWorkoutSession(sessionEntity)

            val templateDetails = dao.getTemplateWithDetails(current.templateId)
            
            val recId: String? = savedStateHandle.get("recId")
            val smartRecommendation = if (recId != null) dao.getSmartDayRecommendation(recId) else null
            val checkIn = if (smartRecommendation != null) dao.getReadinessCheckIn(smartRecommendation.checkInId) else null

            // Calculate Adaptive Cardio
            val cardioRec = calculateAdaptiveCardioUseCase(
                session = sessionEntity,
                checkIn = checkIn,
                smartRecommendation = smartRecommendation,
                averageRpe = avgRpe ?: 0f,
                skippedSetsCount = current.exercises.sumOf { it.sets.count { set -> set.isSkipped } }
            )

            dao.insertAdaptiveCardioRecommendation(cardioRec)
            _cardioRecommendation.value = cardioRec

            // Do not clear the workout session here yet, wait until cardio is handled
        }
    }

    private val _aiWorkoutSummary = MutableStateFlow<String?>(null)
    val aiWorkoutSummary: StateFlow<String?> = _aiWorkoutSummary.asStateFlow()

    private val _showWorkoutCompleteDialog = MutableStateFlow(false)
    val showWorkoutCompleteDialog: StateFlow<Boolean> = _showWorkoutCompleteDialog.asStateFlow()

    fun completeCardioAndClose(durationMinutes: Int, cardioType: String, skipped: Boolean = false) {
        viewModelScope.launch {
            val current = _workoutSession.value ?: return@launch
            
            if (!skipped) {
                dao.insertCardioSession(
                    com.doruk.fitprogress.core.database.entity.CardioSessionEntity(
                        id = UUID.randomUUID().toString(),
                        workoutSessionId = current.sessionId,
                        date = System.currentTimeMillis(),
                        cardioType = cardioType,
                        durationMinutes = durationMinutes,
                        perceivedExertion = null,
                        notes = null
                    )
                )
            }
            // No automatic AI generation here! AI summary is only run manually from WorkoutDetailScreen.
            _workoutSession.value = null
            _cardioRecommendation.value = null
            _showWorkoutCompleteDialog.value = true
        }
    }

    fun dismissWorkoutCompleteAndClose(onClose: () -> Unit) {
        _workoutSession.value = null
        _cardioRecommendation.value = null
        _showWorkoutCompleteDialog.value = false
        onClose()
    }

    fun skipCardioAndClose() {
        completeCardioAndClose(0, "NONE", skipped = true)
    }

    override fun onCleared() {
        super.onCleared()
        timerAlertManager.release()
    }
}

data class RestTimerState(
    val id: String,
    val setLogId: String?,
    val supersetGroupId: String?,
    val roundNumber: Int?,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val totalDurationSeconds: Int,
    val remainingSeconds: Int
)
