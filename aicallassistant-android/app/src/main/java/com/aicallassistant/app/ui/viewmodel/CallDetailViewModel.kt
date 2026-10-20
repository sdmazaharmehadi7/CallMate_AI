package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.event.AppCallEvent
import com.aicallassistant.app.core.event.CallEventBus
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.audio.LocalRecordingManager
import com.aicallassistant.app.data.repository.ActionItemRepository
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultActionItemRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.CallDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallDetailUiState(
    val isLoading: Boolean = true,
    val callDetail: CallDetail? = null,
    val signedRecordingUrl: String? = null,
    val isLoadingRecording: Boolean = false,
    val recordingErrorMessage: String? = null,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class CallDetailViewModel(
    private val callId: String,
    private val callRepository: CallRepository = DefaultCallRepository(),
    private val actionItemRepository: ActionItemRepository = DefaultActionItemRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallDetailUiState(isLoading = true))
    val uiState: StateFlow<CallDetailUiState> = _uiState.asStateFlow()

    init {
        loadCallDetail()
    }

    fun loadCallDetail() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = callRepository.getCallDetail(callId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        callDetail = res.data,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = res.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun toggleImportant() {
        val current = _uiState.value.callDetail ?: return
        viewModelScope.launch {
            val newState = !current.isImportant
            when (val res = callRepository.toggleImportant(callId, newState)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(callDetail = res.data)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun deleteCall(onDeleted: () -> Unit) {
        viewModelScope.launch {
            when (val res = callRepository.deleteCall(callId)) {
                is NetworkResult.Success -> {
                    LocalRecordingManager.deleteRecording(callId)
                    CallEventBus.emit(AppCallEvent.CallDeleted(callId))
                    _uiState.value = _uiState.value.copy(isDeleted = true)
                    onDeleted()
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun requestRecordingPlaybackUrl() {
        _uiState.value = _uiState.value.copy(isLoadingRecording = true, recordingErrorMessage = null)
        viewModelScope.launch {
            when (val res = callRepository.getRecordingUrl(callId)) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingRecording = false,
                        signedRecordingUrl = res.data.signedUrl
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoadingRecording = false,
                        recordingErrorMessage = res.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun toggleActionItem(actionItemId: String, currentCompleted: Boolean) {
        viewModelScope.launch {
            when (val res = actionItemRepository.toggleCompleted(actionItemId, !currentCompleted)) {
                is NetworkResult.Success -> {
                    val currentCall = _uiState.value.callDetail ?: return@launch
                    val updatedItems = currentCall.actionItems.map {
                        if (it.id == actionItemId) res.data else it
                    }
                    _uiState.value = _uiState.value.copy(
                        callDetail = currentCall.copy(actionItems = updatedItems)
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
