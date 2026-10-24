package com.doruk.fitprogress.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.ai.AiCoachingRepository
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.datastore.AiSettingsDataStore
import com.doruk.fitprogress.core.domain.model.AiSyncResult
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.domain.usecase.CalculateAnalyticsUseCase
import com.doruk.fitprogress.core.domain.usecase.ExportProgramToJsonUseCase
import com.doruk.fitprogress.core.domain.usecase.ExportUserProgressionStatsUseCase
import com.doruk.fitprogress.core.domain.usecase.SyncCoachPlanUseCase
import com.doruk.fitprogress.core.util.ClipboardSecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val workoutDao: WorkoutDao,
    private val calculateAnalyticsUseCase: CalculateAnalyticsUseCase,
    private val syncCoachPlanUseCase: SyncCoachPlanUseCase,
    private val exportProgramToJsonUseCase: ExportProgramToJsonUseCase,
    private val exportUserProgressionStatsUseCase: ExportUserProgressionStatsUseCase,
    private val aiCoachingRepository: AiCoachingRepository,
    private val aiSettingsDataStore: AiSettingsDataStore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeDashboardStreams()
        loadAnalyticsSummary()
    }

    fun refresh() {
        loadAnalyticsSummary()
    }

    private fun observeDashboardStreams() {
        viewModelScope.launch {
            combine(
                workoutDao.getAllTemplates(),
                workoutDao.getWorkoutHistoryList(),
                workoutDao.getCompletedWorkoutsCountFlow()
            ) { templates, historyList, totalCount ->
                Triple(templates, historyList, totalCount)
            }
                .catch { _uiState.update { state -> state.copy(isLoading = false) } }
                .collect { (templates, historyList, totalCount) ->
                    runCatching {
                        val effectiveCount = maxOf(historyList.size, totalCount)
                        _uiState.update { state ->
                            state.copy(
                                templates = templates,
                                completedWorkoutsCount = effectiveCount,
                                recentSessions = historyList.map { it.session }.take(3),
                                isLoading = false
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
                _uiState.update { state ->
                    state.copy(
                        analyticsSummary = summary,
                        completedWorkoutsCount = maxOf(
                            state.completedWorkoutsCount,
                            summary.workoutSummary.workoutsCompleted
                        )
                    )
                }
            }
        }
    }

    /**
     * Option 2: Autonomous Native Gemini API Synchronization.
     * Exports anonymized Room progression telemetry, prompts Gemini with `responseMimeType = "application/json"`,
     * validates the returned macrocycle, and saves it atomically to Room.
     */
    fun syncWithGeminiCoach() {
        if (_uiState.value.isSyncingAiPlan) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncingAiPlan = true,
                    importError = null,
                    importSuccessMessage = null,
                    missingApiKeyPrompt = false,
                    showDataSharingConsentDialog = false
                )
            }

            when (val result = aiCoachingRepository.syncMacrocycleWithGemini()) {
                is AiSyncResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            importError = null,
                            importSuccessMessage = "Gemini AI Koç ${result.workoutsImported} antrenman günü (${result.exercisesUpdated} egzersiz) güncelledi!",
                            aiCoachRationale = result.coachRationale
                        )
                    }
                }

                is AiSyncResult.AiDisabled -> {
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            missingApiKeyPrompt = true,
                            importError = "AI özellikleri kapalı. Lütfen Ayarlar ekranından AI özelliklerini etkinleştirin."
                        )
                    }
                }

                is AiSyncResult.DataSharingConsentRequired -> {
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            showDataSharingConsentDialog = true
                        )
                    }
                }

                is AiSyncResult.MissingApiKey -> {
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            missingApiKeyPrompt = true,
                            importError = "Gemini API anahtarı bulunamadı. Lütfen Ayarlar ekranından API anahtarınızı girin."
                        )
                    }
                }

                is AiSyncResult.SchemaValidationError -> {
                    val detailedError = if (!result.rawSnippet.isNullOrBlank() && !result.reason.contains(result.rawSnippet)) {
                        "${result.reason} | İlk 100 krkt: [${result.rawSnippet.take(100)}]"
                    } else {
                        result.reason
                    }
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            importError = detailedError
                        )
                    }
                }

                is AiSyncResult.NetworkError -> {
                    _uiState.update { state ->
                        state.copy(
                            isSyncingAiPlan = false,
                            importError = result.userMessage
                        )
                    }
                }
            }
        }
    }

    fun acceptDataSharingAndSync() {
        viewModelScope.launch {
            aiSettingsDataStore.setDataSharingAccepted(true)
            _uiState.update { it.copy(showDataSharingConsentDialog = false) }
            syncWithGeminiCoach()
        }
    }

    fun dismissDataSharingConsentDialog() {
        _uiState.update { it.copy(showDataSharingConsentDialog = false) }
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
                        importError = result.exceptionOrNull()?.message ?: "Plan içe aktarılamadı",
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
                exportError = null,
                missingApiKeyPrompt = false
            )
        }
    }

    fun clearImportedSessionId() {
        _uiState.update { state -> state.copy(importedSessionId = null) }
    }

    fun exportProgram() {
        viewModelScope.launch {
            runCatching {
                val telemetryJson = exportUserProgressionStatsUseCase.exportAsJsonString(maxRecentSessions = 10)
                withContext(Dispatchers.Main) {
                    // M5: Mark copied progression telemetry JSON as sensitive
                    ClipboardSecurityUtils.copySensitiveText(
                        context = appContext,
                        label = "FitPilot Progression Telemetry JSON",
                        text = telemetryJson
                    )
                }
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        exportError = null,
                        exportSuccessMessage = "İlerleme telemetrisi panoya kopyalandı!"
                    )
                }
            }.onFailure {
                val fallbackResult = exportProgramToJsonUseCase()
                if (fallbackResult.isSuccess) {
                    _uiState.update { state ->
                        state.copy(
                            exportError = null,
                            exportSuccessMessage = "Program panoya kopyalandı!"
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            exportError = fallbackResult.exceptionOrNull()?.message ?: "Program dışa aktarılamadı",
                            exportSuccessMessage = null
                        )
                    }
                }
            }
        }
    }
}

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isSyncingAiPlan: Boolean = false,
    val missingApiKeyPrompt: Boolean = false,
    val showDataSharingConsentDialog: Boolean = false,
    val aiCoachRationale: String? = null,
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
