package com.aicallassistant.app.domain.model

data class ChatThreadItem(
    val callerNumber: String,
    val callerName: String?,
    val calls: List<CallItem>,
    val latestCall: CallItem,
    val latestSummary: String?,
    val latestTimestamp: String,
    val hasRecording: Boolean,
    val isMuted: Boolean = false
) {
    val displayName: String
        get() = callerName?.takeIf { it.isNotBlank() } ?: callerNumber

    companion object {
        fun fromCalls(calls: List<CallItem>): List<ChatThreadItem> {
            if (calls.isEmpty()) return emptyList()

            // Group by normalized caller phone number or fallback caller name
            val grouped = calls.groupBy {
                val num = it.callerNumber.trim()
                if (num.isNotBlank()) num else (it.callerName ?: "Unknown Caller")
            }

            return grouped.map { (key, callList) ->
                // Sort calls inside group descending by creation date
                val sortedCalls = callList.sortedByDescending { it.createdAt }
                val latest = sortedCalls.first()
                val resolvedName = sortedCalls.firstOrNull { !it.callerName.isNullOrBlank() }?.callerName

                ChatThreadItem(
                    callerNumber = if (key.startsWith("+") || key.any { it.isDigit() }) key else latest.callerNumber,
                    callerName = resolvedName,
                    calls = sortedCalls,
                    latestCall = latest,
                    latestSummary = latest.summary?.takeIf { it.isNotBlank() }
                        ?: latest.callerReason?.takeIf { it.isNotBlank() }
                        ?: "Call screened by Equal Assistant",
                    latestTimestamp = latest.createdAt,
                    hasRecording = sortedCalls.any { it.hasRecording }
                )
            }.sortedByDescending { it.latestTimestamp }
        }
    }
}
