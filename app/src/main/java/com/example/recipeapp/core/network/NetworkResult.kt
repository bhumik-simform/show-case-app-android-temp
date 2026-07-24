package com.example.recipeapp.core.network

// Distinguishes *why* a call failed so the UI layer can pick a genuinely different message
// (and retry affordance) instead of every failure collapsing into one generic string.
enum class NetworkErrorType {
    NO_INTERNET,
    HTTP,
    PARSING,
    UNKNOWN
}

sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val type: NetworkErrorType = NetworkErrorType.UNKNOWN
    ) : NetworkResult<Nothing>()
    object Loading : NetworkResult<Nothing>()
}