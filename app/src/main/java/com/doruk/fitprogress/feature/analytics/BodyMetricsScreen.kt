package com.doruk.fitprogress.feature.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.core.ui.components.StandardSurfaceCard
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.core.util.FormatUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyMetricsScreen(
    onNavigateBack: () -> Unit,
    viewModel: BodyMetricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var selectedMetric by remember { mutableStateOf<BodyMetricEntity?>(null) }
    val context = LocalContext.current
    val locale = FormatUtils.getAppLocale(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.body_metrics_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedMetric = null
                    showDialog = true
                },
                containerColor = ElectricCobalt,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Metric")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GradientHeaderCard(
                title = stringResource(R.string.body_metrics_progress),
                subtitle = stringResource(R.string.body_metrics_latest).uppercase(locale)
            )

            // 3-Column Tabular Summary Strip Card (Zero-Shadow + 1.dp Hairline)
            StandardSurfaceCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val weightText = uiState.latestWeight?.let {
                        "${FormatUtils.formatDoubleEditable(it, locale)} kg"
                    } ?: "--"
                    val weightDelta = uiState.weightChange?.let { change ->
                        val sign = if (change > 0) "+" else ""
                        "$sign${FormatUtils.formatDouble(change, locale, 1)} kg"
                    }

                    BodyMetricStripColumn(
                        label = stringResource(R.string.body_metrics_weight),
                        value = weightText,
                        delta = weightDelta,
                        accentColor = ElectricCobalt,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    val waistText = uiState.latestWaist?.let {
                        "${FormatUtils.formatDoubleEditable(it, locale)} cm"
                    } ?: "--"
                    val waistDelta = uiState.waistChange?.let { change ->
                        val sign = if (change > 0) "+" else ""
                        "$sign${FormatUtils.formatDouble(change, locale, 1)} cm"
                    }

                    BodyMetricStripColumn(
                        label = stringResource(R.string.body_metrics_waist),
                        value = waistText,
                        delta = waistDelta,
                        accentColor = AthleticEmerald,
                        modifier = Modifier.weight(1f)
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    val bfText = uiState.latestBodyFat?.let {
                        "${FormatUtils.formatDoubleEditable(it, locale)} %"
                    } ?: "--"

                    BodyMetricStripColumn(
                        label = stringResource(R.string.body_metrics_body_fat),
                        value = bfText,
                        delta = null,
                        accentColor = TelemetryAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.body_metrics_history),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                StatusPillBadge(
                    text = stringResource(R.string.logs_count_format, uiState.metricsList.size),
                    type = BadgeType.NEUTRAL
                )
            }

            if (uiState.metricsList.isEmpty()) {
                GymFlowEmptyStates.NoData(
                    message = stringResource(R.string.body_metrics_no_data_desc),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    items(
                        items = uiState.metricsList,
                        key = { it.id }
                    ) { metric ->
                        BodyMetricItemCard(
                            metric = metric,
                            locale = locale,
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
private fun BodyMetricStripColumn(
    label: String,
    value: String,
    delta: String?,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
        if (delta != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = delta,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BodyMetricItemCard(
    metric: BodyMetricEntity,
    locale: Locale = Locale.getDefault(),
    onClick: () -> Unit
) {
    StandardSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = FormatUtils.formatDateIso(metric.dateIso, locale),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!metric.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = metric.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                metric.bodyWeight?.let {
                    val formattedWeight = FormatUtils.formatDoubleEditable(it, locale)
                    StatusPillBadge(
                        text = "$formattedWeight ${metric.weightUnit}",
                        type = BadgeType.INFO
                    )
                }
                metric.waist?.let {
                    Spacer(modifier = Modifier.height(4.dp))
                    val formattedWaist = FormatUtils.formatDoubleEditable(it, locale)
                    Text(
                        text = "${stringResource(R.string.body_metrics_waist)}: $formattedWaist cm",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
