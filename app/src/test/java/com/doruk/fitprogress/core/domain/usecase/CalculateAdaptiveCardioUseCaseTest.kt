package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import com.doruk.fitprogress.core.database.entity.SmartDayRecommendationEntity
import com.doruk.fitprogress.core.database.entity.WorkoutSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CalculateAdaptiveCardioUseCaseTest {

    private lateinit var useCase: CalculateAdaptiveCardioUseCase

    @Before
    fun setup() {
        useCase = CalculateAdaptiveCardioUseCase()
    }

    private val session = WorkoutSessionEntity(
        id = "sess1",
        templateId = "tmpl1",
        startedAt = 1000L,
        finishedAt = 2000L,
        durationSeconds = 1000,
        totalWorkingVolumeKg = 0f,
        averageRpe = 0f,
        cardioCompleted = false
    )

    private fun createCheckIn(dayCode: String, kneeSensitivity: String = "none", energyLevel: Int = 5) = ReadinessCheckInEntity(
        id = "c1",
        date = System.currentTimeMillis(),
        plannedDayCode = dayCode,
        availableTimeMode = "full",
        energyLevel = energyLevel,
        motivationLevel = 5,
        sleepQuality = 5,
        muscleSoreness = 1,
        kneeSensitivity = kneeSensitivity,
        backDiscomfort = "none",
        previousWorkoutFeltHard = false,
        notes = ""
    )

    private fun createRec(mode: String) = SmartDayRecommendationEntity(
        id = "r1",
        checkInId = "c1",
        suggestedWorkoutTemplateId = "t1",
        recommendedMode = mode,
        selectedMode = mode,
        loadAdjustmentPercent = 0,
        volumeAdjustment = 0,
        cardioRecommendation = null,
        reason = "",
        accepted = true,
        overriddenByUser = false
    )

    @Test
    fun `Test 1 - B Day has no cardio`() {
        val result = useCase(session, createCheckIn("B"), createRec("NORMAL"), 7f, 0)
        assertNull(result.recommendedCardio)
    }

    @Test
    fun `Test 2 - A Day with high knee sensitivity modifies cardio`() {
        val result = useCase(session, createCheckIn("A", kneeSensitivity = "high"), createRec("NORMAL"), 7f, 0)
        assertEquals("15 min stationary bike or elliptical", result.recommendedCardio)
    }

    @Test
    fun `Test 3 - Light mode skips cardio`() {
        val result = useCase(session, createCheckIn("C"), createRec("LIGHT"), 7f, 0)
        assertNull(result.recommendedCardio)
    }

    @Test
    fun `Test 4 - Controlled mode makes cardio optional`() {
        val result = useCase(session, createCheckIn("C"), createRec("CONTROLLED"), 7f, 0)
        assertEquals("15 min elliptical (Optional)", result.recommendedCardio)
    }

    @Test
    fun `Test 5 - High fatigue skips or shortens cardio`() {
        // Normal mode but high average RPE
        val result = useCase(session, createCheckIn("D"), createRec("NORMAL"), 9f, 0)
        assertEquals("Optional 10 min very light walk", result.recommendedCardio)
    }

    @Test
    fun `Test 6 - Normal C Day`() {
        val result = useCase(session, createCheckIn("C"), createRec("NORMAL"), 7f, 0)
        assertEquals("20 min elliptical", result.recommendedCardio)
    }

    @Test
    fun `Test 7 - Normal D Day`() {
        val result = useCase(session, createCheckIn("D"), createRec("NORMAL"), 7f, 0)
        assertEquals("15 min stationary bike", result.recommendedCardio)
    }
}
