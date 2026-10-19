package com.doruk.fitprogress.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.domain.usecase.AnalyticsDashboardSummary
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsDashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.doruk.fitprogress.R.string.analytics), fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(com.doruk.fitprogress.R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is AnalyticsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = ElectricCyan)
                }
                is AnalyticsUiState.Error -> {
                    GymFlowEmptyStates.Error(message = state.message, onRetry = onNavigateBack, modifier = Modifier.align(Alignment.Center))
                }
                is AnalyticsUiState.Success -> {
                    AnalyticsContent(state.summary, viewModel)
                }
            }
        }
    }
}

@Composable
fun AnalyticsContent(summary: AnalyticsDashboardSummary, viewModel: AnalyticsViewModel) {
    val isAiEnabled by viewModel.isAiEnabledFlow.collectAsState(initial = false)
    val aiReview by viewModel.aiReview.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GradientHeaderCard(title = stringResource(com.doruk.fitprogress.R.string.weekly_snapshot), subtitle = stringResource(com.doruk.fitprogress.R.string.last_7_days))

        if (isAiEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = VividViolet.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VividViolet, modifier = Modifier.padding(end = 8.dp))
                        Text(stringResource(com.doruk.fitprogress.R.string.ai_weekly_review), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = VividViolet)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (aiReview != null) {
                        Text(aiReview!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(com.doruk.fitprogress.R.string.ai_review_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = VividViolet)
                    } else {
                        AiActionButton(
                            onClick = { viewModel.generateWeeklyReview() },
                            text = stringResource(com.doruk.fitprogress.R.string.generate_review),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Body Trends
        AnalyticsCard(title = stringResource(com.doruk.fitprogress.R.string.body_trends)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.weight), summary.bodySummary.latestWeight?.let { "$it kg" } ?: "--")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.change_7d), summary.bodySummary.weightChange7Days?.let { if (it > 0) "+$it" else "$it" } ?: "--", isTrend = true, trendValue = summary.bodySummary.weightChange7Days)
                MetricItem(stringResource(com.doruk.fitprogress.R.string.waist), summary.bodySummary.latestWaist?.let { "$it cm" } ?: "--")
            }
        }

        // Workout Consistency
        AnalyticsCard(title = stringResource(com.doruk.fitprogress.R.string.workout_consistency)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.workouts), "${summary.workoutSummary.workoutsCompleted}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.volume), "${summary.workoutSummary.totalVolumeKg.toInt()} kg")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.skipped_sets), "${summary.workoutSummary.skippedSets}")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.a_days), "${summary.workoutSummary.aDays}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.b_days), "${summary.workoutSummary.bDays}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.c_days), "${summary.workoutSummary.cDays}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.d_days), "${summary.workoutSummary.dDays}")
                val avgRpe = summary.workoutSummary.averageRpe?.let { String.format("%.1f", it) } ?: "--"
                MetricItem(stringResource(com.doruk.fitprogress.R.string.avg_rpe), avgRpe)
            }
        }

        // Cardio Trends
        AnalyticsCard(title = stringResource(com.doruk.fitprogress.R.string.cardio_activity)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.sessions), "${summary.cardioSummary.sessionsCompleted}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.total_minutes), "${summary.cardioSummary.totalMinutes} min")
            }
        }

        // Nutrition Consistency
        AnalyticsCard(title = stringResource(com.doruk.fitprogress.R.string.nutrition_averages)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.avg_cals), "${summary.nutritionSummary.averageDailyCalories.toInt()}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.avg_protein), "${summary.nutritionSummary.averageDailyProtein.toInt()} g")
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MetricItem(stringResource(com.doruk.fitprogress.R.string.days_logged), "${summary.nutritionSummary.daysLogged}/7")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.protein_hit), "${summary.nutritionSummary.daysProteinTargetReached}")
                MetricItem(stringResource(com.doruk.fitprogress.R.string.cals_hit), "${summary.nutritionSummary.daysCalorieTargetReachedOrExceeded}")
            }
        }
    }
}

@Composable
fun AnalyticsCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, isTrend: Boolean = false, trendValue: Float? = null) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        val color = if (isTrend && trendValue != null) {
            if (trendValue < 0) com.doruk.fitprogress.theme.NeonGreen else MaterialTheme.colorScheme.error // Weight loss green, gain red (contextual)
        } else MaterialTheme.colorScheme.onSurface
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = color)
    }
}

