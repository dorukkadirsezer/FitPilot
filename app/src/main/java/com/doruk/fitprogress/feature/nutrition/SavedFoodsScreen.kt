package com.doruk.fitprogress.feature.nutrition

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.database.entity.FoodItemEntity
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.ElectricCyan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFoodsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToEditFood: (String) -> Unit,
    viewModel: SavedFoodsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedFood by remember { mutableStateOf<FoodItemEntity?>(null) }
    var multiplierText by remember { mutableStateOf("1.0") }

    LaunchedEffect(state.isLogged) {
        if (state.isLogged) {
            onNavigateUp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Foods", fontWeight = FontWeight.Bold, color = ElectricCyan) },
                navigationIcon = {
                    TextButton(onClick = onNavigateUp) {
                        Text("Back", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            if (state.savedFoods.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GymFlowEmptyStates.NoData(message = "No saved foods yet.")
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(state.savedFoods) { food ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedFood = food },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(food.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${food.calories.toInt()} kcal per ${food.servingSize} ${food.servingUnit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row {
                                        IconButton(onClick = { onNavigateToEditFood(food.id) }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = ElectricCyan)
                                        }
                                        IconButton(onClick = { viewModel.deleteSavedFood(food) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedFood?.let { food ->
        AlertDialog(
            onDismissRequest = { selectedFood = null },
            title = { Text("Log ${food.name}", color = ElectricCyan) },
            text = {
                Column {
                    Text("Base serving: ${food.servingSize} ${food.servingUnit}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    GymFlowTextField(
                        value = multiplierText,
                        onValueChange = { multiplierText = it },
                        label = "Serving Multiplier",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            },
            confirmButton = {
                PrimaryActionButton(
                    onClick = {
                        val multiplier = multiplierText.toFloatOrNull() ?: 1f
                        viewModel.logSavedFood(food, multiplier)
                        selectedFood = null
                    },
                    text = "Log Food"
                )
            },
            dismissButton = {
                TextButton(onClick = { selectedFood = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}
