package com.doruk.fitprogress.feature.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.database.entity.WorkoutTemplateEntity
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.MetricCard
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.StatusBadge
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen

import androidx.compose.ui.res.stringResource
import com.doruk.fitprogress.R

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
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

    androidx.compose.runtime.LaunchedEffect(uiState.importError, uiState.importSuccessMessage) {
        if (uiState.importError != null) {
            snackbarHostState.showSnackbar(uiState.importError!!)
            viewModel.clearImportMessages()
        }
        if (uiState.importSuccessMessage != null) {
            snackbarHostState.showSnackbar(uiState.importSuccessMessage!!)
            viewModel.clearImportMessages()
        }
    }

    androidx.compose.runtime.LaunchedEffect(uiState.importedSessionId) {
        if (uiState.importedSessionId != null) {
            val templates = uiState.templates
            val templateId = templates.find { it.dayCode == "AI_COACH" }?.id ?: "AI_COACH"
            // Start ActiveWorkout without going through coaching (which creates session).
            // We just created it, so navigate there with recId to bypass session creation.
            onNavigateToActiveWorkout("recId=${uiState.importedSessionId}")
            viewModel.clearImportedSessionId()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                actions = {
                    IconButton(onClick = onNavigateToAiSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.importCoachPlan() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Download, contentDescription = null) },
                text = { Text("Import Coach JSON") }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                item {
                    GradientHeaderCard(
                        title = stringResource(R.string.ready_to_crush_it),
                        subtitle = stringResource(R.string.welcome_back)
                    )
                }

                item {
                    uiState.analyticsSummary?.let { summary ->
                        DashboardSummaryCards(
                            summary = summary,
                            onNavigateToAnalytics = onNavigateToAnalytics,
                            onNavigateToWorkoutHistory = onNavigateToWorkoutHistory,
                            onNavigateToBodyMetrics = onNavigateToBodyMetrics,
                            onNavigateToNutrition = onNavigateToNutrition
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.select_workout), style = MaterialTheme.typography.titleLarge)
                }

                items(uiState.templates) { template ->
                    WorkoutTemplateCard(
                        template = template,
                        onClick = { onTemplateClick(template.id) },
                        onStartWorkout = { onNavigateToCoaching(template.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardSummaryCards(
    summary: AnalyticsDashboardSummary,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToWorkoutHistory: () -> Unit,
    onNavigateToBodyMetrics: () -> Unit,
    onNavigateToNutrition: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = stringResource(R.string.recent_workouts),
                modifier = Modifier.weight(1f).clickable { onNavigateToWorkoutHistory() }
            ) {
                Text(
                    "${summary.workoutSummary.workoutsCompleted} ${stringResource(R.string.completed)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            MetricCard(
                title = stringResource(R.string.analytics),
                modifier = Modifier.weight(1f).clickable { onNavigateToAnalytics() }
            ) {
                Text(
                    stringResource(R.string.view_trends),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricCard(
                title = stringResource(R.string.body),
                modifier = Modifier.weight(1f).clickable { onNavigateToBodyMetrics() }
            ) {
                Text(
                    summary.bodySummary.latestWeight?.let { "${it} kg" } ?: stringResource(R.string.no_data),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        MetricCard(
            title = stringResource(R.string.nutrition_tracking),
            modifier = Modifier.clickable { onNavigateToNutrition() }
        ) {
            Text(
                "Avg ${summary.nutritionSummary.averageDailyCalories.toInt()} kcal / ${summary.nutritionSummary.averageDailyProtein.toInt()}g protein",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WorkoutTemplateCard(
    template: WorkoutTemplateEntity,
    onClick: () -> Unit,
    onStartWorkout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusBadge(text = template.dayCode)
                        Text(text = template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = com.doruk.fitprogress.core.util.LocalizationUtils.getTemplateDescription(template.dayFocus), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                PrimaryActionButton(
                    text = stringResource(R.string.start),
                    onClick = onStartWorkout,
                    modifier = Modifier.width(120.dp).height(48.dp)
                )
            }
        }
    }
}
