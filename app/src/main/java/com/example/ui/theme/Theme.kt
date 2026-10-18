package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AmoledColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF001F24),
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = ElectricCyan,
    secondary = NeonMagenta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF3B0920),
    onSecondaryContainer = Color(0xFFFFB1C8),
    tertiary = HyperViolet,
    onTertiary = Color.White,
    background = AmoledBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextSecondary,
    outline = ObsidianBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledColorScheme,
        typography = Typography,
        content = content
    )
}
