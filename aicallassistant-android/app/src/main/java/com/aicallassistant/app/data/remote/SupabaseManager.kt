package com.aicallassistant.app.data.remote

import android.util.Log
import com.aicallassistant.app.core.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton managing Supabase client initialization, authentication lifecycle,
 * session persistence, and Bearer token retrieval.
 */
object SupabaseManager {
    private const val TAG = "SupabaseManager"
    private val scope = CoroutineScope(Dispatchers.Default)

    private var _client: SupabaseClient? = null
    val client: SupabaseClient? get() = _client

    private val _sessionState = MutableStateFlow<SessionStatus>(SessionStatus.NotAuthenticated(false))
    val sessionState: StateFlow<SessionStatus> = _sessionState.asStateFlow()

    fun initialize() {
        if (!AppConfig.isSupabaseConfigured()) {
            Log.w(TAG, "Supabase publishable key or URL not configured. Auth features require valid credentials.")
            return
        }

        try {
            _client = createSupabaseClient(
                supabaseUrl = AppConfig.supabaseUrl,
                supabaseKey = AppConfig.supabasePublishableKey
            ) {
                install(Auth) {
                    alwaysAutoRefresh = true
                }
            }

            // Observe session status
            _client?.let { sc ->
                _sessionState.value = sc.auth.sessionStatus.value
                scope.launch {
                    sc.auth.sessionStatus.collect { status ->
                        _sessionState.value = status
                    }
                }
            }
            Log.i(TAG, "Supabase client initialized successfully with project: ${AppConfig.supabaseUrl}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
        }
    }

    /**
     * Retrieves the active JWT access token for Bearer authorization in backend requests.
     */
    fun getAccessToken(): String? {
        val client = _client ?: return null
        return client.auth.currentAccessTokenOrNull()
    }

    fun getCurrentUser(): UserInfo? {
        val client = _client ?: return null
        return client.auth.currentUserOrNull()
    }

    suspend fun signUp(email: String, pass: String): Boolean {
        val client = _client ?: throw IllegalStateException(
            "Supabase is not configured. Please provide a valid publishable key in local.properties or AppConfig."
        )
        client.auth.signUpWith(Email) {
            this.email = email
            this.password = pass
        }
        val currentSession = client.auth.currentSessionOrNull()
        if (currentSession != null) {
            return true
        }
        // Attempt immediate sign-in in case email confirmation is not required by Supabase project
        return try {
            client.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }
            client.auth.currentSessionOrNull() != null
        } catch (_: Exception) {
            false
        }
    }

    suspend fun signIn(email: String, pass: String) {
        val client = _client ?: throw IllegalStateException(
            "Supabase is not configured. Please provide a valid publishable key in local.properties or AppConfig."
        )
        client.auth.signInWith(Email) {
            this.email = email
            this.password = pass
        }
    }

    suspend fun resetPassword(email: String) {
        val client = _client ?: throw IllegalStateException(
            "Supabase is not configured. Please provide a valid publishable key in local.properties or AppConfig."
        )
        client.auth.resetPasswordForEmail(email)
    }

    suspend fun signOut() {
        val client = _client ?: return
        try {
            client.auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error", e)
        }
    }
}
