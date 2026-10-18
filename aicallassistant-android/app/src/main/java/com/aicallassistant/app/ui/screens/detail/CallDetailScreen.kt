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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.aicallassistant.app.ui.components.ErrorStateView
import com.aicallassistant.app.ui.components.LoadingIndicatorView
import com.aicallassistant.app.ui.components.UrgencyBadge
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
        topBar = {
            TopAppBar(
                title = { Text("Call Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    uiState.callDetail?.let { detail ->
                        IconButton(onClick = { viewModel.toggleImportant() }) {
                            Icon(
                                imageVector = if (detail.isImportant) Icons.Default.Star else Icons.Outlined.StarOutline,
                                contentDescription = "Star call",
                                tint = if (detail.isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete call",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingIndicatorView(message = "Loading call details...")
                }
                uiState.errorMessage != null && uiState.callDetail == null -> {
                    ErrorStateView(
                        message = uiState.errorMessage ?: "Failed to load details",
                        onRetry = { viewModel.loadCallDetail() }
                    )
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
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = detail.callerName ?: "Unknown Caller",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = detail.callerNumber,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        UrgencyBadge(urgency = detail.urgency)
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    androidx.compose.material3.HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Date", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(DateUtils.formatIsoDate(detail.createdAt), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Duration", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(DateUtils.formatDuration(detail.durationSeconds), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Status", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(detail.status.value.uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }

                        // AI Summary Section
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "AI Call Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = detail.summary ?: "No summary recorded for this call.",
                                        style = MaterialTheme.typography.bodyLarge
                                    )

                                    if (detail.callerReason != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text("Caller's Intent: ${detail.callerReason}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }

                                    if (detail.urgencyExplanation != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Urgency Assessment: ${detail.urgencyExplanation}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    // Key Facts
                                    if (detail.keyFacts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Key Facts Extracted:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        detail.keyFacts.forEach { fact ->
                                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                                Icon(Icons.Outlined.FiberManualRecord, contentDescription = null, modifier = Modifier.size(10.dp).padding(top = 4.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(fact, style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }

                                    // Uncertainties
                                    if (!detail.uncertainties.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Outlined.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Uncertainty: ${detail.uncertainties}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Recording Audio Player Card (when recording exists)
                        if (detail.hasRecording) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Call Recording", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        if (uiState.signedRecordingUrl != null) {
                                            Text(
                                                text = "Signed playback link generated (valid 15m):",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = uiState.signedRecordingUrl!!,
                                                style = MaterialTheme.typography.labelSmall,
                                                maxLines = 2
                                            )
                                        } else {
                                            Text(
                                                text = "Private recording stored securely in Supabase Storage bucket.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { viewModel.requestRecordingPlaybackUrl() },
                                                enabled = !uiState.isLoadingRecording
                                            ) {
                                                if (uiState.isLoadingRecording) {
                                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                                                } else {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Get Playback URL")
                                                }
                                            }
                                        }

                                        if (uiState.recordingErrorMessage != null) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = uiState.recordingErrorMessage!!,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
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
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            items(detail.actionItems, key = { it.id }) { actionItem ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.toggleActionItem(actionItem.id, actionItem.completed) }) {
                                            Icon(
                                                imageVector = if (actionItem.completed) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                                contentDescription = "Toggle completion",
                                                tint = if (actionItem.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = actionItem.task,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (actionItem.completed) FontWeight.Normal else FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        // Transcripts
                        item {
                            Text(
                                text = "Conversation Transcript",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (detail.transcripts.isEmpty()) {
                            item {
                                Text(
                                    text = "No live dialog transcripts captured for this call.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(detail.transcripts, key = { it.id }) { transcript ->
                                TranscriptBubble(transcript = transcript)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TranscriptBubble(transcript: TranscriptItem) {
    val isAssistant = transcript.speaker == SpeakerType.ASSISTANT
    val alignment = if (isAssistant) Alignment.End else Alignment.Start
    val bgColor = if (isAssistant) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isAssistant) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Text(
            text = if (isAssistant) "AI Assistant" else "Caller",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = transcript.text,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor
            )
        }
    }
}
