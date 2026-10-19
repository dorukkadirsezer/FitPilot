package com.doruk.fitprogress.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.doruk.fitprogress.core.database.DatabaseSeeder
import com.doruk.fitprogress.core.database.GymFlowDatabase
import com.doruk.fitprogress.core.database.dao.WorkoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        workoutDaoProvider: Provider<WorkoutDao>
    ): GymFlowDatabase {
        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE workout_exercise_sessions ADD COLUMN note TEXT")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN note TEXT")
            }
        }

        val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_exercise_sessions ADD COLUMN restTimerSeconds INTEGER")
            }
        }

        return Room.databaseBuilder(
            context,
            GymFlowDatabase::class.java,
            "gymflow_database"
        )
        .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
        .fallbackToDestructiveMigration()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    DatabaseSeeder(workoutDaoProvider.get()).seedDatabase()
                }
            }
        })
        .build()
    }

    @Provides
    fun provideWorkoutDao(database: GymFlowDatabase): WorkoutDao {
        return database.workoutDao()
    }

    @Provides
    fun provideNutritionDao(database: GymFlowDatabase): com.doruk.fitprogress.core.database.dao.NutritionDao {
        return database.nutritionDao()
    }

    @Provides
    fun provideAnalyticsDao(database: GymFlowDatabase): com.doruk.fitprogress.core.database.dao.AnalyticsDao {
        return database.analyticsDao()
    }

    @Provides
    fun provideAiDao(database: GymFlowDatabase): com.doruk.fitprogress.core.database.dao.AiDao {
        return database.aiDao()
    }
}
