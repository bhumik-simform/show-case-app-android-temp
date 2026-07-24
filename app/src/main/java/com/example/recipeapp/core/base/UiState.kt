package com.example.recipeapp.core.base

import com.example.recipeapp.core.network.NetworkErrorType
import com.example.recipeapp.data.recipes.options.FilterOption

enum class AuthField {
    UserName,
    Password,
    Name,
    Email,
    ConfirmPassword,
    Terms
}


sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(
        val message: String,
        val fieldErrors: Map<AuthField, String> = emptyMap(),
        val code: Int? = null,
        val type: NetworkErrorType = NetworkErrorType.UNKNOWN
    ) : UiState<Nothing>()
}

// Single source of truth for cuisine + diet chip filtering, shared by any screen that offers
// this kind of filtering (Home today, Search later). Bundling all three fields into one
// StateFlow<FilterState> instead of separate flows keeps updates atomic (a ViewModel can
// change selection + rebuild the chip list in one `update {}` call) and avoids the earlier
// bug class where the cuisine chips and the selected-set could briefly disagree.
data class FilterState(
    // Rows to render in the cuisine chip RecyclerView, "All" always first as a reset option.
    val cuisineChips: List<FilterOption> = listOf(FilterOption("All", isSelected = true)),
    // Cuisine names the user has toggled on; empty means "All" (no cuisine filter applied).
    val selectedCuisines: Set<String> = emptySet(),
    // Diet names chosen in DietFilterBottomSheet; empty means no diet filter applied.
    val selectedDiets: Set<String> = emptySet()
) {
    // True when either filter type has at least one selection — drives the active-filter
    // badge dot next to the filter button so the user can tell filtering is in effect.
    val isFilterActive: Boolean
        get() = selectedCuisines.isNotEmpty() || selectedDiets.isNotEmpty()
}