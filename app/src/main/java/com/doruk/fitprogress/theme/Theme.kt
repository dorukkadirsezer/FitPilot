package com.doruk.fitprogress.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.doruk.fitprogress.core.domain.model.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCobalt,
    onPrimary = Color.White,
    primaryContainer = CobaltContainerDark,
    onPrimaryContainer = OnCobaltContainerDark,

    secondary = AthleticEmerald,
    onSecondary = Color(0xFF022C22),
    secondaryContainer = EmeraldContainerDark,
    onSecondaryContainer = OnEmeraldContainerDark,

    tertiary = TelemetryAmber,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,

    error = CrimsonAlert,
    onError = Color.White,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,

    background = DeepObsidianBg,
    onBackground = TextPrimaryDark,

    surface = DarkSlateSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = InteractiveSlateFill,
    onSurfaceVariant = TextSecondaryDark,

    surfaceContainerLowest = DeepObsidianBg,
    surfaceContainerLow = DarkSlateSurface,
    surfaceContainer = ElevatedSlateCard,
    surfaceContainerHigh = InteractiveSlateFill,
    surfaceContainerHighest = SlateOutlineDark,

    outline = SlateOutlineDark,
    outlineVariant = SlateHairlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = AthleticCobalt,
    onPrimary = Color.White,
    primaryContainer = CobaltContainerLight,
    onPrimaryContainer = OnCobaltContainerLight,

    secondary = DeepEmerald,
    onSecondary = Color.White,
    secondaryContainer = EmeraldContainerLight,
    onSecondaryContainer = OnEmeraldContainerLight,

    tertiary = RichAmber,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,

    error = DeepCrimsonAlert,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,

    background = CoolAlabasterBg,
    onBackground = TextPrimaryLight,

    surface = PureWhiteSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSlateFill,
    onSurfaceVariant = TextSecondaryLight,

    surfaceContainerLowest = PureWhiteSurface,
    surfaceContainerLow = CoolAlabasterBg,
    surfaceContainer = SoftSlateCard,
    surfaceContainerHigh = LightSlateFill,
    surfaceContainerHighest = SlateOutlineLight,

    outline = SlateOutlineLight,
    outlineVariant = SlateHairlineLight
)

@Composable
fun GymFlowAITheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM_DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
                window.isStatusBarContrastEnforced = false
            }
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
