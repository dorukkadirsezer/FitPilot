package com.doruk.fitprogress.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt

/**
 * FitPilot Zero-Shadow Standard Surface Card:
 * - 0.dp shadow elevation
 * - 16.dp rounded corners
 * - 1.dp Slate Hairline border (outlineVariant)
 */
@Composable
fun StandardSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    StandardSurfaceCard(
        modifier = modifier,
        onClick = onClick
    ) {
        if (title.isNotBlank()) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        content()
    }
}

@Composable
fun GradientHeaderCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    actionContent: (@Composable () -> Unit)? = null
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            ElectricCobalt.copy(alpha = 0.14f),
            AthleticEmerald.copy(alpha = 0.08f)
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, ElectricCobalt.copy(alpha = 0.30f))
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (subtitle != null) {
                        Text(
                            text = subtitle.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCobalt,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (actionContent != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    actionContent()
                }
            }
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = ElectricCobalt,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    StandardSurfaceCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
