package com.doruk.fitprogress.core.ai

import com.doruk.fitprogress.core.di.AiCoachModule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiCoachGeminiModelFallbackTest {

    @Test
    fun `default and fallback models strictly use Gemini 3_x Flash family without 1_5 2_0 or 2_5`() = runTest {
        assertEquals("gemini-3.8-flash", AiCoachModule.DEFAULT_GEMINI_MODEL)
        assertEquals(
            listOf(
                "gemini-3.8-flash",
                "gemini-3.6-flash",
                "gemini-3.5-flash-lite",
                "gemini-3.1-flash-lite"
            ),
            AiCoachModule.FALLBACK_GEMINI_MODELS
        )
        AiCoachModule.FALLBACK_GEMINI_MODELS.forEach { model ->
            assertFalse(model.contains("1.5"))
            assertFalse(model.contains("2.0"))
            assertFalse(model.contains("2.5"))
        }
    }

    @Test
    fun `buildGenerateContentUrl constructs valid v1beta URL and strips models prefix`() = runTest {
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent",
            AiCoachingRepositoryImpl.buildGenerateContentUrl("gemini-3.8-flash")
        )
        assertEquals(
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent",
            AiCoachingRepositoryImpl.buildGenerateContentUrl("models/gemini-3.6-flash")
        )
    }

    @Test
    fun `isEligibleActiveModelName strictly excludes 1_5 2_0 2_5 and non-text models`() = runTest {
        assertTrue(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-3.8-flash"))
        assertTrue(AiCoachingRepositoryImpl.isEligibleActiveModelName("models/gemini-3.6-flash"))
        assertTrue(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-3.5-flash-lite"))
        assertTrue(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-3.1-flash-lite"))

        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-1.5-flash"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-1.5-flash-latest"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-2.0-flash"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-2.5-flash"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-2.5-flash-lite"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-3.8-flash-tts"))
        assertFalse(AiCoachingRepositoryImpl.isEligibleActiveModelName("gemini-3.8-live"))
    }
}