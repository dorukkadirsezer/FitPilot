package com.doruk.fitprogress.feature.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.domain.model.ActiveExercise
import com.doruk.fitprogress.core.domain.model.ActiveSet
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.StatusBadge

import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet

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
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryActionButton(onClick = onNavigateBack, text = "Go Back")
            }
        }
        return
    }

    val showWorkoutCompleteDialog by viewModel.showWorkoutCompleteDialog.collectAsStateWithLifecycle()
    val aiSummary by viewModel.aiWorkoutSummary.collectAsStateWithLifecycle()

    if (showWorkoutCompleteDialog) {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.Center))
            
            AlertDialog(
                onDismissRequest = { /* forced to click button */ },
                title = { Text("Workout Complete! \uD83C\uDF89", color = NeonGreen, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (aiSummary != null) {
                            Text(aiSummary!!, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            Text("Great job finishing your workout!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                confirmButton = {
                    PrimaryActionButton(onClick = {
                        viewModel.dismissWorkoutCompleteAndClose {
                            onNavigateBack()
                        }
                    }, text = "Close", modifier = Modifier.fillMaxWidth())
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
                title = { Text(s.name, fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Timer Settings", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        activeNoteType = NoteType.Workout(s.note)
                    }) {
                        val tint = if (s.note.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Workout Note", tint = tint)
                    }
                    TextButton(onClick = {
                        if (viewModel.hasIncompleteSets()) {
                            showFinishDialog = true
                        } else {
                            viewModel.finishWorkout(skipIncomplete = false)
                        }
                    }) {
                        Text("Finish", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            if (timerState != null) {
                // Determine context text based on superset or set log
                val contextText = if (timerState!!.supersetGroupId != null) {
                    "Rest after Superset Round ${timerState!!.roundNumber ?: ""}"
                } else {
                    "Rest Time"
                }

                RestTimerPanel(
                    remainingSeconds = timerState!!.remainingSeconds,
                    contextText = contextText,
                    onDismiss = { viewModel.dismissTimer() },
                    onAdd = { viewModel.addTimerSeconds(30) },
                    onSubtract = { viewModel.subtractTimerSeconds(15) }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 120.dp) // Extra padding for bottom bar
        ) {
            // Warmup Instructions
            if (s.warmups.isNotEmpty()) {
                item {
                    Text("General Warm-ups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            s.warmups.forEach { w ->
                                Text("• ${com.doruk.fitprogress.core.util.LocalizationUtils.getWarmupInstruction(w.description)} ${if (w.durationMinutes != null) "(${w.durationMinutes} min)" else ""}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // Group exercises by Superset Group
            val groupedExercises = s.exercises.groupBy { it.supersetGroupId ?: it.exerciseSessionId }

            groupedExercises.forEach { (groupId, exercises) ->
                item {
                    val isSuperset = exercises.size > 1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (isSuperset) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    StatusBadge(text = "SUPERSET", color = VividViolet)
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
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                PrimaryActionButton(
                    text = "Finish Workout",
                    onClick = {
                        if (viewModel.hasIncompleteSets()) {
                            showFinishDialog = true
                        } else {
                            viewModel.finishWorkout(skipIncomplete = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("Unfinished Sets", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            text = { Text("You have unfinished sets. Do you want to save them as skipped and finish the workout?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                PrimaryActionButton(onClick = {
                    showFinishDialog = false
                    viewModel.finishWorkout(skipIncomplete = true)
                }, text = "Finish & Skip")
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    
    var showCardioOptions by remember(cardioRec) { mutableStateOf(cardioRec != null && cardioRec?.recommendedCardio != null) }
    var showCardioTimer by remember { mutableStateOf(false) }
    
    // Parse duration from recommendation text
    val parsedDuration = remember(cardioRec) {
        val text = cardioRec?.recommendedCardio ?: ""
        val match = Regex("(\\d+)\\s*min").find(text)
        match?.groupValues?.get(1)?.toIntOrNull() ?: 15
    }
    
    var activeCardioDuration by remember { mutableStateOf(parsedDuration) }
    var timerRunning by remember { mutableStateOf(false) }
    var timerSecondsRemaining by remember { mutableStateOf(activeCardioDuration * 60) }

    // Cardio Timer Job
    LaunchedEffect(timerRunning, timerSecondsRemaining) {
        if (timerRunning && timerSecondsRemaining > 0) {
            kotlinx.coroutines.delay(1000)
            timerSecondsRemaining -= 1
        }
    }

    if (showCardioOptions && cardioRec != null) {
        AlertDialog(
            onDismissRequest = { 
                // Do not dismiss on outside tap to force a choice
            },
            title = { Text(stringResource(com.doruk.fitprogress.R.string.post_workout_cardio), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            text = { 
                Column {
                    Text("Recommended: ${cardioRec?.recommendedCardio ?: "None"}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Reason: ${cardioRec?.reason}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryActionButton(onClick = {
                        activeCardioDuration = parsedDuration
                        timerSecondsRemaining = activeCardioDuration * 60
                        showCardioOptions = false
                        showCardioTimer = true
                        timerRunning = true
                    }, modifier = Modifier.fillMaxWidth(), text = "Start Cardio")
                    
                    OutlinedButton(onClick = {
                        activeCardioDuration = (parsedDuration - 5).coerceAtLeast(5)
                        timerSecondsRemaining = activeCardioDuration * 60
                        showCardioOptions = false
                        showCardioTimer = true
                        timerRunning = true
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Shorten Cardio (-5 min)", color = MaterialTheme.colorScheme.onSurface)
                    }
                    
                    TextButton(onClick = {
                        viewModel.completeCardioAndClose(parsedDuration, cardioRec?.recommendedCardio ?: "Unknown")
                        showCardioOptions = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Mark Completed Manually", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    
                    TextButton(onClick = {
                        viewModel.skipCardioAndClose()
                        showCardioOptions = false
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(com.doruk.fitprogress.R.string.skip_cardio), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showCardioTimer) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Cardio Timer", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
            text = {
                val mins = timerSecondsRemaining / 60
                val secs = timerSecondsRemaining % 60
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${cardioRec?.recommendedCardio}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(String.format("%02d:%02d", mins, secs), fontSize = 48.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(onClick = {
                    timerRunning = !timerRunning
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)) {
                    Text(if (timerRunning) "Pause" else "Resume")
                }
            },
            dismissButton = {
                PrimaryActionButton(onClick = {
                    timerRunning = false
                    showCardioTimer = false
                    val elapsedMinutes = activeCardioDuration - (timerSecondsRemaining / 60)
                    viewModel.completeCardioAndClose(elapsedMinutes.coerceAtLeast(0), cardioRec?.recommendedCardio ?: "Unknown")
                }, text = "Finish")
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
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(exercise.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onEditNote) {
                val tint = if (exercise.note.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Exercise Note", tint = tint, modifier = Modifier.size(20.dp))
            }
        }
        
        if (exercise.requiresStraps) {
            Spacer(modifier = Modifier.height(4.dp))
            StatusBadge(text = stringResource(com.doruk.fitprogress.R.string.active_workout_straps_required), color = MaterialTheme.colorScheme.error)
        }
        val allReminders = exercise.programRiskReminders.map { com.doruk.fitprogress.core.util.LocalizationUtils.getReminderString(it) }.distinct()
        if (allReminders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            allReminders.forEach { reminder ->
                Text("⚠ $reminder", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        
        if (exercise.coachNote != null) {
            val localizedCoachNote = com.doruk.fitprogress.core.util.LocalizationUtils.getCoachNoteString(exercise.coachNote)
            if (!allReminders.contains(localizedCoachNote)) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("${stringResource(com.doruk.fitprogress.R.string.coach_label)}: $localizedCoachNote", fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Sets header
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("", modifier = Modifier.weight(0.8f))
            Text(stringResource(com.doruk.fitprogress.R.string.active_workout_weight), modifier = Modifier.weight(1.5f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(com.doruk.fitprogress.R.string.active_workout_reps), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("RPE", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(com.doruk.fitprogress.R.string.done),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        var showedWarmupHeader = false
        var showedWorkingHeader = false

        exercise.sets.forEach { set ->
            if (set.isWarmup && !showedWarmupHeader) {
                Text(stringResource(com.doruk.fitprogress.R.string.section_warmup_sets), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                showedWarmupHeader = true
            } else if (!set.isWarmup && !showedWorkingHeader) {
                Text(stringResource(com.doruk.fitprogress.R.string.section_working_sets), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                showedWorkingHeader = true
            }

            SetRow(
                set = set,
                onUpdate = { w, r, rpe -> viewModel.updateSet(exercise.exerciseSessionId, set.id, w, r, rpe) },
                onCompleteToggle = { viewModel.toggleSetCompletion(exercise.exerciseSessionId, set.id) },
                onEditNote = { onEditSetNote(set.id, set.note) }
            )
        }
        
        if (exercise.supersetAdviceText != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)),
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
    
    val backgroundColor = if (isCompleted) NeonGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val borderColor = if (isCompleted) NeonGreen.copy(alpha = 0.5f) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Set Indicator
            Box(
                modifier = Modifier.weight(0.8f).clickable { onEditNote() },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(com.doruk.fitprogress.R.string.label_warmup_short).format(set.setNumber), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!set.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Set Note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Weight Input
            val currentLocale = java.util.Locale.getDefault()
            var weightText by remember(set.id) {
                val initVal = set.loggedWeight ?: set.targetWeightMin
                mutableStateOf(if (initVal != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(initVal, currentLocale) else "")
            }
            GymFlowTextField(
                value = weightText,
                onValueChange = { 
                    weightText = it
                    onUpdate(com.doruk.fitprogress.core.util.FormatUtils.parseFloatSafe(it), set.loggedReps, set.loggedRpe)
                },
                modifier = Modifier.weight(1.5f),
                label = if (set.targetWeightMin != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(set.targetWeightMin, currentLocale) else "Any",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(set.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )

            // Reps Input
            var repsText by remember(set.id) {
                val initVal = set.loggedReps ?: set.targetRepsMin
                mutableStateOf(initVal?.toString() ?: "")
            }
            GymFlowTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    onUpdate(set.loggedWeight, com.doruk.fitprogress.core.util.FormatUtils.parseIntSafe(it), set.loggedRpe)
                },
                modifier = Modifier.weight(1f),
                label = "${set.targetRepsMin}",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Complete Button
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                Button(
                    onClick = onCompleteToggle,
                    modifier = Modifier.height(44.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCompleted) NeonGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (isCompleted) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = "Complete Warmup",
                        modifier = Modifier.size(20.dp)
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
    
    val backgroundColor = if (isCompleted) NeonGreen.copy(alpha = 0.15f) else Color.Transparent
    val borderColor = if (isCompleted) NeonGreen.copy(alpha = 0.5f) else Color.Transparent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Set Indicator
            Box(
                modifier = Modifier.weight(0.8f).clickable { onEditNote() },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(com.doruk.fitprogress.R.string.label_set_short).format(set.setNumber), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    if (!set.note.isNullOrBlank()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Set Note", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Weight Input
            val currentLocale = java.util.Locale.getDefault()
            var weightText by remember(set.id) {
                val initVal = set.loggedWeight ?: set.targetWeightMin
                mutableStateOf(if (initVal != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(initVal, currentLocale) else "")
            }
            GymFlowTextField(
                value = weightText,
                onValueChange = { 
                    weightText = it
                    onUpdate(com.doruk.fitprogress.core.util.FormatUtils.parseFloatSafe(it), set.loggedReps, set.loggedRpe)
                },
                modifier = Modifier.weight(1.5f),
                label = if (set.targetWeightMin != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(set.targetWeightMin, currentLocale) else "Any",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                suffix = { Text(set.unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            )

            // Reps Input
            var repsText by remember(set.id) {
                val initVal = set.loggedReps ?: set.targetRepsMin
                mutableStateOf(initVal?.toString() ?: "")
            }
            GymFlowTextField(
                value = repsText,
                onValueChange = {
                    repsText = it
                    onUpdate(set.loggedWeight, com.doruk.fitprogress.core.util.FormatUtils.parseIntSafe(it), set.loggedRpe)
                },
                modifier = Modifier.weight(1f),
                label = if (set.targetRepsMin != null) "${set.targetRepsMin}" else "Any",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // RPE Input
            var rpeText by remember(set.id) {
                val initVal = set.loggedRpe ?: set.targetRpeMin
                mutableStateOf(if (initVal != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(initVal, currentLocale) else "")
            }
            GymFlowTextField(
                value = rpeText,
                onValueChange = {
                    rpeText = it
                    onUpdate(set.loggedWeight, set.loggedReps, com.doruk.fitprogress.core.util.FormatUtils.parseFloatSafe(it))
                },
                modifier = Modifier.weight(1f),
                label = if (set.targetRpeMin != null) com.doruk.fitprogress.core.util.FormatUtils.formatDoubleEditable(set.targetRpeMin, currentLocale) else "-",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // Check Button
            Box(modifier = Modifier.weight(0.5f), contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onCompleteToggle,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isCompleted) NeonGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = "Complete Set",
                        tint = if (isCompleted) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        if (set.adviceText != null) {
            val adviceColor = when (set.adviceType) {
                "VERY_HIGH_RPE", "LOW_REPS", "PERFORMANCE_DROP" -> MaterialTheme.colorScheme.error
                "HIGH_RPE" -> VividViolet
                "GOOD_SET" -> NeonGreen
                else -> MaterialTheme.colorScheme.primary
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
            .padding(16.dp)
            .padding(bottom = 16.dp), // Extra safe padding
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(contextText.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(timeString, fontSize = 56.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onSubtract,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("-15s")
                }
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text("SKIP")
                }

                OutlinedButton(
                    onClick = onAdd,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("+30s")
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
        is NoteType.Workout -> "Workout Note"
        is NoteType.Exercise -> "Exercise Note"
        is NoteType.Set -> "Set Note"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                placeholder = { Text("Enter your note here...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        confirmButton = {
            PrimaryActionButton(onClick = {
                onSave(noteType, text.ifBlank { null })
            }, text = "Save")
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = { Text(stringResource(com.doruk.fitprogress.R.string.timer_settings), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.doruk.fitprogress.R.string.enable_master_sound), color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = timerSoundEnabled, onCheckedChange = { viewModel.setTimerSoundEnabled(it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.doruk.fitprogress.R.string.thirty_sec_warning_tone), color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = timerWarningEnabled, onCheckedChange = { viewModel.setTimerWarningEnabled(it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(com.doruk.fitprogress.R.string.timer_finished_tone), color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = timerFinishedEnabled, onCheckedChange = { viewModel.setTimerFinishedEnabled(it) })
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Enable Vibration", color = MaterialTheme.colorScheme.onSurface)
                    Switch(checked = timerVibrationEnabled, onCheckedChange = { viewModel.setTimerVibrationEnabled(it) })
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(stringResource(com.doruk.fitprogress.R.string.sound_style), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                val timerSoundStyle by viewModel.timerSoundStyle.collectAsStateWithLifecycle()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val styles = listOf(
                        com.doruk.fitprogress.core.domain.model.TimerSoundStyle.SOFT_BEEP to "Soft Beep",
                        com.doruk.fitprogress.core.domain.model.TimerSoundStyle.CLEAR_TONE to "Clear Chime"
                    )
                    styles.forEach { (style, label) ->
                        val isSelected = style == timerSoundStyle
                        Button(
                            onClick = { viewModel.setTimerSoundStyle(style) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                        ) {
                            Text(label)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { viewModel.testTimerSound() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(stringResource(com.doruk.fitprogress.R.string.test_sound))
                }
            }
        },
        confirmButton = {
            PrimaryActionButton(onClick = onDismiss, text = "Done")
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}


