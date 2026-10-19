package com.doruk.fitprogress.feature.nutrition.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.doruk.fitprogress.core.ui.components.AiActionButton
import com.doruk.fitprogress.core.ui.components.GymFlowEmptyStates
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.core.ui.components.StatusBadge
import com.doruk.fitprogress.theme.ElectricCyan
import com.doruk.fitprogress.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFoodReviewScreen(
    onNavigateBack: () -> Unit,
    onSaveComplete: () -> Unit,
    viewModel: AiFoodReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review AI Estimates", fontWeight = FontWeight.Bold, color = VividViolet) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    if (!state.isParsing && state.error == null && state.items.any { it.isSelected }) {
                        AiActionButton(
                            onClick = { viewModel.saveSelectedItems(onSaveComplete) },
                            text = "Save Selected",
                            modifier = Modifier.padding(end = 8.dp).height(40.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isParsing) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = VividViolet)
                    Spacer(Modifier.height(16.dp))
                    Text("Parsing food with Gemini...", color = VividViolet, fontWeight = FontWeight.Bold)
                }
            } else if (state.error != null) {
                GymFlowEmptyStates.Error(
                    message = state.error ?: "AI Parsing Failed",
                    onRetry = onNavigateBack // Treat as "return"
                )
            } else if (state.items.isEmpty()) {
                GymFlowEmptyStates.NoData(
                    message = "No items parsed.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.items) { item ->
                        AiFoodReviewCard(
                            item = item,
                            onToggleSelection = { viewModel.toggleSelection(item.id) },
                            onUpdate = { updated -> viewModel.updateItem(item.id) { updated } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AiFoodReviewCard(
    item: ReviewableFoodItem,
    onToggleSelection: () -> Unit,
    onUpdate: (ReviewableFoodItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected) 4.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().clickable { onToggleSelection() }.padding(bottom = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = item.isSelected, 
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = VividViolet)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (item.isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (item.isSelected) {
                    StatusBadge(text = "Selected", color = VividViolet)
                }
            }
            
            if (item.isSelected) {
                Spacer(Modifier.height(8.dp))
                
                GymFlowTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = "Name",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                GymFlowTextField(
                    value = item.servingDescription,
                    onValueChange = { onUpdate(item.copy(servingDescription = it)) },
                    label = "Serving Description",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymFlowTextField(
                        value = item.calories.toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(item.copy(calories = v)) } },
                        label = "Cals",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    GymFlowTextField(
                        value = item.protein.toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(item.copy(protein = v)) } },
                        label = "Pro (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymFlowTextField(
                        value = item.carbs.toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(item.copy(carbs = v)) } },
                        label = "Carbs (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    GymFlowTextField(
                        value = item.fat.toString(),
                        onValueChange = { it.toFloatOrNull()?.let { v -> onUpdate(item.copy(fat = v)) } },
                        label = "Fat (g)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(12.dp))
                // Meal type selection using standard Box/DropdownMenu to avoid ExposedDropdownMenuBox deprecation
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Meal Type: ${item.mealType}", color = MaterialTheme.colorScheme.onSurface)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val types = listOf("BREAKFAST", "LUNCH", "DINNER", "SNACK", "PRE_WORKOUT", "POST_WORKOUT")
                        types.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onUpdate(item.copy(mealType = type))
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
