package com.doruk.fitprogress.core.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.theme.AthleticEmerald
import com.doruk.fitprogress.theme.ElectricCobalt

/**
 * FitPilot Beta Vector Monogram ("The Ascending Vector" — F + ▲).
 * Uses exclusively Electric Cobalt (#3B82F6) and Athletic Emerald (#10B981).
 */
@Composable
fun FitPilotBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    showContainer: Boolean = true
) {
    val containerModifier = if (showContainer) {
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111722))
            .border(1.dp, ElectricCobalt.copy(alpha = 0.32f), RoundedCornerShape(10.dp))
            .padding(4.dp)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height

            // 1. Electric Cobalt (#3B82F6) Forward-Leaning "F" Monogram
            val fPath = Path().apply {
                moveTo(w * 0.14f, h * 0.84f)
                lineTo(w * 0.28f, h * 0.16f)
                lineTo(w * 0.78f, h * 0.16f)
                lineTo(w * 0.71f, h * 0.31f)
                lineTo(w * 0.40f, h * 0.31f)
                lineTo(w * 0.36f, h * 0.48f)
                lineTo(w * 0.65f, h * 0.48f)
                lineTo(w * 0.59f, h * 0.63f)
                lineTo(w * 0.33f, h * 0.63f)
                lineTo(w * 0.28f, h * 0.84f)
                close()
            }
            drawPath(path = fPath, color = ElectricCobalt)

            // 2. Athletic Emerald (#10B981) Ascending Telemetry Vector (▲)
            val apexPath = Path().apply {
                moveTo(w * 0.52f, h * 0.84f)
                lineTo(w * 0.76f, h * 0.38f)
                lineTo(w * 0.90f, h * 0.64f)
                lineTo(w * 0.79f, h * 0.64f)
                lineTo(w * 0.75f, h * 0.55f)
                lineTo(w * 0.63f, h * 0.84f)
                close()
            }
            drawPath(path = apexPath, color = AthleticEmerald)
        }
    }
}

@Composable
fun FitPilotBrandLockup(
    modifier: Modifier = Modifier,
    title: String = "FitPilot",
    badgeText: String = "BETA"
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FitPilotBrandLogo(size = 34.dp, showContainer = true)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(percent = 50))
                .background(ElectricCobalt.copy(alpha = 0.14f))
                .border(1.dp, ElectricCobalt.copy(alpha = 0.35f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ElectricCobalt
            )
        }
    }
}
