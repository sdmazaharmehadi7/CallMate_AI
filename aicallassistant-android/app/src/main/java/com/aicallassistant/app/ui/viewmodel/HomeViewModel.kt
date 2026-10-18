package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.data.auth.DefaultAuthRepository
import com.aicallassistant.app.data.repository.CallRepository
import com.aicallassistant.app.data.repository.DefaultCallRepository
import com.aicallassistant.app.domain.model.CallItem
import com.aicallassistant.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val userProfile: UserProfile? = null,
    val recentCalls: List<CallItem> = emptyList(),
    val totalCallsCount: Int = 0,
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
    }

    fun loadDashboardData() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            // Load user profile
            val profileResult = authRepository.fetchProfile()
            val profile = profileResult.getOrNull()

            // Load recent calls
            when (val callsResult = callRepository.getCalls(limit = 5, offset = 0)) {
                is NetworkResult.Success -> {
                    val (calls, total) = callsResult.data
                    _uiState.value = HomeUiState(
                        isLoading = false,
                        userProfile = profile,
                        recentCalls = calls,
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
}
