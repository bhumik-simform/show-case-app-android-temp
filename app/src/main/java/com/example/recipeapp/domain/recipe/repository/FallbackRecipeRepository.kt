package com.example.recipeapp.domain.recipe.repository

import com.example.recipeapp.core.network.NetworkResult
import com.example.recipeapp.data.recipes.uimodel.PaginatedRecipes
import com.example.recipeapp.data.recipes.uimodel.RecipeCardUiModel
import com.example.recipeapp.data.recipes.uimodel.RecipeDetailUiModel

// Spoonacular's free-tier response code when the daily request quota is exhausted.
private const val QUOTA_EXCEEDED_CODE = 402

// Decorator over RecipeRepository: tries the real network impl first, and only
// switches to the bundled-JSON dummy impl when the network call fails specifically
// with a 402 (quota exhausted) — any other error is surfaced as-is, so real failures
// (no internet, 500s, etc.) are never masked by fallback data.
class FallbackRecipeRepository(
    private val remote: RecipeRepository,
    private val dummy: RecipeRepository
) : RecipeRepository {

    // Shared by every network-backed method below so the 402-fallback rule
    // lives in exactly one place instead of being repeated per call.
    private suspend fun <T> withFallback(
        remoteCall: suspend () -> NetworkResult<T>,
        dummyCall: suspend () -> NetworkResult<T>
    ): NetworkResult<T> {
        val result = remoteCall()
        return if (result is NetworkResult.Error && result.code == QUOTA_EXCEEDED_CODE) {
            DummyDataSignal.notifyDummyDataShown()
            dummyCall()
        } else {
            result
        }
    }

    // --- Network-backed reads, routed through withFallback ---
    override suspend fun getExploreRecipes(
        cuisine: String?,
        diet: String?,
        offset: Int
    ): NetworkResult<PaginatedRecipes> = withFallback(
        remoteCall = { remote.getExploreRecipes(cuisine, diet, offset) },
        dummyCall = { dummy.getExploreRecipes(cuisine, diet, offset) }
    )

    override suspend fun getRecipesByIds(
        ids: List<Int>
    ): NetworkResult<List<RecipeCardUiModel>> = withFallback(
        remoteCall = { remote.getRecipesByIds(ids) },
        dummyCall = { dummy.getRecipesByIds(ids) }
    )

    override suspend fun getSavedRecipes(): NetworkResult<List<RecipeCardUiModel>> = withFallback(
        remoteCall = { remote.getSavedRecipes() },
        dummyCall = { dummy.getSavedRecipes() }
    )

    override suspend fun searchRecipes(
        query: String,
        diet: String?
    ): NetworkResult<List<RecipeCardUiModel>> = withFallback(
        remoteCall = { remote.searchRecipes(query, diet) },
        dummyCall = { dummy.searchRecipes(query, diet) }
    )

    override suspend fun getRecipeDetail(recipeId: Int): NetworkResult<RecipeDetailUiModel> = withFallback(
        remoteCall = { remote.getRecipeDetail(recipeId) },
        dummyCall = { dummy.getRecipeDetail(recipeId) }
    )

    // Local-only operations never touch network — no fallback needed, remote/dummy are equivalent here.
    override fun toggleSavedRecipe(recipeId: Int, recipeName: String?, recipeImageUrl: String?) =
        remote.toggleSavedRecipe(recipeId, recipeName, recipeImageUrl)

    override suspend fun removeSavedRecipe(recipeId: Int, recipeName: String?, recipeImageUrl: String?): NetworkResult<Unit> =
        remote.removeSavedRecipe(recipeId, recipeName, recipeImageUrl)

    override fun isRecipeSaved(recipeId: Int): Boolean = remote.isRecipeSaved(recipeId)

    override fun getCuisines(): List<String> = remote.getCuisines()

    override fun getUserName(): String = remote.getUserName()
}
