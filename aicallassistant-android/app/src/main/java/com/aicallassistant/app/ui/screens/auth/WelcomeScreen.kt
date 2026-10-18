package com.aicallassistant.app.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.R
import com.aicallassistant.app.ui.theme.CallMateBackgroundGradient
import com.aicallassistant.app.ui.theme.CallMateCyan
import com.aicallassistant.app.ui.theme.CallMateNavyBg
import com.aicallassistant.app.ui.theme.CallMateNavyElevated
import com.aicallassistant.app.ui.theme.CallMateNavySurface

@Composable
fun WelcomeScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(CallMateNavyBg)
            .background(CallMateBackgroundGradient),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Hero Brand Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // CallMate Logo
                Image(
                    painter = painterResource(id = R.drawable.callmate_logo),
                    contentDescription = "CallMate Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "CallMate",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Let AI take the call.",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CallMateCyan,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your intelligent voice assistant that screens callers, transcribes in real-time, and organizes your action items.",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF94A3B8),
                    lineHeight = 18.sp
                )
            }

            // Feature Highlights in Modern Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FeatureCard(
                    icon = Icons.Outlined.RecordVoiceOver,
                    title = "Real-Time AI Voice Screening",
                    description = "Screens callers live using Gemini Live multimodal speech."
                )
                FeatureCard(
                    icon = Icons.Outlined.CheckCircle,
                    title = "Instant Actionable Summaries",
                    description = "Extracts urgency, key facts, and follow-ups automatically."
                )
                FeatureCard(
                    icon = Icons.Outlined.Security,
                    title = "Private & User-Isolated",
                    description = "Your transcripts and voice data stay strictly in your account."
                )
            }

            // Auth Action Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CallMateCyan,
                        contentColor = Color(0xFF00363D)
                    )
                ) {
                    Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CallMateNavySurface
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CallMateNavyElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CallMateCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )
            }
        }
    }
}
