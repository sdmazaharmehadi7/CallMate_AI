package com.aicallassistant.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.domain.model.UrgencyLevel
import com.aicallassistant.app.ui.theme.UrgencyCriticalBg
import com.aicallassistant.app.ui.theme.UrgencyCriticalText
import com.aicallassistant.app.ui.theme.UrgencyHighBg
import com.aicallassistant.app.ui.theme.UrgencyHighText
import com.aicallassistant.app.ui.theme.UrgencyLowBg
import com.aicallassistant.app.ui.theme.UrgencyLowText
import com.aicallassistant.app.ui.theme.UrgencyMediumBg
import com.aicallassistant.app.ui.theme.UrgencyMediumText

@Composable
fun UrgencyBadge(
    urgency: UrgencyLevel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (urgency) {
        UrgencyLevel.LOW -> Pair(UrgencyLowBg, UrgencyLowText)
        UrgencyLevel.MEDIUM -> Pair(UrgencyMediumBg, UrgencyMediumText)
        UrgencyLevel.HIGH -> Pair(UrgencyHighBg, UrgencyHighText)
        UrgencyLevel.CRITICAL -> Pair(UrgencyCriticalBg, UrgencyCriticalText)
    }

    Text(
        text = urgency.value.uppercase(),
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
