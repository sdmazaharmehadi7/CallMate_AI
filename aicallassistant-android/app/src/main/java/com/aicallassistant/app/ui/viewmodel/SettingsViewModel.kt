package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.data.auth.DefaultAuthRepository
import com.aicallassistant.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val currentBackendUrl: String = AppConfig.getActiveBackendUrl(),
    val isSupabaseConfigured: Boolean = AppConfig.isSupabaseConfigured(),
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val authRepository: AuthRepository = DefaultAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            when (val res = authRepository.fetchProfile()) {
                is NetworkResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = res.data,
                        currentBackendUrl = AppConfig.getActiveBackendUrl(),
                        isSupabaseConfigured = AppConfig.isSupabaseConfigured()
                    )
                }
                is NetworkResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        currentBackendUrl = AppConfig.getActiveBackendUrl(),
                        isSupabaseConfigured = AppConfig.isSupabaseConfigured(),
                        errorMessage = res.message
                    )
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    fun updateBackendUrl(newUrl: String) {
        AppConfig.setCustomBackendUrl(newUrl)
        _uiState.value = _uiState.value.copy(currentBackendUrl = AppConfig.getActiveBackendUrl())
    }

    fun resetBackendUrlToDefault() {
        AppConfig.setCustomBackendUrl(null)
        _uiState.value = _uiState.value.copy(currentBackendUrl = AppConfig.getActiveBackendUrl())
    }

    fun signOut(onSignedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onSignedOut()
        }
    }
}
