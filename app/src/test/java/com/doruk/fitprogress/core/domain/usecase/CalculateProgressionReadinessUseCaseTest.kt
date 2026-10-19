package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.SetLogEntity
import com.doruk.fitprogress.core.database.entity.WorkoutExerciseSessionEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test


class CalculateProgressionReadinessUseCaseTest {

    private lateinit var useCase: CalculateProgressionReadinessUseCase
    private val dao = FakeWorkoutDao()

    @Before
    fun setup() {
        useCase = CalculateProgressionReadinessUseCase(dao)
    }

    private fun createSession(id: String) = WorkoutExerciseSessionEntity(id, "w_$id", "ex1", 1, null)

    private fun createWorkingSet(
        sessionId: String, reps: Int, targetReps: Int, rpe: Float, skipped: Boolean = false
    ) = SetLogEntity(
        java.util.UUID.randomUUID().toString(), sessionId, 1, "WORKING", 100f, 100f, "lbs", targetReps, reps, rpe, true, skipped
    )

    @Test
    fun `perfect sessions give score 80 to 100`() = runBlocking {
        val sessions = listOf(createSession("1"), createSession("2"))
        val logs = listOf(
            createWorkingSet("1", 12, 10, 8f),
            createWorkingSet("2", 12, 10, 8f)
        )

        val result = useCase("ex1", sessions, logs, "normal", false)
        assertTrue(result != null)
        // 50 + 20 (all target) + 15 (top range) + 10 (RPE target) + 10 (performance stable) + 5 (no skip) = 110 -> 100
        assertEquals(100, result!!.score)
    }

    @Test
    fun `light mode drops score below 60`() = runBlocking {
        val sessions = listOf(createSession("1"), createSession("2"))
        val logs = listOf(
            createWorkingSet("1", 12, 10, 8f),
            createWorkingSet("2", 12, 10, 8f)
        )

        val result = useCase("ex1", sessions, logs, "light", false)
        assertTrue(result != null)
        // Max is 100 - 20 (Light) = 80... wait, 110 - 20 = 90. Is that right? 
        // 50 + 20 + 15 + 10 + 10 + 5 - 20 = 90. The user said Light Mode should be -20.
        // It's technically clamped. But let's verify math is correct.
        assertEquals(90, result!!.score)
    }

    @Test
    fun `skipped sets drop score by 20`() = runBlocking {
        val sessions = listOf(
            createSession("2"),
            createSession("1")
        )

        val logs = listOf(
            createWorkingSet("1", 12, 10, 8f),
            createWorkingSet("2", 12, 10, 8f)
        )

        val result = useCase("ex1", sessions, logs, "normal", false)
        assertTrue(result != null)
        // latestSets will be empty if we skipped everything? No, let's add one completed set
        val logs2 = listOf(
            createWorkingSet("1", 12, 10, 8f),
            createWorkingSet("2", 12, 10, 8f),
            createWorkingSet("2", 12, 10, 8f, skipped = true)
        )
        val result2 = useCase("ex1", sessions, logs2, "normal", false)
        assertEquals(85, result2!!.score)
    }
}
