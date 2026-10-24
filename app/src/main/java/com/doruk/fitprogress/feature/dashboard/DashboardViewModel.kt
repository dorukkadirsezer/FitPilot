package com.doruk.fitprogress.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.domain.usecase.CalculateAnalyticsUseCase
import com.doruk.fitprogress.core.domain.usecase.SyncCoachPlanUseCase
import com.doruk.fitprogress.core.domain.usecase.ExportProgramToJsonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val calculateAnalyticsUseCase: CalculateAnalyticsUseCase,
    private val syncCoachPlanUseCase: SyncCoachPlanUseCase,
    private val exportProgramToJsonUseCase: ExportProgramToJsonUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadTemplates()
        observeFinishedWorkouts()
        loadAnalyticsSummary()
    }

    fun refresh() {
        loadAnalyticsSummary()
    }

    private fun loadTemplates() {
        viewModelScope.launch {
            workoutDao.getAllTemplates()
                .catch { _uiState.update { state -> state.copy(isLoading = false) } }
                .collect { templates ->
                    runCatching {
                        _uiState.update { state ->
                            state.copy(
                                templates = templates,
                                isLoading = false
                            )
                        }
                    }
                }
        }
    }

    private fun observeFinishedWorkouts() {
        viewModelScope.launch {
            workoutDao.getAllFinishedWorkoutSessions()
                .catch { /* Prevent stream cancellation on DB error */ }
                .collect { sessions ->
                    runCatching {
                        _uiState.update { state ->
                            state.copy(
                                completedWorkoutsCount = sessions.size,
                                recentSessions = sessions.take(3)
                            )
                        }
                        loadAnalyticsSummary()
                    }
                }
        }
    }

    private fun loadAnalyticsSummary() {
        viewModelScope.launch {
            runCatching {
                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                calculateAnalyticsUseCase.getDashboardSummary(todayIso)
            }.onSuccess { summary ->
                _uiState.update { state -> state.copy(analyticsSummary = summary) }
            }
        }
    }

    fun importCoachPlan() {
        viewModelScope.launch {
            val result = syncCoachPlanUseCase()
            if (result.isSuccess) {
                val successMessage = result.getOrNull()!!
                _uiState.update { state ->
                    state.copy(
                        importError = null,
                        importSuccessMessage = successMessage,
                        importedSessionId = null
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        importError = result.exceptionOrNull()?.message ?: "Failed to import JSON plan",
                        importSuccessMessage = null
                    )
                }
            }
        }
    }

    fun clearImportMessages() {
        _uiState.update { state ->
            state.copy(
                importError = null,
                importSuccessMessage = null,
                exportSuccessMessage = null,
                exportError = null
            )
        }
    }

    fun clearImportedSessionId() {
        _uiState.update { state -> state.copy(importedSessionId = null) }
    }

    fun exportProgram() {
        viewModelScope.launch {
            val result = exportProgramToJsonUseCase()
            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(
                        exportError = null,
                        exportSuccessMessage = "Program copied to clipboard!"
                    )
                }
            } else {
                _uiState.update { state ->
                    state.copy(
                        exportError = result.exceptionOrNull()?.message ?: "Failed to export program",
                        exportSuccessMessage = null
                    )
                }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val templates: List<WorkoutTemplateEntity> = emptyList(),
    val suggestedTemplateId: String? = null,
    val completedWorkoutsCount: Int = 0,
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val analyticsSummary: AnalyticsDashboardSummary? = null,
    val importError: String? = null,
    val importSuccessMessage: String? = null,
    val importedSessionId: String? = null,
    val exportError: String? = null,
    val exportSuccessMessage: String? = null
)
