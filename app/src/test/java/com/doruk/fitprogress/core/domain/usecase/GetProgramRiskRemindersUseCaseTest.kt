package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class GetProgramRiskRemindersUseCaseTest {

    private lateinit var useCase: GetProgramRiskRemindersUseCase

    @Before
    fun setup() {
        useCase = GetProgramRiskRemindersUseCase()
    }

    private fun createExercise(name: String, movementType: String, equipment: String = "barbell", requiresStraps: Boolean = false) = ExerciseEntity(
        id = UUID.randomUUID().toString(),
        name = name,
        muscleGroup = "Legs",
        movementType = movementType,
        equipment = equipment,
        requiresStraps = requiresStraps,
        defaultRestSeconds = 90,
        defaultUnit = "kg",
        isCompound = true,
        coachNote = null,
        createdAt = 0,
        updatedAt = 0
    )

    private fun createTemplateExercise() = WorkoutTemplateExerciseEntity(
        id = UUID.randomUUID().toString(),
        templateId = "t1",
        exerciseId = "e1",
        orderIndex = 1,
        targetSets = 3,
        targetRepsMin = 8,
        targetRepsMax = 12,
        targetWeightMin = null,
        targetWeightMax = null,
        weightUnit = "kg",
        rpeMin = 7f,
        rpeMax = 8f,
        restSeconds = 60,
        isSuperset = false,
        supersetGroupId = null,
        supersetRoundCount = null
    )

    @Test
    fun `Test 1 - Straps requirement`() {
        val ex = createExercise("Lat Pulldown", "pulldown", requiresStraps = true)
        val tEx = createTemplateExercise()
        val result = useCase(ex, tEx, "A")
        assertTrue(result.any { it.contains("REMINDER_STRAPS") })
        assertTrue(result.any { it.contains("REMINDER_SHOULDER_CONTRACTION") })
    }

    @Test
    fun `Test 2 - B Day Smith Machine Squat`() {
        val ex = createExercise("Smith Machine Squat", "squat", "smith_machine")
        val tEx = createTemplateExercise()
        val result = useCase(ex, tEx, "B")
        assertTrue(result.any { it.contains("REMINDER_CONTROLLED_SQUAT") })
    }

    @Test
    fun `Test 3 - B Day RDL`() {
        val ex = createExercise("Romanian Deadlift", "hinge", requiresStraps = true)
        val tEx = createTemplateExercise()
        val result = useCase(ex, tEx, "B")
        assertTrue(result.any { it.contains("REMINDER_RDL_HINGE") })
        assertTrue(result.any { it.contains("REMINDER_STRAPS") })
    }

    @Test
    fun `Test 4 - D Day Hack Squat`() {
        val ex = createExercise("Hack Squat", "squat", "machine")
        val tEx = createTemplateExercise()
        val result = useCase(ex, tEx, "D")
        assertTrue(result.any { it.contains("REMINDER_HACK_SQUAT") })
    }

    @Test
    fun `Test 5 - Facepull`() {
        val ex = createExercise("Cable Facepull", "facepull", "cable")
        val tEx = createTemplateExercise()
        val result = useCase(ex, tEx, "A")
        assertTrue(result.any { it.contains("REMINDER_SHOULDER_CONTRACTION") })
    }
}
