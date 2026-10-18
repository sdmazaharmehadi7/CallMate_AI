package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.CallItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CallHistoryUiState(
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val calls: List<CallItem> = emptyList(),
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val selectedStatus: String? = null,
    val importantOnly: Boolean = false,
    val errorMessage: String? = null
)

class CallHistoryViewModel(
    private val callRepository: CallRepository = DefaultCallRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallHistoryUiState(isLoading = true))
    val uiState: StateFlow<CallHistoryUiState> = _uiState.asStateFlow()

    private val pageSize = 20
    private var currentOffset = 0

    init {
        loadCalls(reset = true)
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadCalls(reset = true)
    }

    fun onStatusFilterChange(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        loadCalls(reset = true)
    }

    fun onImportantFilterToggle() {
        _uiState.value = _uiState.value.copy(importantOnly = !_uiState.value.importantOnly)
        loadCalls(reset = true)
    }

    fun loadCalls(reset: Boolean = false) {
        if (reset) {
            currentOffset = 0
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        } else {
            if (_uiState.value.isPaginating || _uiState.value.calls.size >= _uiState.value.totalCount) {
                return
            }
            _uiState.value = _uiState.value.copy(isPaginating = true)
        }

        val state = _uiState.value
        val search = state.searchQuery.ifBlank { null }
        val status = state.selectedStatus
        val important = if (state.importantOnly) true else null

        viewModelScope.launch {
            val result = callRepository.getCalls(
                limit = pageSize,
                offset = currentOffset,
                status = status,
                search = search,
                importantOnly = important
            )

            when (result) {
                is NetworkResult.Success -> {
                    val (newCalls, total) = result.data
                    val updatedList = if (reset) newCalls else _uiState.value.calls + newCalls
                    currentOffset = updatedList.size
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPaginating = false,
                        calls = updatedList,
                        totalCount = total,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isPaginating = false,
                        errorMessage = result.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun toggleImportant(call: CallItem) {
        viewModelScope.launch {
            val newImportantState = !call.isImportant
            when (val res = callRepository.toggleImportant(call.id, newImportantState)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.calls.map {
                        if (it.id == call.id) it.copy(isImportant = newImportantState) else it
                    }
                    _uiState.value = _uiState.value.copy(calls = updated)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
