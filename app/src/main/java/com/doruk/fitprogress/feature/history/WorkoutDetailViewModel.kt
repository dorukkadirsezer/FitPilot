package com.doruk.fitprogress.feature.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.ai.AiRepository
import com.doruk.fitprogress.core.ai.AiWorkoutSummaryInput
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutDetailDto
import com.doruk.fitprogress.core.domain.usecase.ExportSessionToJsonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutDetailViewModel @Inject constructor(
    private val dao: WorkoutDao,
    private val aiRepository: AiRepository,
    private val exportSessionToJsonUseCase: ExportSessionToJsonUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _detail = MutableStateFlow<WorkoutDetailDto?>(null)
    val detail: StateFlow<WorkoutDetailDto?> = _detail.asStateFlow()

    val isAiEnabled: StateFlow<Boolean> = aiRepository.isAiEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _aiSummary = MutableStateFlow<String?>(null)
    val aiSummary: StateFlow<String?> = _aiSummary.asStateFlow()

    private val _isGeneratingSummary = MutableStateFlow(false)
    val isGeneratingSummary: StateFlow<Boolean> = _isGeneratingSummary.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    init {
        viewModelScope.launch {
            _detail.value = dao.getWorkoutDetail(sessionId)
        }
    }

    fun generateAiSummary() {
        val currentDetail = _detail.value ?: return
        if (!isAiEnabled.value || _isGeneratingSummary.value) return

        viewModelScope.launch {
            _isGeneratingSummary.value = true
            _aiSummary.value = null

            val skippedSets = currentDetail.exercises.sumOf { it.sets.count { s -> s.setLog.isSkipped } }
            val completedSets = currentDetail.exercises.sumOf { it.sets.count { s -> s.setLog.completed && s.setLog.setType != "WARMUP" } }
            
            val cardio = currentDetail.cardioSession
            val cardioNotes = if (cardio != null) "Cardio: \${cardio.durationMinutes} min \${cardio.cardioType}" else "Cardio: None/Skipped"
            
            // Note: dayName would typically come from the Template or a joined table.
            // For now we'll pass a generic name or pull from session if available.
            val dayName = "Workout Session" 

            val input = AiWorkoutSummaryInput(
                dayName = dayName,
                totalExercises = currentDetail.exercises.size,
                completedSets = completedSets,
                skippedSets = skippedSets,
                notes = cardioNotes
            )

            val result = aiRepository.summarizeWorkout(input)
            
            if (result.isSuccess) {
                _aiSummary.value = result.getOrNull()
            } else {
                _aiSummary.value = "Failed to generate AI summary: \${result.exceptionOrNull()?.message}"
            }
            
            _isGeneratingSummary.value = false
        }
    }

    fun exportToCoach() {
        viewModelScope.launch {
            val result = exportSessionToJsonUseCase(sessionId)
            if (result.isSuccess) {
                _exportStatus.value = "Workout data copied to clipboard"
            } else {
                _exportStatus.value = "Failed to export data"
            }
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = null
    }
}
