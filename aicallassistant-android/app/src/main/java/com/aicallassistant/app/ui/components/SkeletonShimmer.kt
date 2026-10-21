package com.aicallassistant.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aicallassistant.app.ui.theme.CallMateBorder
import com.aicallassistant.app.ui.theme.CallMateCard
import com.aicallassistant.app.ui.theme.CallMatePrimary
import com.aicallassistant.app.ui.theme.ShimmerBase
import com.aicallassistant.app.ui.theme.ShimmerHighlight

/**
 * Consistent left-to-right shining shimmer modifier for smooth loading states.
 */
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = ShimmerBase,
    highlightColor: Color = ShimmerHighlight
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -350f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    // True left-to-right metallic shine beam
    val shimmerColors = listOf(
        baseColor,
        baseColor,
        highlightColor,
        highlightColor.copy(alpha = 0.95f),
        baseColor,
        baseColor
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(x = translateAnim.value, y = 0f),
        end = Offset(x = translateAnim.value + 350f, y = 0f)
    )

    this
        .clip(shape)
        .background(brush)
}

@Composable
fun ShimmerBox(
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shimmerEffect(shape)
    )
}

@Composable
fun InstagramSkeletonChatRow(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CallMateCard)
            .border(BorderStroke(1.dp, CallMateBorder), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular Avatar Skeleton
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .shimmerEffect(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShimmerBox(width = 120.dp, height = 15.dp)
                    ShimmerBox(width = 54.dp, height = 12.dp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                ShimmerBox(
                    width = 210.dp,
                    height = 13.dp,
                    shape = RoundedCornerShape(4.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Pill action button skeleton
                ShimmerBox(
                    width = 110.dp,
                    height = 24.dp,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}

@Composable
fun InstagramSkeletonCallRow(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CallMateCard)
            .border(BorderStroke(1.dp, CallMateBorder), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .shimmerEffect(CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShimmerBox(width = 110.dp, height = 15.dp)
                    ShimmerBox(width = 60.dp, height = 12.dp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                ShimmerBox(width = 140.dp, height = 12.dp)

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShimmerBox(width = 70.dp, height = 18.dp, shape = RoundedCornerShape(9.dp))
                    ShimmerBox(width = 50.dp, height = 18.dp, shape = RoundedCornerShape(9.dp))
                }
            }
        }
    }
}

@Composable
fun InstagramSkeletonChatList(
    count: Int = 4,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(count) {
            InstagramSkeletonChatRow()
        }
    }
}

@Composable
fun InstagramSkeletonCallList(
    count: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            InstagramSkeletonCallRow()
        }
    }
}
