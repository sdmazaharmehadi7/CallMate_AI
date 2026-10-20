package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.audio.AudioCaptureManager
import com.aicallassistant.app.data.audio.AudioPlayerManager
import com.aicallassistant.app.data.audio.DefaultAudioPlayerManager
import com.aicallassistant.app.data.remote.KtorVoiceSessionClient
import com.aicallassistant.app.data.remote.SupabaseManager
import com.aicallassistant.app.data.remote.VoiceServerEvent
import com.aicallassistant.app.data.remote.VoiceSessionClient
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.SimulateCallResult
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.UrgencyLevel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

import com.aicallassistant.app.core.event.AppCallEvent
import com.aicallassistant.app.core.event.CallEventBus
import com.aicallassistant.app.data.audio.AudioResampler
import com.aicallassistant.app.data.audio.LocalRecordingManager

enum class CallSessionState {
    IDLE,
    CONNECTING,
    LISTENING,
    PROCESSING,
    AI_SPEAKING,
    RECONNECTING,
    PROCESSING_RESULTS,
    ENDED,
    ERROR
}

data class LiveTranscriptTurn(
    val speaker: SpeakerType,
    val text: String,
    val isFinal: Boolean = false,
    val timestampMs: Long = 0
)

data class SimulateCallUiState(
    val callerName: String = "Delivery Courier (Simulated)",
    val callerNumber: String = "+919876543210",
    val sessionState: CallSessionState = CallSessionState.IDLE,
    val callResult: SimulateCallResult? = null,
    val hasMicrophonePermission: Boolean = false,
    val isMuted: Boolean = false,
    val liveTranscripts: List<LiveTranscriptTurn> = emptyList(),
    val callSummary: String? = null,
    val callUrgency: UrgencyLevel = UrgencyLevel.LOW,
    val urgencyExplanation: String? = null,
    val durationSeconds: Int = 0,
    val keyFacts: List<String> = emptyList(),
    val actionItems: List<String> = emptyList(),
    val localRecordingPath: String? = null,
    val isRecordingReady: Boolean = false,
    val errorMessage: String? = null
)

class SimulateCallViewModel(
    private val callRepository: CallRepository = DefaultCallRepository(),
    private val voiceSessionClient: VoiceSessionClient = KtorVoiceSessionClient(),
    private var audioCaptureManager: AudioCaptureManager? = null,
    private val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager(),
    private val tokenProvider: () -> String? = { SupabaseManager.getAccessToken() },
    private val wsUrlProvider: () -> String = { AppConfig.getActiveWebSocketUrl() },
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimulateCallUiState())
    val uiState: StateFlow<SimulateCallUiState> = _uiState.asStateFlow()

    private var sessionStartTimeMs: Long = 0
    private var endCallJob: Job? = null

    fun initializeCaptureManager(manager: AudioCaptureManager) {
        if (this.audioCaptureManager == null) {
            this.audioCaptureManager = manager
        }
    }

    fun updateCallerDetails(name: String, number: String) {
        _uiState.value = _uiState.value.copy(callerName = name, callerNumber = number)
    }

    fun setMicrophonePermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(hasMicrophonePermission = granted)
    }

    fun toggleMute() {
        val newMuted = !_uiState.value.isMuted
        audioCaptureManager?.setMuted(newMuted)
        _uiState.value = _uiState.value.copy(isMuted = newMuted)
    }

    fun sendInterrupt() {
        if (_uiState.value.sessionState == CallSessionState.AI_SPEAKING) {
            audioPlayerManager.flushAndResume()
            val elapsed = System.currentTimeMillis() - sessionStartTimeMs
            viewModelScope.launch {
                voiceSessionClient.sendInterrupt(elapsed)
            }
            _uiState.value = _uiState.value.copy(sessionState = CallSessionState.LISTENING)
        }
    }

    fun startSimulatedCall() {
        if (!_uiState.value.hasMicrophonePermission) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Microphone permission is required to simulate live answering."
            )
            return
        }

        val token = tokenProvider()
        if (token.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Active Supabase session required. Please sign in before starting a voice call."
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            sessionState = CallSessionState.CONNECTING,
            liveTranscripts = emptyList(),
            callSummary = null,
            durationSeconds = 0,
            keyFacts = emptyList(),
            errorMessage = null,
            isMuted = false
        )

        sessionStartTimeMs = System.currentTimeMillis()

        viewModelScope.launch {
            // 1. Create call record via authenticated FastAPI REST endpoint
            val callResponse = callRepository.simulateCall(
                callerName = _uiState.value.callerName.ifBlank { null },
                callerNumber = _uiState.value.callerNumber
            )

            when (callResponse) {
                is NetworkResult.Success -> {
                    val result = callResponse.data
                    _uiState.value = _uiState.value.copy(callResult = result)

                    // 2. Initialize local-first audio recording session on device
                    LocalRecordingManager.startRecording(result.callId)

                    // 3. Start audio player and register completion listener
                    audioPlayerManager.setOnPlaybackCompleteListener {
                        if (_uiState.value.sessionState == CallSessionState.AI_SPEAKING) {
                            _uiState.value = _uiState.value.copy(sessionState = CallSessionState.LISTENING)
                        }
                    }
                    audioPlayerManager.start()

                    // 4. Connect WebSocket stream to backend
                    val wsBaseUrl = wsUrlProvider()
                    voiceSessionClient.connect(
                        callId = result.callId,
                        token = token,
                        wsBaseUrl = wsBaseUrl,
                        onEvent = { event -> handleVoiceEvent(event) },
                        onError = { throwable -> handleVoiceError(throwable) }
                    )

                    // 5. Start audio capture with acoustic echo suppression and local recording
                    val captureResult = audioCaptureManager?.startCapture { pcmChunk ->
                        // Record caller speech locally into full conversation audio file
                        LocalRecordingManager.appendAudio(result.callId, pcmChunk)

                        if (voiceSessionClient.isConnected()) {
                            val currentState = _uiState.value.sessionState
                            val isMuted = _uiState.value.isMuted
                            // Prevent echo feedback: do not send mic frames while AI speaks out of loudspeaker
                            if (currentState != CallSessionState.AI_SPEAKING && !isMuted) {
                                val elapsed = System.currentTimeMillis() - sessionStartTimeMs
                                viewModelScope.launch {
                                    voiceSessionClient.sendAudio(pcmChunk, elapsed)
                                }
                            }
                        }
                    }

                    if (captureResult != null && captureResult.isFailure) {
                        val ex = captureResult.exceptionOrNull()
                        _uiState.value = _uiState.value.copy(
                            sessionState = CallSessionState.ERROR,
                            errorMessage = "Microphone capture failed to start: ${ex?.message ?: "Hardware error"}"
                        )
                        stopAudioAndDisconnect()
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        sessionState = CallSessionState.ERROR,
                        errorMessage = callResponse.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    private fun handleVoiceEvent(event: VoiceServerEvent) {
        when (event) {
            is VoiceServerEvent.CallStarted -> {
                _uiState.value = _uiState.value.copy(sessionState = CallSessionState.LISTENING)
            }
            is VoiceServerEvent.AudioOut -> {
                audioPlayerManager.playBase64Chunk(event.data)
                if (_uiState.value.sessionState != CallSessionState.AI_SPEAKING) {
                    _uiState.value = _uiState.value.copy(sessionState = CallSessionState.AI_SPEAKING)
                }

                // Record assistant audio locally (resample 24kHz -> 16kHz)
                val callId = _uiState.value.callResult?.callId
                if (callId != null) {
                    try {
                        val rawAiPcm = android.util.Base64.decode(event.data, android.util.Base64.DEFAULT)
                        val resampledAi = AudioResampler.resample24kTo16k(rawAiPcm)
                        LocalRecordingManager.appendAudio(callId, resampledAi)
                    } catch (_: Exception) {}
                }
            }
            is VoiceServerEvent.TurnComplete -> {
                if (_uiState.value.sessionState == CallSessionState.AI_SPEAKING && !audioPlayerManager.isPlaying()) {
                    _uiState.value = _uiState.value.copy(sessionState = CallSessionState.LISTENING)
                }
            }
            is VoiceServerEvent.TranscriptDelta -> {
                val speakerType = SpeakerType.fromValue(event.speaker)
                val turns = _uiState.value.liveTranscripts.toMutableList()
                val lastTurn = turns.lastOrNull()

                if (lastTurn != null && lastTurn.speaker == speakerType && !lastTurn.isFinal) {
                    val glue = if (lastTurn.text.endsWith(" ") || event.text.startsWith(" ")) "" else " "
                    turns[turns.lastIndex] = lastTurn.copy(
                        text = (lastTurn.text + glue + event.text).trim(),
                        isFinal = event.isFinal
                    )
                } else {
                    turns.add(
                        LiveTranscriptTurn(
                            speaker = speakerType,
                            text = event.text.trim(),
                            isFinal = event.isFinal,
                            timestampMs = event.timestampMs
                        )
                    )
                }

                val nextState = when {
                    speakerType == SpeakerType.ASSISTANT -> CallSessionState.AI_SPEAKING
                    event.isFinal -> CallSessionState.PROCESSING
                    else -> _uiState.value.sessionState
                }

                _uiState.value = _uiState.value.copy(
                    liveTranscripts = turns,
                    sessionState = nextState
                )
            }
            is VoiceServerEvent.Interrupted -> {
                audioPlayerManager.flushAndResume()
                _uiState.value = _uiState.value.copy(sessionState = CallSessionState.LISTENING)
            }
            is VoiceServerEvent.CallEnded -> {
                stopAudio()
                val callId = event.callId
                val localFile = LocalRecordingManager.getRecordingFile(callId)
                _uiState.value = _uiState.value.copy(
                    sessionState = CallSessionState.ENDED,
                    callSummary = event.summary ?: _uiState.value.callSummary,
                    callUrgency = event.urgency?.let { UrgencyLevel.fromValue(it) } ?: _uiState.value.callUrgency,
                    urgencyExplanation = event.urgencyExplanation ?: _uiState.value.urgencyExplanation,
                    durationSeconds = if (event.durationSeconds > 0) event.durationSeconds else _uiState.value.durationSeconds,
                    keyFacts = if (event.keyFacts.isNotEmpty()) event.keyFacts else _uiState.value.keyFacts,
                    actionItems = if (event.actionItems.isNotEmpty()) event.actionItems else _uiState.value.actionItems,
                    localRecordingPath = localFile?.absolutePath,
                    isRecordingReady = localFile != null
                )
                // Real-time notification across screens
                CallEventBus.emit(AppCallEvent.CallCompleted(callId))
                // Reconcile saved summary facts from database
                fetchCompletedCallDetails(callId)
            }
            is VoiceServerEvent.StreamError -> {
                stopAudioAndDisconnect()
                val userMsg = formatErrorMessage(event.code, event.message)
                _uiState.value = _uiState.value.copy(
                    sessionState = CallSessionState.ERROR,
                    errorMessage = userMsg
                )
            }
            is VoiceServerEvent.Unknown -> {}
        }
    }

    private fun handleVoiceError(throwable: Throwable) {
        if (_uiState.value.sessionState != CallSessionState.ENDED && _uiState.value.sessionState != CallSessionState.PROCESSING_RESULTS) {
            stopAudio()
            _uiState.value = _uiState.value.copy(
                sessionState = CallSessionState.ERROR,
                errorMessage = throwable.localizedMessage ?: "WebSocket connection interrupted."
            )
        }
    }

    private fun fetchCompletedCallDetails(callId: String) {
        viewModelScope.launch {
            for (attempt in 1..3) {
                when (val detailResult = callRepository.getCallDetail(callId)) {
                    is NetworkResult.Success -> {
                        val detail = detailResult.data
                        val hasSummary = !detail.summary.isNullOrBlank()
                        val localFile = LocalRecordingManager.getRecordingFile(callId)
                        _uiState.value = _uiState.value.copy(
                            callSummary = detail.summary ?: _uiState.value.callSummary,
                            callUrgency = detail.urgency,
                            urgencyExplanation = detail.urgencyExplanation ?: _uiState.value.urgencyExplanation,
                            durationSeconds = if (detail.durationSeconds > 0) detail.durationSeconds else _uiState.value.durationSeconds,
                            keyFacts = if (detail.keyFacts.isNotEmpty()) detail.keyFacts else _uiState.value.keyFacts,
                            actionItems = if (detail.actionItems.isNotEmpty()) detail.actionItems.map { it.task } else _uiState.value.actionItems,
                            localRecordingPath = localFile?.absolutePath ?: _uiState.value.localRecordingPath,
                            isRecordingReady = localFile != null || _uiState.value.isRecordingReady
                        )
                        if (hasSummary || attempt == 3) {
                            break
                        }
                    }
                    else -> {
                        if (attempt == 3) break
                    }
                }
                delay(1000)
            }
        }
    }

    fun endCall() {
        val state = _uiState.value.sessionState
        if (state == CallSessionState.ENDED || state == CallSessionState.IDLE || state == CallSessionState.PROCESSING_RESULTS) {
            return
        }

        val callId = _uiState.value.callResult?.callId

        // Stop capturing immediately so no further microphone frames are sent
        audioCaptureManager?.stopCapture()

        // Give immediate visual feedback that call is ending and processing results
        _uiState.value = _uiState.value.copy(sessionState = CallSessionState.PROCESSING_RESULTS)

        endCallJob?.cancel()
        endCallJob = viewModelScope.launch {
            // Finalize local audio recording in background
            if (callId != null) {
                val recFile = LocalRecordingManager.finalizeRecording(callId, ioDispatcher)
                if (recFile != null) {
                    _uiState.value = _uiState.value.copy(
                        localRecordingPath = recFile.absolutePath,
                        isRecordingReady = true
                    )
                }
            }

            if (voiceSessionClient.isConnected()) {
                voiceSessionClient.sendEndCall()
                // Wait up to 12s for server to finish persisting summary & return CallEnded
                withTimeoutOrNull(12000) {
                    while (voiceSessionClient.isConnected() && _uiState.value.sessionState != CallSessionState.ENDED) {
                        delay(100)
                    }
                }
            }

            stopAudioAndDisconnect()

            if (_uiState.value.sessionState != CallSessionState.ENDED) {
                val localFile = if (callId != null) LocalRecordingManager.getRecordingFile(callId) else null
                _uiState.value = _uiState.value.copy(
                    sessionState = CallSessionState.ENDED,
                    localRecordingPath = localFile?.absolutePath ?: _uiState.value.localRecordingPath,
                    isRecordingReady = localFile != null || _uiState.value.isRecordingReady
                )
                if (callId != null) {
                    CallEventBus.emit(AppCallEvent.CallCompleted(callId))
                    fetchCompletedCallDetails(callId)
                }
            }
        }
    }

    private fun stopAudio() {
        audioCaptureManager?.stopCapture()
        audioPlayerManager.stop()
    }

    private fun stopAudioAndDisconnect() {
        stopAudio()
        viewModelScope.launch {
            voiceSessionClient.disconnect()
        }
    }

    private fun formatErrorMessage(code: String, message: String): String {
        return when (code) {
            "GEMINI_NOT_CONFIGURED" ->
                "Gemini AI voice engine is not configured on the backend server. Set GEMINI_API_KEY in backend .env."
            "SESSION_CONFLICT" ->
                "Another active streaming session is already running for this call."
            "GEMINI_CONNECTION_FAILED" ->
                "Backend could not establish connection with Gemini Live. Check backend logs and network."
            "AUDIO_CHUNK_TOO_LARGE" ->
                "Audio chunk exceeded size limit."
            "UNAUTHORIZED", "FORBIDDEN" ->
                "Access denied. Session token invalid or call ownership check failed."
            else -> message
        }
    }

    fun resetCall() {
        stopAudioAndDisconnect()
        _uiState.value = _uiState.value.copy(
            sessionState = CallSessionState.IDLE,
            callResult = null,
            liveTranscripts = emptyList(),
            callSummary = null,
            durationSeconds = 0,
            keyFacts = emptyList(),
            errorMessage = null,
            isMuted = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        audioCaptureManager?.release()
        audioPlayerManager.release()
        viewModelScope.launch {
            voiceSessionClient.disconnect()
        }
    }
}
