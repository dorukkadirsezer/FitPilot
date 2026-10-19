package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import javax.inject.Inject

class GetProgramRiskRemindersUseCase @Inject constructor() {

    operator fun invoke(
        exercise: ExerciseEntity,
        templateExercise: WorkoutTemplateExerciseEntity,
        dayCode: String // "A", "B", "C", "D"
    ): List<String> {
        val reminders = mutableListOf<String>()

        // 1. Straps requirement
        if (exercise.requiresStraps) {
            reminders.add("REMINDER_STRAPS")
        }

        // 2. Row / Pulldown / Facepull movement type
        if (exercise.movementType.equals("row", ignoreCase = true) ||
            exercise.movementType.equals("pulldown", ignoreCase = true) ||
            exercise.movementType.equals("facepull", ignoreCase = true) ||
            exercise.name.contains("Facepull", ignoreCase = true) || 
            exercise.name.contains("Pulldown", ignoreCase = true) ||
            exercise.name.contains("Row", ignoreCase = true)
        ) {
            reminders.add("REMINDER_SHOULDER_CONTRACTION")
        }

        // 3. Day B specific reminders
        if (dayCode == "B") {
            if (exercise.movementType.equals("squat", ignoreCase = true) || exercise.name.contains("Squat", ignoreCase = true)) {
                reminders.add("REMINDER_CONTROLLED_SQUAT")
            }
            if (exercise.movementType.equals("hinge", ignoreCase = true) || exercise.name.contains("Romanian Deadlift", ignoreCase = true)) {
                reminders.add("REMINDER_RDL_HINGE")
            }
        }

        // 4. Day D specific reminders
        if (dayCode == "D") {
            if (exercise.movementType.equals("squat", ignoreCase = true) || exercise.name.contains("Hack Squat", ignoreCase = true)) {
                reminders.add("REMINDER_HACK_SQUAT")
            }
        }

        return reminders
    }
}
