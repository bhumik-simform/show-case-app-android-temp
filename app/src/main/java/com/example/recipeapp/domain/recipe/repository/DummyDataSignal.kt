package com.example.recipeapp.domain.recipe.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

// Fired by FallbackRecipeRepository whenever it serves bundled dummy data instead of a live
// network response (Spoonacular quota exhausted). Repositories shouldn't reach into Android
// UI directly, so this is a plain event bus the UI layer observes to show a one-shot
// "Dummy data is showing" toast without the repository needing a Context.
object DummyDataSignal {
    // extraBufferCapacity = 1 so notify() never silently drops if no collector is attached yet.
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events

    fun notifyDummyDataShown() {
        _events.tryEmit(Unit)
    }
}
