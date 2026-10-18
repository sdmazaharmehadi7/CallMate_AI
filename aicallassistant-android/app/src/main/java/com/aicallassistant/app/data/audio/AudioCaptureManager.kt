package com.aicallassistant.app.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

interface AudioCaptureManager {
    fun startCapture(onChunkCaptured: (pcmBytes: ByteArray) -> Unit): Result<Unit>
    fun stopCapture()
    fun release()
    fun isCapturing(): Boolean
    fun setMuted(muted: Boolean)
    fun isMuted(): Boolean
}

class DefaultAudioCaptureManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : AudioCaptureManager {

    companion object {
        private const val TAG = "AudioCaptureManager"
        const val SAMPLE_RATE = 16000
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        // 100 ms of 16-bit 16kHz mono audio = 1600 samples = 3200 bytes
        const val FRAME_BYTES = 3200
    }

    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val capturing = AtomicBoolean(false)
    private val muted = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    override fun startCapture(onChunkCaptured: (pcmBytes: ByteArray) -> Unit): Result<Unit> {
        if (capturing.get()) {
            return Result.success(Unit)
        }

        // 1. Check RECORD_AUDIO permission
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            val err = "Microphone permission (RECORD_AUDIO) not granted"
            Log.e(TAG, err)
            return Result.failure(SecurityException(err))
        }

        // 2. Determine buffer size
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        if (minBufferSize <= 0) {
            val err = "AudioRecord returned invalid minBufferSize: $minBufferSize"
            Log.e(TAG, err)
            return Result.failure(IllegalStateException(err))
        }
        val bufferSize = maxOf(minBufferSize, FRAME_BYTES * 2)

        // 3. Initialize AudioRecord
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )
        } catch (e: Exception) {
            Log.w(TAG, "VOICE_COMMUNICATION source failed, falling back to MIC source: ${e.message}")
            try {
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to create AudioRecord: ${e2.message}", e2)
                return Result.failure(e2)
            }
        }

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            val err = "AudioRecord failed to initialize (state=${record.state})"
            Log.e(TAG, err)
            return Result.failure(IllegalStateException(err))
        }

        audioRecord = record

        // 4. Start recording
        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
                record.release()
                audioRecord = null
                val err = "AudioRecord failed to enter RECORDING state"
                Log.e(TAG, err)
                return Result.failure(IllegalStateException(err))
            }
        } catch (e: Exception) {
            record.release()
            audioRecord = null
            Log.e(TAG, "Exception starting recording: ${e.message}", e)
            return Result.failure(e)
        }

        capturing.set(true)

        // 5. Run continuous capture loop on background coroutine
        captureJob = scope.launch(Dispatchers.IO) {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val buffer = ByteArray(FRAME_BYTES)

            while (isActive && capturing.get()) {
                val currentRecord = audioRecord ?: break
                val bytesRead = currentRecord.read(buffer, 0, buffer.size)

                if (bytesRead > 0) {
                    if (!muted.get()) {
                        val chunk = buffer.copyOf(bytesRead)
                        onChunkCaptured(chunk)
                    }
                } else if (bytesRead < 0) {
                    Log.w(TAG, "AudioRecord read error code: $bytesRead")
                    if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION || bytesRead == AudioRecord.ERROR_DEAD_OBJECT) {
                        break
                    }
                }
            }
        }

        Log.i(TAG, "Audio capture started successfully at ${SAMPLE_RATE}Hz mono")
        return Result.success(Unit)
    }

    override fun stopCapture() {
        if (!capturing.getAndSet(false)) {
            return
        }
        captureJob?.cancel()
        captureJob = null

        try {
            audioRecord?.let {
                if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    it.stop()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping AudioRecord: ${e.message}")
        }
        Log.i(TAG, "Audio capture stopped")
    }

    override fun release() {
        stopCapture()
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord: ${e.message}")
        }
        audioRecord = null
    }

    override fun isCapturing(): Boolean = capturing.get()

    override fun setMuted(muted: Boolean) {
        this.muted.set(muted)
        Log.i(TAG, "Microphone mute state changed to: $muted")
    }

    override fun isMuted(): Boolean = muted.get()
}
