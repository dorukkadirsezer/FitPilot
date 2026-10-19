package com.doruk.fitprogress.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_settings")

@Singleton
class AiSettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val IS_AI_ENABLED = booleanPreferencesKey("is_ai_enabled")
        val SAVE_AI_LOGS = booleanPreferencesKey("save_ai_logs")
        private const val GEMINI_API_KEY_PREF = "gemini_api_key_secure"
    }

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _geminiApiKeyFlow = MutableStateFlow<String?>(
        sharedPreferences.getString(GEMINI_API_KEY_PREF, null)
    )

    val isAiEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_AI_ENABLED] ?: false
        }

    val geminiApiKeyFlow: Flow<String?> = _geminiApiKeyFlow

    val saveAiLogsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SAVE_AI_LOGS] ?: true
        }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_AI_ENABLED] = enabled
        }
    }

    suspend fun setGeminiApiKey(key: String?) {
        if (key == null) {
            sharedPreferences.edit().remove(GEMINI_API_KEY_PREF).apply()
        } else {
            sharedPreferences.edit().putString(GEMINI_API_KEY_PREF, key).apply()
        }
        _geminiApiKeyFlow.value = key
        
        // Remove from old datastore if it existed
        context.dataStore.edit { preferences ->
            val oldKey = androidx.datastore.preferences.core.stringPreferencesKey("gemini_api_key")
            preferences.remove(oldKey)
        }
    }

    suspend fun setSaveAiLogs(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_AI_LOGS] = save
        }
    }
}
