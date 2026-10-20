package com.doruk.fitprogress.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.core.datastore.AiSettingsDataStore
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.VividViolet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.doruk.fitprogress.core.datastore.WorkoutSettingsDataStore
import com.doruk.fitprogress.core.domain.model.ThemeMode
import androidx.compose.ui.res.stringResource
import com.doruk.fitprogress.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AiSettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var apiKeyVisible by remember { mutableStateOf(false) }
    var tempApiKey by remember(state.apiKey) { mutableStateOf(state.apiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.smart_settings), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.app_theme),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeOptions = listOf(
                            ThemeMode.LIGHT to stringResource(R.string.light),
                            ThemeMode.DARK to stringResource(R.string.dark),
                            ThemeMode.SYSTEM_DEFAULT to stringResource(R.string.system_default)
                        )
                        themeOptions.forEach { (mode, label) ->
                            val isSelected = state.themeMode == mode
                            Button(
                                onClick = { viewModel.updateThemeMode(mode) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore("-")
                        val langOptions = listOf(
                            "" to stringResource(R.string.system_default),
                            "en" to stringResource(R.string.english),
                            "tr" to stringResource(R.string.turkish)
                        )
                        langOptions.forEach { (langTag, label) ->
                            val isSelected = (currentLocale.isEmpty() && langTag.isEmpty()) || (currentLocale == langTag)
                            Button(
                                onClick = { 
                                    androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                                        androidx.core.os.LocaleListCompat.forLanguageTags(langTag)
                                    ) 
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(label)
                            }
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.ai_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.enable_smart_features),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.unlock_smart_features),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.isAiEnabled,
                            onCheckedChange = { viewModel.toggleAiEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        )
                    }
                }
            }

            if (state.isAiEnabled) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.gemini_api_key),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.api_key_stored_locally),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(Modifier.height(16.dp))

                        GymFlowTextField(
                            value = tempApiKey,
                            onValueChange = { tempApiKey = it },
                            label = stringResource(R.string.api_key),
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )
                        
                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Text(if (apiKeyVisible) stringResource(R.string.hide_key) else stringResource(R.string.show_key), color = MaterialTheme.colorScheme.primary)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { 
                                    tempApiKey = ""
                                    viewModel.clearApiKey() 
                                }) {
                                    Text(stringResource(R.string.clear), color = MaterialTheme.colorScheme.error)
                                }
                                AiActionButton(
                                    onClick = { viewModel.updateApiKey(tempApiKey) },
                                    text = stringResource(R.string.save_key)
                                )
                            }
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.save_ai_logs),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.locally_stores_prompt_summaries),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.saveAiLogs,
                            onCheckedChange = { viewModel.toggleSaveLogs(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.secondary, checkedTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }
}

@HiltViewModel
class AiSettingsViewModel @Inject constructor(
    private val dataStore: AiSettingsDataStore,
    private val workoutSettingsDataStore: WorkoutSettingsDataStore
) : ViewModel() {

    val uiState = combine(
        dataStore.isAiEnabledFlow,
        dataStore.geminiApiKeyFlow,
        dataStore.saveAiLogsFlow,
        workoutSettingsDataStore.themeMode
    ) { isEnabled, apiKey, saveLogs, theme ->
        AiSettingsUiState(
            isAiEnabled = isEnabled,
            apiKey = apiKey ?: "",
            saveAiLogs = saveLogs,
            themeMode = theme
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiSettingsUiState())

    fun toggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch { dataStore.setAiEnabled(enabled) }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch { dataStore.setGeminiApiKey(key) }
    }

    fun clearApiKey() {
        viewModelScope.launch { dataStore.setGeminiApiKey(null) }
    }

    fun toggleSaveLogs(save: Boolean) {
        viewModelScope.launch { dataStore.setSaveAiLogs(save) }
    }
    
    fun updateThemeMode(mode: ThemeMode) {
        viewModelScope.launch { workoutSettingsDataStore.setThemeMode(mode) }
    }
}

data class AiSettingsUiState(
    val isAiEnabled: Boolean = false,
    val apiKey: String = "",
    val saveAiLogs: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT
)
