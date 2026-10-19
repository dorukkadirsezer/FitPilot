package com.doruk.fitprogress.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ProgressionReadinessScoreEntity
import com.doruk.fitprogress.core.database.entity.ProgressionSuggestionEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import com.doruk.fitprogress.core.domain.usecase.CalculateProgressionReadinessUseCase
import com.doruk.fitprogress.core.domain.usecase.CalculateProgressionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.doruk.fitprogress.core.ai.AiRepository
import com.doruk.fitprogress.core.ai.AiProgressionExplanationInput

@HiltViewModel
class ExerciseHistoryViewModel @Inject constructor(
    private val dao: WorkoutDao,
    private val calculateProgressionUseCase: CalculateProgressionUseCase,
    private val calculateProgressionReadinessUseCase: CalculateProgressionReadinessUseCase,
    private val aiRepository: AiRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val exerciseId: String = checkNotNull(savedStateHandle["exerciseId"])

    private val _historySessions = MutableStateFlow<List<com.doruk.fitprogress.core.database.entity.ExerciseHistorySessionDto>>(emptyList())
    val historySessions: StateFlow<List<com.doruk.fitprogress.core.database.entity.ExerciseHistorySessionDto>> = _historySessions.asStateFlow()

    private val _suggestion = MutableStateFlow<ProgressionSuggestionEntity?>(null)
    val suggestion: StateFlow<ProgressionSuggestionEntity?> = _suggestion.asStateFlow()

    private val _readinessScore = MutableStateFlow<ProgressionReadinessScoreEntity?>(null)
    val readinessScore: StateFlow<ProgressionReadinessScoreEntity?> = _readinessScore.asStateFlow()

    private val _exercise = MutableStateFlow<com.doruk.fitprogress.core.database.entity.ExerciseEntity?>(null)
    val exercise: StateFlow<com.doruk.fitprogress.core.database.entity.ExerciseEntity?> = _exercise.asStateFlow()

    private val _aiExplanation = MutableStateFlow<String?>(null)
    val aiExplanation: StateFlow<String?> = _aiExplanation.asStateFlow()
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    
    val isAiEnabledFlow = aiRepository.isAiEnabledFlow

    init {
        loadHistory()
    }

    private fun loadHistory() {
        viewModelScope.launch {
            dao.getAllExercises().collect { list ->
                _exercise.value = list.find { it.id == exerciseId }
            }
        }
        viewModelScope.launch {
            val sessions = dao.getExerciseHistorySessions(exerciseId, limit = 10)
            _historySessions.value = sessions

            // Fetch the latest progression metrics
            _suggestion.value = dao.getLatestProgressionSuggestion(exerciseId)
            _readinessScore.value = dao.getLatestProgressionReadinessScore(exerciseId)
        }
    }

    fun explainProgression() {
        viewModelScope.launch {
            if (!aiRepository.isAiEnabled()) return@launch
            
            _isAiLoading.value = true
            
            val ex = _exercise.value
            val sug = _suggestion.value
            val ready = _readinessScore.value
            
            val historyStrings = _historySessions.value.take(3).map { dto ->
                "Date: \${dto.workoutStartedAt}, Sets: " + dto.sets.joinToString { 
                    "\${it.setLog.actualWeight}x\${it.setLog.actualReps} @ RPE \${it.setLog.rpe}" 
                }
            }

            val input = AiProgressionExplanationInput(
                exerciseName = ex?.name ?: "Unknown Exercise",
                suggestion = sug?.suggestionType ?: "NONE",
                reasoning = ready?.factors ?: "No factors",
                history = historyStrings
            )
            
            val result = aiRepository.explainProgression(input)
            if (result.isSuccess) {
                _aiExplanation.value = result.getOrNull()
            } else {
                _aiExplanation.value = "Failed to load explanation: \${result.exceptionOrNull()?.message}"
            }
            
            _isAiLoading.value = false
        }
    }
}
