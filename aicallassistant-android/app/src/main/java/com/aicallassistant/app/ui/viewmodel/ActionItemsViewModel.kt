package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.event.AppCallEvent
import com.aicallassistant.app.core.event.CallEventBus
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.repository.ActionItemRepository
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultActionItemRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.ActionItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActionItemsUiState(
    val isLoading: Boolean = false,
    val actionItems: List<ActionItem> = emptyList(),
    val errorMessage: String? = null
)

class ActionItemsViewModel(
    private val callRepository: CallRepository = DefaultCallRepository(),
    private val actionItemRepository: ActionItemRepository = DefaultActionItemRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionItemsUiState(isLoading = true))
    val uiState: StateFlow<ActionItemsUiState> = _uiState.asStateFlow()

    init {
        loadActionItems()
        viewModelScope.launch {
            CallEventBus.events.collect { event ->
                when (event) {
                    is AppCallEvent.CallCompleted, is AppCallEvent.CallDeleted, is AppCallEvent.ActionItemUpdated -> {
                        loadActionItems()
                    }
                }
            }
        }
    }

    fun loadActionItems() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // First fetch the latest calls to collect all action items
            when (val callsRes = callRepository.getCalls(limit = 50, offset = 0)) {
                is NetworkResult.Success -> {
                    val calls = callsRes.data.first
                    val aggregatedItems = mutableListOf<ActionItem>()
                    for (c in calls) {
                        when (val itemsRes = actionItemRepository.getActionItems(c.id)) {
                            is NetworkResult.Success -> aggregatedItems.addAll(itemsRes.data)
                            else -> {}
                        }
                    }
                    _uiState.value = ActionItemsUiState(
                        isLoading = false,
                        actionItems = aggregatedItems.sortedByDescending { it.createdAt },
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = callsRes.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun toggleCompleted(item: ActionItem) {
        viewModelScope.launch {
            val newState = !item.completed
            when (val res = actionItemRepository.toggleCompleted(item.id, newState)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.actionItems.map {
                        if (it.id == item.id) res.data else it
                    }
                    _uiState.value = _uiState.value.copy(actionItems = updated)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun deleteActionItem(actionItemId: String) {
        viewModelScope.launch {
            when (val res = actionItemRepository.deleteActionItem(actionItemId)) {
                is NetworkResult.Success -> {
                    val updated = _uiState.value.actionItems.filter { it.id != actionItemId }
                    _uiState.value = _uiState.value.copy(actionItems = updated)
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(errorMessage = res.message)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }
}
