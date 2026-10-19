package com.doruk.fitprogress.feature.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

import com.doruk.fitprogress.core.ai.AiRepository
import com.doruk.fitprogress.core.ai.AiWeeklyReviewInput

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(val summary: AnalyticsDashboardSummary) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val calculateAnalyticsUseCase: CalculateAnalyticsUseCase,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _aiReview = MutableStateFlow<String?>(null)
    val aiReview: StateFlow<String?> = _aiReview.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()
    
    val isAiEnabledFlow = aiRepository.isAiEnabledFlow

    init {
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            try {
                _uiState.value = AnalyticsUiState.Loading
                val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val summary = calculateAnalyticsUseCase.getDashboardSummary(todayIso)
                _uiState.value = AnalyticsUiState.Success(summary)
            } catch (e: Exception) {
                _uiState.value = AnalyticsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun generateWeeklyReview() {
        val currentState = _uiState.value as? AnalyticsUiState.Success ?: return
        viewModelScope.launch {
            if (!aiRepository.isAiEnabled()) return@launch
            
            _isAiLoading.value = true
            
            val summary = currentState.summary
            val input = AiWeeklyReviewInput(
                workoutConsistency = "Workouts: \${summary.workoutSummary.workoutsCompleted}, Volume: \${summary.workoutSummary.totalVolumeKg}kg, Avg RPE: \${summary.workoutSummary.averageRpe}",
                nutritionConsistency = "Avg Cals: \${summary.nutritionSummary.averageDailyCalories}, Avg Protein: \${summary.nutritionSummary.averageDailyProtein}g",
                bodyTrends = "Weight Change: \${summary.bodySummary.weightChange7Days}kg"
            )
            
            val result = aiRepository.generateWeeklyReview(input)
            if (result.isSuccess) {
                _aiReview.value = result.getOrNull()
            } else {
                _aiReview.value = "Failed to generate review: \${result.exceptionOrNull()?.message}"
            }
            
            _isAiLoading.value = false
        }
    }
}
