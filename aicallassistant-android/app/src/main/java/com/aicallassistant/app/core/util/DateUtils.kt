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
            val date = parseIsoToDate(isoString) ?: return isoString
            displayFormat.format(date)
        } catch (_: Exception) {
            isoString
        }
    }

    fun parseIsoToDate(isoString: String?): Date? {
        if (isoString.isNullOrBlank()) return null
        return try {
            val cleanStr = isoString.substringBefore(".").substringBefore("Z").substringBefore("+")
            isoParser.parse(cleanStr)
        } catch (_: Exception) {
            null
        }
    }

    fun getRelativeDateHeader(isoString: String?): String {
        val date = parseIsoToDate(isoString) ?: return "Recent Calls"
        val now = java.util.Calendar.getInstance()
        val callCal = java.util.Calendar.getInstance().apply { time = date }

        val isSameYear = now.get(java.util.Calendar.YEAR) == callCal.get(java.util.Calendar.YEAR)
        val dayDiff = now.get(java.util.Calendar.DAY_OF_YEAR) - callCal.get(java.util.Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayDiff == 0 -> "Today"
            isSameYear && dayDiff == 1 -> "Yesterday"
            isSameYear && dayDiff in 2..6 -> SimpleDateFormat("EEEE", Locale.US).format(date) // e.g. "Sunday"
            isSameYear -> SimpleDateFormat("MMMM d", Locale.US).format(date) // e.g. "October 19"
            else -> SimpleDateFormat("MMMM d, yyyy", Locale.US).format(date)
        }
    }

    fun formatChatTimestamp(isoString: String?): String {
        val date = parseIsoToDate(isoString) ?: return "Recent"
        val now = java.util.Calendar.getInstance()
        val callCal = java.util.Calendar.getInstance().apply { time = date }

        val isSameYear = now.get(java.util.Calendar.YEAR) == callCal.get(java.util.Calendar.YEAR)
        val dayDiff = now.get(java.util.Calendar.DAY_OF_YEAR) - callCal.get(java.util.Calendar.DAY_OF_YEAR)

        return when {
            isSameYear && dayDiff == 0 -> timeOnlyFormat.format(date)
            isSameYear && dayDiff == 1 -> "Yesterday"
            isSameYear && dayDiff in 2..6 -> SimpleDateFormat("EEE", Locale.US).format(date)
            else -> SimpleDateFormat("MMM d", Locale.US).format(date)
        }
    }

    fun formatTimeOnly(isoString: String?): String {
        val date = parseIsoToDate(isoString) ?: return ""
        return timeOnlyFormat.format(date)
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
