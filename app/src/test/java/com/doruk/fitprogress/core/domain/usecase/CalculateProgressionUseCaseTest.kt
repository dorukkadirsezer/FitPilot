package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test


class CalculateProgressionUseCaseTest {

    private lateinit var useCase: CalculateProgressionUseCase
    private val dao = FakeWorkoutDao()

    @Before
    fun setup() {
        useCase = CalculateProgressionUseCase(dao)
    }

    private fun createExercise(equipment: String, unit: String) = ExerciseEntity(
        "ex1", "Test Ex", "chest", equipment, "compound", 100, unit, true, false, null, 0, 0
    )

    private fun createSession(id: String) = WorkoutExerciseSessionEntity(id, "w_$id", "ex1", 1, null)

    private fun createWorkingSet(
        sessionId: String, reps: Int, targetReps: Int, weight: Float, unit: String, rpe: Float, skipped: Boolean = false, completed: Boolean = true
    ) = SetLogEntity(
        java.util.UUID.randomUUID().toString(), sessionId, 1, "WORKING", weight, weight, unit, targetReps, reps, rpe, completed, skipped
    )

    @Test
    fun `two successful sessions trigger increase`() = runBlocking {
        val exercise = createExercise("barbell", "lbs")
        val s1 = createSession("1")
        val s2 = createSession("2")
        val sessions = listOf(createSession("2"), createSession("1"))
        
        // Target: 10, Reps: 12 (top of range +2)
        val logs = listOf(
            createWorkingSet("1", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("1", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("2", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("2", 12, 10, 100f, "lbs", 8f)
        )

        val result = useCase(exercise, sessions, logs, "normal")
        assertEquals("INCREASE_WEIGHT", result.suggestionType)
        assertEquals(105f, result.suggestedWeight)
    }

    @Test
    fun `high RPE prevents increase`() = runBlocking {
        val exercise = createExercise("barbell", "lbs")
        val s1 = createSession("1")
        val s2 = createSession("2")
        val sessions = listOf(createSession("2"), createSession("1"))
        
        // Target: 10, Reps: 12, but RPE 9.5
        val logs = listOf(
            createWorkingSet("1", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("2", 12, 10, 100f, "lbs", 10.0f) // Too high
        )

        val result = useCase(exercise, sessions, logs, "normal")
        assertEquals("REDUCE_LOAD", result.suggestionType) // RPE > 9.5 means reduce
    }

    @Test
    fun `Controlled mode suppresses increase`() = runBlocking {
        val exercise = createExercise("barbell", "lbs")
        val sessions = listOf(createSession("2"), createSession("1"))
        val logs = listOf(
            createWorkingSet("1", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("2", 12, 10, 100f, "lbs", 8f)
        )

        val result = useCase(exercise, sessions, logs, "controlled")
        assertEquals("SUPPRESSED_BY_CONTROLLED_MODE", result.suggestionType)
    }

    @Test
    fun `warm-ups and skipped sets excluded`() = runBlocking {
        val exercise = createExercise("barbell", "lbs")
        val sessions = listOf(createSession("2"), createSession("1"))
        val logs = listOf(
            SetLogEntity("w1", "1", 1, "WARMUP", 50f, 50f, "lbs", 10, 10, null, true, false),
            createWorkingSet("1", 12, 10, 100f, "lbs", 8f),
            createWorkingSet("2", 12, 10, 100f, "lbs", 8f, skipped = true),
            createWorkingSet("2", 12, 10, 100f, "lbs", 8f)
        )

        val result = useCase(exercise, sessions, logs, "normal")
        // Since set 2 in session 2 was skipped, it should just ignore it, only evaluate remaining
        assertEquals("INCREASE_WEIGHT", result.suggestionType)
    }
}
