package com.doruk.fitprogress.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.database.entity.MealLogEntity
import com.doruk.fitprogress.core.ui.components.*
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen
import com.doruk.fitprogress.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionDashboardScreen(
    onNavigateUp: () -> Unit,
    onNavigateToAddFood: (String, String) -> Unit,
    onNavigateToSavedFoods: (String, String) -> Unit,
    onNavigateToAiReview: (String, String) -> Unit,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.doruk.fitprogress.R.string.nutrition), fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(stringResource(com.doruk.fitprogress.R.string.back), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    GymFlowTextButton(text = stringResource(com.doruk.fitprogress.R.string.copy_yesterday), onClick = { viewModel.requestCopyYesterdayMeals() })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (state.isAiEnabled) {
                FloatingActionButton(
                    onClick = { viewModel.toggleAiDialog(true) },
                    containerColor = VividViolet,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "AI Food Log")
                }
            }
        }
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ElectricCyan)
            }
            return@Scaffold
        }

        if (state.showCopyConfirmation) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissCopyConfirmation() },
                title = { Text(stringResource(com.doruk.fitprogress.R.string.copy_yesterdays_meals), color = ElectricCyan) },
                text = { Text(stringResource(com.doruk.fitprogress.R.string.copy_yesterday_confirmation), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmCopyYesterdayMeals() }) {
                        Text(stringResource(com.doruk.fitprogress.R.string.copy), color = ElectricCyan)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissCopyConfirmation() }) {
                        Text(stringResource(com.doruk.fitprogress.R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Picker Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { viewModel.changeDate(-1) }) {
                        Text(stringResource(com.doruk.fitprogress.R.string.previous), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(state.currentDateIso, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { viewModel.changeDate(1) }) {
                        Text(stringResource(com.doruk.fitprogress.R.string.next), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Macros Summary
            item {
                val targets = state.nutritionDay
                if (targets != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(stringResource(com.doruk.fitprogress.R.string.daily_macros), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            MacroProgressBar(stringResource(com.doruk.fitprogress.R.string.calories), state.totalCalories, targets.targetCalories.toFloat(), "kcal", state.remainingCalories, ElectricCyan)
                            MacroProgressBar(stringResource(com.doruk.fitprogress.R.string.protein), state.totalProtein, targets.targetProtein.toFloat(), "g", state.remainingProtein, NeonGreen)
                            MacroProgressBar(stringResource(com.doruk.fitprogress.R.string.carbs), state.totalCarbs, targets.targetCarbs.toFloat(), "g", state.remainingCarbs, VividViolet)
                            MacroProgressBar(stringResource(com.doruk.fitprogress.R.string.fat), state.totalFat, targets.targetFat.toFloat(), "g", state.remainingFat, Color(0xFFFFC107))
                        }
                    }
                }
            }

            // Meal Sections
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
            
            item { Spacer(modifier = Modifier.height(80.dp)) } // FAB padding
        }
    }

    if (state.showAiDialog) {
        var aiInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.toggleAiDialog(false) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = VividViolet, modifier = Modifier.padding(end = 8.dp))
                    Text(stringResource(com.doruk.fitprogress.R.string.ai_food_logger), color = VividViolet, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    GymFlowTextField(
                        value = aiInput,
                        onValueChange = { aiInput = it },
                        label = stringResource(com.doruk.fitprogress.R.string.what_did_you_eat),
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(com.doruk.fitprogress.R.string.ai_food_logger_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                AiActionButton(onClick = {
                    viewModel.toggleAiDialog(false)
                    if (aiInput.isNotBlank() && state.nutritionDay != null) {
                        onNavigateToAiReview(aiInput, state.nutritionDay!!.id)
                    }
                }, text = stringResource(com.doruk.fitprogress.R.string.parse))
            },
            dismissButton = {
                TextButton(onClick = { viewModel.toggleAiDialog(false) }) {
                    Text(stringResource(com.doruk.fitprogress.R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun MacroProgressBar(label: String, actual: Float, target: Float, unit: String, remaining: Float, color: Color) {
    val progress = if (target > 0) (actual / target).coerceIn(0f, 1f) else 0f
    val isOver = remaining < 0
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${actual.toInt()} / ${target.toInt()} $unit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { progress }, 
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (isOver) MaterialTheme.colorScheme.error else color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        val remainingText = if (isOver) "${kotlin.math.abs(remaining).toInt()} $unit  ${stringResource(com.doruk.fitprogress.R.string.over)}" else "${remaining.toInt()} $unit  ${stringResource(com.doruk.fitprogress.R.string.left)}"
        val remainingColor = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        Text(
            text = remainingText,
            style = MaterialTheme.typography.labelSmall,
            color = remainingColor,
            modifier = Modifier.align(Alignment.End)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(com.doruk.fitprogress.core.util.LocalizationUtils.getMealTypeName(mealType), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                Row {
                    GymFlowTextButton(onClick = onSavedFoodClick, text = stringResource(com.doruk.fitprogress.R.string.saved), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onAddFoodClick,
                        modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = ElectricCyan)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (logs.isEmpty()) {
                GymFlowEmptyStates.NoData(message = stringResource(com.doruk.fitprogress.R.string.no_foods_logged), modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
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
                            Text(log.foodNameSnapshot, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("${log.calories.toInt()} kcal • ${log.protein.toInt()}p ${log.carbs.toInt()}c ${log.fat.toInt()}f", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteLog(log) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (index < logs.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

