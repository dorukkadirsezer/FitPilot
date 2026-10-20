package com.doruk.fitprogress.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "profile_settings")

@Singleton
class ProfileDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.profileDataStore

    companion object {
        val ATHLETE_NAME = stringPreferencesKey("athlete_name")
        val TARGET_CALORIES = intPreferencesKey("target_calories")
        val TARGET_PROTEIN = intPreferencesKey("target_protein")
        val TARGET_FAT = intPreferencesKey("target_fat")
        val TARGET_CARBS = intPreferencesKey("target_carbs")
        val DAILY_STEPS_TARGET = intPreferencesKey("daily_steps_target")
        val SLEEP_HOURS_TARGET = floatPreferencesKey("sleep_hours_target")
    }

    val athleteNameFlow: Flow<String?> = dataStore.data.map { it[ATHLETE_NAME] }
    val targetCaloriesFlow: Flow<Int?> = dataStore.data.map { it[TARGET_CALORIES] }
    val targetProteinFlow: Flow<Int?> = dataStore.data.map { it[TARGET_PROTEIN] }
    val targetFatFlow: Flow<Int?> = dataStore.data.map { it[TARGET_FAT] }
    val targetCarbsFlow: Flow<Int?> = dataStore.data.map { it[TARGET_CARBS] }
    val dailyStepsTargetFlow: Flow<Int?> = dataStore.data.map { it[DAILY_STEPS_TARGET] }
    val sleepHoursTargetFlow: Flow<Float?> = dataStore.data.map { it[SLEEP_HOURS_TARGET] }

    suspend fun setAthleteName(name: String) {
        dataStore.edit { it[ATHLETE_NAME] = name }
    }

    suspend fun setNutritionTargets(
        calories: Int,
        protein: Int,
        fat: Int,
        carbs: Int,
        steps: Int,
        sleep: Float
    ) {
        dataStore.edit { prefs ->
            prefs[TARGET_CALORIES] = calories
            prefs[TARGET_PROTEIN] = protein
            prefs[TARGET_FAT] = fat
            prefs[TARGET_CARBS] = carbs
            prefs[DAILY_STEPS_TARGET] = steps
            prefs[SLEEP_HOURS_TARGET] = sleep
        }
    }
}
