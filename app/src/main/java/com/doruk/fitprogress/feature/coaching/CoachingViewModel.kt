package com.doruk.fitprogress.feature.coaching

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import com.doruk.fitprogress.core.database.entity.SmartDayRecommendationEntity
import com.doruk.fitprogress.core.domain.usecase.CalculateSmartRecommendationUseCase
import com.doruk.fitprogress.core.domain.usecase.GenerateTimeLimitedPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CoachingViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val calculateSmartRecommendationUseCase: CalculateSmartRecommendationUseCase,
    private val generateTimeLimitedPlanUseCase: GenerateTimeLimitedPlanUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: String = checkNotNull(savedStateHandle["templateId"])

    private val _uiState = MutableStateFlow(CoachingUiState())
    val uiState: StateFlow<CoachingUiState> = _uiState.asStateFlow()

    init {
        loadTemplate()
    }

    private fun loadTemplate() {
        viewModelScope.launch {
            val template = workoutDao.getTemplateWithDetails(templateId)
            _uiState.value = _uiState.value.copy(
                plannedDayCode = template?.template?.dayCode ?: "A",
                isLoading = false
            )
        }
    }

    fun updateCheckInField(field: CheckInField, value: Any) {
        val current = _uiState.value
        _uiState.value = when (field) {
            CheckInField.AVAILABLE_TIME -> current.copy(availableTimeMode = value as String)
            CheckInField.ENERGY -> current.copy(energyLevel = value as Int)
            CheckInField.MOTIVATION -> current.copy(motivationLevel = value as Int)
            CheckInField.SLEEP -> current.copy(sleepQuality = value as Int)
            CheckInField.SORENESS -> current.copy(muscleSoreness = value as Int)
            CheckInField.KNEE -> current.copy(kneeSensitivity = value as String)
            CheckInField.BACK -> current.copy(backDiscomfort = value as String)
            CheckInField.PREVIOUS_HARD -> current.copy(previousWorkoutFeltHard = value as Boolean)
            CheckInField.NOTE -> current.copy(optionalNote = value as String)
        }
    }

    fun submitCheckIn() {
        viewModelScope.launch {
            val state = _uiState.value
            val checkInId = UUID.randomUUID().toString()
            val checkIn = ReadinessCheckInEntity(
                id = checkInId,
                date = System.currentTimeMillis(),
                plannedDayCode = state.plannedDayCode,
                sleepQuality = state.sleepQuality,
                muscleSoreness = state.muscleSoreness,
                energyLevel = state.energyLevel,
                motivationLevel = state.motivationLevel,
                kneeSensitivity = state.kneeSensitivity,
                backDiscomfort = state.backDiscomfort,
                previousWorkoutFeltHard = state.previousWorkoutFeltHard,
                availableTimeMode = state.availableTimeMode,
                notes = state.optionalNote
            )
            
            workoutDao.insertReadinessCheckIn(checkIn)

            val result = calculateSmartRecommendationUseCase(checkIn)
            val loadAdj = if (result.recommendedMode == "LIGHT") 10 else 0
            
            val recId = UUID.randomUUID().toString()
            val rec = SmartDayRecommendationEntity(
                id = recId,
                checkInId = checkInId,
                suggestedWorkoutTemplateId = templateId,
                recommendedMode = result.recommendedMode,
                selectedMode = null,
                loadAdjustmentPercent = loadAdj,
                volumeAdjustment = 0,
                cardioRecommendation = null,
                reason = result.reason,
                accepted = false,
                overriddenByUser = false
            )
            workoutDao.insertSmartDayRecommendation(rec)

            _uiState.value = state.copy(
                currentScreen = CoachingScreen.RECOMMENDATION,
                recommendationId = recId,
                recommendedMode = result.recommendedMode,
                recommendedReason = result.reason
            )
        }
    }

    fun finalizeDecision(decision: DecisionType) {
        if (_uiState.value.isProcessing) return
        
        _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val state = _uiState.value
                val recId = state.recommendationId ?: throw IllegalStateException("Recommendation ID is missing")
                val rec = workoutDao.getSmartDayRecommendation(recId) ?: throw IllegalStateException("Recommendation not found")

                val isOverride = decision == DecisionType.START_ORIGINAL && rec.recommendedMode != "NORMAL"

                // If REST_OR_WALK mode and the user clicks the main CTA (which we will map to a custom decision)
                // Wait, if the user chooses REST_DAY decision, it means they are logging the rest day.
                // We shouldn't start an active workout session.
                
                val updatedRec = rec.copy(
                    accepted = decision == DecisionType.START_RECOMMENDED || decision == DecisionType.REST_DAY,
                    overriddenByUser = isOverride,
                    selectedMode = decision.name
                )
                workoutDao.insertSmartDayRecommendation(updatedRec)

                if (decision == DecisionType.REST_DAY) {
                    _uiState.value = state.copy(navigateOutToDashboard = true, isProcessing = false)
                    return@launch
                }

                if (decision == DecisionType.START_TIME_LIMITED || 
                    (decision == DecisionType.START_RECOMMENDED && (state.availableTimeMode == "45" || state.availableTimeMode == "30"))) {
                    
                    val template = workoutDao.getTemplateWithDetails(templateId) ?: throw IllegalStateException("Template not found")
                    val timeMode = if (decision == DecisionType.START_TIME_LIMITED && state.availableTimeMode == "full") "45" else state.availableTimeMode
                    val (plan, exercises) = generateTimeLimitedPlanUseCase(template, timeMode, updatedRec.recommendedMode == "LIGHT")
                    
                    workoutDao.insertTimeLimitedWorkoutPlan(plan)
                    workoutDao.insertTimeLimitedWorkoutPlanExercises(exercises)
                    
                    _uiState.value = state.copy(navigateOutToActiveWorkout = true, selectedTimeLimitedPlanId = plan.id, isProcessing = false)
                } else {
                    _uiState.value = state.copy(navigateOutToActiveWorkout = true, isProcessing = false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isProcessing = false, errorMessage = "Failed to process decision: ${e.message}") }
            }
        }
    }
}

enum class CoachingScreen { CHECK_IN, RECOMMENDATION }
enum class CheckInField { AVAILABLE_TIME, ENERGY, MOTIVATION, SLEEP, SORENESS, KNEE, BACK, PREVIOUS_HARD, NOTE }
enum class DecisionType { START_RECOMMENDED, START_ORIGINAL, START_TIME_LIMITED, REST_DAY }

data class CoachingUiState(
    val isLoading: Boolean = true,
    val currentScreen: CoachingScreen = CoachingScreen.CHECK_IN,
    val plannedDayCode: String = "A",
    val availableTimeMode: String = "full",
    val energyLevel: Int = 3,
    val motivationLevel: Int = 3,
    val sleepQuality: Int = 3,
    val muscleSoreness: Int = 1,
    val kneeSensitivity: String = "none",
    val backDiscomfort: String = "none",
    val previousWorkoutFeltHard: Boolean = false,
    val optionalNote: String = "",
    val errorMessage: String? = null,
    val isProcessing: Boolean = false,
    val recommendationId: String? = null,
    val recommendedMode: String? = null,
    val recommendedReason: String? = null,
    val navigateOutToActiveWorkout: Boolean = false,
    val selectedTimeLimitedPlanId: String? = null,
    val navigateOutToDashboard: Boolean = false
)
