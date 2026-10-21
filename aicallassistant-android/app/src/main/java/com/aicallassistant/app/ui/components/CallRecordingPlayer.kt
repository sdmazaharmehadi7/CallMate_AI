package com.aicallassistant.app.ui.components

import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallassistant.app.ui.theme.CallMateBorder
import com.aicallassistant.app.ui.theme.CallMateCard
import com.aicallassistant.app.ui.theme.CallMateInk
import com.aicallassistant.app.ui.theme.CallMatePrimary
import com.aicallassistant.app.ui.theme.CallMateSlate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

private const val TAG = "CallRecordingPlayer"

@Composable
fun CallRecordingPlayer(
    localFile: File?,
    remoteUrl: String? = null,
    title: String = "Call Audio Recording",
    initialDurationSeconds: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPreparing by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(initialDurationSeconds * 1000) }
    var isUserSeeking by remember { mutableStateOf(false) }
    var sliderProgress by remember { mutableFloatStateOf(0f) }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val hasDataSource = (localFile != null && localFile.exists() && localFile.length() > 44) || !remoteUrl.isNullOrBlank()
    val isLocal = localFile != null && localFile.exists() && localFile.length() > 44

    fun cleanupPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
        isPlaying = false
        isPreparing = false
    }

    DisposableEffect(localFile?.absolutePath, remoteUrl) {
        onDispose {
            cleanupPlayer()
        }
    }

    // Polling progress while playing
    LaunchedEffect(isPlaying) {
        while (isActive && isPlaying) {
            mediaPlayer?.let { mp ->
                try {
                    if (!isUserSeeking && mp.isPlaying) {
                        currentPositionMs = mp.currentPosition
                        val total = mp.duration
                        if (total > 0) {
                            durationMs = total
                            sliderProgress = (currentPositionMs.toFloat() / total).coerceIn(0f, 1f)
                        }
                    }
                } catch (_: Exception) {}
            }
            delay(150)
        }
    }

    fun startPlaying() {
        playbackError = null
        try {
            if (mediaPlayer == null) {
                isPreparing = true
                val mp = MediaPlayer()
                mediaPlayer = mp

                if (isLocal) {
                    mp.setDataSource(context, Uri.fromFile(localFile))
                } else if (!remoteUrl.isNullOrBlank()) {
                    mp.setDataSource(remoteUrl)
                } else {
                    playbackError = "No audio file available."
                    isPreparing = false
                    return
                }

                mp.setOnPreparedListener { player ->
                    isPreparing = false
                    val total = player.duration
                    if (total > 0) durationMs = total
                    player.start()
                    isPlaying = true
                }

                mp.setOnCompletionListener {
                    isPlaying = false
                    currentPositionMs = durationMs
                    sliderProgress = 1f
                }

                mp.setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    playbackError = "Unable to play audio recording."
                    cleanupPlayer()
                    true
                }

                mp.prepareAsync()
            } else {
                mediaPlayer?.let { player ->
                    if (sliderProgress >= 1f) {
                        player.seekTo(0)
                        currentPositionMs = 0
                        sliderProgress = 0f
                    }
                    player.start()
                    isPlaying = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start audio playback", e)
            playbackError = "Playback failed: ${e.localizedMessage ?: "Unknown error"}"
            cleanupPlayer()
        }
    }

    fun pausePlaying() {
        try {
            mediaPlayer?.pause()
            isPlaying = false
        } catch (e: Exception) {
            Log.w(TAG, "Pause exception: ${e.message}")
        }
    }

    fun seekToFraction(fraction: Float) {
        val total = durationMs
        if (total > 0) {
            val targetMs = (fraction * total).toInt().coerceIn(0, total)
            currentPositionMs = targetMs
            try {
                mediaPlayer?.seekTo(targetMs)
            } catch (_: Exception) {}
        }
    }

    fun formatTime(ms: Int): String {
        val totalSeconds = (ms / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, CallMateBorder), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = CallMateCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Title & Local/Cloud Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Headphones,
                        contentDescription = null,
                        tint = CallMatePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CallMateInk
                    )
                }

                // Source indicator badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isLocal) CallMatePrimary.copy(alpha = 0.2f)
                            else Color(0xFF64748B).copy(alpha = 0.2f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isLocal) "DEVICE RECORDING" else "CLOUD RECORDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isLocal) CallMatePrimary else CallMateSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!hasDataSource) {
                Text(
                    text = "No audio recording available for this call.",
                    fontSize = 12.sp,
                    color = CallMateSlate
                )
            } else {
                // Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Play / Pause / Loading button
                    Surface(
                        modifier = Modifier.size(46.dp),
                        shape = CircleShape,
                        color = CallMatePrimary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isPreparing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color.White
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) pausePlaying() else startPlaying()
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Progress Slider & Timestamps
                    Column(modifier = Modifier.weight(1f)) {
                        Slider(
                            value = if (isUserSeeking) sliderProgress else (if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f),
                            onValueChange = { frac ->
                                isUserSeeking = true
                                sliderProgress = frac
                            },
                            onValueChangeFinished = {
                                isUserSeeking = false
                                seekToFraction(sliderProgress)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = CallMatePrimary,
                                activeTrackColor = CallMatePrimary,
                                inactiveTrackColor = CallMateBorder
                            ),
                            modifier = Modifier.fillMaxWidth().height(24.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatTime(durationMs),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (playbackError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF7F1D1D).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = playbackError!!,
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { startPlaying() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFFFECACA), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
