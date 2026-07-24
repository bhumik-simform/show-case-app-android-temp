package com.example.recipeapp.ui.recipeDetail.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeapp.core.base.UiState
import com.example.recipeapp.core.network.NetworkResult
import com.example.recipeapp.core.network.toUiState
import com.example.recipeapp.domain.recipe.repository.RecipeRepository
import com.example.recipeapp.data.recipes.uimodel.RecipeDetailUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val recipeRepository: RecipeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<RecipeDetailUiModel>>(UiState.Loading)
    val uiState: StateFlow<UiState<RecipeDetailUiModel>> = _uiState

    private val _targetServings = MutableStateFlow(MIN_SERVINGS)
    val targetServings: StateFlow<Int> = _targetServings

    fun loadRecipeDetail(recipeId: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = recipeRepository.getRecipeDetail(recipeId)
            if (result is NetworkResult.Success) {
                _targetServings.value = result.data.servings.coerceAtLeast(MIN_SERVINGS)
            }
            _uiState.value = result.toUiState()
        }
    }

    fun incrementServings() {
        _targetServings.value = (_targetServings.value + 1).coerceAtMost(MAX_SERVINGS)
    }

    fun decrementServings() {
        _targetServings.value = (_targetServings.value - 1).coerceAtLeast(MIN_SERVINGS)
    }

    fun onSaveToggled() {
        val currentState = _uiState.value
        if (currentState !is UiState.Success) return

        recipeRepository.toggleSavedRecipe(currentState.data.id, currentState.data.title, currentState.data.imageUrl)
        _uiState.value = UiState.Success(currentState.data.copy(isSaved = !currentState.data.isSaved))
    }

    private companion object {
        const val MIN_SERVINGS = 1
        const val MAX_SERVINGS = 10
    }
}
