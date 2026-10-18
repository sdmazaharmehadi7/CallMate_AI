package com.aicallassistant.app

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.audio.AudioCaptureManager
import com.aicallassistant.app.data.audio.AudioPlayerManager
import com.aicallassistant.app.data.remote.VoiceServerEvent
import com.aicallassistant.app.data.remote.VoiceSessionClient
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.domain.model.CallDetail
import com.aicallassistant.app.domain.model.CallStatus
import com.aicallassistant.app.domain.model.CallType
import com.aicallassistant.app.domain.model.SimulateCallResult
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.UrgencyLevel
import com.aicallassistant.app.ui.viewmodel.CallSessionState
import com.aicallassistant.app.ui.viewmodel.SimulateCallViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SimulateCallViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val callRepository = mockk<CallRepository>(relaxed = true)
    private val voiceSessionClient = mockk<VoiceSessionClient>(relaxed = true)
    private val audioCaptureManager = mockk<AudioCaptureManager>(relaxed = true)
    private val audioPlayerManager = mockk<AudioPlayerManager>(relaxed = true)

    private val testSimulateResult = SimulateCallResult(
        callId = "call-12345",
        status = CallStatus.CONNECTING,
        wsUrl = "ws://10.0.2.2:8000/ws/calls/call-12345/stream",
        createdAt = "2026-10-18T12:00:00Z"
    )

    private val testCallDetail = CallDetail(
        id = "call-12345",
        callerName = "Delivery Courier",
        callerNumber = "+919876543210",
        status = CallStatus.COMPLETED,
        callType = CallType.SIMULATED,
        callerReason = "Package delivery",
        summary = "Courier arrived at building gate.",
        urgency = UrgencyLevel.MEDIUM,
        urgencyExplanation = "Requires immediate gate code",
        uncertainties = null,
        keyFacts = listOf("Package from Amazon"),
        isImportant = false,
        durationSeconds = 42,
        recordingPath = null,
        recordingDurationSeconds = null,
        recordingSizeBytes = null,
        hasRecording = false,
        createdAt = "2026-10-18T12:00:00Z",
        startedAt = "2026-10-18T12:00:05Z",
        endedAt = "2026-10-18T12:00:47Z",
        transcripts = emptyList(),
        actionItems = emptyList()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { callRepository.simulateCall(any(), any()) } returns NetworkResult.Success(testSimulateResult)
        coEvery { callRepository.getCallDetail("call-12345") } returns NetworkResult.Success(testCallDetail)
        every { audioCaptureManager.startCapture(any()) } returns Result.success(Unit)
        every { audioPlayerManager.start() } returns Result.success(Unit)
        every { voiceSessionClient.isConnected() } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(token: String? = "valid.jwt.token"): SimulateCallViewModel {
        return SimulateCallViewModel(
            callRepository = callRepository,
            voiceSessionClient = voiceSessionClient,
            audioCaptureManager = audioCaptureManager,
            audioPlayerManager = audioPlayerManager,
            tokenProvider = { token },
            wsUrlProvider = { "ws://10.0.2.2:8000" }
        )
    }

    @Test
    fun startCall_whenNoMicrophonePermission_setsError() = runTest {
        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(false)

        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.IDLE, state.sessionState)
        assertTrue(state.errorMessage?.contains("Microphone permission") == true)
    }

    @Test
    fun startCall_whenUnauthenticated_setsError() = runTest {
        val viewModel = createViewModel(token = null)
        viewModel.setMicrophonePermission(true)

        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.IDLE, state.sessionState)
        assertTrue(state.errorMessage?.contains("Supabase session required") == true)
    }

    @Test
    fun startCall_whenBackendSimulateCallFails_transitionsToError() = runTest {
        coEvery { callRepository.simulateCall(any(), any()) } returns NetworkResult.Error(
            message = "Server internal error",
            code = "INTERNAL_ERROR",
            statusCode = 500
        )

        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)

        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.ERROR, state.sessionState)
        assertEquals("Server internal error", state.errorMessage)
    }

    @Test
    fun startCall_whenAudioCaptureInitFails_transitionsToError() = runTest {
        every { audioCaptureManager.startCapture(any()) } returns Result.failure(
            IllegalStateException("AudioRecord hardware in use")
        )

        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)

        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.ERROR, state.sessionState)
        assertTrue(state.errorMessage?.contains("AudioRecord hardware in use") == true)
    }

    @Test
    fun startCall_successfulFlow_connectsWebSocketAndHandlesEvents() = runTest {
        val onEventSlot = slot<(VoiceServerEvent) -> Unit>()
        coEvery {
            voiceSessionClient.connect(
                callId = "call-12345",
                token = "valid.jwt.token",
                wsBaseUrl = "ws://10.0.2.2:8000",
                onEvent = capture(onEventSlot),
                onError = any()
            )
        } returns Unit

        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)

        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. Handshake event from server
        onEventSlot.captured.invoke(
            VoiceServerEvent.CallStarted(
                callId = "call-12345",
                model = "gemini-3.8-live",
                voice = "Puck"
            )
        )
        assertEquals(CallSessionState.LISTENING, viewModel.uiState.value.sessionState)

        // 2. Audio out event
        onEventSlot.captured.invoke(VoiceServerEvent.AudioOut(data = "AQIDBA==", timestampMs = 120))
        verify { audioPlayerManager.playBase64Chunk("AQIDBA==") }
        assertEquals(CallSessionState.AI_SPEAKING, viewModel.uiState.value.sessionState)

        // 3. Transcript delta event
        onEventSlot.captured.invoke(
            VoiceServerEvent.TranscriptDelta(
                speaker = "assistant",
                text = "Hello! ",
                isFinal = false,
                timestampMs = 200
            )
        )
        onEventSlot.captured.invoke(
            VoiceServerEvent.TranscriptDelta(
                speaker = "assistant",
                text = "How can I help you?",
                isFinal = true,
                timestampMs = 400
            )
        )

        val turns = viewModel.uiState.value.liveTranscripts
        assertEquals(1, turns.size)
        assertEquals("Hello! How can I help you?", turns[0].text)
        assertEquals(SpeakerType.ASSISTANT, turns[0].speaker)

        // 4. Interrupted event
        onEventSlot.captured.invoke(VoiceServerEvent.Interrupted)
        verify { audioPlayerManager.flushAndResume() }
        assertEquals(CallSessionState.LISTENING, viewModel.uiState.value.sessionState)

        // 5. Call ended event
        onEventSlot.captured.invoke(
            VoiceServerEvent.CallEnded(
                callId = "call-12345",
                summary = "Courier arrived at building gate.",
                durationSeconds = 42
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val endedState = viewModel.uiState.value
        assertEquals(CallSessionState.ENDED, endedState.sessionState)
        assertEquals("Courier arrived at building gate.", endedState.callSummary)
        assertEquals(42, endedState.durationSeconds)
        assertEquals(UrgencyLevel.MEDIUM, endedState.callUrgency)
        assertEquals("Requires immediate gate code", endedState.urgencyExplanation)
    }

    @Test
    fun toggleMute_updatesMuteStateAndNotifiesCaptureManager() = runTest {
        val viewModel = createViewModel()
        assertFalse(viewModel.uiState.value.isMuted)

        viewModel.toggleMute()
        assertTrue(viewModel.uiState.value.isMuted)
        verify { audioCaptureManager.setMuted(true) }

        viewModel.toggleMute()
        assertFalse(viewModel.uiState.value.isMuted)
        verify { audioCaptureManager.setMuted(false) }
    }

    @Test
    fun sendInterrupt_flushesAudioAndSendsInterruptFrame() = runTest {
        val onEventSlot = slot<(VoiceServerEvent) -> Unit>()
        coEvery {
            voiceSessionClient.connect(any(), any(), any(), capture(onEventSlot), any())
        } returns Unit

        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)
        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        // Place in AI_SPEAKING state
        onEventSlot.captured.invoke(VoiceServerEvent.AudioOut(data = "AQIDBA==", timestampMs = 100))
        assertEquals(CallSessionState.AI_SPEAKING, viewModel.uiState.value.sessionState)

        // User initiates barge-in
        viewModel.sendInterrupt()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { audioPlayerManager.flushAndResume() }
        coVerify { voiceSessionClient.sendInterrupt(any()) }
        assertEquals(CallSessionState.LISTENING, viewModel.uiState.value.sessionState)
    }

    @Test
    fun endCall_sendsEndCallFrameAndCleansUpResources() = runTest {
        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)
        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.endCall()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { voiceSessionClient.sendEndCall() }
        verify { audioCaptureManager.stopCapture() }
        verify { audioPlayerManager.stop() }
        coVerify { voiceSessionClient.disconnect() }
        assertEquals(CallSessionState.ENDED, viewModel.uiState.value.sessionState)
    }

    @Test
    fun streamError_geminiNotConfigured_displaysFriendlyErrorMessage() = runTest {
        val onEventSlot = slot<(VoiceServerEvent) -> Unit>()
        coEvery {
            voiceSessionClient.connect(any(), any(), any(), capture(onEventSlot), any())
        } returns Unit

        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)
        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        onEventSlot.captured.invoke(
            VoiceServerEvent.StreamError(
                code = "GEMINI_NOT_CONFIGURED",
                message = "Engine not configured."
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.ERROR, state.sessionState)
        assertTrue(state.errorMessage?.contains("GEMINI_API_KEY") == true)
        verify { audioCaptureManager.stopCapture() }
        verify { audioPlayerManager.stop() }
    }

    @Test
    fun resetCall_restoresInitialIdleState() = runTest {
        val viewModel = createViewModel()
        viewModel.setMicrophonePermission(true)
        viewModel.startSimulatedCall()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.resetCall()

        val state = viewModel.uiState.value
        assertEquals(CallSessionState.IDLE, state.sessionState)
        assertNull(state.callResult)
        assertTrue(state.liveTranscripts.isEmpty())
        assertNull(state.errorMessage)
        assertFalse(state.isMuted)
    }
}
