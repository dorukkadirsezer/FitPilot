package com.doruk.fitprogress.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.database.entity.TemplateWithDetails
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.DeepNavy
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.VividViolet
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.doruk.fitprogress.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutPreviewScreen(
    templateId: String,
    onBackClick: () -> Unit,
    onStartWorkoutClick: (String) -> Unit,
    viewModel: WorkoutPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.workout_preview), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is WorkoutPreviewUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is WorkoutPreviewUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is WorkoutPreviewUiState.Success -> {
                    PreviewContent(
                        details = state.details,
                        onStartWorkoutClick = { onStartWorkoutClick(templateId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewContent(
    details: TemplateWithDetails,
    onStartWorkoutClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(details.template.name, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (details.template.dayFocus.isNotBlank()) {
                    Text("${stringResource(R.string.focus_label)} ${details.template.dayFocus}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                }
            }

            if (details.warmupInstructions.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.warm_up), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                items(details.warmupInstructions.sortedBy { it.orderIndex }) { warmup ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(com.doruk.fitprogress.core.util.LocalizationUtils.getWarmupInstruction(warmup.description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (warmup.durationMinutes != null) {
                                Text("${stringResource(R.string.duration_label)} ${warmup.durationMinutes} ${stringResource(R.string.min)}", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            if (details.exercises.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.exercises), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                }
                val grouped = details.exercises.sortedBy { it.templateExercise.orderIndex }.groupBy { it.templateExercise.supersetGroupId }
                
                grouped.forEach { (supersetId, exercisesInGroup) ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (supersetId != null) {
                                    Text(stringResource(R.string.superset), color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                                exercisesInGroup.forEach { ex ->
                                    Text(ex.exercise.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    val sets = ex.templateExercise.targetSets
                                    val reps = if (ex.templateExercise.targetRepsMin == ex.templateExercise.targetRepsMax) "${ex.templateExercise.targetRepsMin}" else "${ex.templateExercise.targetRepsMin}-${ex.templateExercise.targetRepsMax}"
                                    val formattedWeight = if (ex.templateExercise.targetWeightMin != null) com.doruk.fitprogress.core.util.FormatUtils.formatDouble(ex.templateExercise.targetWeightMin.toDouble(), com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current)) else ""
                                    val weight = if (ex.templateExercise.targetWeightMin != null) "${formattedWeight} ${ex.templateExercise.weightUnit}" else stringResource(R.string.bodyweight)
                                    val formattedRpe = if (ex.templateExercise.rpeMin != null) com.doruk.fitprogress.core.util.FormatUtils.formatDouble(ex.templateExercise.rpeMin.toDouble(), com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current)) else ""
                                    val rpe = if (ex.templateExercise.rpeMin != null) " ${stringResource(R.string.at_rpe)} ${formattedRpe}" else ""
                                    Text("${sets} ${stringResource(R.string.sets_x)} ${reps} ${stringResource(R.string.reps_pipe)} ${weight}${rpe}", color = MaterialTheme.colorScheme.onSurface)
                                    if (ex.templateExercise.restSeconds > 0) {
                                        Text("${stringResource(R.string.rest_label)} ${ex.templateExercise.restSeconds} ${stringResource(R.string.s)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (!ex.exercise.coachNote.isNullOrBlank()) {
                                        Text("${stringResource(R.string.note_label)} ${com.doruk.fitprogress.core.util.LocalizationUtils.getCoachNoteString(ex.exercise.coachNote)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (ex.exercise.requiresStraps) {
                                        Text(stringResource(R.string.requires_straps), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            PrimaryActionButton(
                onClick = onStartWorkoutClick,
                text = stringResource(R.string.start_workout)
            )
        }
    }
}