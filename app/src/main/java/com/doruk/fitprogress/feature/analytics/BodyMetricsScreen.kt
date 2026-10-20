package com.doruk.fitprogress.feature.analytics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyMetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf<BodyMetricEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Body Metrics", fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    selectedMetric = null
                    showDialog = true 
                },
                containerColor = ElectricCyan,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Metric")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientHeaderCard(title = stringResource(com.doruk.fitprogress.R.string.body_metrics_progress), subtitle = stringResource(com.doruk.fitprogress.R.string.body_metrics_latest).uppercase())
            
            // Latest Summary
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(com.doruk.fitprogress.R.string.body_metrics_weight), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            val weightText = uiState.latestWeight?.let { "${com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(it, java.util.Locale.getDefault())} kg" } ?: "--"
                            Text(weightText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            uiState.weightChange?.let { change ->
                                val sign = if (change > 0) "+" else ""
                                Text("$sign$change kg", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Column {
                            Text(stringResource(com.doruk.fitprogress.R.string.body_metrics_waist), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            val waistText = uiState.latestWaist?.let { "${com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(it, java.util.Locale.getDefault())} cm" } ?: "--"
                            Text(waistText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            uiState.waistChange?.let { change ->
                                val sign = if (change > 0) "+" else ""
                                Text("$sign$change cm", 
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Column {
                            Text(stringResource(com.doruk.fitprogress.R.string.body_metrics_body_fat), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            val bfText = uiState.latestBodyFat?.let { "${com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(it, java.util.Locale.getDefault())} %" } ?: "--"
                            Text(bfText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            
            Text(stringResource(com.doruk.fitprogress.R.string.body_metrics_history), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
            
            if (uiState.metricsList.isEmpty()) {
                GymFlowEmptyStates.NoData(message = stringResource(com.doruk.fitprogress.R.string.body_metrics_no_data_desc), modifier = Modifier.fillMaxWidth().padding(top = 32.dp))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.metricsList) { metric ->
                        BodyMetricItemCard(
                            metric = metric,
                            onClick = {
                                selectedMetric = metric
                                showDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddBodyMetricDialog(
            initialMetric = selectedMetric,
            onDismiss = { showDialog = false },
            onSave = { dateIso, weight, waist, chest, arm, leg, bf, notes ->
                viewModel.saveBodyMetric(dateIso, weight, waist, chest, arm, leg, bf, notes)
                showDialog = false
            },
            onDelete = {
                selectedMetric?.let { viewModel.deleteMetric(it) }
                showDialog = false
            }
        )
    }
}

@Composable
fun BodyMetricItemCard(
    metric: BodyMetricEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(com.doruk.fitprogress.core.util.FormatUtils.formatDateIso(metric.dateIso, java.util.Locale.getDefault()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (!metric.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(metric.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                metric.bodyWeight?.let { 
                    val formattedWeight = com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(it.toFloat(), java.util.Locale.getDefault())
                    Text("$formattedWeight ${metric.weightUnit}", fontWeight = FontWeight.SemiBold, color = VividViolet) 
                }
                metric.waist?.let { 
                    val formattedWaist = com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(it.toFloat(), java.util.Locale.getDefault())
                    Text("${stringResource(com.doruk.fitprogress.R.string.body_metrics_waist)}: $formattedWaist cm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) 
                }
            }
        }
    }
}
