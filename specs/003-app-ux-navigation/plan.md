# Implementation Plan: App UX Navigation Shell

**Branch**: `003-app-ux-navigation` | **Date**: 2026-05-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-app-ux-navigation/spec.md`

## Summary

Implement the first app UX/navigation slice on top of the completed KMP
foundation and `:design-system` module. This slice creates the shared app shell,
wires `FitTheme`, provides Train/History/Progress/Profile top-level navigation,
adds active-workout overlay and resume-banner behavior, records the last opened
route through the existing session state, and removes the bootstrap Material 3
scaffolding from shared UI. The broader UX contract in the spec remains the
source for later slices: active logging, rest, PR celebration, history details,
progress charts, profile settings, and export.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2;
AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, implemented
`:design-system` module (`com.jjswigut.oopsallprs.ds`), Compose runtime,
foundation, UI, animation, kotlinx-coroutines, kotlinx-datetime, SQLDelight,
and existing domain/use-case state holders. No Material 3 dependency or
Material 3 imports are allowed in `shared` after this slice. The currently
unused `vico-multiplatform-m3` dependency should be removed or deferred until a
Material-free chart adapter is planned.

**Storage**: Existing local SQLDelight/session repository boundary. Reuse
`active_session_state.last_opened_route` for top-level route and active-workout
presentation recovery; no new schema is required for this slice.

**Local Data Model**: Add shared UI/navigation models for top-level
destinations, layout class, shell state, active-workout resume summary,
navigation intents, and route persistence. These models wrap existing
`ActiveSessionState` and do not mutate workout ledger entities.

**Testing**: Shared unit tests for route parsing, layout classification,
navigation state transitions, resume-banner visibility, and active overlay
recovery. Android-first validation with shared Android unit tests and
`:androidApp:assembleDebug`. iOS first-class validation with
`:shared:compileKotlinIosSimulatorArm64`. Material retirement validation with
source/dependency scans for shared Material 3 usage.

**Target Platform**: Android first for executable validation and manual device
checks. iOS remains first-class through shared Compose UI, shared state models,
and adapter-only platform code.

**Platform Scope**: Shared code owns shell state, destination routing, adaptive
layout classification, FitTheme wiring, top-level screen composition, resume
banner, and active-workout overlay state. Android remains a thin app bootstrap
around `App()`. iOS remains a thin bootstrap around the same shared UI.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and
local-first workout state.

**Performance Goals**: Cold launch hydrates the app shell and active-session
summary within the existing foundation target of <=2s on a supported Android
test device. Top-level tab changes are in-memory state updates with no blocking
database work on the main thread. The shell should avoid recomposing full
destination content for unrelated resume timer ticks where practical.

**Constraints**: Offline-only behavior; no account/network requirement; all
visible styling from `FitTheme` tokens or design-system components; touch
targets at least `FitTheme.size.touchMin`; no modal interruption for returning
to active workout; no Android-only navigation APIs in shared code; route values
must be stable strings for future sync/export diagnostics.

**Scale/Scope**: Four top-level destinations, one active-workout overlay, one
resume banner, compact bottom-bar layout, medium/expanded navigation rail
layout, placeholder destination content backed by existing state holders, and
Material retirement in shared. Full set logging, rest notification UI, PR
celebration, detailed history/progress/profile, and export UI are out of scope
for this implementation slice.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The shell keeps Train as the primary entry
  point, exposes active workout resume globally, and does not add modal friction
  before the later active logging slice.
- **Ledger Integrity**: PASS. This slice does not create, confirm, edit, or
  finish sets. It only observes active-session state and persists route
  metadata through explicit session updates.
- **Session Recovery**: PASS. Active workout presence, elapsed display source,
  overlay presentation, and last opened route are defined from
  `ActiveSessionState` and `last_opened_route`.
- **Progress Promise**: PASS. Progress is represented as a top-level
  destination and remains backed by existing PR state. Detailed PR celebration
  and charts are deferred to later slices without changing PR derivation.
- **Local-First Ownership**: PASS. Route/session state is local-only and
  offline. No sync, account, or network dependency is introduced.
- **Shared-First KMP**: PASS. Navigation, layout classification, shell state,
  and destination composition stay in shared Compose code. Platform code remains
  bootstrap/adapters only.
- **Design System and Accessibility**: PASS. The plan replaces the Material 3
  bridge with `FitTheme`, design-system components, and `BasicText` with
  `FitTheme` typography/color tokens where a text primitive is required.
- **Public Release Gates**: PASS. Automated gates include shared state tests,
  Android build, iOS compile, design-system compatibility, dependency/source
  scans, and milestone manual checks for navigation and resume.

## Project Structure

### Documentation (this feature)

```text
specs/003-app-ux-navigation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── app-navigation-contracts.md
└── tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

```text
shared/
├── build.gradle.kts                 # add :design-system, remove Material 3
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── App.kt                       # root FitTheme + AppShell entry
│   ├── AppState.kt                  # exposes shell/session state holders
│   └── ui/
│       ├── designsystem/
│       │   └── DesignSystemBridge.kt # replace Material bridge with FitTheme helpers
│       ├── navigation/
│       │   ├── AppDestination.kt
│       │   ├── AppNavigationState.kt
│       │   ├── AppNavigationStateHolder.kt
│       │   ├── AppShell.kt
│       │   └── NavigationLayout.kt
│       ├── components/
│       │   ├── ResumeBanner.kt
│       │   └── AppNavRail.kt
│       ├── workout/
│       │   ├── WorkoutHomeFlow.kt
│       │   └── ActiveWorkoutFlow.kt
│       ├── routine/
│       ├── progress/
│       └── exercise/
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/
│   ├── AppDestinationTest.kt
│   ├── AppNavigationStateHolderTest.kt
│   └── NavigationLayoutTest.kt
├── src/androidMain/                 # Android adapters only if required
├── src/androidUnitTest/
└── src/iosMain/                     # iOS adapters only if required

androidApp/
└── src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

iosApp/
└── iosApp/

design-system/
└── src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/
```

**Structure Decision**: Keep the implementation inside shared Compose UI and
reuse the existing design-system module. Add a small app-level navigation
package rather than introducing a navigation framework. App-level composites
such as `ResumeBanner` and `AppNavRail` are allowed because they compose
design-system primitives and tokens; they do not create a parallel style system.

## Complexity Tracking

No constitution violations are planned.

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts keep the slice shared-first, local-first,
Material-free in shared UI, and recovery-aware through
`active_session_state.last_opened_route`. Deferred UX stories are explicitly
out of scope for this slice and remain governed by the feature spec for later
plans/tasks.
