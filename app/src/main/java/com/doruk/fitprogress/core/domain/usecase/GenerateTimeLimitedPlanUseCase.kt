package com.doruk.fitprogress.core.domain.usecase

import com.doruk.fitprogress.core.database.entity.TimeLimitedWorkoutPlanEntity
import javax.inject.Inject
import com.doruk.fitprogress.core.database.entity.TimeLimitedWorkoutPlanExerciseEntity
import com.doruk.fitprogress.core.database.entity.TemplateWithDetails
import java.util.UUID

class GenerateTimeLimitedPlanUseCase @Inject constructor() {

    operator fun invoke(
        templateDetails: TemplateWithDetails,
        timeMode: String, // "60", "45", "30"
        isLightMode: Boolean
    ): Pair<TimeLimitedWorkoutPlanEntity, List<TimeLimitedWorkoutPlanExerciseEntity>> {
        val planId = UUID.randomUUID().toString()
        val templateId = templateDetails.template.id
        val dayCode = templateDetails.template.dayCode

        val planEntity = TimeLimitedWorkoutPlanEntity(
            id = planId,
            originalTemplateId = templateId,
            maxDurationMinutes = timeMode.toIntOrNull() ?: 60
        )

        val exerciseEntities = mutableListOf<TimeLimitedWorkoutPlanExerciseEntity>()

        // Helper to quickly find exercise by name or fallback to index
        val sortedEx = templateDetails.exercises.sortedBy { it.templateExercise.orderIndex }
        
        fun addEx(exId: String, status: String, reason: String? = null) {
            val ex = sortedEx.find { it.exercise.id == exId } ?: return
            exerciseEntities.add(
                TimeLimitedWorkoutPlanExerciseEntity(
                    id = UUID.randomUUID().toString(),
                    planId = planId,
                    exerciseId = exId,
                    status = status,
                    reason = reason,
                    orderIndex = ex.templateExercise.orderIndex
                )
            )
        }

        // Apply rules based on day code
        when (dayCode) {
            "A" -> { // Upper 1
                // Bench(1), Pulldown(2), Shoulder Press(3), Cable Row(4), Tri/Hammer(5)
                sortedEx.forEach { ex ->
                    val name = ex.exercise.name.lowercase()
                    val id = ex.exercise.id
                    when (timeMode) {
                        "60" -> {
                            addEx(id, "included") // Keep all
                        }
                        "45" -> {
                            if (name.contains("pushdown") || name.contains("hammer curl")) {
                                addEx(id, "optional", "Superset optional for 45 min")
                            } else {
                                addEx(id, "included")
                            }
                        }
                        "30" -> {
                            if (name.contains("pushdown") || name.contains("hammer curl")) {
                                addEx(id, "skipped", "Removed superset for 30 min")
                            } else if (name.contains("row")) {
                                addEx(id, "skipped", "Removed Row to save time for 30 min")
                            } else {
                                addEx(id, "included") // Keep Bench, Pulldown, Shoulder Press
                            }
                        }
                    }
                }
            }
            "B" -> { // Lower 1
                // Squat(1), RDL(2), Leg Ext(3), Calf(4)
                sortedEx.forEach { ex ->
                    val name = ex.exercise.name.lowercase()
                    val id = ex.exercise.id
                    when (timeMode) {
                        "60" -> addEx(id, "included")
                        "45" -> {
                            if (name.contains("calf")) addEx(id, "optional")
                            else addEx(id, "included")
                        }
                        "30" -> {
                            if (name.contains("calf") || name.contains("extension")) {
                                addEx(id, "skipped", "Skipped accessories for 30 min")
                            } else {
                                addEx(id, "included")
                            }
                        }
                    }
                }
            }
            "C" -> { // Upper 2
                // Incline(1), Pulldown(2), Lateral(3), Facepull(4), Biceps/Triceps(5)
                sortedEx.forEach { ex ->
                    val name = ex.exercise.name.lowercase()
                    val id = ex.exercise.id
                    when (timeMode) {
                        "60" -> addEx(id, "included")
                        "45" -> {
                            if (name.contains("biceps") || name.contains("triceps")) {
                                addEx(id, "optional")
                            } else {
                                addEx(id, "included")
                            }
                        }
                        "30" -> {
                            if (name.contains("biceps") || name.contains("triceps")) {
                                addEx(id, "skipped")
                            } else if (name.contains("facepull")) {
                                addEx(id, "skipped")
                            } else {
                                addEx(id, "included")
                            }
                        }
                    }
                }
            }
            "D" -> { // Lower 2
                // Hack(1), Goblet(2), Leg Curl(3), Calf(4)
                sortedEx.forEach { ex ->
                    val name = ex.exercise.name.lowercase()
                    val id = ex.exercise.id
                    when (timeMode) {
                        "60" -> addEx(id, "included")
                        "45" -> {
                            if (name.contains("calf")) addEx(id, "optional")
                            else addEx(id, "included")
                        }
                        "30" -> {
                            if (name.contains("calf") || name.contains("goblet")) {
                                addEx(id, "skipped")
                            } else {
                                addEx(id, "included")
                            }
                        }
                    }
                }
            }
            else -> {
                sortedEx.forEach { addEx(it.exercise.id, "included") }
            }
        }

        return Pair(planEntity, exerciseEntities)
    }
}
