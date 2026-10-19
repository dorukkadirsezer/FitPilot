package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.ReadinessCheckInEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CalculateSmartRecommendationUseCaseTest {

    private lateinit var useCase: CalculateSmartRecommendationUseCase

    @Before
    fun setup() {
        useCase = CalculateSmartRecommendationUseCase()
    }

    private fun createCheckIn(
        plannedDayCode: String,
        availableTimeMode: String = "full",
        energyLevel: Int = 5,
        motivationLevel: Int = 5,
        sleepQuality: Int = 5,
        muscleSoreness: Int = 1,
        kneeSensitivity: String = "none",
        backDiscomfort: String = "none",
        previousWorkoutFeltHard: Boolean = false
    ) = ReadinessCheckInEntity(
        id = UUID.randomUUID().toString(),
        date = System.currentTimeMillis(),
        plannedDayCode = plannedDayCode,
        availableTimeMode = availableTimeMode,
        energyLevel = energyLevel,
        motivationLevel = motivationLevel,
        sleepQuality = sleepQuality,
        muscleSoreness = muscleSoreness,
        kneeSensitivity = kneeSensitivity,
        backDiscomfort = backDiscomfort,
        previousWorkoutFeltHard = previousWorkoutFeltHard,
        notes = ""
    )

    @Test
    fun `Test 1 - A Day + high knee sensitivity + good energy + full time`() {
        val checkIn = createCheckIn(
            plannedDayCode = "A",
            kneeSensitivity = "high",
            energyLevel = 5
        )
        val result = useCase(checkIn)
        assertTrue(result.recommendedMode == "NORMAL" || result.recommendedMode == "CONTROLLED")
    }

    @Test
    fun `Test 2 - A Day + high knee sensitivity + low energy + 30 minutes`() {
        val checkIn = createCheckIn(
            plannedDayCode = "A",
            kneeSensitivity = "high",
            energyLevel = 2,
            availableTimeMode = "30"
        )
        val result = useCase(checkIn)
        assertEquals("LIGHT", result.recommendedMode)
    }

    @Test
    fun `Test 3 - C Day + high knee sensitivity + good energy`() {
        val checkIn = createCheckIn(
            plannedDayCode = "C",
            kneeSensitivity = "high",
            energyLevel = 5
        )
        val result = useCase(checkIn)
        assertTrue(result.recommendedMode == "NORMAL" || result.recommendedMode == "CONTROLLED")
    }

    @Test
    fun `Test 4 - B Day + high knee sensitivity + low energy`() {
        val checkIn = createCheckIn(
            plannedDayCode = "B",
            kneeSensitivity = "high",
            energyLevel = 2
        )
        val result = useCase(checkIn)
        assertTrue(result.recommendedMode == "REST_OR_WALK" || result.recommendedMode == "LIGHT")
    }

    @Test
    fun `Test 5 - D Day + high knee sensitivity + good energy`() {
        val checkIn = createCheckIn(
            plannedDayCode = "D",
            kneeSensitivity = "high",
            energyLevel = 5
        )
        val result = useCase(checkIn)
        assertTrue(result.recommendedMode == "CONTROLLED" || result.recommendedMode == "LIGHT")
    }

    @Test
    fun `Test 6 - B Day + high back discomfort`() {
        val checkIn = createCheckIn(
            plannedDayCode = "B",
            backDiscomfort = "high",
            energyLevel = 3
        )
        val result = useCase(checkIn)
        assertTrue(result.recommendedMode == "REST_OR_WALK" || result.recommendedMode == "LIGHT")
    }
}
