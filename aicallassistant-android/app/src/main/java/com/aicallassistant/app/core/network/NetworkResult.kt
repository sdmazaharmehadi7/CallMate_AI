package com.aicallassistant.app.core.network

/**
 * Standardized network response wrapper for safe UI consumption.
 */
sealed interface NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>
    data class Error(
        val message: String,
        val code: String? = null,
        val statusCode: Int? = null
    ) : NetworkResult<Nothing>
    data object Loading : NetworkResult<Nothing>

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    fun getOrNull(): T? = (this as? Success)?.data
}
