package com.aicallassistant.app.data.remote

import android.util.Log
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.core.network.NetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.io.IOException

class ApiClient(
    private val tokenProvider: () -> String? = { SupabaseManager.getAccessToken() },
    private val baseUrlProvider: () -> String = { AppConfig.getActiveBackendUrl() }
) {
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
            tokenProvider()?.let { token ->
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    private suspend inline fun <reified T> safeRequest(block: () -> HttpResponse): NetworkResult<T> {
        return try {
            val response = block()
            if (response.status.isSuccess()) {
                val data = response.body<T>()
                NetworkResult.Success(data)
            } else {
                val errorBody = response.bodyAsText()
                val parsedError = try {
                    jsonConfig.decodeFromString<ErrorResponseDto>(errorBody).error
                } catch (_: Exception) {
                    null
                }
                val code = parsedError?.code ?: response.status.description
                val message = parsedError?.message ?: "Request failed with HTTP ${response.status.value}"
                NetworkResult.Error(
                    message = message,
                    code = code,
                    statusCode = response.status.value
                )
            }
        } catch (e: IOException) {
            Log.e("ApiClient", "Network connectivity error", e)
            NetworkResult.Error(
                message = "Unable to connect to server at ${baseUrlProvider()}. Check network or IP settings.",
                code = "NETWORK_ERROR"
            )
        } catch (e: Exception) {
            Log.e("ApiClient", "Unexpected request error", e)
            NetworkResult.Error(
                message = e.localizedMessage ?: "Unexpected error occurred",
                code = "UNKNOWN_ERROR"
            )
        }
    }

    suspend fun getMe(): NetworkResult<UserProfileDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/auth/me")
        }
    }

    suspend fun simulateCall(callerName: String?, callerNumber: String): NetworkResult<SimulateCallResponseDto> {
        return safeRequest {
            httpClient.post("${baseUrlProvider()}/api/v1/calls/simulate") {
                setBody(SimulateCallDto(callerName = callerName, callerNumber = callerNumber))
            }
        }
    }

    suspend fun getCalls(
        limit: Int = 20,
        offset: Int = 0,
        status: String? = null,
        search: String? = null,
        importantOnly: Boolean? = null
    ): NetworkResult<CallListResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls") {
                parameter("limit", limit)
                parameter("offset", offset)
                status?.let { parameter("status", it) }
                search?.let { parameter("search", it) }
                importantOnly?.let { parameter("important_only", it) }
            }
        }
    }

    suspend fun getCallDetail(callId: String): NetworkResult<CallDetailResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls/$callId")
        }
    }

    suspend fun patchCall(callId: String, update: CallUpdateDto): NetworkResult<CallDetailResponseDto> {
        return safeRequest {
            httpClient.patch("${baseUrlProvider()}/api/v1/calls/$callId") {
                setBody(update)
            }
        }
    }

    suspend fun deleteCall(callId: String): NetworkResult<DeleteResponseDto> {
        return safeRequest {
            httpClient.delete("${baseUrlProvider()}/api/v1/calls/$callId")
        }
    }

    suspend fun getTranscripts(callId: String): NetworkResult<TranscriptListResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls/$callId/transcripts")
        }
    }

    suspend fun getSummary(callId: String): NetworkResult<CallSummaryResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls/$callId/summary")
        }
    }

    suspend fun getRecordingUrl(callId: String): NetworkResult<SignedUrlResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls/$callId/recording-url")
        }
    }

    suspend fun getActionItems(callId: String): NetworkResult<ActionItemListResponseDto> {
        return safeRequest {
            httpClient.get("${baseUrlProvider()}/api/v1/calls/$callId/action-items")
        }
    }

    suspend fun createActionItem(callId: String, task: String, dueDate: String? = null): NetworkResult<ActionItemDto> {
        return safeRequest {
            httpClient.post("${baseUrlProvider()}/api/v1/calls/$callId/action-items") {
                setBody(CreateActionItemDto(task = task, dueDate = dueDate))
            }
        }
    }

    suspend fun patchActionItem(actionItemId: String, update: UpdateActionItemDto): NetworkResult<ActionItemDto> {
        return safeRequest {
            httpClient.patch("${baseUrlProvider()}/api/v1/action-items/$actionItemId") {
                setBody(update)
            }
        }
    }

    suspend fun deleteActionItem(actionItemId: String): NetworkResult<DeleteResponseDto> {
        return safeRequest {
            httpClient.delete("${baseUrlProvider()}/api/v1/action-items/$actionItemId")
        }
    }
}
