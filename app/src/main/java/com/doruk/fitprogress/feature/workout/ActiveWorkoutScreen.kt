package com.doruk.fitprogress.feature.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.domain.model.ActiveExercise
import com.doruk.fitprogress.core.domain.model.ActiveSet
import com.doruk.fitprogress.core.ui.components.BadgeType
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.StatusBadge
import com.doruk.fitprogress.core.ui.components.StatusPillBadge
import com.doruk.fitprogress.core.util.FormatUtils
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.TelemetryAmber
import com.doruk.fitprogress.theme.VividViolet
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveWorkoutScreen(
    onNavigateBack: () -> Unit,
    viewModel: ActiveWorkoutViewModel = hiltViewModel()
) {
    val session by viewModel.workoutSession.collectAsStateWithLifecycle()
    val timerState by viewModel.restTimerState.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var showFinishDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var activeNoteType by remember { mutableStateOf<NoteType?>(null) }

    if (errorMessage != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryActionButton(onClick = onNavigateBack, text = stringResource(R.string.back))
            }
        }
        return
    }

    val showWorkoutCompleteDialog by viewModel.showWorkoutCompleteDialog.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiWorkoutSummary.collectAsStateWithLifecycle()

    if (showWorkoutCompleteDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Center)
            )

            AlertDialog(
                onDismissRequest = { /* forced to click button */ },
                title = {
                    Text(
                        text = stringResource(R.string.workout_complete_title),
                        color = AthleticEmerald,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (aiSummary != null) {
                            Text(aiSummary!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text(
                                stringResource(R.string.workout_complete_default_msg),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    PrimaryActionButton(
                        onClick = {
                            viewModel.dismissWorkoutCompleteAndClose {
                                onNavigateBack()
                            }
                        },
                        text = stringResource(R.string.done),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
        return
    }

    if (session == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val s = session!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.timer_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {
                        activeNoteType = NoteType.Workout(s.note)
                    }) {
                        val tint = if (s.note.isNullOrBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.workout_note),
                            tint = tint
                        )
                    }
                    TextButton(
                        onClick = {
                            if (viewModel.hasIncompleteSets()) {
                                showFinishDialog = true
                            } else {
                                viewModel.finishWorkout(skipIncomplete = false)
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.active_workout_finish),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = timerState != null,
                enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(220)) + fadeIn(tween(220)),
                exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180)) + fadeOut(tween(180))
            ) {
                val activeTimer = timerState
                if (activeTimer != null) {
                    val contextText = if (activeTimer.supersetGroupId != null) {
                        "Rest after Superset Round ${activeTimer.roundNumber ?: ""}"
                    } else {
                        stringResource(R.string.active_workout_rest_time)
                    }

                    RestTimerPanel(
                        remainingSeconds = activeTimer.remainingSeconds,
                        contextText = contextText,
                        onDismiss = { viewModel.dismissTimer() },
                        onAdd = { viewModel.addTimerSeconds(30) },
                        onSubtract = { viewModel.subtractTimerSeconds(15) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 140.dp)
        ) {
            // Warmup Instructions
            if (s.warmups.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.active_workout_general_warmups),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricCobalt
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            s.warmups.forEach { w ->
                                Text(
                                    text = "• ${LocalizationUtils.getWarmupInstruction(w.description)} ${if (w.durationMinutes != null) "(${w.durationMinutes} min)" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Group exercises by Superset Group
            val groupedExercises = s.exercises.groupBy { it.supersetGroupId ?: it.exerciseSessionId }

            groupedExercises.forEach { (_, exercises) ->
                item {
                    val isSuperset = exercises.size > 1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSuperset) ElectricCobalt.copy(alpha = 0.40f) else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (isSuperset) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusPillBadge(text = stringResource(R.string.superset), type = BadgeType.AI)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            exercises.forEachIndexed { index, ex ->
                                ExerciseItem(
                                    exercise = ex,
                                    viewModel = viewModel,
                                    onEditNote = {
                                        activeNoteType = NoteType.Exercise(ex.exerciseSessionId, ex.note)
                                    },
                                    onEditSetNote = { setId, note ->
                                        activeNoteType = NoteType.Set(ex.exerciseSessionId, setId, note)
                                    }
                                )
                                if (index < exercises.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 20.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryActionButton(
                    text = stringResource(R.string.active_workout_finish),
                    onClick = {
                        if (viewModel.hasIncompleteSets()) {
                            showFinishDialog = true
                        } else {
                            viewModel.finishWorkout(skipIncomplete = false)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = {
                Text(
                    stringResource(R.string.unfinished_sets_title),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.unfinished_sets_message),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                PrimaryActionButton(
                    onClick = {
                        showFinishDialog = false
                        viewModel.finishWorkout(skipIncomplete = true)
                    },
                    text = stringResource(R.string.finish_and_skip)
                )
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (activeNoteType != null) {
        NoteDialog(
            noteType = activeNoteType!!,
            onDismiss = { activeNoteType = null },
            onSave = { noteType, newNote ->
                when (noteType) {
                    is NoteType.Workout -> viewModel.updateWorkoutNote(newNote)
                    is NoteType.Exercise -> viewModel.updateExerciseNote(noteType.exerciseSessionId, newNote)
                    is NoteType.Set -> viewModel.updateSetNote(noteType.exerciseSessionId, noteType.setId, newNote)
                }
                activeNoteType = null
            }
        )
    }

    if (showSettingsDialog) {
        TimerSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }

    val cardioRec by viewModel.cardioRecommendation.collectAsStateWithLifecycle()

    LaunchedEffect(cardioRec) {
        if (cardioRec != null && cardioRec?.recommendedCardio == null) {
            viewModel.skipCardioAndClose()
        }
    }

    var showCardioOptions by remember(cardioRec) {
        mutableStateOf(cardioRec != null && cardioRec?.recommendedCardio != null)
    }
    var showCardioTimer by remember { mutableStateOf(false) }

    val parsedDuration = remember(cardioRec) {
        val text = cardioRec?.recommendedCardio ?: ""
        val match = Regex("(\\d+)\\s*min").find(text)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 15
    }

    var activeCardioDuration by remember { mutableStateOf(parsedDuration) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerSecondsRemaining by remember { mutableStateOf(activeCardioDuration * 60) }

    LaunchedEffect(timerRunning, timerSecondsRemaining) {
        if (timerRunning && timerSecondsRemaining > 0) {
            delay(1000)
            timerSecondsRemaining -= 1
        }
    }

    if (showCardioOptions && cardioRec != null) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    stringResource(R.string.post_workout_cardio),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Recommended: ${cardioRec?.recommendedCardio ?: "None"}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Reason: ${cardioRec?.reason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrimaryActionButton(
                        onClick = {
                            activeCardioDuration = parsedDuration
                            timerSecondsRemaining = activeCardioDuration * 60
                            showCardioOptions = false
                            showCardioTimer = true
                            timerRunning = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.start_cardio)
                    )

                    OutlinedButton(
                        onClick = {
                            activeCardioDuration = (parsedDuration - 5).coerceAtLeast(5)
                            timerSecondsRemaining = activeCardioDuration * 60
                            showCardioOptions = false
                            showCardioTimer = true
                            timerRunning = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.shorten_cardio_5m), color = MaterialTheme.colorScheme.onSurface)
                    }

                    TextButton(
                        onClick = {
                            viewModel.completeCardioAndClose(
                                parsedDuration,
                                cardioRec?.recommendedCardio ?: "Unknown"
                            )
                            showCardioOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.mark_completed_manually), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    TextButton(
                        onClick = {
                            viewModel.skipCardioAndClose()
                            showCardioOptions = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.skip_cardio), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showCardioTimer) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    stringResource(R.string.dialog_cardio_timer),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                val mins = timerSecondsRemaining / 60
                val secs = timerSecondsRemaining % 60
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "${cardioRec?.recommendedCardio}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        String.format("%02d:%02d", mins, secs),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { timerRunning = !timerRunning },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(if (timerRunning) stringResource(R.string.dialog_pause) else stringResource(R.string.dialog_resume))
                }
            },
            dismissButton = {
                PrimaryActionButton(
                    onClick = {
                        timerRunning = false
                        showCardioTimer = false
                        val elapsedMinutes = activeCardioDuration - (timerSecondsRemaining / 60)
                        viewModel.completeCardioAndClose(
                            elapsedMinutes.coerceAtLeast(0),
                            cardioRec?.recommendedCardio ?: "Unknown"
                        )
                    },
                    text = stringResource(R.string.dialog_finish)
                )
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun ExerciseItem(
    exercise: ActiveExercise,
    viewModel: ActiveWorkoutViewModel,
    onEditNote: () -> Unit,
    onEditSetNote: (String, String?) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = exercise.name,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onEditNote) {
                val tint = if (exercise.note.isNullOrBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Exercise Note",
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (exercise.requiresStraps) {
            Spacer(modifier = Modifier.height(6.dp))
            StatusPillBadge(
                text = stringResource(R.string.active_workout_straps_required),
                type = BadgeType.WARNING
            )
        }

        val allReminders = exercise.programRiskReminders
            .map { LocalizationUtils.getReminderString(it) }
            .distinct()
        if (allReminders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            allReminders.forEach { reminder ->
                Text(
                    text = "⚠ $reminder",
                    color = TelemetryAmber,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (exercise.coachNote != null) {
            val localizedCoachNote = LocalizationUtils.getCoachNoteString(exercise.coachNote)
            if (!allReminders.contains(localizedCoachNote)) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${stringResource(R.string.coach_label)}: $localizedCoachNote",
                    fontStyle = FontStyle.Italic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ElectricCobalt
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sets Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.set).uppercase(),
                modifier = Modifier.weight(0.9f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.active_workout_weight).uppercase(),
                modifier = Modifier.weight(1.5f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.active_workout_reps).uppercase(),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "RPE",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.weight(0.55f), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.done),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))

        var showedWarmupHeader = false
        var showedWorkingHeader = false

        exercise.sets.forEach { set ->
            if (set.isWarmup && !showedWarmupHeader) {
                Text(
                    text = stringResource(R.string.section_warmup_sets),
                    style = MaterialTheme.typography.labelLarge,
                    color = TelemetryAmber,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                showedWarmupHeader = true
            } else if (!set.isWarmup && !showedWorkingHeader) {
                Text(
                    text = stringResource(R.string.section_working_sets),
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCobalt,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                showedWorkingHeader = true
            }

            SetRow(
                set = set,
                onUpdate = { w, r, rpe ->
                    viewModel.updateSet(exercise.exerciseSessionId, set.id, w, r, rpe)
                },
                onCompleteToggle = {
                    viewModel.toggleSetCompletion(exercise.exerciseSessionId, set.id)
                },
                onEditNote = { onEditSetNote(set.id, set.note) }
            )
        }

        if (exercise.supersetAdviceText != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TelemetryAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Superset Advice:\n${exercise.supersetAdviceText}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
fun SetRow(
    set: ActiveSet,
    onUpdate: (Float?, Int?, Float?) -> Unit,
    onCompleteToggle: () -> Unit,
    onEditNote: () -> Unit
) {
    if (set.isWarmup) {
        WarmupSetRow(set, onUpdate, onCompleteToggle, onEditNote)
    } else {
        WorkingSetRow(set, onUpdate, onCompleteToggle, onEditNote)
    }
}

@Composable
fun WarmupSetRow(
    set: ActiveSet,
    onUpdate: (Float?, Int?, Float?) -> Unit,
    onCompleteToggle: () -> Unit,
    onEditNote: () -> Unit
) {
    val isCompleted = set.isCompleted

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) AthleticEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        animationSpec = tween(220),
        label = "warmupRowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) AthleticEmerald.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(220),
        label = "warmupRowBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .clickable { onEditNote() },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPillBadge(
                        text = "W${set.setNumber}",
                        type = if (isCompleted) BadgeType.SUCCESS else BadgeType.WARNING
                    )
                    if (!set.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val currentLocale = java.util.Locale.getDefault()
            var weightText by remember(set.id) {
                val initVal = set.loggedWeight ?: set.targetWeightMin
                mutableStateOf(
                    if (initVal != null) FormatUtils.formatDoubleEditable(initVal, currentLocale) else ""
                )
            }
            GymFlowTextField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    onUpdate(FormatUtils.parseFloatSafe(it), set.loggedReps, set.loggedRpe)
                },
                modifier = Modifier.weight(1.5f),
                label = if (set.targetWeightMin != null) {
                    FormatUtils.formatDoubleEditable(set.targetWeightMin, currentLocale)
                } else {
                    "Any"
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = {
                    Text(
                        set.unit,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            var repsText by remember(set.id) {
                val initVal = set.loggedReps ?: set.targetRepsMin
                mutableStateOf(initVal?.toString() ?: "")
            }
            GymFlowTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    onUpdate(set.loggedWeight, FormatUtils.parseIntSafe(it), set.loggedRpe)
                },
                modifier = Modifier.weight(1f),
                label = "${set.targetRepsMin}",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Box(modifier = Modifier.weight(1.55f), contentAlignment = Alignment.CenterEnd) {
                IconButton(
                    onClick = onCompleteToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCompleted) AthleticEmerald.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isCompleted) AthleticEmerald else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = "Complete Warmup",
                        tint = if (isCompleted) AthleticEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WorkingSetRow(
    set: ActiveSet,
    onUpdate: (Float?, Int?, Float?) -> Unit,
    onCompleteToggle: () -> Unit,
    onEditNote: () -> Unit
) {
    val isCompleted = set.isCompleted

    val backgroundColor by animateColorAsState(
        targetValue = if (isCompleted) AthleticEmerald.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
        animationSpec = tween(220),
        label = "workingRowBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isCompleted) AthleticEmerald.copy(alpha = 0.60f) else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = tween(220),
        label = "workingRowBorder"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .clickable { onEditNote() },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPillBadge(
                        text = "S${set.setNumber}",
                        type = if (isCompleted) BadgeType.SUCCESS else BadgeType.INFO
                    )
                    if (!set.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Set Note",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            val currentLocale = java.util.Locale.getDefault()
            var weightText by remember(set.id) {
                val initVal = set.loggedWeight ?: set.targetWeightMin
                mutableStateOf(
                    if (initVal != null) FormatUtils.formatDoubleEditable(initVal, currentLocale) else ""
                )
            }
            GymFlowTextField(
                value = weightText,
                onValueChange = {
                    weightText = it
                    onUpdate(FormatUtils.parseFloatSafe(it), set.loggedReps, set.loggedRpe)
                },
                modifier = Modifier.weight(1.5f),
                label = if (set.targetWeightMin != null) {
                    FormatUtils.formatDoubleEditable(set.targetWeightMin, currentLocale)
                } else {
                    "Any"
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = {
                    Text(
                        set.unit,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )

            var repsText by remember(set.id) {
                val initVal = set.loggedReps ?: set.targetRepsMin
                mutableStateOf(initVal?.toString() ?: "")
            }
            GymFlowTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    onUpdate(set.loggedWeight, FormatUtils.parseIntSafe(it), set.loggedRpe)
                },
                modifier = Modifier.weight(1f),
                label = if (set.targetRepsMin != null) "${set.targetRepsMin}" else "Any",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            var rpeText by remember(set.id) {
                val initVal = set.loggedRpe ?: set.targetRpeMin
                mutableStateOf(
                    if (initVal != null) FormatUtils.formatDoubleEditable(initVal, currentLocale) else ""
                )
            }
            GymFlowTextField(
                value = rpeText,
                onValueChange = {
                    rpeText = it
                    onUpdate(set.loggedWeight, set.loggedReps, FormatUtils.parseFloatSafe(it))
                },
                modifier = Modifier.weight(1f),
                label = if (set.targetRpeMin != null) {
                    FormatUtils.formatDoubleEditable(set.targetRpeMin, currentLocale)
                } else {
                    "-"
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Box(modifier = Modifier.weight(0.55f), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onCompleteToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isCompleted) AthleticEmerald.copy(alpha = 0.22f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .border(
                            1.dp,
                            if (isCompleted) AthleticEmerald else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = "Complete Set",
                        tint = if (isCompleted) AthleticEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (set.adviceText != null) {
            val adviceColor = when (set.adviceType) {
                "VERY_HIGH_RPE", "LOW_REPS", "PERFORMANCE_DROP" -> MaterialTheme.colorScheme.error
                "HIGH_RPE" -> TelemetryAmber
                "GOOD_SET" -> AthleticEmerald
                else -> ElectricCobalt
            }
            Text(
                text = set.adviceText,
                color = adviceColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun RestTimerPanel(
    remainingSeconds: Int,
    contextText: String,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onSubtract: () -> Unit
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeString = String.format("%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ElectricCobalt.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = contextText.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = ElectricCobalt,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSubtract,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("-15s", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        stringResource(R.string.active_workout_skip).uppercase(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricCobalt),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCobalt)
                ) {
                    Text("+30s", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

sealed class NoteType {
    data class Workout(val currentNote: String?) : NoteType()
    data class Exercise(val exerciseSessionId: String, val currentNote: String?) : NoteType()
    data class Set(val exerciseSessionId: String, val setId: String, val currentNote: String?) : NoteType()
}

@Composable
fun NoteDialog(
    noteType: NoteType,
    onDismiss: () -> Unit,
    onSave: (NoteType, String?) -> Unit
) {
    var text by remember {
        mutableStateOf(
            when (noteType) {
                is NoteType.Workout -> noteType.currentNote ?: ""
                is NoteType.Exercise -> noteType.currentNote ?: ""
                is NoteType.Set -> noteType.currentNote ?: ""
            }
        )
    }

    val title = when (noteType) {
        is NoteType.Workout -> stringResource(R.string.workout_note)
        is NoteType.Exercise -> stringResource(R.string.exercise_note_title)
        is NoteType.Set -> stringResource(R.string.set_note_title)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.note_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = { onSave(noteType, text.ifBlank { null }) },
                text = stringResource(R.string.action_save)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun TimerSettingsDialog(
    viewModel: ActiveWorkoutViewModel,
    onDismiss: () -> Unit
) {
    val timerSoundEnabled by viewModel.timerSoundEnabled.collectAsStateWithLifecycle()
    val timerWarningEnabled by viewModel.timerWarningEnabled.collectAsStateWithLifecycle()
    val timerFinishedEnabled by viewModel.timerFinishedEnabled.collectAsStateWithLifecycle()
    val timerVibrationEnabled by viewModel.timerVibrationEnabled.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.timer_settings),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.enable_master_sound),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = timerSoundEnabled,
                        onCheckedChange = { viewModel.setTimerSoundEnabled(it) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.thirty_sec_warning_tone),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = timerWarningEnabled,
                        onCheckedChange = { viewModel.setTimerWarningEnabled(it) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        stringResource(R.string.timer_finished_tone),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = timerFinishedEnabled,
                        onCheckedChange = { viewModel.setTimerFinishedEnabled(it) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.enable_vibration), color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = timerVibrationEnabled,
                        onCheckedChange = { viewModel.setTimerVibrationEnabled(it) }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.sound_style),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                val timerSoundStyle by viewModel.timerSoundStyle.collectAsStateWithLifecycle()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val styles = listOf(
                        com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP to stringResource(R.string.sound_soft_beep),
                        com.doruk.fitprogress.core.domain.model.TimerSoundStyle.CLEAR_TONE to stringResource(R.string.sound_clear_tone)
                    )
                    styles.forEach { (style, label) ->
                        val isSelected = style == timerSoundStyle
                        Button(
                            onClick = { viewModel.setTimerSoundStyle(style) },
                            shape = RoundedCornerShape(12.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.testTimerSound() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(R.string.test_sound))
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(onClick = onDismiss, text = stringResource(R.string.done))
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
