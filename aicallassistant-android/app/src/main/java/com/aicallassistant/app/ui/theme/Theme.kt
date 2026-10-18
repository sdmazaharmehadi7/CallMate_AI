package com.aicallassistant.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CallMateColorScheme = darkColorScheme(
    primary = CallMateCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = CallMateNavyElevated,
    onPrimaryContainer = Color(0xFFE0F7FA),
    secondary = CallMateBlue,
    onSecondary = Color(0xFF002F6C),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = CallMatePurple,
    background = CallMateNavyBg,
    onBackground = Color(0xFFF1F5F9),
    surface = CallMateNavySurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = CallMateNavyElevated,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = CallMateNavyBorder,
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
