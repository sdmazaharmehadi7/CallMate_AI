package com.aicallassistant.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SimulateCallDto(
    @SerialName("caller_name") val callerName: String? = null,
    @SerialName("caller_number") val callerNumber: String,
    @SerialName("voice_persona") val voicePersona: String? = "Puck",
    @SerialName("language") val language: String? = "en-IN"
)

@Serializable
data class SimulateCallResponseDto(
    @SerialName("call_id") val callId: String,
    @SerialName("status") val status: String,
    @SerialName("ws_url") val wsUrl: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CallListItemDto(
    @SerialName("id") val id: String,
    @SerialName("caller_name") val callerName: String? = null,
    @SerialName("caller_number") val callerNumber: String,
    @SerialName("status") val status: String,
    @SerialName("call_type") val callType: String = "simulated",
    @SerialName("caller_reason") val callerReason: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("urgency") val urgency: String = "low",
    @SerialName("is_important") val isImportant: Boolean = false,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("has_recording") val hasRecording: Boolean = false,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CallListResponseDto(
    @SerialName("calls") val calls: List<CallListItemDto>,
    @SerialName("total") val total: Int,
    @SerialName("limit") val limit: Int,
    @SerialName("offset") val offset: Int
)

@Serializable
data class CallDetailResponseDto(
    @SerialName("id") val id: String,
    @SerialName("caller_name") val callerName: String? = null,
    @SerialName("caller_number") val callerNumber: String,
    @SerialName("status") val status: String,
    @SerialName("call_type") val callType: String = "simulated",
    @SerialName("caller_reason") val callerReason: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("urgency") val urgency: String = "low",
    @SerialName("urgency_explanation") val urgencyExplanation: String? = null,
    @SerialName("uncertainties") val uncertainties: String? = null,
    @SerialName("key_facts") val keyFacts: List<JsonElement> = emptyList(),
    @SerialName("is_important") val isImportant: Boolean = false,
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("recording_path") val recordingPath: String? = null,
    @SerialName("recording_duration_seconds") val recordingDurationSeconds: Int? = null,
    @SerialName("recording_size_bytes") val recordingSizeBytes: Long? = null,
    @SerialName("has_recording") val hasRecording: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("ended_at") val endedAt: String? = null,
    @SerialName("transcripts") val transcripts: List<TranscriptItemDto> = emptyList(),
    @SerialName("action_items") val actionItems: List<ActionItemDto> = emptyList()
)

@Serializable
data class CallUpdateDto(
    @SerialName("is_important") val isImportant: Boolean? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("caller_reason") val callerReason: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("urgency") val urgency: String? = null,
    @SerialName("urgency_explanation") val urgencyExplanation: String? = null,
    @SerialName("uncertainties") val uncertainties: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null
)

@Serializable
data class TranscriptItemDto(
    @SerialName("id") val id: String,
    @SerialName("call_id") val callId: String,
    @SerialName("speaker") val speaker: String,
    @SerialName("text") val text: String,
    @SerialName("timestamp_ms") val timestampMs: Long = 0,
    @SerialName("sequence_order") val sequenceOrder: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class TranscriptListResponseDto(
    @SerialName("call_id") val callId: String,
    @SerialName("transcripts") val transcripts: List<TranscriptItemDto>,
    @SerialName("total") val total: Int
)

@Serializable
data class ActionItemDto(
    @SerialName("id") val id: String,
    @SerialName("call_id") val callId: String,
    @SerialName("task") val task: String,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed") val completed: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class CreateActionItemDto(
    @SerialName("task") val task: String,
    @SerialName("due_date") val dueDate: String? = null
)

@Serializable
data class UpdateActionItemDto(
    @SerialName("completed") val completed: Boolean? = null,
    @SerialName("task") val task: String? = null,
    @SerialName("due_date") val dueDate: String? = null
)

@Serializable
data class ActionItemListResponseDto(
    @SerialName("call_id") val callId: String,
    @SerialName("action_items") val actionItems: List<ActionItemDto>,
    @SerialName("total") val total: Int
)

@Serializable
data class CallSummaryResponseDto(
    @SerialName("call_id") val callId: String,
    @SerialName("summary") val summary: String? = null,
    @SerialName("caller_reason") val callerReason: String? = null,
    @SerialName("urgency") val urgency: String = "low",
    @SerialName("urgency_explanation") val urgencyExplanation: String? = null,
    @SerialName("uncertainties") val uncertainties: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Int = 0
)

@Serializable
data class SignedUrlResponseDto(
    @SerialName("call_id") val callId: String,
    @SerialName("signed_url") val signedUrl: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("recording_path") val recordingPath: String,
    @SerialName("file_size_bytes") val fileSizeBytes: Long? = null
)

@Serializable
data class DeleteResponseDto(
    @SerialName("deleted") val deleted: Boolean = true,
    @SerialName("id") val id: String
)

@Serializable
data class UserProfileDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("role") val role: String = "authenticated",
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class ErrorDetailDto(
    @SerialName("code") val code: String? = null,
    @SerialName("message") val message: String,
    @SerialName("details") val details: JsonElement? = null
)

@Serializable
data class ErrorResponseDto(
    @SerialName("error") val error: ErrorDetailDto
)
