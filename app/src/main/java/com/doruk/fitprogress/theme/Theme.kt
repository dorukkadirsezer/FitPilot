package com.doruk.fitprogress.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    secondary = VividViolet,
    tertiary = NeonGreen,
    background = DeepNavy,
    surface = DarkCharcoal,
    surfaceVariant = DarkCharcoal, // Fallback for some components
    error = CoralRed,
    onPrimary = DeepNavy, // Text on primary button should be dark for high contrast
    onSecondary = TextPrimaryDark,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricCyanLight,
    secondary = VividVioletLight,
    tertiary = NeonGreen,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurface,
    error = CoralRed,
    onPrimary = DeepNavy,
    onSecondary = TextPrimaryLight,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    onError = TextPrimaryLight
)

@Composable
fun GymFlowAITheme(
    themeMode: com.doruk.fitprogress.core.domain.model.ThemeMode = com.doruk.fitprogress.core.domain.model.ThemeMode.SYSTEM_DEFAULT,
    // Disable dynamic color to maintain the strict branding colors (Cyan/Violet)
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        com.doruk.fitprogress.core.domain.model.ThemeMode.LIGHT -> false
        com.doruk.fitprogress.core.domain.model.ThemeMode.DARK -> true
        com.doruk.fitprogress.core.domain.model.ThemeMode.SYSTEM_DEFAULT -> isSystemDark
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes, // Reference to custom Shapes
        content = content
    )
}
