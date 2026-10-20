package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.ProgressionSuggestionEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class CalculateProgressionUseCaseTest {

    private lateinit var workoutDao: WorkoutDao
    private lateinit var useCase: CalculateProgressionUseCase

    private val exercise = ExerciseEntity(
        id = "ex_1",
        name = "Bench Press",
        muscleGroup = "Chest",
        equipment = "barbell",
        movementType = "compound",
        defaultRestSeconds = 90,
        defaultUnit = "kg",
        isCompound = true,
        requiresStraps = false,
        coachNote = null,
        createdAt = 0L,
        updatedAt = 0L
    )

    private val session1Id = "session_1"
    private val session2Id = "session_2"

    private val recentSessions = listOf(
        WorkoutExerciseSessionEntity(
            id = session1Id,
            workoutSessionId = "ws_1",
            exerciseId = "ex_1",
            orderIndex = 1,
            supersetGroupId = null,
            note = null
        ),
        WorkoutExerciseSessionEntity(
            id = session2Id,
            workoutSessionId = "ws_2",
            exerciseId = "ex_1",
            orderIndex = 1,
            supersetGroupId = null,
            note = null
        )
    )

    @Before
    fun setup() {
        workoutDao = mockk(relaxed = true)
        coEvery { workoutDao.getProgressionSuggestionForExerciseSession(any()) } returns null
        useCase = CalculateProgressionUseCase(workoutDao)
    }

    @Test
    fun `invoke ignores WARMUP sets and only uses WORKING sets`() = runTest {
        val setLogs = listOf(
            createSetLog(session2Id, setType = "WARMUP", weight = 50f, reps = 10, targetReps = 10),
            createSetLog(session2Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5),
            createSetLog(session1Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5)
        )

        val result = useCase(exercise, recentSessions, setLogs, null)

        assertEquals("BUILD_REPS", result.suggestionType)
        assertEquals(100f, result.suggestedWeight)
    }

    @Test
    fun `invoke triggers INCREASE_WEIGHT when top reps are hit`() = runTest {
        val setLogs = listOf(
            createSetLog(session2Id, setType = "WORKING", weight = 100f, reps = 7, targetReps = 5),
            createSetLog(session1Id, setType = "WORKING", weight = 100f, reps = 7, targetReps = 5)
        )

        val result = useCase(exercise, recentSessions, setLogs, null)

        assertEquals("INCREASE_WEIGHT", result.suggestionType)
        assertEquals(102.5f, result.suggestedWeight) // Barbell in kg -> 2.5f increment
    }

    @Test
    fun `invoke filters out skipped sets and null weights`() = runTest {
        val setLogs = listOf(
            createSetLog(session2Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5),
            createSetLog(session2Id, setType = "WORKING", weight = null, reps = 5, targetReps = 5), // null weight should be ignored
            createSetLog(session2Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5, isSkipped = true), // skipped should be ignored
            createSetLog(session1Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5)
        )

        val result = useCase(exercise, recentSessions, setLogs, null)

        assertEquals("BUILD_REPS", result.suggestionType)
        assertEquals(100f, result.suggestedWeight)
    }

    @Test
    fun `invoke handles insufficient data when all sets are filtered out`() = runTest {
        val setLogs = listOf(
            createSetLog(session2Id, setType = "WORKING", weight = null, reps = null, targetReps = 5), // Filtered
            createSetLog(session1Id, setType = "WORKING", weight = 100f, reps = 5, targetReps = 5, isSkipped = true) // Filtered
        )

        val result = useCase(exercise, recentSessions, setLogs, null)

        assertEquals("INSUFFICIENT_DATA", result.suggestionType)
        assertEquals("No valid working sets found.", result.reason)
    }

    private fun createSetLog(
        sessionId: String,
        setType: String,
        weight: Float?,
        reps: Int?,
        targetReps: Int,
        isSkipped: Boolean = false
    ): SetLogEntity {
        return SetLogEntity(
            id = UUID.randomUUID().toString(),
            workoutExerciseSessionId = sessionId,
            setNumber = 1,
            setType = setType,
            targetWeight = weight,
            targetReps = targetReps,
            weightUnit = "kg",
            rpe = null,
            actualWeight = weight,
            actualReps = reps,
            completed = true,
            isSkipped = isSkipped,
            note = null
        )
    }
}
