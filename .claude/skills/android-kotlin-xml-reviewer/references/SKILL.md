---
name: android-kotlin-xml-developer
description: Builds Android features in Kotlin + XML Views (not Compose) for apps using Retrofit, MVVM + Repository architecture, and Koin DI. Use this whenever the user asks Claude to build, implement, add, or scaffold an Android feature (Fragment, ViewModel, Repository, API service, XML layout, Adapter) — including working through a plan.md/task-list file feature-by-feature. Also use when the user shares existing Kotlin/XML code and asks for a review, refactor, or fix, since the same architecture and style rules apply to both writing and correcting code. Do not use for iOS/Swift code, Jetpack Compose code, or non-Android Kotlin.
---

# Android Kotlin + XML Developer

Claude is the developer here. The user is the approver — they review finished, working code and either accept it or send back specific change requests. Claude does not stop mid-task to ask permission for implementation-level decisions; it makes the call using the architecture and style rules below, builds the complete piece, and hands it over ready to run.

Only interrupt the user for things that actually require their input: which plan.md task to pick up if ambiguous, a genuine product/business-logic decision the rules below don't cover (e.g. "should saving a recipe require login"), or a missing dependency/credential. Never interrupt to ask "should I use a Repository here" or "is this layout okay" — the rules already answer that.

## Step 0: Check for a task plan

Before starting any feature work, check if the project has a plan.md in the project root. If one exists and the user hasn't specified exactly what to build:

- Read it to find the next incomplete task in the current phase
- If task is building the UI in xml than ask user to describe the design link(or provide a screenshot) before starting, since the plan.md won't contain that information
- Ask which task to pick up only if genuinely ambiguous — otherwise just take the next one in sequence
- Implement exactly that task's scope, since tasks are already broken down small — don't cascade into the next task unasked
- After finishing, state which task was completed and what changed, then stop and let the user approve before continuing

If no plan.md exists and the user is describing a feature directly, just build what they ask for.

## Step 1: Understand what you're building or fixing

Identify the piece's role: Fragment/Activity, ViewModel, Repository, API service/Retrofit interface, Adapter, or XML layout. Architecture rules below apply differently per role.

If a whole feature spans multiple files, build/fix all of them together as one coherent unit before handing back for approval — don't hand back one file at a time for a single feature.

## Step 2: Apply architecture rules

These matter more than style. When building new code, design it this way from the start. When fixing existing code, find these before nitpicking naming. Read references/architecture-checks.md for the full checklist and code examples. In short, watch for:

- God Fragments/Activities — UI class doing networking, business logic, or data transformation instead of delegating to ViewModel/Repository
- ViewModel calling Retrofit/API services directly — must go through a Repository
- Missing Repository abstraction — ViewModel talking to raw DTOs instead of domain models, or no clean seam between network and UI layers
- Leaking Android framework types into ViewModel (Context, View, etc. — breaks testability)
- LiveData/StateFlow misuse — mutable state exposed publicly instead of via a private backing field + public read-only exposure
- Tight coupling — Fragment directly instantiating a Repository/ApiService instead of receiving it via Koin DI
- Adapters holding business logic instead of just binding pre-processed data
- Unnecessary nested layouts — see Step 3, this is treated as an architecture-level layout issue, not just style

## Step 3: Enforce layout-simplicity rule (XML)

This is a hard rule: prefer the flattest layout that can achieve the design. Before accepting any nested ViewGroup (LinearLayout inside ConstraintLayout, nested LinearLayouts, etc.), ask "can ConstraintLayout alone express this?" If yes, flatten it — ConstraintLayout constraints replace most nesting needs (chains replace nested LinearLayouts, barriers/guidelines replace nested wrapper layouts, groups replace nested visibility-toggle containers).

Only keep nesting when it's genuinely necessary (e.g. a horizontally scrolling row inside a vertical list item, or a reusable include component). Always call out removed nesting explicitly in the review as an Architecture-level fix, not a style nit.

## Step 4: Apply style conventions

Every file must match the user's fixed conventions in references/style-conventions.md. These include: explicit private on all properties/helpers, fixed onCreate call order, all click listeners centralized in configureOnClicks(), root layout id always @+id/main, XML id prefixes (tv_, til_, et_, btn_, view_), all text via string resources (no hardcoded strings), colors via theme attributes (no hardcoded hex), Material3 outlined inputs at 14dp corner radius, Poppins font, Koin for DI, Retrofit + Kotlin Serialization for networking.

Read the reference file for the full list with code examples — don't rely on memory for the complete rule set, it's easy to miss one.

## Step 5: Hand off for approval

New code: deliver the complete, working file(s) — nothing partial, nothing "left for you to fill in" — plus a short note on any non-obvious decision made (e.g. why a mapper was added, why a value was hoisted to the Repository). If working from plan.md, state which task this completes. End with the code ready to build/run, not a question about whether to proceed with it.

Fixed/reviewed code: structure the response as:

1. Architecture issues found (bulleted, one line each — what was wrong, why it matters)
2. Style issues found (bulleted, one line each)
3. Fixed code — full corrected file(s), ready to drop in
4. Notes — any judgment calls made, or "no issues found" if genuinely clean

If the file was already good, say so plainly — don't invent problems to fill sections. The user approves or sends back specific change requests; Claude doesn't need to re-ask about decisions already covered by the rules in this skill.