package com.aicallassistant.app.domain.model

enum class CallStatus(val value: String) {
    CONNECTING("connecting"),
    ACTIVE("active"),
    COMPLETED("completed"),
    FAILED("failed"),
    CANCELLED("cancelled");

    companion object {
        fun fromValue(value: String): CallStatus =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: CONNECTING
    }
}

enum class CallType(val value: String) {
    SIMULATED("simulated"),
    CELLULAR_FORWARDED("cellular_forwarded");

    companion object {
        fun fromValue(value: String): CallType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: SIMULATED
    }
}

enum class UrgencyLevel(val value: String) {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical");

    companion object {
        fun fromValue(value: String): UrgencyLevel =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: LOW
    }
}

enum class SpeakerType(val value: String) {
    CALLER("caller"),
    ASSISTANT("assistant");

    companion object {
        fun fromValue(value: String): SpeakerType =
            entries.find { it.value.equals(value, ignoreCase = true) } ?: CALLER
    }
}

data class CallItem(
    val id: String,
    val callerName: String?,
    val callerNumber: String,
    val status: CallStatus,
    val callType: CallType,
    val callerReason: String?,
    val summary: String?,
    val urgency: UrgencyLevel,
    val isImportant: Boolean,
    val durationSeconds: Int,
    val hasRecording: Boolean,
    val createdAt: String
)

data class TranscriptItem(
    val id: String,
    val callId: String,
    val speaker: SpeakerType,
    val text: String,
    val timestampMs: Long,
    val sequenceOrder: Int,
    val createdAt: String
)

data class ActionItem(
    val id: String,
    val callId: String,
    val task: String,
    val dueDate: String?,
    val completed: Boolean,
    val completedAt: String?,
    val createdAt: String
)

data class CallDetail(
    val id: String,
    val callerName: String?,
    val callerNumber: String,
    val status: CallStatus,
    val callType: CallType,
    val callerReason: String?,
    val summary: String?,
    val urgency: UrgencyLevel,
    val urgencyExplanation: String?,
    val uncertainties: String?,
    val keyFacts: List<String>,
    val isImportant: Boolean,
    val durationSeconds: Int,
    val recordingPath: String?,
    val recordingDurationSeconds: Int?,
    val recordingSizeBytes: Long?,
    val hasRecording: Boolean,
    val createdAt: String,
    val startedAt: String?,
    val endedAt: String?,
    val transcripts: List<TranscriptItem>,
    val actionItems: List<ActionItem>
)

data class UserProfile(
    val id: String,
    val email: String,
    val fullName: String?,
    val role: String,
    val createdAt: String
)

data class SimulateCallResult(
    val callId: String,
    val status: CallStatus,
    val wsUrl: String,
    val createdAt: String
)

data class RecordingUrlResult(
    val callId: String,
    val signedUrl: String,
    val expiresAt: String,
    val recordingPath: String,
    val fileSizeBytes: Long?
)
