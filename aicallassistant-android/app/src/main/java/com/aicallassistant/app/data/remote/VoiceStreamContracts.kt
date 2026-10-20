package com.aicallassistant.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

@Serializable
data class WsAudioInMessage(
    @SerialName("event") val event: String = "audio_in",
    @SerialName("data") val data: String,
    @SerialName("timestamp_ms") val timestampMs: Long
)

@Serializable
data class WsInterruptMessage(
    @SerialName("event") val event: String = "interrupt",
    @SerialName("timestamp_ms") val timestampMs: Long
)

@Serializable
data class WsEndCallMessage(
    @SerialName("event") val event: String = "end_call",
    @SerialName("reason") val reason: String = "user_hangup"
)

sealed class VoiceServerEvent {
    data class CallStarted(
        val callId: String,
        val model: String,
        val voice: String
    ) : VoiceServerEvent()

    data class AudioOut(
        val data: String,
        val timestampMs: Long
    ) : VoiceServerEvent()

    data class TranscriptDelta(
        val speaker: String,
        val text: String,
        val isFinal: Boolean,
        val timestampMs: Long
    ) : VoiceServerEvent()

    data class TurnComplete(
        val timestampMs: Long
    ) : VoiceServerEvent()

    object Interrupted : VoiceServerEvent()

    data class CallEnded(
        val callId: String,
        val summary: String?,
        val durationSeconds: Int,
        val keyFacts: List<String> = emptyList(),
        val actionItems: List<String> = emptyList(),
        val urgency: String? = null,
        val urgencyExplanation: String? = null,
        val callerReason: String? = null
    ) : VoiceServerEvent()

    data class StreamError(
        val code: String,
        val message: String
    ) : VoiceServerEvent()

    data class Unknown(val raw: String) : VoiceServerEvent()
}

object VoiceEventParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parse(text: String): VoiceServerEvent {
        return try {
            val root = json.parseToJsonElement(text) as? JsonObject ?: return VoiceServerEvent.Unknown(text)
            val eventType = root["event"]?.jsonPrimitive?.contentOrNull ?: return VoiceServerEvent.Unknown(text)

            when (eventType) {
                "call_started" -> {
                    val callId = root["call_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val model = root["model"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val voice = root["voice"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    VoiceServerEvent.CallStarted(callId = callId, model = model, voice = voice)
                }
                "audio_out" -> {
                    val data = root["data"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val timestampMs = root["timestamp_ms"]?.jsonPrimitive?.longOrNull ?: 0L
                    VoiceServerEvent.AudioOut(data = data, timestampMs = timestampMs)
                }
                "transcript_delta" -> {
                    val speaker = root["speaker"]?.jsonPrimitive?.contentOrNull ?: "assistant"
                    val chunkText = root["text"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val isFinal = root["is_final"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false
                    val timestampMs = root["timestamp_ms"]?.jsonPrimitive?.longOrNull ?: 0L
                    VoiceServerEvent.TranscriptDelta(
                        speaker = speaker,
                        text = chunkText,
                        isFinal = isFinal,
                        timestampMs = timestampMs
                    )
                }
                "turn_complete" -> {
                    val timestampMs = root["timestamp_ms"]?.jsonPrimitive?.longOrNull ?: 0L
                    VoiceServerEvent.TurnComplete(timestampMs = timestampMs)
                }
                "interrupted" -> VoiceServerEvent.Interrupted
                "call_ended" -> {
                    val callId = root["call_id"]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val summary = root["summary"]?.jsonPrimitive?.contentOrNull
                    val duration = root["duration_seconds"]?.jsonPrimitive?.intOrNull ?: 0
                    val keyFacts = (root["key_facts"] as? kotlinx.serialization.json.JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val actionItems = (root["action_items"] as? kotlinx.serialization.json.JsonArray)
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
                    val urgency = root["urgency"]?.jsonPrimitive?.contentOrNull
                    val urgencyExplanation = root["urgency_explanation"]?.jsonPrimitive?.contentOrNull
                    val callerReason = root["caller_reason"]?.jsonPrimitive?.contentOrNull
                    VoiceServerEvent.CallEnded(
                        callId = callId,
                        summary = summary,
                        durationSeconds = duration,
                        keyFacts = keyFacts,
                        actionItems = actionItems,
                        urgency = urgency,
                        urgencyExplanation = urgencyExplanation,
                        callerReason = callerReason
                    )
                }
                "error" -> {
                    val code = root["code"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN_ERROR"
                    val message = root["message"]?.jsonPrimitive?.contentOrNull ?: "Unknown server error"
                    VoiceServerEvent.StreamError(code = code, message = message)
                }
                else -> VoiceServerEvent.Unknown(text)
            }
        } catch (_: Exception) {
            VoiceServerEvent.Unknown(text)
        }
    }
}
