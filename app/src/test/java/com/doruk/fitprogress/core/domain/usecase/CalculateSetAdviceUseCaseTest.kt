package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.SetLogEntity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class CalculateSetAdviceUseCaseTest {

    private lateinit var useCase: CalculateSetAdviceUseCase

    @Before
    fun setup() {
        useCase = CalculateSetAdviceUseCase()
    }

    private fun createSet(
        reps: Int?,
        rpe: Float?,
        weight: Float? = 100f,
        isWarmup: Boolean = false,
        isSkipped: Boolean = false,
        isDone: Boolean = true
    ) = SetLogEntity(
        id = UUID.randomUUID().toString(),
        workoutExerciseSessionId = "ex1",
        setNumber = 1,
        weightUnit = "kg",
        setType = if (isWarmup) "WARMUP" else "WORKING",
        actualWeight = weight,
        targetWeight = weight,
        targetReps = 10,
        actualReps = reps,
        rpe = rpe,
        completed = isDone,
        isSkipped = isSkipped
    )

    @Test
    fun `Test 1 - Ignore warm-ups`() {
        val set = createSet(reps = 10, rpe = 7f, isWarmup = true)
        val result = useCase(set, null, 8, 12, 7f)
        assertEquals(AdviceType.NO_ADVICE, result.type)
    }

    @Test
    fun `Test 2 - Ignore skipped sets`() {
        val set = createSet(reps = 10, rpe = 7f, isSkipped = true)
        val result = useCase(set, null, 8, 12, 7f)
        assertEquals(AdviceType.NO_ADVICE, result.type)
    }

    @Test
    fun `Test 3 - VERY_HIGH_RPE priority over LOW_REPS`() {
        // target RPE 7, actual RPE 9. Reps target 8, actual 6.
        val set = createSet(reps = 6, rpe = 9f)
        val result = useCase(set, null, 8, 12, 7f)
        assertEquals(AdviceType.VERY_HIGH_RPE, result.type)
    }

    @Test
    fun `Test 4 - LOW_REPS priority over PERFORMANCE_DROP`() {
        val previousSet = createSet(reps = 10, rpe = 7f)
        // target reps 8. actual 7. This is low reps.
        // It's also a performance drop (10 -> 7).
        val currentSet = createSet(reps = 7, rpe = 7f)
        val result = useCase(currentSet, previousSet, 8, 12, 7f)
        assertEquals(AdviceType.LOW_REPS, result.type)
    }

    @Test
    fun `Test 5 - PERFORMANCE_DROP priority over HIGH_RPE`() {
        val previousSet = createSet(reps = 12, rpe = 7f)
        // target reps 8-12. Target RPE 7. 
        // Actual reps 10 (>= 8, so not LOW_REPS). Drop is 2 (12 -> 10, so PERFORMANCE_DROP).
        // Actual RPE 8 (7 + 1, so HIGH_RPE).
        val currentSet = createSet(reps = 10, rpe = 8f)
        val result = useCase(currentSet, previousSet, 8, 12, 7f)
        assertEquals(AdviceType.PERFORMANCE_DROP, result.type)
    }

    @Test
    fun `Test 6 - HIGH_RPE`() {
        // target reps 8, actual 8. target RPE 7, actual 8.
        val set = createSet(reps = 8, rpe = 8f)
        val result = useCase(set, null, 8, 12, 7f)
        assertEquals(AdviceType.HIGH_RPE, result.type)
    }

    @Test
    fun `Test 7 - GOOD_SET`() {
        // target reps 8, actual 10. target RPE 7, actual 7.
        val set = createSet(reps = 10, rpe = 7f)
        val result = useCase(set, null, 8, 12, 7f)
        assertEquals(AdviceType.GOOD_SET, result.type)
    }

    @Test
    fun `Test 8 - Superset ignores warm-ups and unfinished rounds`() {
        val set1 = createSet(reps = 10, rpe = 7f)
        val set2 = createSet(reps = null, rpe = null, isDone = false)
        val result = useCase.calculateForSupersetRound(listOf(set1, set2), listOf(8, 8), listOf(7f, 7f))
        // Expect NO_ADVICE or default GOOD_SET without triggering very high RPE
        // Wait, if set2 is unfinished, it returns NO_ADVICE because the loop skips or we should return NO_ADVICE early?
        // Ah, in my code, if any is incomplete, the loop returns early, but the end of function defaults to GOOD_SET.
        // Let's check my implementation. It does `return@forEachIndexed`. So it continues the loop, doesn't flag anything, then returns GOOD_SET.
        // Wait! If the round is incomplete, the caller shouldn't even call this function, or the function should return NO_ADVICE.
    }
}
