package com.doruk.fitprogress.feature.dashboard

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.ui.components.*
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onTemplateClick: (String) -> Unit,
    onNavigateToCoaching: (String) -> Unit,
    onNavigateToNutrition: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToWorkoutHistory: () -> Unit,
    onNavigateToBodyMetrics: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToActiveWorkout: (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    LaunchedEffect(
        uiState.importSuccessMessage,
        uiState.importError,
        uiState.exportSuccessMessage,
        uiState.exportError
    ) {
        val msg = uiState.importSuccessMessage
            ?: uiState.importError
            ?: uiState.exportSuccessMessage
            ?: uiState.exportError
        if (msg != null) {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearImportMessages()
        }
    }

    LaunchedEffect(uiState.importedSessionId) {
        val sessionId = uiState.importedSessionId
        if (!sessionId.isNullOrBlank()) {
            onNavigateToActiveWorkout(sessionId)
            viewModel.clearImportedSessionId()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    FitPilotBrandLogo(
                        modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                        size = 38.dp,
                        showContainer = true
                    )
                },
                title = {
                    FitPilotBrandLockup(
                        title = "FitPilot",
                        badgeText = "BETA",
                        showLogoIcon = false
                    )
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAiSettings,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.smart_settings),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        contentWindowInsets = WindowInsets.systemBars,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // 1. Hero Header Card (Zero-Shadow + Hairline Border)
                item {
                    GradientHeaderCard(
                        title = stringResource(R.string.ready_to_crush_it),
                        subtitle = stringResource(R.string.welcome_back)
                    )
                }

                // 2. AI Coach Program Sync Hub Card (Zero-Shadow, 16.dp radius, 12.dp buttons)
                item {
                    AiCoachSyncHubCard(
                        onImportClick = { viewModel.importCoachPlan() },
                        onExportClick = { viewModel.exportProgram() }
                    )
                }

                // 3. Nutrition Telemetry Capsule Card
                item {
                    DashboardNutritionTelemetryCard(
                        uiState = uiState,
                        onClick = onNavigateToNutrition
                    )
                }

                // 4. Quick Telemetry Navigation Row (Analytics, Body Metrics, History)
                item {
                    val completedCount = uiState.completedWorkoutsCount
                    val latestWeight = uiState.analyticsSummary?.bodySummary?.latestWeight
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickNavTelemetryPillCard(
                            title = stringResource(R.string.analytics),
                            subtitle = stringResource(R.string.weekly_snapshot),
                            icon = Icons.Default.Insights,
                            accentColor = ElectricCobalt,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAnalytics
                        )
                        QuickNavTelemetryPillCard(
                            title = stringResource(R.string.body),
                            subtitle = if (latestWeight != null) "$latestWeight kg" else stringResource(R.string.view_trends),
                            icon = Icons.Default.MonitorWeight,
                            accentColor = AthleticEmerald,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToBodyMetrics
                        )
                        QuickNavTelemetryPillCard(
                            title = stringResource(R.string.history),
                            subtitle = "$completedCount ${stringResource(R.string.completed)}",
                            icon = Icons.Default.History,
                            accentColor = TelemetryAmber,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToWorkoutHistory
                        )
                    }
                }

                // 5. Select a Workout Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.select_workout),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        StatusPillBadge(
                            text = stringResource(R.string.plans_count_format, uiState.templates.size),
                            type = BadgeType.INFO
                        )
                    }
                }

                // 6. Workout Template Cards
                items(
                    items = uiState.templates,
                    key = { it.id }
                ) { template ->
                    DashboardTemplateCard(
                        template = template,
                        onCardClick = { onTemplateClick(template.id) },
                        onStartClick = { onNavigateToCoaching(template.id) }
                    )
                }

                // 7. Bottom Edge-to-Edge Navigation Bar Inset Spacer
                item {
                    Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
                }
            }
        }
    }
}

@Composable
private fun AiCoachSyncHubCard(
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    StandardSurfaceCard(
        borderColor = ElectricCobalt.copy(alpha = 0.35f),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ElectricCobalt.copy(alpha = 0.14f))
                        .border(1.dp, ElectricCobalt.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCobalt,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.ai_coach_sync_hub),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.ai_coach_sync_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPillBadge(text = stringResource(R.string.status_ready), type = BadgeType.AI)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onImportClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.import_plan),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = onExportClick,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = ElectricCobalt
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.export_stats),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DashboardNutritionTelemetryCard(
    uiState: DashboardUiState,
    onClick: () -> Unit
) {
    val nutSummary = uiState.analyticsSummary?.nutritionSummary
    val avgCals = nutSummary?.averageDailyCalories?.toInt() ?: 0
    val avgProtein = nutSummary?.averageDailyProtein?.toInt() ?: 0
    val daysLogged = nutSummary?.daysLogged ?: 0

    StandardSurfaceCard(
        onClick = onClick,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = AthleticEmerald,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.nutrition_tracking),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            StatusPillBadge(
                text = stringResource(R.string.kcal_avg_format, avgCals),
                type = BadgeType.SUCCESS
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        val calProgress = (avgCals.toFloat() / 2500f).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { calProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50)),
            color = ElectricCobalt,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.avg_protein)}: ${avgProtein}g",
                style = MaterialTheme.typography.labelMedium,
                color = AthleticEmerald,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${stringResource(R.string.days_logged)}: $daysLogged/7",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickNavTelemetryPillCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    StandardSurfaceCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = MaterialTheme.colorScheme.outlineVariant
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun DashboardTemplateCard(
    template: WorkoutTemplateEntity,
    onCardClick: () -> Unit,
    onStartClick: () -> Unit
) {
    StandardSurfaceCard(
        onClick = onCardClick,
        borderColor = if (template.isAiProgram) {
            ElectricCobalt.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeLabel = if (template.dayCode.startsWith("PROGRAM_DAY_")) {
                        val dayNum = template.dayCode.removePrefix("PROGRAM_DAY_").toIntOrNull() ?: 1
                        stringResource(R.string.day_number_format, dayNum).uppercase()
                    } else {
                        "${stringResource(R.string.day).uppercase()} ${template.dayCode}"
                    }
                    StatusPillBadge(
                        text = badgeLabel,
                        type = if (template.isAiProgram) BadgeType.AI else BadgeType.INFO
                    )
                    if (template.isAiProgram) {
                        StatusPillBadge(
                            text = stringResource(R.string.ai_coach_badge),
                            type = BadgeType.SUCCESS
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = template.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = LocalizationUtils.getTemplateDescription(template.dayFocus),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = onStartClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.start),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
