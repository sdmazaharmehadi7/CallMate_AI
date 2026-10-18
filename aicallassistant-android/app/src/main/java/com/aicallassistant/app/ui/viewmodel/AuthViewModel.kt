package com.aicallassistant.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aicallassistant.app.data.auth.AuthRepository
import com.aicallassistant.app.data.auth.DefaultAuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val userId: String) : AuthUiState
    data class Error(val message: String) : AuthUiState
    data class PasswordResetSent(val email: String) : AuthUiState
    data class SignUpSuccess(val email: String, val requiresConfirmation: Boolean) : AuthUiState
}

class AuthViewModel(
    private val authRepository: AuthRepository = DefaultAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Monitor session status
        viewModelScope.launch {
            authRepository.sessionState.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _uiState.value = AuthUiState.Authenticated(status.session.user?.id ?: "")
                    }
                    is SessionStatus.NotAuthenticated -> {
                        if (_uiState.value !is AuthUiState.Error && _uiState.value !is AuthUiState.Loading) {
                            _uiState.value = AuthUiState.Idle
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun signIn(email: String, pass: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signIn(trimmedEmail, pass)
            result.fold(
                onSuccess = {
                    // SessionStatus collector handles navigation state
                },
                onFailure = { err ->
                    _uiState.value = AuthUiState.Error(err.localizedMessage ?: "Failed to sign in. Please verify your credentials.")
                }
            )
        }
    }

    fun signUp(email: String, pass: String, confirmPass: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }
        if (pass.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters.")
            return
        }
        if (pass != confirmPass) {
            _uiState.value = AuthUiState.Error("Passwords do not match.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.signUp(trimmedEmail, pass)
            result.fold(
                onSuccess = { autoLoggedIn ->
                    if (autoLoggedIn) {
                        val currentUserId = com.aicallassistant.app.data.remote.SupabaseManager.getCurrentUser()?.id ?: ""
                        _uiState.value = AuthUiState.Authenticated(currentUserId)
                    } else {
                        _uiState.value = AuthUiState.SignUpSuccess(trimmedEmail, requiresConfirmation = true)
                    }
                },
                onFailure = { err ->
                    _uiState.value = AuthUiState.Error(err.localizedMessage ?: "Registration failed. Please try again.")
                }
            )
        }
    }

    fun resetPassword(email: String) {
        val trimmedEmail = email.trim()
        if (!isValidEmail(trimmedEmail)) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            val result = authRepository.resetPassword(trimmedEmail)
            result.fold(
                onSuccess = {
                    _uiState.value = AuthUiState.PasswordResetSent(trimmedEmail)
                },
                onFailure = { err ->
                    _uiState.value = AuthUiState.Error(err.localizedMessage ?: "Failed to send password reset email.")
                }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    fun resetToIdle() {
        _uiState.value = AuthUiState.Idle
    }

    private val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && emailRegex.matches(email)
    }
}
