package com.doruk.fitprogress.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.domain.usecase.CalculateAnalyticsUseCase
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
    private val calculateAnalyticsUseCase: CalculateAnalyticsUseCase
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
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val suggestedTemplateId: String? = null,
    val analyticsSummary: AnalyticsDashboardSummary? = null
)
