package com.aicallassistant.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val isoParser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val displayFormat = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US)
    private val timeOnlyFormat = SimpleDateFormat("h:mm a", Locale.US)

    fun formatIsoDate(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Unknown"
        return try {
            // Strip milliseconds/time zone offset for clean simple parsing if needed
            val cleanStr = isoString.substringBefore(".").substringBefore("Z").substringBefore("+")
            val date = isoParser.parse(cleanStr) ?: return isoString
            displayFormat.format(date)
        } catch (_: Exception) {
            isoString
        }
    }

    fun formatDuration(durationSeconds: Int): String {
        if (durationSeconds <= 0) return "0s"
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return if (minutes > 0) {
            "${minutes}m ${seconds}s"
        } else {
            "${seconds}s"
        }
    }

    fun formatDurationClock(durationSeconds: Int): String {
        val minutes = durationSeconds / 60
        val seconds = durationSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }
}
