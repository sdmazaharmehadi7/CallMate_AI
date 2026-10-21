package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.event.AppCallEvent
import com.aicallassistant.app.core.event.CallEventBus
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.core.util.DateUtils
import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.data.auth.DefaultAuthRepository
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.ChatThreadItem
import com.aicallassistant.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ChatDateFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week")
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val recentCalls: List<CallItem> = emptyList(),
    val chatThreads: List<ChatThreadItem> = emptyList(),
    val filteredThreads: List<ChatThreadItem> = emptyList(),
    val totalCallsCount: Int = 0,
    val searchQuery: String = "",
    val selectedDateFilter: ChatDateFilter = ChatDateFilter.ALL,
    val isAssistantOn: Boolean = true,
    val mutedCallers: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val authRepository: AuthRepository = DefaultAuthRepository(),
    private val callRepository: CallRepository = DefaultCallRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeCallEvents()
    }

    private fun observeCallEvents() {
        viewModelScope.launch {
            CallEventBus.events.collect { event ->
                when (event) {
                    is AppCallEvent.CallCompleted, is AppCallEvent.CallDeleted -> {
                        loadDashboardData(isRefresh = true)
                    }
                    else -> {}
                }
            }
        }
    }

    fun loadDashboardData(isRefresh: Boolean = false) {
        if (!isRefresh) {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        }
        viewModelScope.launch {
            // Load user profile
            val profileResult = authRepository.fetchProfile()
            val profile = profileResult.getOrNull()

            // Fetch larger batch of calls to populate chat threads and grouping
            when (val callsResult = callRepository.getCalls(limit = 40, offset = 0)) {
                is NetworkResult.Success -> {
                    val (calls, total) = callsResult.data
                    val threads = ChatThreadItem.fromCalls(calls).map { thread ->
                        thread.copy(isMuted = _uiState.value.mutedCallers.contains(thread.callerNumber))
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = profile,
                        recentCalls = calls,
                        chatThreads = threads,
                        filteredThreads = applyFilters(
                            threads = threads,
                            query = _uiState.value.searchQuery,
                            dateFilter = _uiState.value.selectedDateFilter
                        ),
                        totalCallsCount = total,
                        errorMessage = null
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        userProfile = profile,
                        errorMessage = callsResult.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredThreads = applyFilters(
                threads = _uiState.value.chatThreads,
                query = query,
                dateFilter = _uiState.value.selectedDateFilter
            )
        )
    }

    fun onDateFilterSelect(filter: ChatDateFilter) {
        _uiState.value = _uiState.value.copy(
            selectedDateFilter = filter,
            filteredThreads = applyFilters(
                threads = _uiState.value.chatThreads,
                query = _uiState.value.searchQuery,
                dateFilter = filter
            )
        )
    }

    fun toggleAssistant(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAssistantOn = enabled)
    }

    fun toggleMuteCaller(callerNumber: String) {
        val currentMuted = _uiState.value.mutedCallers.toMutableSet()
        if (currentMuted.contains(callerNumber)) {
            currentMuted.remove(callerNumber)
        } else {
            currentMuted.add(callerNumber)
        }
        val updatedThreads = _uiState.value.chatThreads.map {
            if (it.callerNumber == callerNumber) it.copy(isMuted = currentMuted.contains(callerNumber)) else it
        }
        _uiState.value = _uiState.value.copy(
            mutedCallers = currentMuted,
            chatThreads = updatedThreads,
            filteredThreads = applyFilters(
                threads = updatedThreads,
                query = _uiState.value.searchQuery,
                dateFilter = _uiState.value.selectedDateFilter
            )
        )
    }

    private fun applyFilters(
        threads: List<ChatThreadItem>,
        query: String,
        dateFilter: ChatDateFilter
    ): List<ChatThreadItem> {
        return threads.filter { thread ->
            // Search query filter (matches name, phone number, or summary)
            val matchesQuery = query.isBlank() ||
                    thread.displayName.contains(query, ignoreCase = true) ||
                    thread.callerNumber.contains(query, ignoreCase = true) ||
                    (thread.latestSummary?.contains(query, ignoreCase = true) == true)

            if (!matchesQuery) return@filter false

            // Date filter
            val header = DateUtils.getRelativeDateHeader(thread.latestTimestamp)
            when (dateFilter) {
                ChatDateFilter.ALL -> true
                ChatDateFilter.TODAY -> header == "Today"
                ChatDateFilter.YESTERDAY -> header == "Yesterday"
                ChatDateFilter.THIS_WEEK -> header == "Today" || header == "Yesterday" ||
                        header in listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
            }
        }
    }
}
