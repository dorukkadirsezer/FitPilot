package com.doruk.fitprogress.core.ai

import com.doruk.fitprogress.core.database.dao.AiDao
import com.doruk.fitprogress.core.database.entity.AiInteractionLogEntity
import com.doruk.fitprogress.core.datastore.AiSettingsDataStore
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiRepository @Inject constructor(
    private val aiDao: AiDao,
    private val settingsDataStore: AiSettingsDataStore
) {
    private val geminiService = GeminiAiService()
    private val disabledService = DisabledAiService()

    private suspend fun getActiveService(): Pair<AiService, String?> {
        val isEnabled = settingsDataStore.isAiEnabledFlow.first()
        val apiKey = settingsDataStore.geminiApiKeyFlow.first()
        
        return if (isEnabled && !apiKey.isNullOrBlank()) {
            Pair(geminiService, apiKey)
        } else {
            Pair(disabledService, null)
        }
    }

    private suspend fun logInteraction(
        type: String,
        promptSummary: String,
        response: String
    ) {
        val saveLogs = settingsDataStore.saveAiLogsFlow.first()
        if (saveLogs) {
            aiDao.insertInteractionLog(
                AiInteractionLogEntity(
                    id = UUID.randomUUID().toString(),
                    interactionType = type,
                    promptSummary = promptSummary,
                    response = response,
                    createdAt = System.currentTimeMillis(),
                    savedByUser = true
                )
            )
        }
    }

    suspend fun isAiEnabled(): Boolean {
        val isEnabled = settingsDataStore.isAiEnabledFlow.first()
        val apiKey = settingsDataStore.geminiApiKeyFlow.first()
        return isEnabled && !apiKey.isNullOrBlank()
    }

    val isAiEnabledFlow = kotlinx.coroutines.flow.combine(
        settingsDataStore.isAiEnabledFlow,
        settingsDataStore.geminiApiKeyFlow
    ) { enabled, key ->
        enabled && !key.isNullOrBlank()
    }

    suspend fun parseFoodText(text: String): Result<List<AiFoodCandidate>> {
        val (service, key) = getActiveService()
        if (key == null) return Result.failure(IllegalStateException("AI is disabled or API key missing."))
        
        val result = service.parseFoodText(text, key)
        if (result.isSuccess) {
            val count = result.getOrNull()?.size ?: 0
            logInteraction(
                type = "FOOD_PARSER",
                promptSummary = "Parsed \$count foods from natural language text.",
                response = "Parsed \$count items successfully."
            )
        }
        return result
    }

    suspend fun summarizeWorkout(input: AiWorkoutSummaryInput): Result<String> {
        val (service, key) = getActiveService()
        if (key == null) return Result.failure(IllegalStateException("AI is disabled or API key missing."))
        
        val result = service.summarizeWorkout(input, key)
        if (result.isSuccess) {
            logInteraction(
                type = "WORKOUT_SUMMARY",
                promptSummary = "Summarized workout: \${input.dayName}",
                response = result.getOrNull() ?: ""
            )
        }
        return result
    }

    suspend fun explainProgression(input: AiProgressionExplanationInput): Result<String> {
        val (service, key) = getActiveService()
        if (key == null) return Result.failure(IllegalStateException("AI is disabled or API key missing."))
        
        val result = service.explainProgression(input, key)
        if (result.isSuccess) {
            logInteraction(
                type = "PROGRESSION_EXPLANATION",
                promptSummary = "Explained progression for: \${input.exerciseName}",
                response = result.getOrNull() ?: ""
            )
        }
        return result
    }

    suspend fun generateWeeklyReview(input: AiWeeklyReviewInput): Result<String> {
        val (service, key) = getActiveService()
        if (key == null) return Result.failure(IllegalStateException("AI is disabled or API key missing."))
        
        val result = service.generateWeeklyReview(input, key)
        if (result.isSuccess) {
            logInteraction(
                type = "WEEKLY_REVIEW",
                promptSummary = "Generated weekly review.",
                response = result.getOrNull() ?: ""
            )
        }
        return result
    }
}
