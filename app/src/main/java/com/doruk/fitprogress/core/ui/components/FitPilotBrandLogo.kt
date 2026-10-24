package com.doruk.fitprogress.core.ui.components

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doruk.fitprogress.R
import com.doruk.fitprogress.theme.ElectricCobalt

/**
 * FitPilot Beta Vector Monogram ("The Ascending Vector" — F + ▲).
 * Uses exclusively Electric Cobalt (#3B82F6) and Athletic Emerald (#10B981)
 * backed by `R.drawable.ic_fitpilot_logo`.
 */
@Composable
fun FitPilotBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    showContainer: Boolean = true
) {
    val containerModifier = if (showContainer) {
        modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111722))
            .border(1.dp, ElectricCobalt.copy(alpha = 0.40f), RoundedCornerShape(10.dp))
            .padding(4.dp)
    } else {
        modifier.size(size)
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_fitpilot_logo),
            contentDescription = "FitPilot Brand Logo",
            modifier = Modifier.matchParentSize()
        )
    }
}

@Composable
fun FitPilotBrandLockup(
    modifier: Modifier = Modifier,
    title: String = "FitPilot",
    badgeText: String = "BETA",
    showLogoIcon: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showLogoIcon) {
            FitPilotBrandLogo(size = 36.dp, showContainer = true)
        }
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
