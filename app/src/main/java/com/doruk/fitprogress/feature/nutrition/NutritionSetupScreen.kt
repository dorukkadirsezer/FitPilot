package com.doruk.fitprogress.feature.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.ElectricCyan
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionSetupScreen(
    onNavigateUp: () -> Unit,
    viewModel: NutritionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    
    var cals by remember(state.nutritionDay) { mutableStateOf(state.nutritionDay?.targetCalories?.toString() ?: "2500") }
    var pro by remember(state.nutritionDay) { mutableStateOf(state.nutritionDay?.targetProtein?.toString() ?: "150") }
    var carbs by remember(state.nutritionDay) { mutableStateOf(state.nutritionDay?.targetCarbs?.toString() ?: "250") }
    var fat by remember(state.nutritionDay) { mutableStateOf(state.nutritionDay?.targetFat?.toString() ?: "80") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrition Targets", fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Set your daily macro targets. (Updates apply to today)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    GymFlowTextField(
                        value = cals,
                        onValueChange = { cals = it },
                        label = "Target Calories (kcal)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    GymFlowTextField(
                        value = pro,
                        onValueChange = { pro = it },
                        label = "Target Protein (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    GymFlowTextField(
                        value = carbs,
                        onValueChange = { carbs = it },
                        label = "Target Carbs (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    GymFlowTextField(
                        value = fat,
                        onValueChange = { fat = it },
                        label = "Target Fat (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryActionButton(
                onClick = {
                    val calsInt = cals.toIntOrNull() ?: 2500
                    val proInt = pro.toIntOrNull() ?: 150
                    val carbsInt = carbs.toIntOrNull() ?: 250
                    val fatInt = fat.toIntOrNull() ?: 80
                    viewModel.updateTargets(calsInt, proInt, carbsInt, fatInt)
                    onNavigateUp()
                },
                text = "Save Targets",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
