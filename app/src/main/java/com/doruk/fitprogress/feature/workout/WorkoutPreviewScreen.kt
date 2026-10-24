package com.doruk.fitprogress.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.database.entity.TemplateExerciseWithDetails
import com.doruk.fitprogress.core.ui.components.*
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPreviewScreen(
    templateId: String,
    onBackClick: () -> Unit,
    onStartWorkoutClick: () -> Unit,
    viewModel: WorkoutPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titleText = when (val state = uiState) {
                        is WorkoutPreviewUiState.Success -> state.details.template.name
                        else -> stringResource(R.string.workout_preview)
                    }
                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
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
        bottomBar = {
            if (uiState is WorkoutPreviewUiState.Success) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            PrimaryActionButton(
                                text = stringResource(R.string.start_workout),
                                onClick = onStartWorkoutClick,
                                icon = Icons.Default.PlayArrow
                            )
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = uiState) {
            is WorkoutPreviewUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is WorkoutPreviewUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    GymFlowEmptyStates.Error(
                        message = state.message,
                        onRetry = onBackClick
                    )
                }
            }
            is WorkoutPreviewUiState.Success -> {
                val currentDetails = state.details
                val sortedWarmups = currentDetails.warmupInstructions.sortedBy { it.orderIndex }
                val sortedExercises = currentDetails.exercises.sortedBy { it.templateExercise.orderIndex }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    // Header Summary Card
                    item {
                        val subtitleLabel = if (currentDetails.template.dayCode.startsWith("PROGRAM_DAY_")) {
                            val dayNum = currentDetails.template.dayCode.removePrefix("PROGRAM_DAY_").toIntOrNull() ?: 1
                            stringResource(R.string.day_number_format, dayNum).uppercase()
                        } else {
                            "${stringResource(R.string.day).uppercase()} ${currentDetails.template.dayCode}"
                        }
                        GradientHeaderCard(
                            title = currentDetails.template.name,
                            subtitle = subtitleLabel
                        )
                    }

                    // Warm-up Section
                    if (sortedWarmups.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(R.string.warm_up),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ElectricCobalt,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        items(sortedWarmups, key = { it.id }) { warmup ->
                            StandardSurfaceCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = LocalizationUtils.getWarmupInstruction(warmup.description),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (warmup.durationMinutes != null) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        StatusPillBadge(
                                            text = "${warmup.durationMinutes} ${stringResource(R.string.min)}",
                                            type = BadgeType.INFO
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Exercises Header
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.exercises),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            StatusPillBadge(
                                text = stringResource(R.string.movements_count_format, sortedExercises.size),
                                type = BadgeType.NEUTRAL
                            )
                        }
                    }

                    // Exercises List
                    items(sortedExercises, key = { it.templateExercise.id }) { exItem ->
                        ExercisePreviewTelemetryCard(exItem)
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePreviewTelemetryCard(item: TemplateExerciseWithDetails) {
    val tplEx = item.templateExercise
    val ex = item.exercise

    StandardSurfaceCard(
        borderColor = if (tplEx.isSuperset) {
            ElectricCobalt.copy(alpha = 0.40f)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
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
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${tplEx.orderIndex}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = ex.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (tplEx.isSuperset) {
                StatusPillBadge(
                    text = stringResource(R.string.superset),
                    type = BadgeType.AI
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val weightStr = when {
            tplEx.targetWeightMin != null && tplEx.targetWeightMax != null -> {
                if (tplEx.targetWeightMin == tplEx.targetWeightMax) {
                    "${tplEx.targetWeightMin} ${tplEx.weightUnit}"
                } else {
                    "${tplEx.targetWeightMin}-${tplEx.targetWeightMax} ${tplEx.weightUnit}"
                }
            }
            tplEx.targetWeightMin != null -> "${tplEx.targetWeightMin} ${tplEx.weightUnit}"
            else -> stringResource(R.string.bodyweight)
        }

        val repsStr = if (tplEx.targetRepsMin == tplEx.targetRepsMax) {
            "${tplEx.targetRepsMin}"
        } else {
            "${tplEx.targetRepsMin}-${tplEx.targetRepsMax}"
        }

        val rpeStr = if (tplEx.rpeMin != null) {
            if (tplEx.rpeMin == tplEx.rpeMax || tplEx.rpeMax == null) {
                "${stringResource(R.string.at_rpe)} ${tplEx.rpeMin}"
            } else {
                "${stringResource(R.string.at_rpe)} ${tplEx.rpeMin}-${tplEx.rpeMax}"
            }
        } else ""

        // Telemetry Prescription Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${tplEx.targetSets} ${stringResource(R.string.sets_x)} $repsStr ${stringResource(R.string.reps_pipe)} $weightStr $rpeStr".trim(),
                style = MaterialTheme.typography.labelLarge,
                color = ElectricCobalt,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "${stringResource(R.string.rest_label)} ${tplEx.restSeconds}${stringResource(R.string.s)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (!ex.coachNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.note_label)} ${LocalizationUtils.getCoachNoteString(ex.coachNote)}",
                style = MaterialTheme.typography.bodySmall,
                color = AthleticEmerald,
                fontWeight = FontWeight.Medium
            )
        }

        if (ex.requiresStraps) {
            Spacer(modifier = Modifier.height(8.dp))
            StatusPillBadge(
                text = stringResource(R.string.requires_straps),
                type = BadgeType.WARNING
            )
        }
    }
}
