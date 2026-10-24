package com.doruk.fitprogress.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.doruk.fitprogress.core.database.entity.WorkoutHistoryListItemDto
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.core.ui.components.StandardSurfaceCard
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.core.util.FormatUtils
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutHistoryScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: WorkoutHistoryViewModel = hiltViewModel()
) {
    val historyList by viewModel.history.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_history),
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
    ) { paddingValues ->
        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                GymFlowEmptyStates.NoData(
                    message = stringResource(R.string.no_completed_workouts_yet)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                items(
                    items = historyList,
                    key = { it.session.id }
                ) { item ->
                    WorkoutHistoryTelemetryCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.session.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryTelemetryCard(
    item: WorkoutHistoryListItemDto,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val locale = FormatUtils.getAppLocale(context)
    val dateMillis = item.session.finishedAt ?: item.session.startedAt
    val dateStr = FormatUtils.formatDate(dateMillis, locale)
    val durationStr = FormatUtils.formatDuration(item.session.durationSeconds, locale)

    StandardSurfaceCard(onClick = onClick) {
        // 1. Header Row: Day Code + Workout Title & %50 Pill Date Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val dayPrefix = when {
                    item.dayCode == null -> ""
                    item.dayCode.startsWith("PROGRAM_DAY_") -> "Day ${item.dayCode.removePrefix("PROGRAM_DAY_")} — "
                    else -> "${stringResource(R.string.day)} ${item.dayCode} — "
                }
                Text(
                    text = "$dayPrefix${item.workoutName ?: stringResource(R.string.custom_workout)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            StatusPillBadge(
                text = dateStr,
                type = BadgeType.INFO
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. 3-Column Tabular Metric Strip (Duration | Volume | Avg RPE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabularMetricStripCell(
                label = stringResource(R.string.duration),
                value = durationStr,
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            TabularMetricStripCell(
                label = stringResource(R.string.volume),
                value = "${FormatUtils.formatDouble(item.session.totalWorkingVolumeKg, locale, 0)} kg",
                valueColor = ElectricCobalt,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            TabularMetricStripCell(
                label = stringResource(R.string.avg_rpe),
                value = if (item.session.averageRpe != null) {
                    FormatUtils.formatDouble(item.session.averageRpe, locale, 1)
                } else {
                    stringResource(R.string.workout_detail_na)
                },
                valueColor = TelemetryAmber,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Footer Pill Badges (Completed Sets, Smart Coaching Mode, Cardio, Notes)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPillBadge(
                text = "${item.completedSetCount} ${stringResource(R.string.sets_completed)}",
                type = BadgeType.SUCCESS
            )

            if (item.skippedSetCount > 0) {
                StatusPillBadge(
                    text = "${item.skippedSetCount} ${stringResource(R.string.skipped)}",
                    type = BadgeType.DANGER
                )
            }

            if (item.smartCoachingMode != null) {
                StatusPillBadge(
                    text = LocalizationUtils.getEnumString(item.smartCoachingMode),
                    type = BadgeType.AI
                )
            }

            if (item.hasNotes) {
                StatusPillBadge(
                    text = stringResource(R.string.notes_added),
                    type = BadgeType.WARNING
                )
            }
        }

        val c = item.cardioSession
        if (c != null || item.session.cardioCompleted) {
            Spacer(modifier = Modifier.height(8.dp))
            val cardioText = if (c != null) {
                "${c.cardioType} • ${c.durationMinutes} ${stringResource(R.string.min)}"
            } else {
                stringResource(R.string.completed_status)
            }
            Text(
                text = "${stringResource(R.string.cardio_label)} $cardioText",
                style = MaterialTheme.typography.bodySmall,
                color = AthleticEmerald,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TabularMetricStripCell(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
