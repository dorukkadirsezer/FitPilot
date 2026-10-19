package com.doruk.fitprogress.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.workoutSettingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "workout_settings")

@Singleton
class WorkoutSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.workoutSettingsDataStore

    companion object {
        val TIMER_SOUND_ENABLED = booleanPreferencesKey("timer_sound_enabled")
        val TIMER_WARNING_ENABLED = booleanPreferencesKey("timer_warning_enabled")
        val TIMER_FINISHED_ENABLED = booleanPreferencesKey("timer_finished_enabled")
        val TIMER_VIBRATION_ENABLED = booleanPreferencesKey("timer_vibration_enabled")
        val APP_THEME_MODE = androidx.datastore.preferences.core.stringPreferencesKey("app_theme_mode")
        val TIMER_SOUND_STYLE = androidx.datastore.preferences.core.stringPreferencesKey("timer_sound_style")
    }

    val timerSoundEnabled: Flow<Boolean> = dataStore.data.map { it[TIMER_SOUND_ENABLED] ?: true }
    val timerWarningEnabled: Flow<Boolean> = dataStore.data.map { it[TIMER_WARNING_ENABLED] ?: true }
    val timerFinishedEnabled: Flow<Boolean> = dataStore.data.map { it[TIMER_FINISHED_ENABLED] ?: true }
    val timerVibrationEnabled: Flow<Boolean> = dataStore.data.map { it[TIMER_VIBRATION_ENABLED] ?: true }

    val themeMode: Flow<com.doruk.fitprogress.core.domain.model.ThemeMode> = dataStore.data.map { prefs ->
        val modeString = prefs[APP_THEME_MODE] ?: com.doruk.fitprogress.core.domain.model.ThemeMode.SYSTEM_DEFAULT.name
        try {
            com.doruk.fitprogress.core.domain.model.ThemeMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            com.doruk.fitprogress.core.domain.model.ThemeMode.SYSTEM_DEFAULT
        }
    }

    val timerSoundStyle: Flow<com.doruk.fitprogress.core.domain.model.TimerSoundStyle> = dataStore.data.map { prefs ->
        val styleString = prefs[TIMER_SOUND_STYLE] ?: com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP.name
        try {
            com.doruk.fitprogress.core.domain.model.TimerSoundStyle.valueOf(styleString)
        } catch (e: IllegalArgumentException) {
            com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP
        }
    }

    suspend fun setTimerSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[TIMER_SOUND_ENABLED] = enabled }
    }

    suspend fun setTimerWarningEnabled(enabled: Boolean) {
        dataStore.edit { it[TIMER_WARNING_ENABLED] = enabled }
    }

    suspend fun setTimerFinishedEnabled(enabled: Boolean) {
        dataStore.edit { it[TIMER_FINISHED_ENABLED] = enabled }
    }

    suspend fun setTimerVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[TIMER_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setThemeMode(mode: com.doruk.fitprogress.core.domain.model.ThemeMode) {
        dataStore.edit { it[APP_THEME_MODE] = mode.name }
    }

    suspend fun setTimerSoundStyle(style: com.doruk.fitprogress.core.domain.model.TimerSoundStyle) {
        dataStore.edit { it[TIMER_SOUND_STYLE] = style.name }
    }
}
