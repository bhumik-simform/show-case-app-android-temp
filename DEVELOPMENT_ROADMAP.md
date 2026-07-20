# RecipeApp Development Roadmap

> Last updated: 2026-07-19  
> Status: Phase 0-1 branches ready for merge; Phase 2 planning complete

---

## Overview

This document outlines the ordered development phases for completing the RecipeApp MVP and enhancing user experience. Work is organized into logical phases with clear merge dependencies.

---

## Phase 0 — Merge Core Infrastructure (No New Code)

**Goal:** Integrate the foundational layers that all features depend on.

### Steps
1. **Merge `feature/core-infra` → `development`**
   - 5 commits: auth token-refresh, recipe data layer, Home adaptation, shared utilities
   - ✅ Builds standalone
   - All subsequent branches depend on this

2. **Merge `feature/recipe-detail` → `development`**
   - 5 commits: detail activity, adapters, layouts, resources, Home wiring
   - ✅ Builds standalone
   - Unblocks Search feature

3. **Merge `feature/saved` → `development`** (can merge anytime after step 1)
   - 1 commit: Saved screen with undo-remove Snackbar
   - ✅ Builds standalone
   - No dependencies on recipe-detail or search

### Merge Order Rationale
- `core-infra` must be first — everything else needs it
- `recipe-detail` must be before `search` merge (search currently fails only on missing `RecipeDetailActivity`)
- `saved` is independent; order doesn't matter after `core-infra` is merged

---

## Phase 1 — Finish & Merge Feature Branches (Add Polish, Then Merge)

**Goal:** Complete in-progress features and integrate them.

### Steps

4. **Polish & merge `feature/profile`**
   - Current state: 1 commit, builds standalone
   - **Add**: Profile screen improvements (scope TBD by you)
   - Create new commit(s) on this branch → build-verify → merge → `development`

5. **Rebase, polish & merge `feature/search`**
   - Current state: 1 commit, fails only on missing `RecipeDetailActivity` reference
   - **Rebase** onto `development` (after step 2 merges) to pick up recipe-detail
   - **Add**: Search screen improvements (e.g. better filtering, smarter result ranking)
   - Create new commit(s) on this branch → build-verify → merge → `development`

### Merge Order Rationale
- Profile has no dependencies; add improvements anytime after Phase 0 merges
- Search must rebase after recipe-detail merges to compile
- This leaves `development` clean and complete with all core features before UX polish begins

### Commit Guidelines for Phase 1
- Each improvement commit: ≤10 files (or ≤500 lines as fallback)
- Prefix: `TE-T733:` (maintain convention)
- Message: 3–4 bullet points max, focus on the "why"

---

## Phase 2 — UX & Error Handling (New Branches, After Phase 0+1 Complete)

**Goal:** Build a cohesive error-handling and loading-state system, then layer in polish.

### Step 6 — Customizable Error Handling (MUST do before shimmer)

**Why first:** Error handling touches the shared `NetworkResult` contract that every screen already consumes. Doing this after shimmer would require revisiting each screen's state-rendering twice. Doing it first means shimmer just slots into an already-established "loading / empty / error" pattern.

**New branch:** `feature/error-handling` (off `development` after Phase 0+1 merge)

**Changes:**

#### 6a. Enhance `NetworkResult` to carry typed errors
```kotlin
// Before
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val message: String) : NetworkResult<T>()
    object Loading : NetworkResult<T>()
}

// After
sealed class NetworkResult<T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val type: ErrorType, val message: String) : NetworkResult<T>()
    object Loading : NetworkResult<T>()
}

enum class ErrorType {
    NoConnection,        // No internet / network timeout
    Unauthorized,        // 401 — refresh failed
    Forbidden,          // 403 — user not allowed
    NotFound,           // 404
    ServerError,        // 5xx
    Unknown             // Everything else
}
```

#### 6b. Global error handler for 401 (Session expired)
```kotlin
// In DashboardActivity or MainActivity
if (error.type == ErrorType.Unauthorized) {
    sessionManager.clearSession()
    navigate(LoginActivity::class)
    showToast("Session expired. Please log in again.")
    // Short-circuit; no screen renders this error locally
}
```

#### 6c. Per-screen error rendering rules

**No Connection** → Inline banner + retry button
```
"No internet connection. Check your network and try again."
[Retry Button]
```

**Empty-but-successful** (NOT an error — a `UiState.Success(emptyList())`)
- Home: "No recipes match your filters. Adjust filters or explore all recipes."
- Saved: "You haven't saved any recipes yet. Explore recipes and save your favorites!"
- Search: "No results for 'xyz'. Try a different search."

**Everything else** (generic fallback)
```
"Something went wrong. Please try again."
[Retry Button]
```

**401 after refresh** → Handled globally (step 6b)

#### 6d. Rollout order
1. Home + Recipe Detail (highest traffic, most states to handle)
2. Search + Saved
3. Profile (simplest, least network calls)

**Commit structure (1 commit per phase 6d rollout step)**
- `TE-T733: Add typed error handling and global 401 handler`
- `TE-T733: Apply error handling to Home & Recipe Detail screens`
- `TE-T733: Apply error handling to Search & Saved screens`
- `TE-T733: Apply error handling to Profile screen`

### Step 7 — Shimmer Loading States

**New branch:** `feature/loading-states` (off `development` after step 6 merges)

**Why after step 6:** Shimmer only touches the `UiState.Loading` path. Step 6 established the overall "loading / empty / error" pattern; shimmer now just plugs into it without disrupting that foundation.

**Changes:**

#### 7a. Add shimmer library to `build.gradle`
```gradle
dependencies {
    implementation 'com.facebook.shimmer:shimmer:0.5.0'
}
```

#### 7b. Create reusable shimmer components
```kotlin
// common/ui/ShimmerRecipeCard.kt
// A placeholder that mimics item_recipe.xml structure
// Animate with Shimmer effect

// common/ui/ShimmerRecipeDetailLayout.kt
// Placeholder for recipe-detail state
```

#### 7c. Rollout order
1. Home (explore list + saved section shimmer)
2. Recipe Detail (image, title, tabs shimmer)
3. Search (results list shimmer)
4. Saved (list shimmer)

**Commit structure (1 commit per phase 7c rollout step)**
- `TE-T733: Add shimmer library and reusable shimmer components`
- `TE-T733: Add shimmer loading states to Home screen`
- `TE-T733: Add shimmer loading states to Recipe Detail screen`
- `TE-T733: Add shimmer loading states to Search screen`
- `TE-T733: Add shimmer loading states to Saved screen`

---

## Merge Timeline & Dependencies

```
Phase 0 (no new code)
├─ feature/core-infra ───┐
│                        ├──> development (complete)
├─ feature/recipe-detail ┤
│                        │
└─ feature/saved ────────┘
                         │
Phase 1 (add polish on branches)
                         │
├─ feature/profile ──────┤
│ (add improvements)     ├──> development (complete)
│                        │
└─ feature/search ───────┤
   (rebase + improve)    │
                         │
Phase 2 (new UX branches)
                         │
├─ feature/error-handling───> development (complete)
│                        │
└─ feature/loading-states────> development (complete)
                        │
                        ✅ MVP + UX complete
```

---

## File & Commit Guidelines

### Commit Size Limits
- **Preferred:** ≤10 files per commit
- **Fallback:** ≤500 lines if a single logical unit exceeds 10 files
- **Message prefix:** `TE-T733:` (maintain existing convention)
- **Message body:** 3–4 bullet points max, focus on "why", not "what"

### Shared File Conflicts (expected in Phase 0+1)
The following files get additions from multiple branches:
- `KoinModules.kt` — each branch adds its own ViewModel registrations (clean 3-way merges)
- `strings.xml` — each branch adds its own string blocks (clean 3-way merges)
- `dimens.xml` / `styles.xml` — occasional additions (usually clean merges)

**If git flags a conflict:**
- It will be in the registration section (each branch's imports and module body lines)
- Keep both sides; don't drop either registration
- Result: one merged `KoinModules.kt` with all registrations present

### Resources & Drawables
- Intentional duplication of some drawable assets (e.g., `ic_arrow_back.xml` in both recipe-detail and search)
- Rationale: features are independently cherry-pickable off `core-infra` alone
- Post-MVP: deduplicate after shimmer is merged and the feature set stabilizes

---

## Testing Strategy

### Phase 0+1 Testing
- **Per-branch:** Manual feature test before merge
- **Post-merge:** Smoke test the full app (Home → Search → Recipe Detail → Saved → Profile → Logout)
- **No specific unit/UI test additions** (testing infrastructure is out of Phase 0-2 scope)

### Phase 2 Testing
- **Error handling:** Manually trigger each error type (disconnect network, use fake 401, empty saved list) and verify messaging + retry flow
- **Shimmer:** Observe animations on first load; verify they complete before data appears

---

## Known Limitations & Future Work

1. **Notifications tab:** Placeholder only; no real implementation. Can be postponed indefinitely or addressed in a follow-up phase.
2. **Pagination:** Not yet implemented in Search or Saved. Add on top of Phase 2 if needed.
3. **Animations:** Only shimmer in Phase 2. Gesture-based transitions (swipe, drag) are out of MVP scope.
4. **Offline mode:** Phase 2 error handling supports showing cached data on no-connection, but actual caching layer is not implemented. Possible Phase 3.
5. **Accessibility (a11y):** Not explicitly addressed in Phases 0-2. Audit and fix after Phase 2 if compliance is required.

---

## Quick Reference — Next Steps

| What | Who | When | Branch | Estimated Time |
|---|---|---|---|---|
| Merge core-infra | You | Now | → development | 5 min (git merge) |
| Merge recipe-detail | You | After core-infra | → development | 5 min |
| Merge saved | You | After core-infra | → development | 5 min |
| Polish profile | You | Parallel with search | feature/profile | TBD |
| Rebase & polish search | You | After recipe-detail merged | feature/search | TBD |
| Merge profile + search | You | After improvements tested | → development | 5 min each |
| Error handling (new) | You | After Phase 0+1 merged | feature/error-handling | ~2–3 hours |
| Shimmer (new) | You | After error handling merged | feature/loading-states | ~2–3 hours |

---

## Questions Before You Start?

- Clarify Phase 1 improvements for profile/search? (UI tweaks, filtering logic, etc.)
- Want to create error-handling branch immediately, or wait until Phase 0+1 testing is done?
- Interested in adding automated tests alongside error handling / shimmer, or keep manual testing only?

---

**Document Version:** 1.0  
**Last Updated:** 2026-07-19  
**Status:** Ready for Phase 0 merges
