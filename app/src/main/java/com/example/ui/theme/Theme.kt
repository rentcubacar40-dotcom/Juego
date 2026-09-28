package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TacticalColorScheme = darkColorScheme(
    primary = TacticalAmber,
    onPrimary = TacticalDark,
    primaryContainer = TacticalAmberDark,
    onPrimaryContainer = Color.White,
    secondary = TacticalCyan,
    onSecondary = TacticalDark,
    secondaryContainer = TacticalSurfaceVariant,
    onSecondaryContainer = TacticalCyan,
    tertiary = TacticalGreen,
    onTertiary = TacticalDark,
    background = TacticalDark,
    onBackground = TacticalTextPrimary,
    surface = TacticalSurface,
    onSurface = TacticalTextPrimary,
    surfaceVariant = TacticalSurfaceVariant,
    onSurfaceVariant = TacticalTextSecondary,
    outline = TacticalBorder,
    error = TacticalRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TacticalColorScheme,
        typography = Typography,
        content = content
    )
}
