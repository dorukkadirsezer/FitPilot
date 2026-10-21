package com.doruk.fitprogress.feature.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.GradientHeaderCard
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.NeonGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onNavigateUp: () -> Unit,
    onNavigateToSavedFoods: (String, String) -> Unit = { _, _ -> },
    viewModel: AddFoodViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.foodItemIdToEdit != null) stringResource(com.doruk.fitprogress.R.string.saved) else stringResource(com.doruk.fitprogress.R.string.add_food_log_food), fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text(stringResource(com.doruk.fitprogress.R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.dateIso.isNotBlank()) {
                val formattedDate = com.doruk.fitprogress.core.util.FormatUtils.formatDateIso(state.dateIso, java.util.Locale.getDefault())
                GradientHeaderCard(title = com.doruk.fitprogress.core.util.LocalizationUtils.getMealTypeName(state.mealType), subtitle = formattedDate)
                
                // Saved Foods entry point
                OutlinedButton(
                    onClick = { onNavigateToSavedFoods(state.dateIso, state.mealType) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(com.doruk.fitprogress.R.string.add_food_choose_saved), color = ElectricCyan, fontWeight = FontWeight.Bold)
                }
            } else {
                GradientHeaderCard(title = stringResource(com.doruk.fitprogress.R.string.saved), subtitle = "EDIT ITEM")
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GymFlowTextField(
                        value = state.name,
                        onValueChange = viewModel::onNameChange,
                        label = stringResource(com.doruk.fitprogress.R.string.add_food_food_name),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GymFlowTextField(
                            value = state.servingSize,
                            onValueChange = viewModel::onServingSizeChange,
                            label = stringResource(com.doruk.fitprogress.R.string.add_food_serving_size),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymFlowTextField(
                            value = state.servingUnit,
                            onValueChange = viewModel::onServingUnitChange,
                            label = stringResource(com.doruk.fitprogress.R.string.add_food_unit),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    GymFlowTextField(
                        value = state.calories,
                        onValueChange = viewModel::onCaloriesChange,
                        label = stringResource(com.doruk.fitprogress.R.string.add_food_calories),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GymFlowTextField(
                            value = state.protein,
                            onValueChange = viewModel::onProteinChange,
                            label = stringResource(com.doruk.fitprogress.R.string.add_food_protein),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymFlowTextField(
                            value = state.carbs,
                            onValueChange = viewModel::onCarbsChange,
                            label = stringResource(com.doruk.fitprogress.R.string.add_food_carbs),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymFlowTextField(
                            value = state.fat,
                            onValueChange = viewModel::onFatChange,
                            label = stringResource(com.doruk.fitprogress.R.string.add_food_fat),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (state.dateIso.isNotBlank() && state.foodItemIdToEdit == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(), 
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(com.doruk.fitprogress.R.string.add_food_save_future), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = state.saveForFuture, 
                            onCheckedChange = viewModel::onSaveForFutureChange,
                            colors = SwitchDefaults.colors(checkedThumbColor = ElectricCyan, checkedTrackColor = ElectricCyan.copy(alpha = 0.5f))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            PrimaryActionButton(
                onClick = { viewModel.saveFoodAndLog() },
                modifier = Modifier.fillMaxWidth(),
                enabled = state.name.isNotBlank() && state.calories.isNotBlank(),
                text = if (state.dateIso.isNotBlank()) stringResource(com.doruk.fitprogress.R.string.add_food_log_food) else stringResource(com.doruk.fitprogress.R.string.saved)
            )
        }
    }
}
