package com.aicallassistant.app.data.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Process
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface AudioPlayerManager {
    fun start(): Result<Unit>
    fun playChunk(pcmBytes: ByteArray)
    fun playBase64Chunk(base64Data: String)
    fun flushAndResume()
    fun stop()
    fun release()
    fun isPlaying(): Boolean
}

class DefaultAudioPlayerManager(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AudioPlayerManager {

    companion object {
        private const val TAG = "AudioPlayerManager"
        const val SAMPLE_RATE = 24000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val isRunning = AtomicBoolean(false)
    private val audioQueue = Channel<ByteArray>(Channel.UNLIMITED)

    override fun start(): Result<Unit> {
        if (isRunning.get()) {
            return Result.success(Unit)
        }

        val minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            val err = "AudioTrack returned invalid minBufferSize: $minBufferSize"
            Log.e(TAG, err)
            return Result.failure(IllegalStateException(err))
        }

        val bufferSize = maxOf(minBufferSize, 4800 * 4) // ~200ms buffer capacity

        val track = try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .build()

            AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate AudioTrack: ${e.message}", e)
            return Result.failure(e)
        }

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            val err = "AudioTrack failed to initialize (state=${track.state})"
            Log.e(TAG, err)
            return Result.failure(IllegalStateException(err))
        }

        try {
            track.play()
        } catch (e: Exception) {
            track.release()
            Log.e(TAG, "Failed to start AudioTrack playback: ${e.message}", e)
            return Result.failure(e)
        }

        audioTrack = track
        isRunning.set(true)

        // Launch consumer coroutine on IO dispatcher with AUDIO thread priority
        playbackJob = scope.launch(Dispatchers.IO) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            while (isActive && isRunning.get()) {
                val chunk = try {
                    audioQueue.receive()
                } catch (_: Exception) {
                    break
                }

                if (!isRunning.get()) break

                val currentTrack = audioTrack ?: break
                try {
                    var written = 0
                    while (written < chunk.size && isRunning.get()) {
                        val result = currentTrack.write(chunk, written, chunk.size - written, AudioTrack.WRITE_BLOCKING)
                        if (result > 0) {
                            written += result
                        } else {
                            Log.w(TAG, "AudioTrack write returned: $result")
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "AudioTrack write error: ${e.message}")
                }
            }
        }

        Log.i(TAG, "AudioPlayer started successfully at ${SAMPLE_RATE}Hz mono")
        return Result.success(Unit)
    }

    override fun playChunk(pcmBytes: ByteArray) {
        if (!isRunning.get() || pcmBytes.isEmpty()) return
        audioQueue.trySend(pcmBytes)
    }

    override fun playBase64Chunk(base64Data: String) {
        if (!isRunning.get() || base64Data.isBlank()) return
        try {
            val pcmBytes = Base64.decode(base64Data, Base64.DEFAULT)
            if (pcmBytes.isNotEmpty()) {
                playChunk(pcmBytes)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to decode base64 audio chunk: ${e.message}")
        }
    }

    override fun flushAndResume() {
        if (!isRunning.get()) return
        try {
            // Drain in-memory queue
            while (audioQueue.tryReceive().isSuccess) {
                // Drain all pending chunks
            }
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                    track.play()
                }
            }
            Log.i(TAG, "AudioPlayer flushed buffer due to interruption")
        } catch (e: Exception) {
            Log.w(TAG, "Error flushing AudioTrack: ${e.message}")
        }
    }

    override fun stop() {
        if (!isRunning.getAndSet(false)) return
        playbackJob?.cancel()
        playbackJob = null

        // Drain queue
        while (audioQueue.tryReceive().isSuccess) {}

        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.pause()
                    track.flush()
                    track.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioTrack: ${e.message}")
        }
        Log.i(TAG, "AudioPlayer stopped")
    }

    override fun release() {
        stop()
        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack: ${e.message}")
        }
        audioTrack = null
    }

    override fun isPlaying(): Boolean = isRunning.get()
}
