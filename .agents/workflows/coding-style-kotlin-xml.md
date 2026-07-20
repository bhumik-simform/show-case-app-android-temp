---
description: This is conding style which need to implement in any android project which uses Koltin and Xml
---

# RecipeApp — Coding Style Rules

> Extracted from real code you shared (`LoginActivity`, `MainActivity`, `activity_log_in.xml`).
> Two tiers: **Rule** = directly observed in your code, follow exactly. **Suggested** = not yet seen in your code, proposed only for consistency — flagged separately so you can accept or reject each one.

**Where to put this for Antigravity**: save as `AGENTS.md` in your project root (cross-tool standard, also read by Cursor/Claude Code), or under `.agent/rules/code-style.md` as a workspace rule.

---

## 1. Access Modifiers

- **Rule**: view/state-holding properties are always explicit `private` — e.g. `private lateinit var binding: ActivityLoginBinding`.
- **Rule**: helper functions that aren't lifecycle overrides are `private fun` — e.g. `private fun configureOnClicks()`.
- **Rule**: never write `public` explicitly. Kotlin defaults to public, so the keyword is omitted everywhere in your code.
- **Rule**: `override fun onCreate(...)` carries no modifier beyond `override` — visibility is inherited from the superclass automatically.
- **Suggested**: apply the same instinct one layer deeper once ViewModels arrive — a `MutableStateFlow` backing a public `StateFlow` should be explicit `private`, the public `StateFlow` gets no modifier.

## 2. Class Member Ordering

**Rule**, observed consistently in both samples:
1. Property declarations first (`binding`)
2. Lifecycle override (`onCreate`) directly after
3. Private helper methods below, in the same order they're called inside `onCreate`

**Suggested**: once ViewModels are wired in, add `observeViewModel()` as the next private method after `configureOnClicks()`, called right after it in `onCreate` — same "declare then call in order" habit, just one more step.

## 3. `onCreate()` — Fixed Call Order

**Rule**, both samples follow this order without exception:
1. `installSplashScreen()` — launcher Activity only, must come before `super.onCreate()`
2. `super.onCreate(savedInstanceState)`
3. `enableEdgeToEdge()`
4. `binding = ActivityXBinding.inflate(layoutInflater)` + `setContentView(binding.root)`
5. `ViewCompat.setOnApplyWindowInsetsListener(binding.main) { ... }` — inset padding block, copied as-is per Activity
6. Private setup calls (`configureOnClicks()`, future `observeViewModel()`)

## 4. Naming Conventions

- **Rule**: Activity classes end in `Activity` — `LoginActivity`, `SignupActivity`, `DashboardActivity`.
- **Rule**: the ViewBinding variable is always named `binding`.
- **Rule**: the click-listener setup method is always `configureOnClicks()` — all listeners grouped in one place, never scattered through `onCreate`.
- **Rule**: root layout ID is always `@+id/main` — this is what makes the inset-listener block copy-paste-safe across every Activity, since it never needs its ID changed.
- **Rule**: XML view ID prefixes — `tv_` TextView, `til_` TextInputLayout, `et_` EditText, `btn_` Button, `view_` divider/plain View.
- **Suggested**: extend the same prefix habit to types not yet seen — `iv_` ImageView, `rv_` RecyclerView, `fab_` FloatingActionButton, `cl_`/`ll_` for containers, but only assign an ID at all if the view is actually referenced in Kotlin.

## 5. Comments

- **Rule**: comments are sparse — they only appear where logic isn't implemented yet or genuinely isn't obvious from reading the code (e.g. a one-line comment inside an empty click listener describing what will eventually happen there).
- **Rule**: comments are short, lowercase, plain-English, and describe *intent*, not mechanics — they never restate what the line of code already says.
- **Rule**: no comments on self-explanatory boilerplate — `super.onCreate(...)`, `enableEdgeToEdge()`, binding inflation are all uncommented in your code.
- **Suggested**: prefix placeholder comments with `TODO:` instead of a bare `//` — same intent, but every unfinished spot then shows up automatically in Android Studio's TODO tool window. Purely optional, doesn't change your style otherwise.

## 6. Navigation

- **Rule**: Activity-to-Activity navigation is a plain Intent built inline at the call site — `startActivity(Intent(this, SignupActivity::class.java))`, no factory wrapper.
- **Suggested**: once an Activity needs to receive data (e.g. a recipe ID for a detail screen), add a `companion object` with a `newIntent(context, ...)` function that builds the Intent and puts the extras — avoids typo'd extra-keys, and keeps the no-args case exactly as simple as it is today.

## 7. XML Layout Conventions

(from `activity_log_in.xml`)

- **Rule**: `ScrollView` (`fillViewport="true"`) wrapping a single `ConstraintLayout` root, for any screen that might overflow on small devices.
- **Rule**: all user-facing text via `@string/` resources — never hardcoded.
- **Rule**: colors via theme attributes (`?attr/colorOnSurface`, `?attr/colorSecondary`, `?attr/colorOutline`); hardcoded `@color/` reserved for neutral, non-theme values like dividers (`gray_4`).
- **Rule**: `tools:viewBindingIgnore="true"` on any TextView that's purely static and never touched from Kotlin — keeps the generated Binding class lean.
- **Rule**: font family via `@font/poppins_[weight]` — semibold for primary titles, regular for body/labels, medium for secondary emphasis (links like "Sign Up").
- **Rule**: text size scale — 30sp screen title → 20sp subtitle → 14sp field label/links → 11sp input text.
- **Rule**: Material3 `TextInputLayout`, style `Widget.Material3.TextInputLayout.OutlinedBox`, all four corner radii `14dp`, `hintEnabled="false"` — label is a separate `TextView` above the field, not the Material floating hint.
- **Rule**: `MaterialButton` primary CTA — `cornerRadius="14dp"`, trailing icon (`iconGravity="textEnd"`, `iconPadding="16dp"`).
- **Suggested**: since `14dp` corner radius repeats across `TextInputLayout` and `MaterialButton`, promote it to `@dimen/corner_radius_standard` — one place to change if the design updates later.

## 8. What's intentionally left out

Anything from the architecture plan that hasn't shown up in a real code sample yet — ViewModel internals, Koin module wiring, Repository method bodies, sealed-class `UiState` handling inside a Fragment — isn't a confirmed rule here, it's still a proposal from planning. Share the first real ViewModel or Repository once it's written, and its actual style gets folded into this doc as confirmed rules instead of guesses.