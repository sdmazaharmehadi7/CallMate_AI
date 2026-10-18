package com.aicallassistant.app.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.R
import com.aicallassistant.app.data.remote.SupabaseManager
import com.aicallassistant.app.ui.theme.CallMateBackgroundGradient
import com.aicallassistant.app.ui.theme.CallMateNavyBg
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToWelcome: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.92f) }

    LaunchedEffect(Unit) {
        // Smooth fade-in and scale animation
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
        )

        // Hold briefly so user enjoys the premium splash branding
        delay(600)

        // Route to Main if authenticated, else Welcome
        if (SupabaseManager.getAccessToken() != null) {
            onNavigateToMain()
        } else {
            onNavigateToWelcome()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallMateNavyBg)
            .background(CallMateBackgroundGradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(32.dp)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // Centered CallMate Logo with Aspect Fit
            Image(
                painter = painterResource(id = R.drawable.callmate_logo),
                contentDescription = "CallMate Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(32.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "CallMate",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Let AI take the call.",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF94A3B8),
                letterSpacing = 0.5.sp
            )
        }
    }
}
