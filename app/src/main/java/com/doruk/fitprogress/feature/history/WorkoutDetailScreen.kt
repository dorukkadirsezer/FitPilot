package com.doruk.fitprogress.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatExerciseId(id: String): String {
    return id.removePrefix("ex_")
        .split("_")
        .joinToString(" ") { word -> 
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() } 
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()

    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.doruk.fitprogress.R.string.workout_details), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(stringResource(com.doruk.fitprogress.R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (detail == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val d = detail!!
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
                item {
                    val finishedAt = d.session.finishedAt ?: d.session.startedAt
                    val locale = com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current)
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("${stringResource(com.doruk.fitprogress.R.string.date_label)} ${com.doruk.fitprogress.core.util.FormatUtils.formatDate(finishedAt, locale)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val completedSetCount = d.exercises.sumOf { ex -> ex.sets.count { it.setLog.completed && !it.setLog.isSkipped } }
                        val skippedSetCount = d.exercises.sumOf { ex -> ex.sets.count { it.setLog.isSkipped } }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${d.session.durationSeconds?.let { it / 60 } ?: 0} min", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.completed_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$completedSetCount sets", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.skipped_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("$skippedSetCount sets", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.total_volume), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${com.doruk.fitprogress.core.util.FormatUtils.formatDouble(d.session.totalWorkingVolumeKg, com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current))} kg", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.avg_rpe), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(com.doruk.fitprogress.core.util.FormatUtils.formatDouble(d.session.averageRpe, com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current)), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text(stringResource(com.doruk.fitprogress.R.string.smart_coaching), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val overrideStr = if (d.smartCoachingOverridden == true) "Overridden" else "Followed"
                                Text(overrideStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        
                        if (!d.session.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(modifier = Modifier.padding(16.dp)) {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.AutoAwesome, 
                                        contentDescription = "Workout Note",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(stringResource(com.doruk.fitprogress.R.string.workout_note), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(d.session.note ?: "", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    val isAiEnabled by viewModel.isAiEnabled.collectAsStateWithLifecycle()
                    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
                    val isGenerating by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()
                    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
                    val snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() }

                    androidx.compose.runtime.LaunchedEffect(exportStatus) {
                        if (exportStatus != null) {
                            snackbarHostState.showSnackbar(exportStatus!!)
                            viewModel.clearExportStatus()
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
                    }

                    if (isAiEnabled) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(end = 8.dp))
                                    Text(stringResource(com.doruk.fitprogress.R.string.ai_workout_summary), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (aiSummary != null) {
                                    Text(aiSummary!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(stringResource(com.doruk.fitprogress.R.string.ai_summary_disclaimer), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    AiActionButton(
                                        onClick = { viewModel.generateAiSummary() },
                                        text = if (isGenerating) stringResource(com.doruk.fitprogress.R.string.generating_summary) else stringResource(com.doruk.fitprogress.R.string.generate_summary),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isGenerating
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                PrimaryActionButton(
                                    onClick = { viewModel.exportToCoach() },
                                    text = "Export to Coach",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // AI Disabled but we still want to show the export button
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                PrimaryActionButton(
                                    onClick = { viewModel.exportToCoach() },
                                    text = "Export to Coach",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                items(d.exercises) { exDto ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(formatExerciseId(exDto.exerciseSession.exerciseId), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            
                            if (!exDto.exerciseSession.note.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Edit, 
                                            contentDescription = "Exercise Note",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("${exDto.exerciseSession.note}", style = MaterialTheme.typography.bodyMedium, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                }
                            }
                            
                            val warmups = exDto.sets.filter { it.setLog.setType == "WARMUP" }
                            val working = exDto.sets.filter { it.setLog.setType == "WORKING" }

                            if (warmups.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(stringResource(com.doruk.fitprogress.R.string.warm_ups), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                warmups.forEach { logWithDetails ->
                                    val log = logWithDetails.setLog
                                    val status = if (log.isSkipped) " (Skipped)" else if (!log.completed) " (Incomplete)" else ""
                                    val actualWeightStr = com.doruk.fitprogress.core.util.FormatUtils.formatDouble(log.actualWeight, com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current))
                                    Text("${stringResource(com.doruk.fitprogress.R.string.warmup_set_number).format(log.setNumber)}: ${log.actualReps ?: log.targetReps} reps @ $actualWeightStr ${log.weightUnit}$status", style = MaterialTheme.typography.bodyMedium)
                                    if (!log.note.isNullOrBlank()) {
                                        Row(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                                contentDescription = "Set Note",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${log.note}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            
                            if (working.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(stringResource(com.doruk.fitprogress.R.string.working_sets), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                working.forEach { logWithDetails ->
                                    val log = logWithDetails.setLog
                                    val status = if (log.isSkipped) " (Skipped)" else if (!log.completed) " (Incomplete)" else ""
                                    
                                    val logColor = if (log.isSkipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                    
                                    val actualWeightStr = com.doruk.fitprogress.core.util.FormatUtils.formatDouble(log.actualWeight ?: log.targetWeight, com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current))
                                    Text("${stringResource(com.doruk.fitprogress.R.string.working_set_number).format(log.setNumber)}: ${log.actualReps ?: log.targetReps}/${log.targetReps} reps @ $actualWeightStr ${log.weightUnit}  RPE: ${log.rpe ?: "N/A"}$status", style = MaterialTheme.typography.bodyMedium, color = logColor)
                                    
                                    val rest = logWithDetails.restTimerLog
                                    if (rest != null) {
                                        Text(stringResource(com.doruk.fitprogress.R.string.workout_detail_rest_format).format(rest.actualRestSeconds, rest.plannedRestSeconds), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    
                                    val advice = logWithDetails.setAdvice
                                    if (advice != null) {
                                        Text(stringResource(com.doruk.fitprogress.R.string.workout_detail_advice_format).format(advice.adviceText), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                    }
                                    
                                    if (!log.note.isNullOrBlank()) {
                                        Row(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = androidx.compose.material.icons.Icons.Default.Edit,
                                                contentDescription = "Set Note",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("${log.note}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    if (d.cardioSession != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(com.doruk.fitprogress.R.string.cardio_session_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(com.doruk.fitprogress.R.string.cardio_type_format).format(d.cardioSession.cardioType), style = MaterialTheme.typography.bodyMedium)
                                Text(stringResource(com.doruk.fitprogress.R.string.cardio_duration_format).format(d.cardioSession.durationMinutes), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

