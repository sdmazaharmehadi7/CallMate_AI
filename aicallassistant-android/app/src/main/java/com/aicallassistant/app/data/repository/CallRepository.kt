package com.aicallassistant.app.data.repository

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.remote.ApiClient
import com.aicallassistant.app.data.remote.CallUpdateDto
import com.aicallassistant.app.domain.model.ActionItem
import com.aicallassistant.app.domain.model.CallDetail
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.CallStatus
import com.aicallassistant.app.domain.model.CallType
import com.aicallassistant.app.domain.model.RecordingUrlResult
import com.aicallassistant.app.domain.model.SimulateCallResult
import com.aicallassistant.app.domain.model.SpeakerType
import com.aicallassistant.app.domain.model.TranscriptItem
import com.aicallassistant.app.domain.model.UrgencyLevel

interface CallRepository {
    suspend fun simulateCall(callerName: String?, callerNumber: String): NetworkResult<SimulateCallResult>
    suspend fun getCalls(
        limit: Int = 20,
        offset: Int = 0,
        status: String? = null,
        search: String? = null,
        importantOnly: Boolean? = null
    ): NetworkResult<Pair<List<CallItem>, Int>>
    suspend fun getCallDetail(callId: String): NetworkResult<CallDetail>
    suspend fun toggleImportant(callId: String, isImportant: Boolean): NetworkResult<CallDetail>
    suspend fun updateCallStatus(callId: String, status: String): NetworkResult<CallDetail>
    suspend fun deleteCall(callId: String): NetworkResult<Boolean>
    suspend fun getTranscripts(callId: String): NetworkResult<List<TranscriptItem>>
    suspend fun getRecordingUrl(callId: String): NetworkResult<RecordingUrlResult>
}

class DefaultCallRepository(
    private val apiClient: ApiClient = ApiClient()
) : CallRepository {

    override suspend fun simulateCall(callerName: String?, callerNumber: String): NetworkResult<SimulateCallResult> {
        return when (val res = apiClient.simulateCall(callerName, callerNumber)) {
            is NetworkResult.Success -> NetworkResult.Success(
                SimulateCallResult(
                    callId = res.data.callId,
                    status = CallStatus.fromValue(res.data.status),
                    wsUrl = res.data.wsUrl,
                    createdAt = res.data.createdAt
                )
            )
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getCalls(
        limit: Int,
        offset: Int,
        status: String?,
        search: String?,
        importantOnly: Boolean?
    ): NetworkResult<Pair<List<CallItem>, Int>> {
        return when (val res = apiClient.getCalls(limit, offset, status, search, importantOnly)) {
            is NetworkResult.Success -> {
                val items = res.data.calls.map { dto ->
                    CallItem(
                        id = dto.id,
                        callerName = dto.callerName,
                        callerNumber = dto.callerNumber,
                        status = CallStatus.fromValue(dto.status),
                        callType = CallType.fromValue(dto.callType),
                        callerReason = dto.callerReason,
                        summary = dto.summary,
                        urgency = UrgencyLevel.fromValue(dto.urgency),
                        isImportant = dto.isImportant,
                        durationSeconds = dto.durationSeconds,
                        hasRecording = dto.hasRecording,
                        createdAt = dto.createdAt
                    )
                }
                NetworkResult.Success(Pair(items, res.data.total))
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getCallDetail(callId: String): NetworkResult<CallDetail> {
        return when (val res = apiClient.getCallDetail(callId)) {
            is NetworkResult.Success -> {
                val d = res.data
                val transcripts = d.transcripts.map { t ->
                    TranscriptItem(
                        id = t.id,
                        callId = t.callId,
                        speaker = SpeakerType.fromValue(t.speaker),
                        text = t.text,
                        timestampMs = t.timestampMs,
                        sequenceOrder = t.sequenceOrder,
                        createdAt = t.createdAt
                    )
                }
                val actionItems = d.actionItems.map { a ->
                    ActionItem(
                        id = a.id,
                        callId = a.callId,
                        task = a.task,
                        dueDate = a.dueDate,
                        completed = a.completed,
                        completedAt = a.completedAt,
                        createdAt = a.createdAt
                    )
                }
                val facts = d.keyFacts.mapNotNull {
                    it.toString().trim('"')
                }

                NetworkResult.Success(
                    CallDetail(
                        id = d.id,
                        callerName = d.callerName,
                        callerNumber = d.callerNumber,
                        status = CallStatus.fromValue(d.status),
                        callType = CallType.fromValue(d.callType),
                        callerReason = d.callerReason,
                        summary = d.summary,
                        urgency = UrgencyLevel.fromValue(d.urgency),
                        urgencyExplanation = d.urgencyExplanation,
                        uncertainties = d.uncertainties,
                        keyFacts = facts,
                        isImportant = d.isImportant,
                        durationSeconds = d.durationSeconds,
                        recordingPath = d.recordingPath,
                        recordingDurationSeconds = d.recordingDurationSeconds,
                        recordingSizeBytes = d.recordingSizeBytes,
                        hasRecording = d.hasRecording,
                        createdAt = d.createdAt,
                        startedAt = d.startedAt,
                        endedAt = d.endedAt,
                        transcripts = transcripts,
                        actionItems = actionItems
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun toggleImportant(callId: String, isImportant: Boolean): NetworkResult<CallDetail> {
        return when (val res = apiClient.patchCall(callId, CallUpdateDto(isImportant = isImportant))) {
            is NetworkResult.Success -> getCallDetail(callId)
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun updateCallStatus(callId: String, status: String): NetworkResult<CallDetail> {
        return when (val res = apiClient.patchCall(callId, CallUpdateDto(status = status))) {
            is NetworkResult.Success -> getCallDetail(callId)
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun deleteCall(callId: String): NetworkResult<Boolean> {
        return when (val res = apiClient.deleteCall(callId)) {
            is NetworkResult.Success -> NetworkResult.Success(res.data.deleted)
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getTranscripts(callId: String): NetworkResult<List<TranscriptItem>> {
        return when (val res = apiClient.getTranscripts(callId)) {
            is NetworkResult.Success -> {
                val list = res.data.transcripts.map { t ->
                    TranscriptItem(
                        id = t.id,
                        callId = t.callId,
                        speaker = SpeakerType.fromValue(t.speaker),
                        text = t.text,
                        timestampMs = t.timestampMs,
                        sequenceOrder = t.sequenceOrder,
                        createdAt = t.createdAt
                    )
                }
                NetworkResult.Success(list)
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun getRecordingUrl(callId: String): NetworkResult<RecordingUrlResult> {
        return when (val res = apiClient.getRecordingUrl(callId)) {
            is NetworkResult.Success -> NetworkResult.Success(
                RecordingUrlResult(
                    callId = res.data.callId,
                    signedUrl = res.data.signedUrl,
                    expiresAt = res.data.expiresAt,
                    recordingPath = res.data.recordingPath,
                    fileSizeBytes = res.data.fileSizeBytes
                )
            )
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
