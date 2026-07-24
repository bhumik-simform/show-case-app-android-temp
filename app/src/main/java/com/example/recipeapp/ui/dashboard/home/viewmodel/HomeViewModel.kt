package com.example.recipeapp.ui.dashboard.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.recipeapp.core.base.FilterState
import com.example.recipeapp.core.base.UiState
import com.example.recipeapp.core.network.NetworkResult
import com.example.recipeapp.core.network.toUiState
import com.example.recipeapp.storage.session.SessionStorage
import com.example.recipeapp.domain.recipe.repository.RecipeRepository
import com.example.recipeapp.data.recipes.options.FilterOption
import com.example.recipeapp.data.recipes.uimodel.RecipeCardUiModel
import com.example.recipeapp.data.recipes.options.toFilterOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Backs the Home screen: explore/saved recipe lists, pagination, and cuisine+diet filtering.
// Survives fragment recreation (config changes, backgrounding) since it's scoped to the
// Fragment's lifecycle via Koin, which is what actually keeps filter selections in place —
// the ChipGroup rendering bug fixed in DietFilterBottomSheet only masked that this state was
// already being held correctly.
class HomeViewModel(
    private val recipeRepository: RecipeRepository,
    private val sessionManager: SessionStorage
) : ViewModel() {

    private val _exploreUiState = MutableStateFlow<UiState<List<RecipeCardUiModel>>>(UiState.Idle)
    val exploreUiState: StateFlow<UiState<List<RecipeCardUiModel>>> = _exploreUiState

    private val _savedUiState = MutableStateFlow<UiState<List<RecipeCardUiModel>>>(UiState.Idle)
    val savedUiState: StateFlow<UiState<List<RecipeCardUiModel>>> = _savedUiState

    // Lazily initialize to avoid doing work (or throwing) during ViewModel construction in Koin
    private val cuisineOptions: List<String> by lazy {
        try {
            recipeRepository.getCuisines()
        } catch (_: Exception) {
            emptyList()
        }
    }

    val userName: String by lazy {
        try {
            sessionManager.getUserName()
        } catch (_: Exception) {
            ""
        }
    }

    // Cuisine chips: "All" is a virtual reset option, never part of the selected set
    private val selectableCuisines: List<String> by lazy { cuisineOptions.filterNot { it == "All" } }

    // Single StateFlow holding cuisine chips + both filter selections (see FilterState) so
    // HomeFragment only needs to collect one flow to keep chips, selections, and the
    // active-filter badge all in sync.
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState

    // Explore-list pagination bookkeeping. Plain vars (not part of FilterState) since they're
    // pure implementation detail of loadNextExplorePage and never rendered directly.
    private var currentOffset = 0
    private var isLastPage = false
    private var isLoadingMore = false
    private val exploreItems = mutableListOf<RecipeCardUiModel>()

    init {
        // cuisineOptions/selectableCuisines aren't ready until first access, so the chip list
        // in the default FilterState() needs to be (re)built once construction completes.
        _filterState.update { it.copy(cuisineChips = buildCuisineChips(it.selectedCuisines)) }
    }

    // Rebuilds the chip row from the master cuisine list plus whichever cuisines are
    // currently selected — "All" is checked only when selectedCuisines is empty.
    private fun buildCuisineChips(selectedCuisines: Set<String>): List<FilterOption> {
        val chips = mutableListOf(FilterOption("All", selectedCuisines.isEmpty()))
        chips.addAll(selectableCuisines.toFilterOptions(selectedCuisines))
        return chips
    }

    // Multi-select toggle for a single cuisine chip tap: tapping "All" clears every cuisine
    // selection; tapping an already-selected cuisine deselects just that one; otherwise it's
    // added to the selection. Re-fetches the explore list whenever the selection changes.
    fun toggleFilter(cuisine: String) {
        val current = _filterState.value.selectedCuisines
        val newSelection = when {
            cuisine == "All" -> emptySet()
            cuisine in current -> current - cuisine
            else -> current + cuisine
        }
        if (newSelection == current) return
        _filterState.update {
            it.copy(selectedCuisines = newSelection, cuisineChips = buildCuisineChips(newSelection))
        }
        loadInitial()
    }

    // Called with the full checked-diet list from DietFilterBottomSheet's Apply button.
    fun applyDietFilter(diets: List<String>) {
        val newSelection = diets.toSet()
        if (newSelection == _filterState.value.selectedDiets) return
        _filterState.update { it.copy(selectedDiets = newSelection) }
        loadInitial()
    }

    // Resets pagination and re-fetches both lists from page 0 — used on first load and
    // whenever the cuisine/diet filter selection changes.
    fun loadInitial() {
        currentOffset = 0
        isLastPage = false
        exploreItems.clear()
        loadNextExplorePage()
        loadSavedRecipes()
    }

    // Fetches the next page of explore recipes for the current filter selection, appending
    // to the running list. Guarded by isLoadingMore/isLastPage so pagination-scroll triggers
    // (see HomeFragment's paginationScrollListener) can't fire overlapping requests.
    fun loadNextExplorePage() {
        if (isLoadingMore || isLastPage) return
        isLoadingMore = true
        viewModelScope.launch {
            if (exploreItems.isEmpty()) {
                _exploreUiState.value = UiState.Loading
            }
            val cuisine = _filterState.value.selectedCuisines.takeIf { it.isNotEmpty() }?.joinToString(",")
            val diet = _filterState.value.selectedDiets.takeIf { it.isNotEmpty() }?.joinToString(",")
            when (val result = recipeRepository.getExploreRecipes(cuisine, diet, currentOffset)) {
                is NetworkResult.Success -> {
                    val page = result.data
                    exploreItems.addAll(page.results)
                    currentOffset += page.number
                    isLastPage = currentOffset >= page.totalResults
                    _exploreUiState.value = UiState.Success(exploreItems.toList())
                }
                is NetworkResult.Error -> {
                    _exploreUiState.value = result.toUiState()
                }
                is NetworkResult.Loading -> Unit
            }
            isLoadingMore = false
        }
    }

    fun loadSavedRecipes() {
        viewModelScope.launch {
            _savedUiState.value = UiState.Loading
            when (val result = recipeRepository.getSavedRecipes()) {
                is NetworkResult.Success -> {
                    _savedUiState.value = UiState.Success(result.data)
                }
                is NetworkResult.Error -> {
                    _savedUiState.value = result.toUiState()
                }
                is NetworkResult.Loading -> Unit
            }
        }
    }

    // Optimistically flips the saved flag on the tapped explore-list item so the UI reacts
    // immediately, then reloads the saved-recipes section to reflect the change there too.
    fun onSaveToggled(recipeId: Int) {
        val tapped = exploreItems.firstOrNull { it.id == recipeId }
        recipeRepository.toggleSavedRecipe(recipeId, tapped?.title, tapped?.imageUrl)
        val updatedIndex = exploreItems.indexOfFirst { it.id == recipeId }
        if (updatedIndex != -1) {
            exploreItems[updatedIndex] = exploreItems[updatedIndex].copy(
                isSaved = !exploreItems[updatedIndex].isSaved
            )
            _exploreUiState.value = UiState.Success(exploreItems.toList())
        }
        loadSavedRecipes()
    }
}
