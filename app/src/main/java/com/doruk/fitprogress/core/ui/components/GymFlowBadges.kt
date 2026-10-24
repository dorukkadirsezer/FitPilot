package com.doruk.fitprogress.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.CrimsonAlert
import com.doruk.fitprogress.theme.ElectricCobalt
import com.doruk.fitprogress.theme.TelemetryAmber

enum class BadgeType {
    INFO,
    AI,
    SUCCESS,
    WARNING,
    DANGER,
    NEUTRAL
}

/**
 * Standardized %50 Pill Badge (RoundedCornerShape(percent = 50))
 * with subtle hairline outline and tabular numerical readiness.
 */
@Composable
fun StatusPillBadge(
    text: String,
    type: BadgeType = BadgeType.NEUTRAL,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (type) {
        BadgeType.INFO -> Triple(
            ElectricCobalt.copy(alpha = 0.14f),
            ElectricCobalt,
            ElectricCobalt.copy(alpha = 0.35f)
        )
        BadgeType.AI -> Triple(
            ElectricCobalt.copy(alpha = 0.16f),
            ElectricCobalt,
            ElectricCobalt.copy(alpha = 0.40f)
        )
        BadgeType.SUCCESS -> Triple(
            AthleticEmerald.copy(alpha = 0.15f),
            AthleticEmerald,
            AthleticEmerald.copy(alpha = 0.38f)
        )
        BadgeType.WARNING -> Triple(
            TelemetryAmber.copy(alpha = 0.16f),
            TelemetryAmber,
            TelemetryAmber.copy(alpha = 0.40f)
        )
        BadgeType.DANGER -> Triple(
            CrimsonAlert.copy(alpha = 0.15f),
            CrimsonAlert,
            CrimsonAlert.copy(alpha = 0.38f)
        )
        BadgeType.NEUTRAL -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            MaterialTheme.colorScheme.outlineVariant
        )
    }

    val pillShape = RoundedCornerShape(percent = 50)

    Box(
        modifier = modifier
            .clip(pillShape)
            .background(bgColor)
            .border(BorderStroke(1.dp, borderColor), pillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val pillShape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .clip(pillShape)
            .background(color.copy(alpha = 0.14f))
            .border(BorderStroke(1.dp, color.copy(alpha = 0.36f)), pillShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}
