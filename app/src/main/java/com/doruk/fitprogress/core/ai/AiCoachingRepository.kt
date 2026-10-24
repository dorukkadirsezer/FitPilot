package com.doruk.fitprogress.core.ai

import com.doruk.fitprogress.core.domain.model.AiSyncResult

/**
 * Repository contract for autonomous in-app Gemini AI Coach synchronization.
 */
interface AiCoachingRepository {
    /**
     * Reads the user's Gemini API key from secure storage, exports workout progression
     * telemetry from Room, prompts Gemini with `responseMimeType = "application/json"`,
     * validates the returned [com.doruk.fitprogress.core.domain.model.ValidatedCoachPlanDto],
     * and atomically updates the macrocycle in Room.
     */
    suspend fun syncMacrocycleWithGemini(): AiSyncResult
}
