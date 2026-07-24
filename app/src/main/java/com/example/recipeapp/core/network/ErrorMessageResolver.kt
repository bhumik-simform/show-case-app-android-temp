package com.example.recipeapp.core.network

// Single source of truth for turning a raw failure (type + optional HTTP code) into copy a
// user can actually act on. Every content-loading screen (Home/RecipeDetail/Search/Saved/
// Profile) goes through this via NetworkResult.toUiState() instead of showing the raw
// exception message, which was either too technical ("Unexpected response format") or, for
// HttpException, whatever string Retrofit/OkHttp happened to localize.
object ErrorMessageResolver {

    fun resolve(type: NetworkErrorType, code: Int?): String = when (type) {
        NetworkErrorType.NO_INTERNET ->
            "No internet connection. Please try again."

        NetworkErrorType.PARSING ->
            "There's an issue loading this data. Please check back after a while."

        NetworkErrorType.HTTP -> resolveHttp(code)

        NetworkErrorType.UNKNOWN ->
            "Something went wrong. Please try again after a while."
    }

    private fun resolveHttp(code: Int?): String {
        if (code == null) return "Something went wrong. Please try again after a while."
        return when (code) {
            // Spoonacular's quota-exhausted response. This should normally never reach the UI
            // as an error — FallbackRecipeRepository intercepts it and serves dummy data
            // instead — but a friendly fallback message is kept here in case that ever changes.
            402 -> "Something went wrong. Please try again after a while."
            401 -> "Your session isn't valid anymore. Please log in again."
            403 -> "You don't have permission to do that."
            404 -> "We couldn't find what you were looking for."
            429 -> "Too many requests. Please wait a moment and try again."
            in 400..499 -> "Something went wrong with that request. Please try again."
            in 500..599 -> "Something went wrong on our end. Please try again after a while."
            else -> "Something went wrong. Please try again after a while."
        }
    }
}
