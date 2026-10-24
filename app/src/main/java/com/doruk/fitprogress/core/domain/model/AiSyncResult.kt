package com.doruk.fitprogress.core.domain.model

/**
 * Domain result hierarchy for autonomous Gemini AI Coach macrocycle synchronization.
 * Guarantees that no unhandled exceptions leak into ViewModels or Compose UI layers.
 */
sealed interface AiSyncResult {
    data class Success(
        val workoutsImported: Int,
        val exercisesUpdated: Int,
        val coachRationale: String? = null
    ) : AiSyncResult

    data object AiDisabled : AiSyncResult

    data object MissingApiKey : AiSyncResult

    data class SchemaValidationError(
        val reason: String,
        val rawSnippet: String? = null
    ) : AiSyncResult

    data class NetworkError(
        val userMessage: String,
        val isQuotaOrAuthError: Boolean = false
    ) : AiSyncResult
}
