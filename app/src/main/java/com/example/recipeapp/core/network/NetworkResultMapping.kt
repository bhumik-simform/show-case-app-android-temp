package com.example.recipeapp.core.network

import com.example.recipeapp.core.base.UiState

// Maps a NetworkResult to UiState using ErrorMessageResolver's user-friendly copy instead of
// the raw exception-derived message. Used by content-loading ViewModels (Home, RecipeDetail,
// Search, Saved, Profile's recipes tab). Auth flows (Login/Signup) keep their own manual
// `UiState.Error(result.message)` mapping since the backend's own messages there (e.g.
// "Invalid credentials") are more specific than a generic resolver could be.
fun <T> NetworkResult<T>.toUiState(): UiState<T> = when (this) {
    is NetworkResult.Success -> UiState.Success(data)
    is NetworkResult.Loading -> UiState.Loading
    is NetworkResult.Error -> UiState.Error(
        message = ErrorMessageResolver.resolve(type, code),
        code = code,
        type = type
    )
}
