package com.doruk.fitprogress.core.database

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import com.doruk.fitprogress.core.database.entity.ExerciseEntity
import com.doruk.fitprogress.core.database.entity.TemplateWarmupInstructionEntity
import com.doruk.fitprogress.core.database.entity.TemplateWarmupSetEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateExerciseEntity
import java.util.UUID

class DatabaseSeeder(private val dao: WorkoutDao) {

    suspend fun seedDatabase() {
        // Pre-populate Exercises
        val exercises = listOf(
            // A Day
            ExerciseEntity("ex_paused_bench_press", "Paused Bench Press", "chest", "barbell", "compound", 150, "lbs", true, false, "COACH_LOCK_SHOULDER_BLADES", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_lat_pulldown_wide", "Lat Pulldown — Wide Grip", "back", "cable", "machine", 120, "lbs", false, true, "COACH_STRAPS_REQUIRED", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_seated_db_shoulder_press", "Seated Dumbbell Shoulder Press", "shoulders", "dumbbells", "compound", 120, "kg", true, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_seated_cable_row", "Seated Cable Row", "back", "cable", "machine", 120, "lbs", true, true, "COACH_STRAPS_REQUIRED", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_triceps_pushdown", "Triceps Pushdown", "triceps", "cable", "isolation", 90, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_hammer_curl", "Hammer Curl", "biceps", "dumbbells", "isolation", 90, "kg", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            // B Day
            ExerciseEntity("ex_smith_machine_squat", "Smith Machine Squat", "legs", "smith machine", "compound", 180, "lbs", true, false, "Because of previous knee sensitivity, form is the top priority. Lower under control. Do not lock the knees aggressively at the top and avoid placing extra stress on the joint.", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_romanian_deadlift", "Romanian Deadlift", "legs", "barbell", "compound", 150, "lbs", true, true, "COACH_PUSH_HIPS_BACK", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_leg_extension", "Leg Extension", "legs", "machine", "isolation", 120, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_calf_raise_smith", "Calf Raise — Smith Machine", "calves", "smith machine", "isolation", 90, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            // C Day
            ExerciseEntity("ex_incline_machine_press", "Incline Machine Press", "chest", "machine", "compound", 150, "kg", true, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_lat_pulldown_narrow", "Lat Pulldown — Narrow / Neutral Grip", "back", "cable", "machine", 120, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_db_lateral_raise", "Dumbbell Lateral Raise", "shoulders", "dumbbells", "isolation", 90, "kg", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_facepull_cable", "Facepull — Cable", "shoulders", "cable", "isolation", 90, "kg", false, false, "As you pull the rope toward your face, move your shoulders back and focus on improving posture.", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_biceps_curl_machine", "Biceps Curl Machine", "biceps", "machine", "isolation", 90, "kg", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_db_triceps_extension", "Dumbbell Triceps Extension", "triceps", "dumbbells", "isolation", 90, "kg", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            // D Day
            ExerciseEntity("ex_smith_hack_squat", "Smith Machine Hack Squat — Feet Forward", "legs", "smith machine", "compound", 150, "lbs", true, false, "Place your feet slightly forward to minimize lower-back loading and focus directly on the quads. This angle helps reduce knee stress when performed with controlled form.", System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_db_goblet_squat", "Dumbbell Goblet Squat", "legs", "dumbbells", "compound", 120, "kg", true, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_lying_leg_curl", "Lying Leg Curl", "legs", "machine", "isolation", 120, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis()),
            ExerciseEntity("ex_calf_raise", "Calf Raise", "calves", "machine", "isolation", 90, "lbs", false, false, null, System.currentTimeMillis(), System.currentTimeMillis())
        )
        dao.insertExercises(exercises)

        // Pre-populate Templates
        val templates = listOf(
            WorkoutTemplateEntity("tpl_a_day", "A Day — Upper Body 1", "A", "DESC_TPL_A", "INCLINE_WALK", 15),
            WorkoutTemplateEntity("tpl_b_day", "B Day — Lower Body 1", "B", "DESC_TPL_B", null, null),
            WorkoutTemplateEntity("tpl_c_day", "C Day — Upper Body 2", "C", "DESC_TPL_C", "ELLIPTICAL", 20),
            WorkoutTemplateEntity("tpl_d_day", "D Day — Lower Body 2", "D", "DESC_TPL_D", "STATIONARY_BIKE", 15)
        )
        dao.insertTemplates(templates)

        // Generate template exercises
        val templateExercises = mutableListOf<WorkoutTemplateExerciseEntity>()
        val aDaySupersetGroup = UUID.randomUUID().toString()
        val cDaySupersetGroup = UUID.randomUUID().toString()

        // A Day
        val aDayEx1 = UUID.randomUUID().toString()
        templateExercises.add(WorkoutTemplateExerciseEntity(aDayEx1, "tpl_a_day", "ex_paused_bench_press", 1, false, null, null, 3, 8, 10, 95f, 105f, "lbs", 150, 7f, 7f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_a_day", "ex_lat_pulldown_wide", 2, false, null, null, 3, 10, 12, 80f, 80f, "lbs", 120, 7f, 7f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_a_day", "ex_seated_db_shoulder_press", 3, false, null, null, 3, 10, 12, 10f, 12.5f, "kg", 120, 7f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_a_day", "ex_seated_cable_row", 4, false, null, null, 3, 10, 12, 80f, 80f, "lbs", 120, 7.5f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_a_day", "ex_triceps_pushdown", 5, true, aDaySupersetGroup, 3, 3, 12, 15, 20f, 30f, "lbs", 90, 8f, 8f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_a_day", "ex_hammer_curl", 6, true, aDaySupersetGroup, 3, 3, 12, 15, 7.5f, 10f, "kg", 90, 8f, 8f))

        // B Day
        val bDayEx1 = UUID.randomUUID().toString()
        templateExercises.add(WorkoutTemplateExerciseEntity(bDayEx1, "tpl_b_day", "ex_smith_machine_squat", 1, false, null, null, 3, 8, 10, 95f, 105f, "lbs", 180, 7f, 7f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_b_day", "ex_romanian_deadlift", 2, false, null, null, 3, 10, 12, 95f, 95f, "lbs", 180, 7f, 7f)) // using 180 as it's 150-180
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_b_day", "ex_leg_extension", 3, false, null, null, 3, 12, 15, 70f, 70f, "lbs", 120, 7.5f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_b_day", "ex_calf_raise_smith", 4, false, null, null, 3, 15, 20, null, null, "lbs", 90, 8f, 8f))

        // C Day
        val cDayEx1 = UUID.randomUUID().toString()
        templateExercises.add(WorkoutTemplateExerciseEntity(cDayEx1, "tpl_c_day", "ex_incline_machine_press", 1, false, null, null, 3, 8, 10, 25f, 30f, "kg", 150, 7f, 7f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_c_day", "ex_lat_pulldown_narrow", 2, false, null, null, 3, 10, 12, 70f, 80f, "lbs", 120, 7f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_c_day", "ex_db_lateral_raise", 3, false, null, null, 3, 12, 15, 7.5f, 7.5f, "kg", 90, 7.5f, 8f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_c_day", "ex_facepull_cable", 4, false, null, null, 3, 15, 20, 15f, 20f, "kg", 90, 7.5f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_c_day", "ex_biceps_curl_machine", 5, true, cDaySupersetGroup, 3, 3, 12, 15, null, null, "kg", 90, 8f, 8f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_c_day", "ex_db_triceps_extension", 6, true, cDaySupersetGroup, 3, 3, 12, 15, null, null, "kg", 90, 8f, 8f))

        // D Day
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_d_day", "ex_smith_hack_squat", 1, false, null, null, 3, 10, 12, 70f, 90f, "lbs", 150, 7f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_d_day", "ex_db_goblet_squat", 2, false, null, null, 3, 10, 12, 15f, 15f, "kg", 120, 7.5f, 7.5f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_d_day", "ex_lying_leg_curl", 3, false, null, null, 3, 12, 15, null, null, "lbs", 120, 7.5f, 8f))
        templateExercises.add(WorkoutTemplateExerciseEntity(UUID.randomUUID().toString(), "tpl_d_day", "ex_calf_raise", 4, false, null, null, 3, 15, 20, null, null, "lbs", 90, 8f, 8f))

        dao.insertTemplateExercises(templateExercises)

        // Warmup instructions
        val warmups = listOf(
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_a_day", "WARMUP_LIGHT_WALKING", 1, 5, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_a_day", "WARMUP_SHOULDER_CHEST_MOBILITY", 2, null, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_b_day", "WARMUP_BIKE_LIGHT", 1, 5, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_b_day", "WARMUP_LOWER_BODY_DYNAMIC", 2, null, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_c_day", "WARMUP_ELLIPTICAL_LIGHT", 1, 5, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_c_day", "WARMUP_UPPER_BODY_DYNAMIC", 2, null, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_d_day", "WARMUP_BIKE_LIGHT", 1, 5, true),
            TemplateWarmupInstructionEntity(UUID.randomUUID().toString(), "tpl_d_day", "WARMUP_HIP_ANKLE_MOBILITY", 2, null, true)
        )
        dao.insertWarmupInstructions(warmups)

        // Add warmup sets
        val warmupSets = listOf(
            TemplateWarmupSetEntity(UUID.randomUUID().toString(), aDayEx1, 1, 10, null, "lbs", "NOTE_EMPTY_BAR", 60),
            TemplateWarmupSetEntity(UUID.randomUUID().toString(), aDayEx1, 2, 6, 65f, "lbs", "NOTE_LIGHT", 90),
            
            TemplateWarmupSetEntity(UUID.randomUUID().toString(), bDayEx1, 1, 10, null, "lbs", "NOTE_EMPTY_BAR", 60),
            
            TemplateWarmupSetEntity(UUID.randomUUID().toString(), cDayEx1, 1, 10, null, "kg", "NOTE_LIGHT", 60)
        )
        dao.insertWarmupSets(warmupSets)
    }
}


