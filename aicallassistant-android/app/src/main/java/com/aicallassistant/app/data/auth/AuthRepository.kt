package com.aicallassistant.app.data.auth

import com.aicallassistant.app.core.network.NetworkResult
import com.aicallassistant.app.data.remote.ApiClient
import com.aicallassistant.app.data.remote.SupabaseManager
import com.aicallassistant.app.domain.model.UserProfile
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionStatus>
    val isAuthenticated: Boolean
    suspend fun signUp(email: String, pass: String): Result<Boolean>
    suspend fun signIn(email: String, pass: String): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
    suspend fun signOut()
    suspend fun fetchProfile(): NetworkResult<UserProfile>
}

class DefaultAuthRepository(
    private val apiClient: ApiClient = ApiClient()
) : AuthRepository {

    override val sessionState: StateFlow<SessionStatus>
        get() = SupabaseManager.sessionState

    override val isAuthenticated: Boolean
        get() = SupabaseManager.getAccessToken() != null

    override suspend fun signUp(email: String, pass: String): Result<Boolean> {
        return try {
            val autoLoggedIn = SupabaseManager.signUp(email, pass)
            Result.success(autoLoggedIn)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, pass: String): Result<Unit> {
        return try {
            SupabaseManager.signIn(email, pass)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            SupabaseManager.resetPassword(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        SupabaseManager.signOut()
    }

    override suspend fun fetchProfile(): NetworkResult<UserProfile> {
        return when (val res = apiClient.getMe()) {
            is NetworkResult.Success -> {
                NetworkResult.Success(
                    UserProfile(
                        id = res.data.id,
                        email = res.data.email,
                        fullName = res.data.fullName,
                        role = res.data.role,
                        createdAt = res.data.createdAt
                    )
                )
            }
            is NetworkResult.Error -> NetworkResult.Error(res.message, res.code, res.statusCode)
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }
}
