package com.doruk.fitprogress.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.datastore.AiSettingsDataStore
import com.doruk.fitprogress.core.datastore.WorkoutSettingsDataStore
import com.doruk.fitprogress.core.domain.model.ThemeMode
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.StandardSurfaceCard
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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
                title = {
                    Text(
                        text = stringResource(R.string.smart_settings),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance & Language Card (Zero-Shadow + 1.dp Hairline)
            StandardSurfaceCard {
                Text(
                    text = stringResource(R.string.app_theme),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.language),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentLocale = AppCompatDelegate.getApplicationLocales()
                        .toLanguageTags()
                        .substringBefore("-")
                    val langOptions = listOf(
                        "" to stringResource(R.string.system_default),
                        "en" to stringResource(R.string.english),
                        "tr" to stringResource(R.string.turkish)
                    )
                    langOptions.forEach { (langTag, label) ->
                        val isSelected = (currentLocale.isEmpty() && langTag.isEmpty()) || (currentLocale == langTag)
                        Button(
                            onClick = {
                                AppCompatDelegate.setApplicationLocales(
                                    LocaleListCompat.forLanguageTags(langTag)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // 2. AI Disclaimer Banner Card
            StandardSurfaceCard(
                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.40f),
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ai_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3. Smart Features Master Switch Card
            StandardSurfaceCard {
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
                            color = ElectricCobalt
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
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AthleticEmerald,
                            checkedTrackColor = AthleticEmerald.copy(alpha = 0.35f)
                        )
                    )
                }
            }

            // 4. Encrypted Gemini API Key Vault Card
            if (state.isAiEnabled) {
                StandardSurfaceCard(borderColor = ElectricCobalt.copy(alpha = 0.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.gemini_api_key),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        StatusPillBadge(
                            text = if (state.apiKey.isNotBlank()) stringResource(R.string.status_configured) else stringResource(R.string.status_empty),
                            type = if (state.apiKey.isNotBlank()) BadgeType.SUCCESS else BadgeType.WARNING
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.api_key_stored_locally),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(14.dp))

                    GymFlowTextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        label = stringResource(R.string.api_key),
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (apiKeyVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Text(
                                text = if (apiKeyVisible) stringResource(R.string.hide_key) else stringResource(R.string.show_key),
                                color = ElectricCobalt,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    tempApiKey = ""
                                    viewModel.clearApiKey()
                                }
                            ) {
                                Text(
                                    text = stringResource(R.string.clear),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            AiActionButton(
                                onClick = { viewModel.updateApiKey(tempApiKey) },
                                text = stringResource(R.string.save_key)
                            )
                        }
                    }
                }

                // 5. Save AI Prompt Logs Toggle Card
                StandardSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AthleticEmerald,
                                checkedTrackColor = AthleticEmerald.copy(alpha = 0.35f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
