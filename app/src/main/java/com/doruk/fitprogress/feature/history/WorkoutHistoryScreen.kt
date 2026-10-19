package com.doruk.fitprogress.feature.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.core.ui.components.StatusBadge
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val history by viewModel.history.collectAsState()
    val format = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.doruk.fitprogress.R.string.workout_history), fontWeight = FontWeight.Bold, color = ElectricCyan) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                GymFlowEmptyStates.NoData(message = androidx.compose.ui.res.stringResource(com.doruk.fitprogress.R.string.no_completed_workouts_yet))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(history) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDetail(item.session.id) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val locale = com.doruk.fitprogress.core.util.FormatUtils.getAppLocale(androidx.compose.ui.platform.LocalContext.current)
                            val startedAt = item.session.startedAt
                            val finishedAt = item.session.finishedAt ?: startedAt
                            val dateString = com.doruk.fitprogress.core.util.FormatUtils.formatDate(finishedAt, locale)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(dateString, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                StatusBadge(text = "${item.dayCode ?: "?"} ${stringResource(com.doruk.fitprogress.R.string.day)}", color = ElectricCyan)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val workoutNameText = item.workoutName ?: stringResource(com.doruk.fitprogress.R.string.custom_workout)
                            Text(workoutNameText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(stringResource(com.doruk.fitprogress.R.string.duration), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(com.doruk.fitprogress.core.util.FormatUtils.formatDuration(item.session.durationSeconds, locale), style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text(stringResource(com.doruk.fitprogress.R.string.volume), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${com.doruk.fitprogress.core.util.FormatUtils.formatDouble(item.session.totalWorkingVolumeKg, locale)} kg", style = MaterialTheme.typography.bodyLarge)
                                }
                                Column {
                                    Text(stringResource(com.doruk.fitprogress.R.string.avg_rpe), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(com.doruk.fitprogress.core.util.FormatUtils.formatDouble(item.session.averageRpe, locale), style = MaterialTheme.typography.bodyLarge)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val modeStr = item.smartCoachingMode ?: "Normal"
                            val modeLocalized = com.doruk.fitprogress.core.util.LocalizationUtils.getEnumString(modeStr.uppercase())
                            val overrideStr = if (item.smartCoachingOverridden == true) " (${com.doruk.fitprogress.core.util.LocalizationUtils.getEnumString("OVERRIDDEN")})" else ""
                            val modeColor = when (modeStr) {
                                "Normal" -> NeonGreen
                                "Light" -> VividViolet
                                "Rest-Walk" -> MaterialTheme.colorScheme.error
                                else -> androidx.compose.ui.graphics.Color(0xFFFFC107)
                            }
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${stringResource(com.doruk.fitprogress.R.string.mode_label)} $modeLocalized$overrideStr", style = MaterialTheme.typography.bodySmall, color = modeColor)
                                
                                val cardioStatus = if (item.cardioSession != null) {
                                    val type = item.cardioSession.cardioType
                                    val target = com.doruk.fitprogress.core.util.FormatUtils.formatDuration(item.cardioSession.durationMinutes.toLong() * 60, locale)
                                    // For History UI, prefer a compact string to avoid wrapping
                                    "$type: $target"
                                } else if (item.session.cardioCompleted) stringResource(com.doruk.fitprogress.R.string.completed_status) else stringResource(com.doruk.fitprogress.R.string.skipped_none)
                                
                                Text(cardioStatus, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${item.completedSetCount} ${stringResource(com.doruk.fitprogress.R.string.sets_completed)} • ${item.skippedSetCount} ${stringResource(com.doruk.fitprogress.R.string.skipped)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                if (item.hasNotes) {
                                    StatusBadge(text = stringResource(com.doruk.fitprogress.R.string.notes_added), color = VividViolet)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
