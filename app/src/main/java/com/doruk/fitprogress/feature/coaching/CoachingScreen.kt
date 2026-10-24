package com.doruk.fitprogress.feature.coaching

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.SecondaryActionButton
import com.doruk.fitprogress.core.ui.components.StandardSurfaceCard
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.CrimsonAlert
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber
import kotlin.math.roundToInt

@Composable
fun CoachingScreenHost(
    viewModel: CoachingViewModel = hiltViewModel(),
    onNavigateToActiveWorkout: (String, String?) -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateOutToActiveWorkout, state.navigateOutToDashboard) {
        if (state.navigateOutToActiveWorkout && state.recommendationId != null) {
            onNavigateToActiveWorkout(state.recommendationId!!, state.selectedTimeLimitedPlanId)
        } else if (state.navigateOutToDashboard) {
            onNavigateToDashboard()
        }
    }

    when (state.currentScreen) {
        CoachingScreen.CHECK_IN -> ReadinessCheckInScreen(state = state, viewModel = viewModel)
        CoachingScreen.RECOMMENDATION -> SmartRecommendationScreen(state = state, viewModel = viewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessCheckInScreen(
    state: CoachingUiState,
    viewModel: CoachingViewModel
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.smart_coaching),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientHeaderCard(
                title = stringResource(R.string.readiness_check_in),
                subtitle = stringResource(R.string.how_are_you_feeling)
            )

            // Available Time Selector Card (Zero-Shadow + 1.dp Hairline)
            StandardSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val modeDisplay = if (state.availableTimeMode == "full") "60 MIN" else "${state.availableTimeMode} MIN"
                    StatusPillBadge(
                        text = modeDisplay,
                        type = BadgeType.INFO
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val options = listOf(
                        "full" to "60 min",
                        "45" to "45 min",
                        "30" to "30 min"
                    )
                    options.forEach { (modeKey, label) ->
                        val isSelected = state.availableTimeMode == modeKey
                        Button(
                            onClick = { viewModel.updateCheckInField(CheckInField.AVAILABLE_TIME, modeKey) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Readiness Telemetry Sliders Card
            StandardSurfaceCard {
                SliderRow("Sleep Quality (1 = Poor, 5 = Great)", state.sleepQuality) {
                    viewModel.updateCheckInField(CheckInField.SLEEP, it)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SliderRow("Energy Level (1 = Low, 5 = High)", state.energyLevel) {
                    viewModel.updateCheckInField(CheckInField.ENERGY, it)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SliderRow("Motivation (1 = Low, 5 = Pumped)", state.motivationLevel) {
                    viewModel.updateCheckInField(CheckInField.MOTIVATION, it)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SliderRow("Muscle Soreness (1 = None, 5 = Severe)", state.muscleSoreness) {
                    viewModel.updateCheckInField(CheckInField.SORENESS, it)
                }
            }

            // Joint & Recovery Sensitivity Card
            StandardSurfaceCard {
                SensitivitySelectorRow(
                    title = "Knee Sensitivity",
                    currentValue = state.kneeSensitivity,
                    onSelect = { viewModel.updateCheckInField(CheckInField.KNEE, it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                SensitivitySelectorRow(
                    title = "Lower Back Discomfort",
                    currentValue = state.backDiscomfort,
                    onSelect = { viewModel.updateCheckInField(CheckInField.BACK, it) }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previous Workout Felt Very Hard",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.previousWorkoutFeltHard,
                        onCheckedChange = { viewModel.updateCheckInField(CheckInField.PREVIOUS_HARD, it) }
                    )
                }
            }

            PrimaryActionButton(
                text = stringResource(R.string.get_smart_recommendation),
                onClick = { viewModel.submitCheckIn() }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SensitivitySelectorRow(
    title: String,
    currentValue: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("none" to "None", "mild" to "Mild", "high" to "High").forEach { (key, label) ->
                val isSelected = currentValue == key
                Button(
                    onClick = { onSelect(key) },
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp),
                    shape = RoundedCornerShape(10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SliderRow(label: String, value: Int, onValueChange: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            StatusPillBadge(
                text = "$value / 5",
                type = BadgeType.NEUTRAL
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors(
                thumbColor = ElectricCobalt,
                activeTrackColor = ElectricCobalt,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartRecommendationScreen(
    state: CoachingUiState,
    viewModel: CoachingViewModel
) {
    val recMode = state.recommendedMode ?: "NORMAL"
    val recReason = state.recommendedReason ?: ""

    val badgeType = when (recMode) {
        "NORMAL" -> BadgeType.SUCCESS
        "CONTROLLED" -> BadgeType.INFO
        "LIGHT" -> BadgeType.WARNING
        "REST_OR_WALK" -> BadgeType.DANGER
        else -> BadgeType.INFO
    }
    val modeAccentColor = when (recMode) {
        "NORMAL" -> AthleticEmerald
        "CONTROLLED" -> ElectricCobalt
        "LIGHT" -> TelemetryAmber
        "REST_OR_WALK" -> CrimsonAlert
        else -> ElectricCobalt
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.smart_recommendation),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            GradientHeaderCard(
                title = stringResource(R.string.smart_recommendation),
                subtitle = stringResource(R.string.analysis_complete)
            )

            StandardSurfaceCard(borderColor = modeAccentColor.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.mode_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    StatusPillBadge(
                        text = LocalizationUtils.getEnumString(recMode).uppercase(),
                        type = badgeType
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = LocalizationUtils.getRecommendationReasonString(recReason),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (state.availableTimeMode != "full" && recMode != "REST_OR_WALK") {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusPillBadge(
                        text = "TIME-LIMITED: ${state.availableTimeMode} MIN",
                        type = BadgeType.AI
                    )
                }

                if (state.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (recMode != "REST_OR_WALK") {
                    PrimaryActionButton(
                        text = stringResource(R.string.start_recommended_plan),
                        enabled = !state.isProcessing,
                        onClick = { viewModel.finalizeDecision(DecisionType.START_RECOMMENDED) }
                    )
                } else {
                    PrimaryActionButton(
                        text = stringResource(R.string.log_rest_walk_day),
                        enabled = !state.isProcessing,
                        onClick = { viewModel.finalizeDecision(DecisionType.REST_DAY) }
                    )
                }

                SecondaryActionButton(
                    text = stringResource(R.string.start_original_plan_anyway),
                    enabled = !state.isProcessing,
                    onClick = { viewModel.finalizeDecision(DecisionType.START_ORIGINAL) }
                )

                if (state.availableTimeMode == "full" && recMode != "REST_OR_WALK") {
                    TextButton(
                        onClick = { viewModel.finalizeDecision(DecisionType.START_TIME_LIMITED) },
                        enabled = !state.isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${stringResource(R.string.select_time_limited_instead)} (45m)",
                            color = ElectricCobalt,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (recMode != "REST_OR_WALK") {
                    TextButton(
                        onClick = { viewModel.finalizeDecision(DecisionType.REST_DAY) },
                        enabled = !state.isProcessing,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.take_a_rest_walk_day),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
