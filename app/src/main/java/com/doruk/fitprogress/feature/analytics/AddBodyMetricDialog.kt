package com.doruk.fitprogress.feature.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.core.database.entity.BodyMetricEntity
import com.doruk.fitprogress.core.ui.components.GymFlowTextField
import com.doruk.fitprogress.core.ui.components.PrimaryActionButton
import com.doruk.fitprogress.theme.ElectricCyan
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AddBodyMetricDialog(
    initialMetric: BodyMetricEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Float?, Float?, Float?, Float?, Float?, Float?, String?) -> Unit,
    onDelete: () -> Unit
) {
    var dateIso by remember { mutableStateOf(initialMetric?.dateIso ?: LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var weightStr by remember { mutableStateOf(initialMetric?.bodyWeight?.toString() ?: "") }
    var waistStr by remember { mutableStateOf(initialMetric?.waist?.toString() ?: "") }
    var chestStr by remember { mutableStateOf(initialMetric?.chest?.toString() ?: "") }
    var armStr by remember { mutableStateOf(initialMetric?.arm?.toString() ?: "") }
    var legStr by remember { mutableStateOf(initialMetric?.leg?.toString() ?: "") }
    var bodyFatStr by remember { mutableStateOf(initialMetric?.bodyFatPercentage?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialMetric?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialMetric == null) "Log Body Metrics" else "Edit Body Metrics", color = ElectricCyan, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GymFlowTextField(
                    value = dateIso,
                    onValueChange = { dateIso = it },
                    label = "Date (YYYY-MM-DD)",
                    modifier = Modifier.fillMaxWidth()
                )
                
                GymFlowTextField(
                    value = weightStr,
                    onValueChange = { weightStr = it },
                    label = "Weight (kg)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                GymFlowTextField(
                    value = waistStr,
                    onValueChange = { waistStr = it },
                    label = "Waist (cm)",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymFlowTextField(
                        value = chestStr,
                        onValueChange = { chestStr = it },
                        label = "Chest (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    
                    GymFlowTextField(
                        value = armStr,
                        onValueChange = { armStr = it },
                        label = "Arm (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymFlowTextField(
                        value = legStr,
                        onValueChange = { legStr = it },
                        label = "Leg (cm)",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )

                    GymFlowTextField(
                        value = bodyFatStr,
                        onValueChange = { bodyFatStr = it },
                        label = "Body Fat %",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                GymFlowTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Notes",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false
                )
            }
        },
        confirmButton = {
            PrimaryActionButton(
                onClick = {
                    onSave(
                        dateIso,
                        weightStr.toFloatOrNull(),
                        waistStr.toFloatOrNull(),
                        chestStr.toFloatOrNull(),
                        armStr.toFloatOrNull(),
                        legStr.toFloatOrNull(),
                        bodyFatStr.toFloatOrNull(),
                        notes.takeIf { it.isNotBlank() }
                    )
                },
                text = "Save",
                modifier = Modifier.fillMaxWidth()
            )
        },
        dismissButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (initialMetric != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                } else {
                    Spacer(Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
