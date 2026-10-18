package com.aicallassistant.app.data.remote

import android.util.Base64
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readReason
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.ConnectException
import java.util.concurrent.atomic.AtomicBoolean

interface VoiceSessionClient {
    suspend fun connect(
        callId: String,
        token: String,
        wsBaseUrl: String,
        onEvent: (VoiceServerEvent) -> Unit,
        onError: (Throwable) -> Unit
    )
    suspend fun sendAudio(pcmBytes: ByteArray, timestampMs: Long)
    suspend fun sendInterrupt(timestampMs: Long)
    suspend fun sendEndCall()
    suspend fun disconnect()
    fun isConnected(): Boolean
}

class KtorVoiceSessionClient(
    private val client: HttpClient = HttpClient(OkHttp) {
        install(WebSockets) {
            pingIntervalMillis = 15_000
        }
    },
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : VoiceSessionClient {

    companion object {
        private const val TAG = "VoiceSessionClient"
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private var activeSession: DefaultClientWebSocketSession? = null
    private var connectionJob: Job? = null
    private val connected = AtomicBoolean(false)
    private val connectionMutex = Mutex()

    override suspend fun connect(
        callId: String,
        token: String,
        wsBaseUrl: String,
        onEvent: (VoiceServerEvent) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        connectionMutex.withLock {
            disconnectInternal()

            val sanitizedUrl = "${wsBaseUrl.trimEnd('/')}/ws/calls/$callId/stream?token=***"
            Log.i(TAG, "Connecting voice WebSocket stream: $sanitizedUrl")

            val fullUrl = "${wsBaseUrl.trimEnd('/')}/ws/calls/$callId/stream?token=$token"

            connectionJob = scope.launch(Dispatchers.IO) {
                try {
                    client.webSocket(fullUrl) {
                        activeSession = this
                        connected.set(true)
                        Log.i(TAG, "WebSocket connected successfully for call: $callId")

                        try {
                            for (frame in incoming) {
                                if (!isActive) break
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        val event = VoiceEventParser.parse(text)
                                        onEvent(event)
                                    }
                                    is Frame.Close -> {
                                        val reason = frame.readReason()
                                        Log.i(TAG, "WebSocket server sent close frame: code=${reason?.code}, msg=${reason?.message}")
                                        break
                                    }
                                    else -> {}
                                }
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception in incoming WebSocket frame loop: ${e.message}")
                            onError(e)
                        } finally {
                            connected.set(false)
                            activeSession = null
                        }
                    }
                } catch (e: CancellationException) {
                    Log.i(TAG, "WebSocket connection cancelled")
                } catch (e: ConnectException) {
                    val sanitizedError = "Failed to connect to backend at $wsBaseUrl. Verify server is running and IP is reachable."
                    Log.e(TAG, sanitizedError, e)
                    connected.set(false)
                    onError(ConnectException(sanitizedError))
                } catch (e: Exception) {
                    Log.e(TAG, "WebSocket connection error: ${e.message}", e)
                    connected.set(false)
                    onError(e)
                } finally {
                    connected.set(false)
                    activeSession = null
                }
            }
        }
    }

    override suspend fun sendAudio(pcmBytes: ByteArray, timestampMs: Long) {
        if (!connected.get() || pcmBytes.isEmpty()) return
        val session = activeSession ?: return

        try {
            val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            val msg = WsAudioInMessage(data = b64, timestampMs = timestampMs)
            val jsonText = json.encodeToString(msg)
            session.send(Frame.Text(jsonText))
        } catch (e: ClosedSendChannelException) {
            Log.w(TAG, "Cannot send audio chunk: channel closed")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send audio chunk: ${e.message}")
        }
    }

    override suspend fun sendInterrupt(timestampMs: Long) {
        if (!connected.get()) return
        val session = activeSession ?: return

        try {
            val msg = WsInterruptMessage(timestampMs = timestampMs)
            val jsonText = json.encodeToString(msg)
            session.send(Frame.Text(jsonText))
            Log.i(TAG, "Sent interrupt frame to voice engine")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send interrupt: ${e.message}")
        }
    }

    override suspend fun sendEndCall() {
        if (!connected.get()) return
        val session = activeSession ?: return

        try {
            val msg = WsEndCallMessage(reason = "user_hangup")
            val jsonText = json.encodeToString(msg)
            session.send(Frame.Text(jsonText))
            Log.i(TAG, "Sent end_call frame to server")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send end_call: ${e.message}")
        }
    }

    override suspend fun disconnect() {
        connectionMutex.withLock {
            disconnectInternal()
        }
    }

    private suspend fun disconnectInternal() {
        connected.set(false)
        try {
            activeSession?.close(CloseReason(CloseReason.Codes.NORMAL, "Call completed"))
        } catch (_: Exception) {}
        activeSession = null
        connectionJob?.cancel()
        connectionJob = null
    }

    override fun isConnected(): Boolean = connected.get()
}
