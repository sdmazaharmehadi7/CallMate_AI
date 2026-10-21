package com.aicallassistant.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CallMateColorScheme = darkColorScheme(
    primary = CallMatePrimary,
    onPrimary = Color.White,
    primaryContainer = CallMateCardElevated,
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = CallMateBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = CallMatePrimary,
    background = CallMateCanvas,
    onBackground = CallMateInk,
    surface = CallMateSurface,
    onSurface = CallMateInk,
    surfaceVariant = CallMateCard,
    onSurfaceVariant = CallMateSlate,
    outline = CallMateBorder,
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun CallMateTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CallMateColorScheme,
        typography = Typography,
        content = content
    )
}

// Backward-compatible alias for existing references
@Composable
fun AiCallAssistantTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CallMateTheme(content = content)
}
