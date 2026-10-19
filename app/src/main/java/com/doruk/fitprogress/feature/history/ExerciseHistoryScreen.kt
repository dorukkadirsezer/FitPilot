package com.doruk.fitprogress.feature.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ExerciseHistoryViewModel = hiltViewModel()
) {
    val historySessions by viewModel.historySessions.collectAsState()
    val suggestion by viewModel.suggestion.collectAsState()
    val readinessScore by viewModel.readinessScore.collectAsState()
    
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Exercise History", fontWeight = FontWeight.Bold, color = ElectricCyan) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            
            // Progression Readiness Section
            item {
                readinessScore?.let { score ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Progression Readiness", fontWeight = FontWeight.Bold, color = ElectricCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val scoreColor = when (score.score) {
                                in 80..100 -> NeonGreen
                                in 60..79 -> VividViolet
                                in 40..59 -> androidx.compose.ui.graphics.Color(0xFFFF9800)
                                else -> MaterialTheme.colorScheme.error
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${score.score}", fontSize = 40.sp, fontWeight = FontWeight.Black, color = scoreColor)
                                Text(" / 100", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.Bottom).padding(bottom = 6.dp))
                            }
                            
                            val status = when (score.score) {
                                in 80..100 -> "Ready to increase weight or reps"
                                in 60..79 -> "Repeat current load and confirm"
                                in 40..59 -> "Build consistency at current load"
                                else -> "Do not increase load yet"
                            }
                            Text(status, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Factors:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(score.factors.replace(" | ", "\n"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Progression Suggestion Section
            item {
                suggestion?.let { sug ->
                    val isAiEnabled by viewModel.isAiEnabledFlow.collectAsState(initial = false)
                    val aiExplanation by viewModel.aiExplanation.collectAsState()
                    val isAiLoading by viewModel.isAiLoading.collectAsState()

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Smart Suggestion", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(sug.suggestionType.replace("_", " "), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(sug.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            if (isAiEnabled) {
                                Spacer(modifier = Modifier.height(16.dp))
                                if (aiExplanation != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VividViolet, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                        Text("AI Coach Explanation", fontWeight = FontWeight.SemiBold, color = VividViolet)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(aiExplanation!!, style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    AiActionButton(
                                        onClick = { viewModel.explainProgression() },
                                        text = if (isAiLoading) "Asking..." else "Ask AI Coach for details",
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isAiLoading
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // History List
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
            
            items(historySessions) { sessionDto ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val sessionDate = sessionDto.workoutFinishedAt ?: sessionDto.workoutStartedAt
                        Text(format.format(Date(sessionDate)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        
                        if (!sessionDto.exerciseSession.note.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Exercise Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${sessionDto.exerciseSession.note}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                }
                            }
                        }
                        
                        val working = sessionDto.sets.filter { it.setLog.setType == "WORKING" }
                        
                        if (working.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            working.forEach { logWithDetails ->
                                val log = logWithDetails.setLog
                                val status = if (log.isSkipped) " (Skipped)" else if (!log.completed) " (Incomplete)" else ""
                                val textColor = if (log.isSkipped) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                
                                Text("Set ${log.setNumber}: ${log.actualReps ?: log.targetReps}/${log.targetReps} reps @ ${log.actualWeight ?: log.targetWeight} ${log.weightUnit}  RPE: ${log.rpe ?: "N/A"}$status", style = MaterialTheme.typography.bodyMedium, color = textColor)
                                
                                // Estimated 1RM
                                val ex = viewModel.exercise.collectAsState().value
                                if (ex?.movementType == "compound" && !log.isSkipped && log.completed && log.actualWeight != null && log.actualWeight > 0 && log.actualReps != null) {
                                    val estimated1RM = log.actualWeight * (1f + log.actualReps / 30f)
                                    Text("Est. 1RM: ${String.format("%.1f", estimated1RM)} ${log.weightUnit}", style = MaterialTheme.typography.bodySmall, color = VividViolet)
                                }
                                
                                if (!log.note.isNullOrBlank()) {
                                    Row(modifier = Modifier.padding(top = 2.dp)) {
                                        Text("↳ Note: ${log.note}", style = MaterialTheme.typography.bodySmall, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
