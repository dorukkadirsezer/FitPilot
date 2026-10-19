package com.doruk.fitprogress.core.database

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE)
class DatabaseSeederTest {

    private lateinit var db: GymFlowDatabase
    private lateinit var seeder: DatabaseSeeder

    @Before
    fun createDb() {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        db = Room.inMemoryDatabaseBuilder(context, GymFlowDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        seeder = DatabaseSeeder(db.workoutDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun verifySeeding() = runBlocking {
        // Run seeder
        seeder.seedDatabase()

        // 1. Verify exactly 4 main workout templates exist
        val templates = db.workoutDao().getAllTemplates().first()
        assertEquals(4, templates.size)
        
        val aDay = templates.find { it.dayCode == "A" }
        val bDay = templates.find { it.dayCode == "B" }
        val cDay = templates.find { it.dayCode == "C" }
        val dDay = templates.find { it.dayCode == "D" }
        
        assertTrue(aDay != null && bDay != null && cDay != null && dDay != null)

        // 2. Verify cardio rules
        assertEquals("INCLINE_WALK", aDay?.defaultCardioType)
        assertEquals(15, aDay?.defaultCardioMinutes)
        
        assertEquals(null, bDay?.defaultCardioType)
        assertEquals(null, bDay?.defaultCardioMinutes)

        assertEquals("ELLIPTICAL", cDay?.defaultCardioType)
        assertEquals(20, cDay?.defaultCardioMinutes)

        assertEquals("STATIONARY_BIKE", dDay?.defaultCardioType)
        assertEquals(15, dDay?.defaultCardioMinutes)

        // 3. We'd usually query template exercises, but WorkoutDao currently only returns all templates and all exercises
        val exercises = db.workoutDao().getAllExercises().first()
        assertEquals(20, exercises.size) // 6(A) + 4(B) + 6(C) + 4(D) = 20

        // Verify kg/lbs units exist in the exercises
        val bench = exercises.find { it.id == "ex_paused_bench_press" }
        assertEquals("lbs", bench?.defaultUnit)

        val shoulderDb = exercises.find { it.id == "ex_seated_db_shoulder_press" }
        assertEquals("kg", shoulderDb?.defaultUnit)
    }
}
