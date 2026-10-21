package com.aicallassistant.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.ui.theme.EqualGreen
import com.aicallassistant.app.ui.theme.TealPrimary

/**
 * Illustrated user profile avatar matching the provided reference screenshot
 * (Olive/Lime circular backdrop, dark wavy hair, brown skin tone, neat mustache, teal shirt).
 */
@Composable
fun UserProfileAvatar(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFB5E853), Color(0xFF83C324))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Head / Face (warm skin tone)
            val faceRadius = w * 0.28f
            val faceCenter = Offset(w * 0.5f, h * 0.44f)
            drawCircle(
                color = Color(0xFFC67D5A),
                radius = faceRadius,
                center = faceCenter
            )

            // Ears
            drawCircle(
                color = Color(0xFFB46E4C),
                radius = w * 0.08f,
                center = Offset(w * 0.22f, h * 0.46f)
            )
            drawCircle(
                color = Color(0xFFB46E4C),
                radius = w * 0.08f,
                center = Offset(w * 0.78f, h * 0.46f)
            )

            // Hair (dark wavy hair on top)
            drawCircle(
                color = Color(0xFF3E2312),
                radius = w * 0.25f,
                center = Offset(w * 0.5f, h * 0.24f)
            )
            drawCircle(
                color = Color(0xFF3E2312),
                radius = w * 0.17f,
                center = Offset(w * 0.32f, h * 0.28f)
            )
            drawCircle(
                color = Color(0xFF3E2312),
                radius = w * 0.17f,
                center = Offset(w * 0.68f, h * 0.28f)
            )

            // Eyes
            drawCircle(
                color = Color(0xFF1E130B),
                radius = w * 0.035f,
                center = Offset(w * 0.42f, h * 0.42f)
            )
            drawCircle(
                color = Color(0xFF1E130B),
                radius = w * 0.035f,
                center = Offset(w * 0.58f, h * 0.42f)
            )

            // Mustache
            drawRoundRect(
                color = Color(0xFF2C180C),
                topLeft = Offset(w * 0.36f, h * 0.52f),
                size = Size(w * 0.28f, h * 0.075f),
                cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
            )

            // Shirt (Teal collar/shirt)
            drawRoundRect(
                color = Color(0xFF0D5C58),
                topLeft = Offset(w * 0.2f, h * 0.74f),
                size = Size(w * 0.6f, h * 0.35f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
            )
        }
    }
}

/**
 * Caller Avatar matching the reference screenshot:
 * - Equal AI Assistant: Vibrant Lime-green with dots / robot icon
 * - Phone contacts: Warm retro red phone icon with rounded soft backdrop
 */
@Composable
fun CallerAvatar(
    callerName: String?,
    callerNumber: String,
    modifier: Modifier = Modifier,
    isAssistant: Boolean = false,
    size: Dp = 46.dp
) {
    val isAiAssistant = isAssistant ||
            (callerName?.contains("equal", ignoreCase = true) == true) ||
            (callerName?.contains("assistant", ignoreCase = true) == true)

    if (isAiAssistant) {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(EqualGreen),
            contentAlignment = Alignment.Center
        ) {
            // Three dots in 2 rows like Equal logo
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Equal Assistant",
                    tint = Color(0xFF0F141C),
                    modifier = Modifier.size(size * 0.58f)
                )
            }
        }
    } else {
        // Vintage Red Phone styling as seen in Screenshot 3
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFFFECE5), Color(0xFFFFD5CC))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(size * 0.75f)) {
                val w = this.size.width
                val h = this.size.height

                // Retro red phone base
                drawRoundRect(
                    color = Color(0xFFDC2626),
                    topLeft = Offset(w * 0.15f, h * 0.40f),
                    size = Size(w * 0.70f, h * 0.48f),
                    cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
                )
                // Dial ring
                drawCircle(
                    color = Color(0xFFFEE2E2),
                    radius = w * 0.17f,
                    center = Offset(w * 0.5f, h * 0.62f)
                )
                drawCircle(
                    color = Color(0xFFB91C1C),
                    radius = w * 0.07f,
                    center = Offset(w * 0.5f, h * 0.62f)
                )
                // Receiver on top
                drawRoundRect(
                    color = Color(0xFFB91C1C),
                    topLeft = Offset(w * 0.08f, h * 0.18f),
                    size = Size(w * 0.84f, h * 0.22f),
                    cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
                )
            }
        }
    }
}
