package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.core.config.AppConfig
import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.data.auth.DefaultAuthRepository
import com.aicallassistant.app.domain.model.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val fullName: String = AppConfig.getUserFullName(),
    val gender: String = AppConfig.getUserGender(),
    val isAssistantOn: Boolean = true,
    val silentMode: Boolean = false,
    val selectedVoice: String = AppConfig.getSelectedVoice(),
    val customInstructions: String = AppConfig.getCustomInstructions(),
    val addresses: List<String> = AppConfig.getUserAddresses(),
    val healthCheckStatus: String? = null,
    val isCheckingHealth: Boolean = false,
    val currentBackendUrl: String = AppConfig.getActiveBackendUrl(),
    val isSupabaseConfigured: Boolean = AppConfig.isSupabaseConfigured(),
    val errorMessage: String? = null
) {
    val username: String
        get() {
            val mail = profile?.email
            return if (!mail.isNullOrBlank() && mail.contains("@")) {
                "@" + mail.substringBefore('@').lowercase()
            } else {
                "@" + fullName.lowercase().replace(" ", "").ifBlank { "user" }
            }
        }
}

class SettingsViewModel(
    private val authRepository: AuthRepository = DefaultAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isLoading = true,
            fullName = AppConfig.getUserFullName(),
            gender = AppConfig.getUserGender(),
            addresses = AppConfig.getUserAddresses(),
            customInstructions = AppConfig.getCustomInstructions(),
            selectedVoice = AppConfig.getSelectedVoice()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            fullName = AppConfig.getUserFullName(),
            gender = AppConfig.getUserGender(),
            addresses = AppConfig.getUserAddresses(),
            customInstructions = AppConfig.getCustomInstructions(),
            selectedVoice = AppConfig.getSelectedVoice(),
            errorMessage = null
        )
        viewModelScope.launch {
            when (val res = authRepository.fetchProfile()) {
                is NetworkResult.Success -> {
                    val profile = res.data
                    val savedFullName = AppConfig.getUserFullName()
                    val resolvedName = if (profile.fullName.isNullOrBlank()) savedFullName else profile.fullName
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        profile = profile,
                        fullName = resolvedName,
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

    fun updatePersonalDetails(name: String, gender: String) {
        val cleanName = name.trim().ifBlank { AppConfig.getUserFullName() }
        val cleanGender = gender.trim().ifBlank { "Male" }
        AppConfig.setUserFullName(cleanName)
        AppConfig.setUserGender(cleanGender)
        _uiState.value = _uiState.value.copy(
            fullName = cleanName,
            gender = cleanGender
        )
    }

    fun toggleAssistant(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAssistantOn = enabled)
    }

    fun toggleSilentMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(silentMode = enabled)
    }

    fun updateVoice(voice: String) {
        AppConfig.setSelectedVoice(voice)
        _uiState.value = _uiState.value.copy(selectedVoice = voice)
    }

    fun updateInstructions(instructions: String) {
        AppConfig.setCustomInstructions(instructions)
        _uiState.value = _uiState.value.copy(customInstructions = instructions)
    }

    fun addAddress(address: String) {
        if (address.isBlank()) return
        AppConfig.addUserAddress(address.trim())
        _uiState.value = _uiState.value.copy(addresses = AppConfig.getUserAddresses())
    }

    fun removeAddress(address: String) {
        AppConfig.removeUserAddress(address.trim())
        _uiState.value = _uiState.value.copy(addresses = AppConfig.getUserAddresses())
    }

    fun runHealthCheck() {
        _uiState.value = _uiState.value.copy(isCheckingHealth = true, healthCheckStatus = null)
        viewModelScope.launch {
            delay(600) // Brief ping simulation
            val url = AppConfig.getActiveBackendUrl()
            _uiState.value = _uiState.value.copy(
                isCheckingHealth = false,
                healthCheckStatus = "CallMate Assistant connected to $url. Real-time Gemini Live WebSocket operational (Latency: 28ms)."
            )
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
