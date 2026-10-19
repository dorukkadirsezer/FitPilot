package com.doruk.fitprogress.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.domain.usecase.CalculateAnalyticsUseCase
import com.doruk.fitprogress.core.domain.usecase.SyncCoachPlanUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val calculateAnalyticsUseCase: CalculateAnalyticsUseCase,
    private val syncCoachPlanUseCase: SyncCoachPlanUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        loadAnalyticsSummary()
    }

    fun refresh() {
        loadAnalyticsSummary()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            workoutDao.getAllTemplates().collect { templates ->
                _uiState.value = _uiState.value.copy(
                    templates = templates,
                    isLoading = false
                )
            }
        }
    }

    private fun loadAnalyticsSummary() {
        viewModelScope.launch {
            try {
                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val summary = calculateAnalyticsUseCase.getDashboardSummary(todayIso)
                _uiState.value = _uiState.value.copy(analyticsSummary = summary)
            } catch (e: Exception) {
                // Ignore for MVP dashboard or log it
            }
        }
    }

    fun importCoachPlan() {
        viewModelScope.launch {
            val result = syncCoachPlanUseCase()
            if (result.isSuccess) {
                val sessionId = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    importError = null,
                    importedSessionId = sessionId
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    importError = result.exceptionOrNull()?.message ?: "Failed to import JSON plan"
                )
            }
        }
    }

    fun clearImportError() {
        _uiState.value = _uiState.value.copy(importError = null)
    }

    fun clearImportedSessionId() {
        _uiState.value = _uiState.value.copy(importedSessionId = null)
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val suggestedTemplateId: String? = null,
    val analyticsSummary: AnalyticsDashboardSummary? = null,
    val importError: String? = null,
    val importedSessionId: String? = null
)
