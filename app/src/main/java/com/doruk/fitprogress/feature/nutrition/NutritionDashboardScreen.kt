package com.doruk.fitprogress.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.doruk.fitprogress.R
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.ui.components.*
import com.doruk.fitprogress.core.util.LocalizationUtils
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDashboardScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAddFood: (String, String) -> Unit,
    onNavigateToSavedFoods: (String, String) -> Unit,
    onNavigateToAiReview: (String, String) -> Unit,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nutrition),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    GymFlowTextButton(
                        text = stringResource(R.string.copy_yesterday),
                        onClick = { viewModel.requestCopyYesterdayMeals() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (state.isAiEnabled) {
                FloatingActionButton(
                    onClick = { viewModel.toggleAiDialog(true) },
                    containerColor = ElectricCobalt,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Food Log")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (state.showCopyConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCopyConfirmation() },
                title = {
                    Text(
                        stringResource(R.string.copy_yesterdays_meals),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        stringResource(R.string.copy_yesterday_confirmation),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmCopyYesterdayMeals() }) {
                        Text(
                            stringResource(R.string.copy),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCopyConfirmation() }) {
                        Text(
                            stringResource(R.string.cancel),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Date Navigation Pill Bar
            item {
                StandardSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GymFlowTextButton(
                            text = "← ${stringResource(R.string.previous)}",
                            onClick = { viewModel.changeDate(-1) }
                        )
                        StatusPillBadge(
                            text = state.currentDateIso,
                            type = BadgeType.INFO
                        )
                        GymFlowTextButton(
                            text = "${stringResource(R.string.next)} →",
                            onClick = { viewModel.changeDate(1) }
                        )
                    }
                }
            }

            // 2. Daily Macros Telemetry Card (3-Column Strip + %50 Pill Capsule Bars)
            item {
                val targets = state.nutritionDay
                if (targets != null) {
                    StandardSurfaceCard(borderColor = ElectricCobalt.copy(alpha = 0.35f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.daily_macros),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            StatusPillBadge(
                                text = "${state.totalCalories.toInt()} / ${targets.targetCalories} kcal",
                                type = BadgeType.INFO
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3-Column Tabular Macro Strip (Protein | Carbs | Fat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MacroStripColumn(
                                label = stringResource(R.string.protein),
                                value = "${state.totalProtein.toInt()}/${targets.targetProtein}g",
                                accentColor = AthleticEmerald,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            MacroStripColumn(
                                label = stringResource(R.string.carbs),
                                value = "${state.totalCarbs.toInt()}/${targets.targetCarbs}g",
                                accentColor = ElectricCobalt,
                                modifier = Modifier.weight(1f)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            )
                            MacroStripColumn(
                                label = stringResource(R.string.fat),
                                value = "${state.totalFat.toInt()}/${targets.targetFat}g",
                                accentColor = TelemetryAmber,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            MacroProgressBar(
                                label = stringResource(R.string.calories),
                                actual = state.totalCalories,
                                target = targets.targetCalories.toFloat(),
                                unit = "kcal",
                                remaining = state.remainingCalories,
                                color = ElectricCobalt
                            )
                            MacroProgressBar(
                                label = stringResource(R.string.protein),
                                actual = state.totalProtein,
                                target = targets.targetProtein.toFloat(),
                                unit = "g",
                                remaining = state.remainingProtein,
                                color = AthleticEmerald
                            )
                            MacroProgressBar(
                                label = stringResource(R.string.carbs),
                                actual = state.totalCarbs,
                                target = targets.targetCarbs.toFloat(),
                                unit = "g",
                                remaining = state.remainingCarbs,
                                color = ElectricCobalt
                            )
                            MacroProgressBar(
                                label = stringResource(R.string.fat),
                                actual = state.totalFat,
                                target = targets.targetFat.toFloat(),
                                unit = "g",
                                remaining = state.remainingFat,
                                color = TelemetryAmber
                            )
                        }
                    }
                }
            }

            // 3. Meal Sections
            val mealTypes = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK", "PRE_WORKOUT", "POST_WORKOUT")
            mealTypes.forEach { mealType ->
                val logsForMeal = state.mealLogs.filter { it.mealType == mealType }
                item {
                    MealSection(
                        mealType = mealType,
                        logs = logsForMeal,
                        onAddFoodClick = { onNavigateToAddFood(state.currentDateIso, mealType) },
                        onSavedFoodClick = { onNavigateToSavedFoods(state.currentDateIso, mealType) },
                        onDeleteLog = { viewModel.deleteMealLog(it) }
                    )
                }
            }
        }
    }

    if (state.showAiDialog) {
        var aiInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.toggleAiDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = ElectricCobalt,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        stringResource(R.string.ai_food_logger),
                        color = ElectricCobalt,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    GymFlowTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        label = stringResource(R.string.what_did_you_eat),
                        singleLine = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.ai_food_logger_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                AiActionButton(
                    onClick = {
                        viewModel.toggleAiDialog(false)
                        if (aiInput.isNotBlank() && state.nutritionDay != null) {
                            onNavigateToAiReview(aiInput, state.nutritionDay!!.id)
                        }
                    },
                    text = stringResource(R.string.parse)
                )
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleAiDialog(false) }) {
                    Text(
                        stringResource(R.string.cancel),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun MacroStripColumn(
    label: String,
    value: String,
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
fun MacroProgressBar(
    label: String,
    actual: Float,
    target: Float,
    unit: String,
    remaining: Float,
    color: Color
) {
    val progress = if (target > 0) (actual / target).coerceIn(0f, 1f) else 0f
    val isOver = remaining < 0
    val remainingText = if (isOver) {
        "${abs(remaining).toInt()} $unit ${stringResource(R.string.over)}"
    } else {
        "${remaining.toInt()} $unit ${stringResource(R.string.left)}"
    }
    val remainingColor = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${actual.toInt()} / ${target.toInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "($remainingText)",
                    style = MaterialTheme.typography.labelSmall,
                    color = remainingColor
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(percent = 50)),
            color = if (isOver) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun MealSection(
    mealType: String,
    logs: List<MealLogEntity>,
    onAddFoodClick: () -> Unit,
    onSavedFoodClick: () -> Unit,
    onDeleteLog: (MealLogEntity) -> Unit
) {
    StandardSurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = LocalizationUtils.getMealTypeName(mealType),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ElectricCobalt
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                GymFlowTextButton(
                    onClick = onSavedFoodClick,
                    text = stringResource(R.string.saved),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onAddFoodClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = ElectricCobalt
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            GymFlowEmptyStates.NoData(
                message = stringResource(R.string.no_foods_logged),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        } else {
            logs.forEachIndexed { index, log ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = log.foodNameSnapshot,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${log.calories.toInt()} kcal • ${log.protein.toInt()}p ${log.carbs.toInt()}c ${log.fat.toInt()}f",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onDeleteLog(log) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                if (index < logs.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
