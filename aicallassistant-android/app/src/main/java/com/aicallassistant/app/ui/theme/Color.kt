package com.aicallassistant.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// CallMate Brand Colors (Matching User-Provided Logo)
val CallMateNavyBg = Color(0xFF080B27)
val CallMateNavySurface = Color(0xFF0D1126)
val CallMateNavyElevated = Color(0xFF141A36)
val CallMateNavyBorder = Color(0xFF1F274E)

val CallMateCyan = Color(0xFF00E5FF)
val CallMateBlue = Color(0xFF4FACFE)
val CallMatePurple = Color(0xFF6C5CE7)
val CallMateTeal = Color(0xFF00BCD4)

// Gradient Brushes
val CallMatePrimaryGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF4FACFE), Color(0xFF00E5FF))
)

val CallMatePurpleCyanGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF6C5CE7), Color(0xFF00E5FF))
)

val CallMateBackgroundGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF080B27), Color(0xFF050719))
)

// Material 3 Palette Mappings
val PrimaryCyan = Color(0xFF00E5FF)
val PrimaryCyanDark = Color(0xFF4FACFE)
val PrimaryContainerDark = Color(0xFF141A36)
val OnPrimaryContainerDark = Color(0xFFE0F7FA)

val BackgroundDark = Color(0xFF080B27)
val SurfaceDark = Color(0xFF0D1126)
val OnSurfaceDark = Color(0xFFF1F5F9)
val SurfaceVariantDark = Color(0xFF141A36)
val OnSurfaceVariantDark = Color(0xFF94A3B8)

// Urgency badge colors
val UrgencyLowBg = Color(0xFF064E3B)
val UrgencyLowText = Color(0xFF34D399)

val UrgencyMediumBg = Color(0xFF451A03)
val UrgencyMediumText = Color(0xFFFBBF24)

val UrgencyHighBg = Color(0xFF7C2D12)
val UrgencyHighText = Color(0xFFFB923C)

val UrgencyCriticalBg = Color(0xFF7F1D1D)
val UrgencyCriticalText = Color(0xFFF87171)
