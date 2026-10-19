package com.doruk.fitprogress.core.database

import com.doruk.fitprogress.core.database.dao.WorkoutDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MojibakeMigrationUtils {
    
    /**
     * Replaces corrupted strings (mojibake) in seeded templates and exercises.
     * This is idempotent and strictly limited to built-in templates and exercises.
     */
    suspend fun repairDatabaseMojibake(dao: WorkoutDao) {
        withContext(Dispatchers.IO) {
            val badSequence = "\u00E2\u20AC\u201D"
            val goodSequence = "\u2014"
            
            val templates = dao.getAllTemplatesSync()
            val exercises = dao.getAllExercisesSync()
            
            // Repair seeded templates (tpl_*)
            templates.filter { it.id.startsWith("tpl_") && it.name.contains(badSequence) }
                .forEach { template ->
                    val repaired = template.copy(name = template.name.replace(badSequence, goodSequence))
                    dao.insertTemplates(listOf(repaired))
                }
                
            // Repair seeded exercises (ex_*)
            exercises.filter { it.id.startsWith("ex_") && it.name.contains(badSequence) }
                .forEach { exercise ->
                    val repaired = exercise.copy(name = exercise.name.replace(badSequence, goodSequence))
                    dao.insertExercises(listOf(repaired))
                }
        }
    }
}
