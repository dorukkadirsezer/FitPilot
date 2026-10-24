package com.doruk.fitprogress.core.datastore

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.GeneralSecurityException
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
        val HAS_ACCEPTED_AI_DATA_SHARING = booleanPreferencesKey("has_accepted_ai_data_sharing")
        private const val SECURE_PREFS_FILE = "ai_secure_prefs"
        private const val GEMINI_API_KEY_PREF = "gemini_api_key_secure"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        createSecurePrefsOrRecover()
    }

    private val _geminiApiKeyFlow: MutableStateFlow<String?> by lazy {
        MutableStateFlow(
            runCatching { sharedPreferences.getString(GEMINI_API_KEY_PREF, null) }.getOrNull()
        )
    }

    private fun createSecurePrefsOrRecover(): SharedPreferences {
        fun buildEncryptedPrefs(): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context,
                SECURE_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        return try {
            buildEncryptedPrefs()
        } catch (e: GeneralSecurityException) {
            // H1 Recovery: If Keystore master key was invalidated/restored on a new device,
            // clear corrupted encrypted prefs file and recreate cleanly instead of crashing at startup.
            runCatching { context.deleteSharedPreferences(SECURE_PREFS_FILE) }
            buildEncryptedPrefs()
        } catch (e: IOException) {
            runCatching { context.deleteSharedPreferences(SECURE_PREFS_FILE) }
            buildEncryptedPrefs()
        }
    }

    val isAiEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_AI_ENABLED] ?: false
        }

    val hasAcceptedDataSharingFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[HAS_ACCEPTED_AI_DATA_SHARING] ?: false
        }

    val geminiApiKeyFlow: Flow<String?>
        get() = _geminiApiKeyFlow

    val saveAiLogsFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[SAVE_AI_LOGS] ?: true
        }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_AI_ENABLED] = enabled
            if (!enabled) {
                preferences[HAS_ACCEPTED_AI_DATA_SHARING] = false
            }
        }
    }

    suspend fun setDataSharingAccepted(accepted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_ACCEPTED_AI_DATA_SHARING] = accepted
            if (accepted) {
                preferences[IS_AI_ENABLED] = true
            }
        }
    }

    suspend fun setGeminiApiKey(key: String?) {
        val trimmed = key?.trim()
        if (trimmed.isNullOrBlank()) {
            sharedPreferences.edit().remove(GEMINI_API_KEY_PREF).apply()
            _geminiApiKeyFlow.value = null
        } else {
            sharedPreferences.edit().putString(GEMINI_API_KEY_PREF, trimmed).apply()
            _geminiApiKeyFlow.value = trimmed
        }

        // Remove legacy plain key if it ever existed; do NOT override user's explicit IS_AI_ENABLED consent toggle (H2)
        context.dataStore.edit { preferences ->
            val oldKey = stringPreferencesKey("gemini_api_key")
            preferences.remove(oldKey)
        }
    }

    suspend fun setSaveAiLogs(save: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_AI_LOGS] = save
        }
    }
}
