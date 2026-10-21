package com.aicallassistant.app.ui.screens.simulate

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aicallassistant.app.R
import com.aicallassistant.app.data.audio.DefaultAudioCaptureManager
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.UrgencyLevel
import com.aicallassistant.app.data.audio.LocalRecordingManager
import com.aicallassistant.app.ui.components.CallRecordingPlayer
import com.aicallassistant.app.ui.components.UrgencyBadge
import com.aicallassistant.app.ui.theme.CallMateBackgroundGradient
import com.aicallassistant.app.ui.theme.CallMateCyan
import com.aicallassistant.app.ui.theme.CallMateNavyBg
import com.aicallassistant.app.ui.theme.CallMateNavyElevated
import com.aicallassistant.app.ui.theme.CallMateNavySurface
import com.aicallassistant.app.ui.theme.CallMatePrimary
import com.aicallassistant.app.ui.viewmodel.CallSessionState
import com.aicallassistant.app.ui.viewmodel.LiveTranscriptTurn
import com.aicallassistant.app.ui.viewmodel.SimulateCallViewModel

private data class ScenarioPreset(
    val title: String,
    val name: String,
    val number: String
)

private val SCENARIO_PRESETS = listOf(
    ScenarioPreset("Delivery", "Amazon Courier", "+1 (555) 234-5678"),
    ScenarioPreset("Doctor Clinic", "Dr. Smith Office", "+1 (555) 345-6789"),
    ScenarioPreset("Bank Security", "Chase Fraud Dept", "+1 (555) 456-7890"),
    ScenarioPreset("Custom", "Unknown Caller", "+1 (555) 000-0000")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulateCallScreen(
    viewModel: SimulateCallViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCallDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.initializeCaptureManager(DefaultAudioCaptureManager(context.applicationContext))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setMicrophonePermission(isGranted)
    }

    LaunchedEffect(Unit) {
        val isGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.setMicrophonePermission(isGranted)
    }

    Scaffold(
        containerColor = CallMateNavyBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.callmate_logo),
                            contentDescription = "CallMate",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI Voice Screening",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CallMateNavyBg,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CallMateBackgroundGradient)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Microphone Permission Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CallMateNavySurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.hasMicrophonePermission) CallMateCyan.copy(alpha = 0.2f)
                                    else Color(0xFFEF4444).copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.hasMicrophonePermission) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = null,
                                tint = if (uiState.hasMicrophonePermission) CallMateCyan else Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Microphone Access",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (uiState.hasMicrophonePermission) "Ready for voice streaming" else "Microphone permission required",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    if (!uiState.hasMicrophonePermission) {
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CallMateCyan,
                                contentColor = CallMateNavyBg
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // IDLE STATE: Scenario selector & Start screening
            if (uiState.sessionState == CallSessionState.IDLE) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CallMateNavySurface
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Test Call Scenarios",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Scenario Chips & Random Generator
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SCENARIO_PRESETS.take(3).forEach { preset ->
                                val isSelected = uiState.callerName == preset.name
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.updateCallerDetails(preset.name, preset.number)
                                        },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) CallMateCyan.copy(alpha = 0.2f) else CallMateNavyElevated,
                                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CallMateCyan) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = preset.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) CallMateCyan else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Random Caller generator button
                        OutlinedButton(
                            onClick = {
                                val randomPool = listOf(
                                    ScenarioPreset("Delivery", "Amazon Courier (Simulated)", "+1 (555) 019-2831"),
                                    ScenarioPreset("Clinic", "Dr. Chen's Clinic (Simulated)", "+1 (555) 014-9923"),
                                    ScenarioPreset("Colleague", "Sarah from Marketing (Simulated)", "+1 (555) 018-4451"),
                                    ScenarioPreset("Bank", "First Horizon Fraud Alert (Simulated)", "+1 (555) 012-7782"),
                                    ScenarioPreset("Contractor", "Dave Plumbing Service (Simulated)", "+1 (555) 016-3390"),
                                    ScenarioPreset("Recruiter", "Apex Tech Talent (Simulated)", "+1 (555) 017-8824")
                                )
                                val picked = randomPool.random()
                                viewModel.updateCallerDetails(picked.name, picked.number)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CallMateCyan.copy(alpha = 0.6f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CallMateCyan)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🎲 Generate Random Test Caller", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Caller Name
                        OutlinedTextField(
                            value = uiState.callerName,
                            onValueChange = { viewModel.updateCallerDetails(it, uiState.callerNumber) },
                            label = { Text("Caller Name", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CallMateCyan,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = CallMateCyan
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Caller Phone
                        OutlinedTextField(
                            value = uiState.callerNumber,
                            onValueChange = { viewModel.updateCallerDetails(uiState.callerName, it) },
                            label = { Text("Caller Phone Number", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CallMateCyan,
                                unfocusedBorderColor = Color(0xFF334155),
                                cursorColor = CallMateCyan
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.startSimulatedCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CallMateCyan,
                                contentColor = CallMateNavyBg
                            )
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, tint = CallMateNavyBg)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Start AI Call Screening",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = CallMateNavyBg
                            )
                        }
                    }
                }
            }

            // CONNECTING STATE
            if (uiState.sessionState == CallSessionState.CONNECTING) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CallMateNavySurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = CallMateCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Connecting CallMate AI...",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Establishing WebSocket session and initializing 16 kHz audio pipeline.",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ACTIVE CALL STATES: LISTENING, PROCESSING, AI_SPEAKING, RECONNECTING
            if (uiState.sessionState == CallSessionState.LISTENING ||
                uiState.sessionState == CallSessionState.PROCESSING ||
                uiState.sessionState == CallSessionState.AI_SPEAKING ||
                uiState.sessionState == CallSessionState.RECONNECTING
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CallMateNavySurface
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Caller header & Status pill
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = uiState.callerName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = uiState.callerNumber,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            CallMateStatusPill(
                                state = uiState.sessionState,
                                isMuted = uiState.isMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Animated Pulse Ring
                        CallMatePulseOrb(
                            state = uiState.sessionState,
                            isMuted = uiState.isMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Live Transcripts Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE TRANSCRIPT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CallMateCyan,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${uiState.liveTranscripts.size} turns",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LiveTranscriptBox(
                            turns = uiState.liveTranscripts,
                            currentState = uiState.sessionState
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Controls: Mute, Interrupt, End Call
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute button
                            Button(
                                onClick = { viewModel.toggleMute() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isMuted) Color(0xFF7F1D1D) else CallMateNavyElevated,
                                    contentColor = if (uiState.isMuted) Color(0xFFFCA5A5) else Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (uiState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isMuted) "Muted" else "Mute",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Interrupt (Barge-in)
                            if (uiState.sessionState == CallSessionState.AI_SPEAKING) {
                                Button(
                                    onClick = { viewModel.sendInterrupt() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CallMateNavyElevated,
                                        contentColor = CallMateCyan
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.GraphicEq,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Barge-In", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // End call button
                            Button(
                                onClick = { viewModel.endCall() },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFDC2626),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CallEnd,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("End Call", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // PROCESSING RESULTS STATE
            if (uiState.sessionState == CallSessionState.PROCESSING_RESULTS) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CallMateNavySurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = CallMateCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Finalizing Call Analysis...",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CallMate is generating the AI call summary, extracting key facts, actionable tasks, and finalizing local recording.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // ENDED STATE
            if (uiState.sessionState == CallSessionState.ENDED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CallMateNavySurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(CallMateCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = CallMateCyan,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Call Screened & Saved",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (uiState.durationSeconds > 0) {
                            Text(
                                text = "Duration: ${uiState.durationSeconds}s",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // AI Post-Call Summary Card
                        if (!uiState.callSummary.isNullOrBlank()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = CallMateNavyElevated
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "AI CALL SUMMARY",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CallMateCyan
                                        )
                                        UrgencyBadge(urgency = uiState.callUrgency)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = uiState.callSummary!!,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        lineHeight = 18.sp
                                    )
                                    if (!uiState.urgencyExplanation.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Reason: ${uiState.urgencyExplanation}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                    if (uiState.keyFacts.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Key Facts:",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        uiState.keyFacts.forEach { fact ->
                                            Text(
                                                text = "• $fact",
                                                fontSize = 12.sp,
                                                color = Color(0xFFCBD5E1)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Extracted Follow-up Tasks / Action Items Card
                        if (uiState.actionItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = CallMateNavyElevated
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = CallMateCyan,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "FOLLOW-UP ACTION ITEMS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CallMateCyan
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    uiState.actionItems.forEach { task ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Text(
                                                text = "✓",
                                                color = CallMateCyan,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = task,
                                                fontSize = 13.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Local Audio Recording Player
                        val recordingCallId = uiState.callResult?.callId
                        val localRecFile = recordingCallId?.let { LocalRecordingManager.getRecordingFile(it) }
                        if (localRecFile != null || uiState.isRecordingReady) {
                            Spacer(modifier = Modifier.height(14.dp))
                            CallRecordingPlayer(
                                localFile = localRecFile,
                                initialDurationSeconds = uiState.durationSeconds,
                                title = "Call Recording (On Device)"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        uiState.callResult?.let { res ->
                            Button(
                                onClick = { onNavigateToCallDetail(res.callId) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CallMateCyan,
                                    contentColor = CallMateNavyBg
                                )
                            ) {
                                Text(
                                    "View Full Call Details & Actions",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = CallMateNavyBg
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { viewModel.resetCall() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Screen Another Call", fontSize = 14.sp)
                        }
                    }
                }
            }

            // ERROR DISPLAY
            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF7F1D1D)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Call Error",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFECACA)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.errorMessage!!,
                            color = Color(0xFFFCA5A5),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.resetCall() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF991B1B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Dismiss & Try Again", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CallMatePulseOrb(state: CallSessionState, isMuted: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    val orbColor = when {
        isMuted -> Color(0xFFEF4444)
        state == CallSessionState.AI_SPEAKING -> CallMateCyan
        state == CallSessionState.PROCESSING -> CallMatePrimary
        state == CallSessionState.RECONNECTING -> Color(0xFFF59E0B)
        else -> Color(0xFF38BDF8)
    }

    Box(
        modifier = Modifier
            .size(90.dp)
            .scale(if (state == CallSessionState.AI_SPEAKING || state == CallSessionState.LISTENING || state == CallSessionState.PROCESSING) pulseScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(orbColor.copy(alpha = 0.15f))
        )
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(orbColor.copy(alpha = 0.3f))
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(orbColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    isMuted -> Icons.Default.MicOff
                    state == CallSessionState.AI_SPEAKING -> Icons.AutoMirrored.Filled.VolumeUp
                    state == CallSessionState.PROCESSING -> Icons.Default.SmartToy
                    else -> Icons.Default.Mic
                },
                contentDescription = null,
                tint = CallMateNavyBg,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CallMateStatusPill(state: CallSessionState, isMuted: Boolean) {
    val (bgColor, textColor, icon, label) = when {
        isMuted -> Quadruple(Color(0xFF7F1D1D), Color(0xFFFECACA), Icons.Default.MicOff, "Muted")
        state == CallSessionState.AI_SPEAKING -> Quadruple(CallMateCyan.copy(alpha = 0.2f), CallMateCyan, Icons.AutoMirrored.Filled.VolumeUp, "AI Speaking")
        state == CallSessionState.PROCESSING -> Quadruple(Color(0xFF1E3A8A), Color(0xFFBFDBFE), Icons.Default.SmartToy, "Thinking...")
        state == CallSessionState.RECONNECTING -> Quadruple(Color(0xFF78350F), Color(0xFFFDE68A), Icons.Outlined.Warning, "Reconnecting")
        else -> Quadruple(Color(0xFF065F46), Color(0xFFA7F3D0), Icons.Default.Mic, "Listening")
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LiveTranscriptBox(
    turns: List<LiveTranscriptTurn>,
    currentState: CallSessionState
) {
    val listState = rememberLazyListState()

    LaunchedEffect(turns.size, turns.lastOrNull()?.text) {
        if (turns.isNotEmpty()) {
            listState.animateScrollToItem(turns.size - 1)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp, max = 260.dp),
        shape = RoundedCornerShape(14.dp),
        color = CallMateNavyElevated
    ) {
        if (turns.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CallMate AI is listening...\nSpeak into your phone's microphone to begin conversation.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(turns) { turn ->
                    val isAssistant = turn.speaker == SpeakerType.ASSISTANT
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isAssistant) Alignment.Start else Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (isAssistant) Icons.Default.SmartToy else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isAssistant) CallMateCyan else Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAssistant) "CallMate AI" else "Caller (You)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 12.dp,
                                topEnd = 12.dp,
                                bottomStart = if (isAssistant) 2.dp else 12.dp,
                                bottomEnd = if (isAssistant) 12.dp else 2.dp
                            ),
                            color = if (isAssistant) CallMateNavySurface else Color(0xFF1E293B),
                            border = if (isAssistant) androidx.compose.foundation.BorderStroke(1.dp, CallMateCyan.copy(alpha = 0.3f)) else null
                        ) {
                            Text(
                                text = com.aicallassistant.app.ui.screens.detail.formatChatBubbleText(turn.text),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
