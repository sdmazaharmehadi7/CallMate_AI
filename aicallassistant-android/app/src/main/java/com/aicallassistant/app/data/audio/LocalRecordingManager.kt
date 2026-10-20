package com.aicallassistant.app.data.audio

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata record for a locally stored call recording.
 */
data class LocalRecordingMetadata(
    val callId: String,
    val filePath: String,
    val durationSeconds: Int,
    val fileSizeBytes: Long,
    val sampleRate: Int = 16000,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Local-First Call Audio Recording Manager for CallMate.
 *
 * Saves full duplex conversation audio directly to Android internal app storage
 * (context.filesDir/recordings/{callId}.wav) without cloud uploads.
 * Survives process restarts and app navigation.
 */
object LocalRecordingManager {

    private const val TAG = "LocalRecordingManager"
    private const val PREFS_NAME = "callmate_local_recordings_meta"
    private const val RECORDINGS_DIR = "recordings"
    private const val SAMPLE_RATE = 16000
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16
    private const val BYTES_PER_SECOND = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8) // 32,000 bytes/sec

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    // In-memory active recording file output streams keyed by callId
    private val activeStreams = ConcurrentHashMap<String, FileOutputStream>()
    private val activePcmFiles = ConcurrentHashMap<String, File>()

    fun initialize(context: Context) {
        val app = context.applicationContext
        appContext = app
        prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        getRecordingsDir(app).mkdirs()
        Log.i(TAG, "LocalRecordingManager initialized with directory: ${getRecordingsDir(app).absolutePath}")
    }

    private fun getRecordingsDir(context: Context): File {
        val dir = File(context.filesDir, RECORDINGS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Prepares a raw PCM output buffer for the given callId.
     */
    fun startRecording(callId: String) {
        val context = appContext ?: return
        try {
            val dir = getRecordingsDir(context)
            val pcmFile = File(dir, "$callId.pcm.tmp")
            if (pcmFile.exists()) {
                pcmFile.delete()
            }
            val fos = FileOutputStream(pcmFile, true)
            activeStreams[callId] = fos
            activePcmFiles[callId] = pcmFile
            Log.d(TAG, "Started local recording session for call $callId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start local recording for call $callId", e)
        }
    }

    /**
     * Appends 16kHz mono PCM frames (caller speech or resampled assistant speech).
     */
    fun appendAudio(callId: String, pcm16Chunk: ByteArray) {
        if (pcm16Chunk.isEmpty()) return
        val stream = activeStreams[callId] ?: return
        try {
            synchronized(stream) {
                stream.write(pcm16Chunk)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error appending audio chunk for call $callId: ${e.message}")
        }
    }

    /**
     * Finalizes the audio file, packaging raw PCM into a standard 16kHz mono RIFF/WAV file.
     * Persists metadata and returns the completed WAV File.
     */
    suspend fun finalizeRecording(
        callId: String,
        dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO
    ): File? = withContext(dispatcher) {
        val context = appContext ?: return@withContext null
        val stream = activeStreams.remove(callId)
        val pcmFile = activePcmFiles.remove(callId)

        try {
            stream?.flush()
            stream?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing raw PCM stream for $callId: ${e.message}")
        }

        if (pcmFile == null || !pcmFile.exists() || pcmFile.length() == 0L) {
            Log.w(TAG, "No PCM audio was recorded for call $callId")
            pcmFile?.delete()
            return@withContext null
        }

        val dir = getRecordingsDir(context)
        val wavFile = File(dir, "$callId.wav")
        if (wavFile.exists()) {
            wavFile.delete()
        }

        try {
            val pcmDataLength = pcmFile.length()
            val totalDataLen = pcmDataLength + 36
            val byteRate = BYTES_PER_SECOND

            FileOutputStream(wavFile).use { wavOut ->
                // Write 44-byte standard RIFF/WAV header
                val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
                    put("RIFF".toByteArray())
                    putInt(totalDataLen.toInt())
                    put("WAVE".toByteArray())
                    put("fmt ".toByteArray())
                    putInt(16) // Subchunk1Size for PCM
                    putShort(1) // AudioFormat (1 = PCM)
                    putShort(CHANNELS.toShort()) // NumChannels = 1
                    putInt(SAMPLE_RATE) // SampleRate = 16000
                    putInt(byteRate) // ByteRate = 32000
                    putShort((CHANNELS * (BITS_PER_SAMPLE / 8)).toShort()) // BlockAlign = 2
                    putShort(BITS_PER_SAMPLE.toShort()) // BitsPerSample = 16
                    put("data".toByteArray())
                    putInt(pcmDataLength.toInt())
                }
                wavOut.write(header.array())

                // Copy PCM data
                pcmFile.inputStream().use { pcmIn ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (pcmIn.read(buffer).also { bytesRead = it } != -1) {
                        wavOut.write(buffer, 0, bytesRead)
                    }
                }
                wavOut.flush()
            }

            // Cleanup temp PCM file
            pcmFile.delete()

            val durationSeconds = maxOf(1, (pcmDataLength / BYTES_PER_SECOND).toInt())

            // Persist metadata
            prefs?.edit()?.apply {
                putString("rec_path_$callId", wavFile.absolutePath)
                putInt("rec_dur_$callId", durationSeconds)
                putLong("rec_size_$callId", wavFile.length())
                putLong("rec_time_$callId", System.currentTimeMillis())
                apply()
            }

            Log.i(TAG, "Successfully finalized local recording for call $callId: ${wavFile.absolutePath} (${durationSeconds}s, ${wavFile.length()} bytes)")
            return@withContext wavFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to build WAV recording for call $callId", e)
            pcmFile.delete()
            wavFile.delete()
            return@withContext null
        }
    }

    /**
     * Checks if a valid, non-empty local recording file exists for [callId].
     */
    fun hasRecording(callId: String): Boolean {
        val file = getRecordingFile(callId)
        return file != null && file.exists() && file.length() > 44
    }

    /**
     * Retrieves the completed WAV File for [callId] if it exists and is valid.
     */
    fun getRecordingFile(callId: String): File? {
        val context = appContext ?: return null
        val directFile = File(getRecordingsDir(context), "$callId.wav")
        if (directFile.exists() && directFile.length() > 44) {
            return directFile
        }

        val savedPath = prefs?.getString("rec_path_$callId", null)
        if (!savedPath.isNullOrBlank()) {
            val file = File(savedPath)
            if (file.exists() && file.length() > 44) {
                return file
            }
        }
        return null
    }

    /**
     * Retrieves duration in seconds for [callId].
     */
    fun getRecordingDuration(callId: String): Int {
        val cached = prefs?.getInt("rec_dur_$callId", 0) ?: 0
        if (cached > 0) return cached

        val file = getRecordingFile(callId) ?: return 0
        val pcmBytes = file.length() - 44
        return if (pcmBytes > 0) (pcmBytes / BYTES_PER_SECOND).toInt() else 0
    }

    /**
     * Deletes local recording audio and clears metadata.
     */
    fun deleteRecording(callId: String): Boolean {
        val file = getRecordingFile(callId)
        val deleted = file?.delete() ?: true
        prefs?.edit()?.apply {
            remove("rec_path_$callId")
            remove("rec_dur_$callId")
            remove("rec_size_$callId")
            remove("rec_time_$callId")
            apply()
        }
        return deleted
    }
}
