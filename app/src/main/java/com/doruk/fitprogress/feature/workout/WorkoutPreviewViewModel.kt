package com.doruk.fitprogress.feature.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.TemplateWithDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WorkoutPreviewViewModel @Inject constructor(
    private val dao: WorkoutDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val templateId: String = savedStateHandle.get<String>("templateId") ?: ""

    private val _uiState = MutableStateFlow<WorkoutPreviewUiState>(WorkoutPreviewUiState.Loading)
    val uiState: StateFlow<WorkoutPreviewUiState> = _uiState.asStateFlow()

    init {
        loadPreview()
    }

    private fun loadPreview() {
        if (templateId.isBlank()) {
            _uiState.value = WorkoutPreviewUiState.Error("Workout template not provided.")
            return
        }

        viewModelScope.launch {
            val details = dao.getTemplateWithDetails(templateId)
            if (details != null) {
                _uiState.value = WorkoutPreviewUiState.Success(details)
            } else {
                _uiState.value = WorkoutPreviewUiState.Error("Failed to load workout details.")
            }
        }
    }
}

sealed interface WorkoutPreviewUiState {
    object Loading : WorkoutPreviewUiState
    data class Success(val details: TemplateWithDetails) : WorkoutPreviewUiState
    data class Error(val message: String) : WorkoutPreviewUiState
}