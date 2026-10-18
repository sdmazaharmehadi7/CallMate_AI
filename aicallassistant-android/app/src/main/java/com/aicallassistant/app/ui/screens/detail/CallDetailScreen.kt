package com.aicallassistant.app.ui.screens.detail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.TranscriptItem
import com.aicallassistant.app.ui.components.DeleteConfirmationDialog
import com.aicallassistant.app.ui.components.UrgencyBadge
import com.aicallassistant.app.ui.theme.CallMateBackgroundGradient
import com.aicallassistant.app.ui.theme.CallMateCyan
import com.aicallassistant.app.ui.theme.CallMateNavyBg
import com.aicallassistant.app.ui.theme.CallMateNavyElevated
import com.aicallassistant.app.ui.theme.CallMateNavySurface
import com.aicallassistant.app.ui.viewmodel.CallDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    viewModel: CallDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        DeleteConfirmationDialog(
            title = "Delete Call Record?",
            message = "This call, its transcripts, action items, and stored recording will be permanently deleted.",
            onConfirm = { viewModel.deleteCall(onDeleted = onNavigateBack) },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        containerColor = CallMateNavyBg,
        topBar = {
            TopAppBar(
                title = { Text("Call Details", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    uiState.callDetail?.let { detail ->
                        IconButton(onClick = { viewModel.loadCallDetail() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh call details",
                                tint = CallMateCyan
                            )
                        }
                        IconButton(onClick = { viewModel.toggleImportant() }) {
                            Icon(
                                imageVector = if (detail.isImportant) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star call",
                                tint = if (detail.isImportant) Color(0xFFFBBF24) else Color(0xFF94A3B8)
                            )
                        }
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
                    containerColor = CallMateNavyBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadCallDetail() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateBackgroundGradient)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = CallMateCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Loading call details...", color = Color(0xFF94A3B8), fontSize = 13.sp)
                        }
                    }
                }
                uiState.errorMessage != null && uiState.callDetail == null -> {
                    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed to load details", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(uiState.errorMessage ?: "", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadCallDetail() },
                                colors = ButtonDefaults.buttonColors(containerColor = CallMateCyan, contentColor = CallMateNavyBg)
                            ) {
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                uiState.callDetail != null -> {
                    val detail = uiState.callDetail!!
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Info Card
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = CallMateNavySurface
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = detail.callerName ?: "Unknown Caller",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = detail.callerNumber,
                                                fontSize = 13.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }

                                        UrgencyBadge(urgency = detail.urgency)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = Color(0xFF1E293B))
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                        Text(DateUtils.formatIsoDate(detail.createdAt), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                        Text(DateUtils.formatDuration(detail.durationSeconds), fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Status", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                        Text(detail.status.value.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CallMateCyan)
                                    }
                                }
                            }
                        }

                        // AI Summary Section
                        item {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = CallMateNavySurface
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "AI Call Summary",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CallMateCyan
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = detail.summary ?: "No summary recorded for this call.",
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        lineHeight = 20.sp
                                    )

                                    if (detail.callerReason != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Caller's Intent: ${detail.callerReason}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFE2E8F0)
                                        )
                                    }

                                    if (detail.urgencyExplanation != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Urgency Assessment: ${detail.urgencyExplanation}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    // Key Facts
                                    if (detail.keyFacts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "Key Facts Extracted:",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        detail.keyFacts.forEach { fact ->
                                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                                Icon(
                                                    Icons.Outlined.FiberManualRecord,
                                                    contentDescription = null,
                                                    tint = CallMateCyan,
                                                    modifier = Modifier.size(10.dp).padding(top = 4.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(fact, fontSize = 13.sp, color = Color(0xFFCBD5E1))
                                            }
                                        }
                                    }

                                    // Uncertainties
                                    if (!detail.uncertainties.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(Color(0xFF78350F).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Uncertainty: ${detail.uncertainties}",
                                                fontSize = 12.sp,
                                                color = Color(0xFFFDE68A)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recording Audio Player Card
                        if (detail.hasRecording) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = CallMateNavySurface
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Headphones, contentDescription = null, tint = CallMateCyan)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Call Recording",
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        val context = androidx.compose.ui.platform.LocalContext.current
                                        var isAudioPlaying by remember { mutableStateOf(false) }
                                        var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

                                        androidx.compose.runtime.DisposableEffect(Unit) {
                                            onDispose {
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                            }
                                        }

                                        if (uiState.signedRecordingUrl != null) {
                                            val signedUrl = uiState.signedRecordingUrl!!
                                            Text(
                                                text = "Recording ready to play (private signed URL):",
                                                fontSize = 12.sp,
                                                color = CallMateCyan
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (isAudioPlaying) {
                                                            mediaPlayer?.pause()
                                                            isAudioPlaying = false
                                                        } else {
                                                            if (mediaPlayer == null) {
                                                                mediaPlayer = android.media.MediaPlayer().apply {
                                                                    setDataSource(signedUrl)
                                                                    setOnPreparedListener { mp ->
                                                                        mp.start()
                                                                        isAudioPlaying = true
                                                                    }
                                                                    setOnCompletionListener {
                                                                        isAudioPlaying = false
                                                                    }
                                                                    setOnErrorListener { _, _, _ ->
                                                                        isAudioPlaying = false
                                                                        true
                                                                    }
                                                                    prepareAsync()
                                                                }
                                                            } else {
                                                                mediaPlayer?.start()
                                                                isAudioPlaying = true
                                                            }
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isAudioPlaying) Color(0xFFEF4444) else CallMateCyan,
                                                        contentColor = if (isAudioPlaying) Color.White else CallMateNavyBg
                                                    ),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (isAudioPlaying) Icons.Default.Delete else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = if (isAudioPlaying) "Pause Audio" else "Play Recording",
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        } else {
                                            Text(
                                                text = "Private recording stored securely in Supabase Storage bucket.",
                                                fontSize = 12.sp,
                                                color = Color(0xFF94A3B8)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { viewModel.requestRecordingPlaybackUrl() },
                                                enabled = !uiState.isLoadingRecording,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = CallMateCyan,
                                                    contentColor = CallMateNavyBg
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                if (uiState.isLoadingRecording) {
                                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CallMateNavyBg)
                                                } else {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Load Recording Player", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        if (uiState.recordingErrorMessage != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = uiState.recordingErrorMessage!!,
                                                color = Color(0xFFFCA5A5),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Linked Action Items
                        if (detail.actionItems.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Action Items (${detail.actionItems.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            items(detail.actionItems, key = { it.id }) { actionItem ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CallMateNavySurface
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.toggleActionItem(actionItem.id, actionItem.completed) }) {
                                            Icon(
                                                imageVector = if (actionItem.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = "Toggle completion",
                                                tint = if (actionItem.completed) CallMateCyan else Color(0xFF64748B)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = actionItem.task,
                                            fontSize = 13.sp,
                                            fontWeight = if (actionItem.completed) FontWeight.Normal else FontWeight.SemiBold,
                                            color = if (actionItem.completed) Color(0xFF64748B) else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Transcripts
                        item {
                            Text(
                                text = "Conversation Transcript",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        if (detail.transcripts.isEmpty()) {
                            item {
                                Text(
                                    text = "No live dialog transcripts captured for this call.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        } else {
                            items(detail.transcripts, key = { it.id }) { transcript ->
                                DetailTranscriptBubble(transcript = transcript)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTranscriptBubble(transcript: TranscriptItem) {
    val isAssistant = transcript.speaker == SpeakerType.ASSISTANT
    val alignment = if (isAssistant) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isAssistant) "CallMate AI" else "Caller",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAssistant) CallMateCyan else Color(0xFF94A3B8),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isAssistant) 12.dp else 2.dp,
                bottomEnd = if (isAssistant) 2.dp else 12.dp
            ),
            color = if (isAssistant) CallMateNavyElevated else Color(0xFF1E293B),
            border = if (isAssistant) androidx.compose.foundation.BorderStroke(1.dp, CallMateCyan.copy(alpha = 0.3f)) else null
        ) {
            Text(
                text = transcript.text,
                fontSize = 13.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            )
        }
    }
}
