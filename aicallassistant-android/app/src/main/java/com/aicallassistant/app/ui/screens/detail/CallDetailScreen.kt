package com.aicallassistant.app.ui.screens.detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.data.audio.LocalRecordingManager
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.TranscriptItem
import com.aicallassistant.app.ui.components.CallerAvatar
import com.aicallassistant.app.ui.components.CallRecordingPlayer
import com.aicallassistant.app.ui.components.DeleteConfirmationDialog
import com.aicallassistant.app.ui.components.UrgencyBadge
import com.aicallassistant.app.ui.theme.EqualDarkBg
import com.aicallassistant.app.ui.theme.EqualDarkBorder
import com.aicallassistant.app.ui.theme.EqualDarkCard
import com.aicallassistant.app.ui.theme.EqualDarkSurface
import com.aicallassistant.app.ui.theme.EqualGreen
import com.aicallassistant.app.ui.theme.EqualGreenText
import com.aicallassistant.app.ui.theme.EqualTextPrimary
import com.aicallassistant.app.ui.theme.EqualTextSecondary
import com.aicallassistant.app.ui.theme.TealPrimary
import com.aicallassistant.app.ui.theme.WhatsAppCheckBlue
import com.aicallassistant.app.ui.theme.WhatsAppIncomingBubble
import com.aicallassistant.app.ui.theme.WhatsAppOutgoingBubble
import com.aicallassistant.app.ui.viewmodel.CallDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    viewModel: CallDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var noteInput by remember { mutableStateOf("") }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Delete Call Record?",
            message = "This call, its transcripts, action items, and stored recording will be permanently deleted.",
            onConfirm = { viewModel.deleteCall(onDeleted = onNavigateBack) },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    val detail = uiState.callDetail

    Scaffold(
        containerColor = EqualDarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        CallerAvatar(
                            callerName = detail?.callerName,
                            callerNumber = detail?.callerNumber ?: "",
                            size = 38.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = detail?.callerName?.takeIf { it.isNotBlank() }
                                    ?: detail?.callerNumber
                                    ?: "Call Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = EqualTextPrimary
                            )
                            Text(
                                text = if (detail != null) "${detail.callerNumber} • Equal Assistant" else "Loading...",
                                fontSize = 11.sp,
                                color = EqualGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = EqualTextPrimary
                        )
                    }
                },
                actions = {
                    if (detail != null) {
                        // Phone Call Back button
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${detail.callerNumber}"))
                            context.startActivity(intent)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call back",
                                tint = TealPrimary
                            )
                        }

                        // Star / Important
                        IconButton(onClick = { viewModel.toggleImportant() }) {
                            Icon(
                                imageVector = if (detail.isImportant) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star call",
                                tint = if (detail.isImportant) Color(0xFFFBBF24) else EqualTextSecondary
                            )
                        }

                        // Delete
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete call",
                                tint = Color(0xFFF87171)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EqualDarkSurface,
                    titleContentColor = EqualTextPrimary
                )
            )
        },
        bottomBar = {
            // WhatsApp-style message/instruction bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = EqualDarkSurface,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        placeholder = {
                            Text(
                                "Type instruction for Equal Assistant...",
                                color = EqualTextSecondary,
                                fontSize = 13.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = EqualTextPrimary,
                            unfocusedTextColor = EqualTextPrimary,
                            focusedBorderColor = TealPrimary,
                            unfocusedBorderColor = EqualDarkBorder,
                            focusedContainerColor = EqualDarkCard,
                            unfocusedContainerColor = EqualDarkCard,
                            cursorColor = TealPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (noteInput.isNotBlank()) TealPrimary else EqualGreen.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                if (noteInput.isNotBlank()) {
                                    // Feedback notification or note saving
                                    noteInput = ""
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send instruction",
                                tint = Color(0xFF0F141C),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadCallDetail() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(EqualDarkBg)
        ) {
            when {
                uiState.isLoading && detail == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = TealPrimary)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Opening WhatsApp chat...", color = EqualTextSecondary, fontSize = 13.sp)
                        }
                    }
                }
                uiState.errorMessage != null && detail == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load chat", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(uiState.errorMessage ?: "", color = EqualTextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadCallDetail() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color(0xFF0F141C))
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                detail != null -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Multi-call session selector if the same caller called multiple times
                        if (uiState.callerCalls.size > 1) {
                            item {
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        text = "Call Sessions with ${detail.callerName ?: detail.callerNumber} (${uiState.callerCalls.size})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EqualGreenText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp)
                                    ) {
                                        items(uiState.callerCalls, key = { it.id }) { sessionCall ->
                                            val isSelected = sessionCall.id == uiState.selectedCallId
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { viewModel.selectSession(sessionCall.id) },
                                                label = {
                                                    Text(
                                                        text = "${DateUtils.formatChatTimestamp(sessionCall.createdAt)} (${DateUtils.formatDuration(sessionCall.durationSeconds)})",
                                                        fontSize = 11.sp,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = TealPrimary,
                                                    selectedLabelColor = Color(0xFF0F141C),
                                                    containerColor = EqualDarkSurface,
                                                    labelColor = EqualTextSecondary
                                                ),
                                                border = FilterChipDefaults.filterChipBorder(
                                                    enabled = true,
                                                    selected = isSelected,
                                                    borderColor = if (isSelected) TealPrimary else EqualDarkBorder
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // WhatsApp Date Divider Pill
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = EqualDarkSurface,
                                    shadowElevation = 2.dp
                                ) {
                                    Text(
                                        text = "${DateUtils.getRelativeDateHeader(detail.createdAt)} • ${DateUtils.formatTimeOnly(detail.createdAt)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = EqualTextSecondary,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // AI Call Summary Card (Pinned in Chat)
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = EqualDarkCard)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.SmartToy,
                                                contentDescription = null,
                                                tint = EqualGreen,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AI Call Summary",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EqualGreenText
                                            )
                                        }

                                        UrgencyBadge(urgency = detail.urgency)
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = detail.summary ?: "No summary recorded for this screened call.",
                                        fontSize = 13.sp,
                                        color = EqualTextPrimary,
                                        lineHeight = 19.sp
                                    )

                                    if (detail.keyFacts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = "Key Facts Captured:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TealPrimary
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        detail.keyFacts.forEach { fact ->
                                            Row(
                                                modifier = Modifier.padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.FiberManualRecord,
                                                    contentDescription = null,
                                                    tint = TealPrimary,
                                                    modifier = Modifier.size(8.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(fact, fontSize = 12.sp, color = EqualTextSecondary)
                                            }
                                        }
                                    }

                                    if (!detail.uncertainties.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF78350F).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                                .padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Uncertainty: ${detail.uncertainties}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFFDE68A)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Call Recording Audio Player (Embedded directly in chat)
                        val localRecFile = LocalRecordingManager.getRecordingFile(detail.id)
                        val hasAnyRecording = localRecFile != null || detail.hasRecording

                        if (hasAnyRecording) {
                            item {
                                if (localRecFile != null) {
                                    CallRecordingPlayer(
                                        localFile = localRecFile,
                                        initialDurationSeconds = detail.durationSeconds,
                                        title = "Call Audio Recording (${DateUtils.formatDuration(detail.durationSeconds)})"
                                    )
                                } else if (uiState.signedRecordingUrl != null) {
                                    CallRecordingPlayer(
                                        localFile = null,
                                        remoteUrl = uiState.signedRecordingUrl,
                                        initialDurationSeconds = detail.durationSeconds,
                                        title = "Cloud Audio Recording (${DateUtils.formatDuration(detail.durationSeconds)})"
                                    )
                                } else {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = EqualDarkCard
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Outlined.Headphones,
                                                    contentDescription = null,
                                                    tint = TealPrimary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(
                                                        text = "Call Audio Recording",
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EqualTextPrimary
                                                    )
                                                    Text(
                                                        text = "${DateUtils.formatDuration(detail.durationSeconds)} • Tap to play",
                                                        fontSize = 11.sp,
                                                        color = EqualTextSecondary
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = { viewModel.requestRecordingPlaybackUrl() },
                                                enabled = !uiState.isLoadingRecording,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = TealPrimary,
                                                    contentColor = Color(0xFF0F141C)
                                                ),
                                                shape = RoundedCornerShape(20.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                                            ) {
                                                if (uiState.isLoadingRecording) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        color = Color(0xFF0F141C)
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Play", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Action Items generated from this call
                        if (detail.actionItems.isNotEmpty()) {
                            item {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text(
                                        text = "Tasks from this call (${detail.actionItems.size}):",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EqualGreenText
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    detail.actionItems.forEach { actionItem ->
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            color = EqualDarkSurface
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { viewModel.toggleActionItem(actionItem.id, actionItem.completed) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (actionItem.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                        contentDescription = "Toggle completion",
                                                        tint = if (actionItem.completed) EqualGreen else EqualTextSecondary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = actionItem.task,
                                                    fontSize = 12.sp,
                                                    color = if (actionItem.completed) EqualTextSecondary else EqualTextPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // WhatsApp Conversation Section Header
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Screened Conversation",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = EqualTextSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Dialog Bubbles (WhatsApp layout)
                        if (detail.transcripts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No live speech turns recorded.",
                                        fontSize = 12.sp,
                                        color = EqualTextSecondary
                                    )
                                }
                            }
                        } else {
                            items(detail.transcripts, key = { it.id }) { transcript ->
                                WhatsAppChatBubble(
                                    transcript = transcript,
                                    callerName = detail.callerName ?: "Caller"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * WhatsApp-style message bubble.
 * - Caller (Incoming): Left aligned, dark neutral bubble, caller name in teal, time stamp on bottom right.
 * - Equal AI Assistant (Outgoing): Right aligned, WhatsApp green/teal bubble, Equal badge, time stamp + double blue checkmarks!
 */
@Composable
fun WhatsAppChatBubble(
    transcript: TranscriptItem,
    callerName: String
) {
    val isAssistant = transcript.speaker == SpeakerType.ASSISTANT
    val alignment = if (isAssistant) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isAssistant) WhatsAppOutgoingBubble else WhatsAppIncomingBubble
    val bubbleShape = if (isAssistant) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        contentAlignment = alignment
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            shadowElevation = 2.dp,
            modifier = Modifier.widthIn(min = 120.dp, max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Sender label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    if (isAssistant) {
                        Text(
                            text = "Equal AI Assistant",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EqualGreenText
                        )
                    } else {
                        Text(
                            text = callerName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }

                // Message Text
                Text(
                    text = transcript.text,
                    fontSize = 13.sp,
                    color = EqualTextPrimary,
                    lineHeight = 18.sp
                )

                // Timestamp & WhatsApp Checkmarks
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val timeStr = DateUtils.formatTimeOnly(transcript.createdAt).ifBlank {
                        val secs = (transcript.timestampMs / 1000).toInt()
                        DateUtils.formatDurationClock(secs)
                    }
                    Text(
                        text = timeStr,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )

                    if (isAssistant) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Read",
                            tint = WhatsAppCheckBlue,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
