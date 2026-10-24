package com.doruk.fitprogress.feature.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.database.entity.SetLogWithDetailsDto
import com.doruk.fitprogress.core.database.entity.WorkoutDetailExerciseDto
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.StandardSurfaceCard
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.core.util.FormatUtils
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: WorkoutDetailViewModel = hiltViewModel()
) {
    val detailState by viewModel.detail.collectAsStateWithLifecycle()
    val isAiEnabled by viewModel.isAiEnabled.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiSummary.collectAsStateWithLifecycle()
    val isGeneratingSummary by viewModel.isGeneratingSummary.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.workout_details),
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
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (detailState == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        val detail = detailState!!
        val context = LocalContext.current
        val locale = FormatUtils.getAppLocale(context)
        val dateMillis = detail.session.finishedAt ?: detail.session.startedAt
        val dateStr = FormatUtils.formatDate(dateMillis, locale)
        val durationStr = FormatUtils.formatDuration(detail.session.durationSeconds, locale)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // 1. Session Header & 3-Column Tabular Metric Strip
            item {
                StandardSurfaceCard(borderColor = ElectricCobalt.copy(alpha = 0.35f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val dayPrefix = when {
                                detail.dayCode == null -> ""
                                detail.dayCode.startsWith("PROGRAM_DAY_") -> {
                                    val dayNum = detail.dayCode.removePrefix("PROGRAM_DAY_").toIntOrNull() ?: 1
                                    "${stringResource(R.string.day_number_format, dayNum)} — "
                                }
                                else -> "${stringResource(R.string.day)} ${detail.dayCode} — "
                            }
                            Text(
                                text = "$dayPrefix${detail.workoutName ?: stringResource(R.string.custom_workout)}",
                                style = MaterialTheme.typography.headlineMedium,
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3-Column Tabular Metric Strip
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
                            label = stringResource(R.string.total_volume),
                            value = "${FormatUtils.formatDouble(detail.session.totalWorkingVolumeKg, locale, 0)} kg",
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
                            value = if (detail.session.averageRpe != null) {
                                FormatUtils.formatDouble(detail.session.averageRpe, locale, 1)
                            } else {
                                stringResource(R.string.workout_detail_na)
                            },
                            valueColor = TelemetryAmber,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (detail.smartCoachingMode != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StatusPillBadge(
                            text = "${stringResource(R.string.smart_coaching)}: ${LocalizationUtils.getEnumString(detail.smartCoachingMode)}",
                            type = BadgeType.AI
                        )
                    }

                    if (!detail.session.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "${stringResource(R.string.workout_note)}: ${detail.session.note}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val exportToastMsg = stringResource(R.string.session_copied_toast)
                    // Side-by-Side Action Bar: Export to Coach & AI Summary (Zero Layout Shift)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val exportText = buildString {
                                    appendLine("Workout: ${detail.workoutName ?: "Session"} ($dateStr)")
                                    appendLine("Volume: ${detail.session.totalWorkingVolumeKg} kg | Duration: $durationStr | Avg RPE: ${detail.session.averageRpe ?: "-"}")
                                    detail.exercises.forEach { ex ->
                                        val workingCount = ex.sets.count { it.setLog.setType != "WARMUP" && it.setLog.completed }
                                        appendLine("- ${ex.exercise?.name ?: "Exercise"}: $workingCount working sets")
                                    }
                                }
                                clipboard.setPrimaryClip(ClipData.newPlainText("FitPilot Session Export", exportText))
                                Toast.makeText(context, exportToastMsg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = ElectricCobalt
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.export_to_coach),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        if (isAiEnabled) {
                            Button(
                                onClick = { viewModel.generateAiSummary() },
                                enabled = !isGeneratingSummary,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                // Fixed 18.dp Box + Crossfade prevents any horizontal width jump
                                Box(
                                    modifier = Modifier.size(18.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Crossfade(
                                        targetState = isGeneratingSummary,
                                        animationSpec = tween(180),
                                        label = "aiSummarySpinner"
                                    ) { generating ->
                                        if (generating) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = ElectricCobalt
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.generate_summary),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            // 2. AI Workout Summary Card (if generated)
            if (aiSummary != null) {
                item {
                    StandardSurfaceCard(borderColor = AthleticEmerald.copy(alpha = 0.45f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = AthleticEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.ai_workout_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AthleticEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = aiSummary!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.ai_summary_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3. Exercises List
            val sortedExercises = detail.exercises.sortedBy { it.exerciseSession.orderIndex }
            items(
                items = sortedExercises,
                key = { it.exerciseSession.id }
            ) { ex ->
                ExerciseDetailTelemetryCard(ex, locale)
            }

            // 4. Cardio Session Summary
            if (detail.cardioSession != null) {
                item {
                    val c = detail.cardioSession
                    StandardSurfaceCard(borderColor = AthleticEmerald.copy(alpha = 0.4f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.cardio_session_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AthleticEmerald
                            )
                            StatusPillBadge(
                                text = stringResource(R.string.completed_label),
                                type = BadgeType.SUCCESS
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.cardio_type_format, c.cardioType),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(
                                R.string.cardio_duration_format,
                                "${c.durationMinutes}"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!c.notes.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${stringResource(R.string.note_label)} ${c.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExerciseDetailTelemetryCard(
    ex: WorkoutDetailExerciseDto,
    locale: Locale
) {
    val warmupSets = ex.sets.filter { it.setLog.setType == "WARMUP" }.sortedBy { it.setLog.setNumber }
    val workingSets = ex.sets.filter { it.setLog.setType != "WARMUP" }.sortedBy { it.setLog.setNumber }

    StandardSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ex.exercise?.name ?: "Exercise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (ex.exerciseSession.supersetGroupId != null) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusPillBadge(
                    text = stringResource(R.string.superset),
                    type = BadgeType.AI
                )
            }
        }

        if (!ex.exerciseSession.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${stringResource(R.string.note_label)} ${ex.exerciseSession.note}",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = ElectricCobalt
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (warmupSets.isNotEmpty()) {
            Text(
                text = stringResource(R.string.warm_ups),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = TelemetryAmber
            )
            Spacer(modifier = Modifier.height(6.dp))
            warmupSets.forEach { setDto ->
                SetDetailTelemetryRow(setDto = setDto, isWarmup = true, locale = locale)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (workingSets.isNotEmpty()) {
            Text(
                text = stringResource(R.string.workout_detail_working_sets),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = ElectricCobalt
            )
            Spacer(modifier = Modifier.height(6.dp))
            workingSets.forEach { setDto ->
                SetDetailTelemetryRow(setDto = setDto, isWarmup = false, locale = locale)
            }
        }
    }
}

@Composable
fun SetDetailTelemetryRow(
    setDto: SetLogWithDetailsDto,
    isWarmup: Boolean,
    locale: Locale
) {
    val set = setDto.setLog
    val restLog = setDto.restTimerLog
    val advice = setDto.setAdvice

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPillBadge(
                text = if (isWarmup) "W${set.setNumber}" else "S${set.setNumber}",
                type = if (isWarmup) BadgeType.WARNING else BadgeType.INFO
            )

            if (set.isSkipped) {
                StatusPillBadge(
                    text = stringResource(R.string.workout_detail_skipped),
                    type = BadgeType.DANGER
                )
            } else if (set.completed) {
                val w = if (set.actualWeight != null) {
                    "${FormatUtils.formatDouble(set.actualWeight, locale, 1)} ${set.weightUnit}"
                } else {
                    "BW"
                }
                val r = "${set.actualReps ?: 0} ${stringResource(R.string.workout_detail_reps)}"
                val rpe = if (set.rpe != null) "@ RPE ${FormatUtils.formatDouble(set.rpe, locale, 1)}" else ""
                Text(
                    text = "$w × $r $rpe".trim(),
                    style = MaterialTheme.typography.labelLarge,
                    color = AthleticEmerald,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = stringResource(R.string.status_incomplete),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (restLog != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.workout_detail_rest_format,
                    restLog.actualRestSeconds ?: 0,
                    restLog.plannedRestSeconds
                ).trim(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (advice != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.workout_detail_advice_format, advice.adviceText).trim(),
                style = MaterialTheme.typography.labelSmall,
                color = ElectricCobalt
            )
        }

        if (!set.note.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${stringResource(R.string.note_label)} ${set.note}",
                style = MaterialTheme.typography.labelSmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
