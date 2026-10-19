package com.doruk.fitprogress.feature.coaching

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.StatusBadge
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet

@Composable
fun CoachingScreenHost(
    viewModel: CoachingViewModel = hiltViewModel(),
    onNavigateToActiveWorkout: (String, String?) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.navigateOutToActiveWorkout) {
        onNavigateToActiveWorkout(uiState.recommendationId ?: "", uiState.selectedTimeLimitedPlanId)
        return
    }
    if (uiState.navigateOutToDashboard) {
        onNavigateToDashboard()
        return
    }

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ElectricCyan)
        }
        return
    }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Smart Coaching", fontWeight = FontWeight.Bold, color = ElectricCyan) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (uiState.currentScreen) {
                CoachingScreen.CHECK_IN -> ReadinessCheckinScreen(uiState, viewModel)
                CoachingScreen.RECOMMENDATION -> SmartRecommendationScreen(uiState, viewModel)
            }
        }
    }
}

@Composable
fun ReadinessCheckinScreen(
    state: CoachingUiState,
    viewModel: CoachingViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        GradientHeaderCard(title = "Readiness Check-in", subtitle = "HOW ARE YOU FEELING?")

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Available Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("full", "60", "45", "30").forEach { mode ->
                        val isSelected = state.availableTimeMode == mode
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) ElectricCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.updateCheckInField(CheckInField.AVAILABLE_TIME, mode) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(mode, color = if (isSelected) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Metrics (1-5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                SliderField("Energy Level", state.energyLevel) { viewModel.updateCheckInField(CheckInField.ENERGY, it) }
                SliderField("Motivation", state.motivationLevel) { viewModel.updateCheckInField(CheckInField.MOTIVATION, it) }
                SliderField("Sleep Quality", state.sleepQuality) { viewModel.updateCheckInField(CheckInField.SLEEP, it) }
                SliderField("Muscle Soreness", state.muscleSoreness) { viewModel.updateCheckInField(CheckInField.SORENESS, it) }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Sensitivities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                SensitivityField("Knee Sensitivity", state.kneeSensitivity) { viewModel.updateCheckInField(CheckInField.KNEE, it) }
                SensitivityField("Back Discomfort", state.backDiscomfort) { viewModel.updateCheckInField(CheckInField.BACK, it) }
                
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.updateCheckInField(CheckInField.PREVIOUS_HARD, !state.previousWorkoutFeltHard) }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = state.previousWorkoutFeltHard, 
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                    )
                    Text("Previous workout felt unusually hard", modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        PrimaryActionButton(
            text = "Get Smart Recommendation",
            onClick = { viewModel.submitCheckIn() },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun SmartRecommendationScreen(
    state: CoachingUiState,
    viewModel: CoachingViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val modeColor = when (state.recommendedMode) {
            "NORMAL" -> NeonGreen
            "CONTROLLED" -> Color(0xFFFFC107) // Amber
            "LIGHT" -> VividViolet
            "REST_OR_WALK" -> Color(0xFFFF5252) // Coral/Red
            else -> ElectricCyan
        }
        
        GradientHeaderCard(title = stringResource(R.string.smart_recommendation), subtitle = stringResource(R.string.analysis_complete))
        
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(), 
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.mode_label) + ": ", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = com.doruk.fitprogress.core.util.LocalizationUtils.getEnumString(state.recommendedMode ?: "Unknown"), 
                        style = MaterialTheme.typography.headlineMedium, 
                        fontWeight = FontWeight.Black, 
                        color = modeColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(com.doruk.fitprogress.core.util.LocalizationUtils.getRecommendationReasonString(state.recommendedReason ?: ""), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isProcessing) {
            Box(modifier = Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                androidx.compose.material3.CircularProgressIndicator(color = ElectricCyan)
            }
        } else {
            if (state.recommendedMode == "REST_OR_WALK") {
                PrimaryActionButton(
                    text = stringResource(R.string.log_rest_walk_day),
                    onClick = { viewModel.finalizeDecision(DecisionType.REST_DAY) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.start_recommended_plan),
                    onClick = { viewModel.finalizeDecision(DecisionType.START_RECOMMENDED) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }

            OutlinedButton(
                onClick = { viewModel.finalizeDecision(DecisionType.START_ORIGINAL) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.start_original_plan_anyway), color = MaterialTheme.colorScheme.onSurface)
            }

            if (state.recommendedMode != "REST_OR_WALK") {
                OutlinedButton(
                    onClick = { viewModel.finalizeDecision(DecisionType.START_TIME_LIMITED) }, 
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.select_time_limited_instead), color = MaterialTheme.colorScheme.onSurface)
                }
            }

            TextButton(
                onClick = { viewModel.finalizeDecision(DecisionType.REST_DAY) }, 
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(stringResource(R.string.take_rest_walk_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SliderField(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(value.toString(), fontWeight = FontWeight.Bold, color = ElectricCyan)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCyan,
                activeTrackColor = ElectricCyan,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@Composable
fun SensitivityField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("none", "mild", "moderate", "high").forEach { lvl ->
                val isSelected = value == lvl
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onValueChange(lvl) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(lvl, style = MaterialTheme.typography.bodySmall, color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
