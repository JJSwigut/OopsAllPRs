# Implementation Plan: Active Workout Resume

**Branch**: `020-active-workout-resume` | **Date**: 2026-06-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/020-active-workout-resume/spec.md`

## Summary

Clean up the Train screen when an active workout exists by removing redundant Start workout actions, making the persistent active-workout card the only resume/discard surface, and suppressing the "active workout already in progress" home error path. Also adjust seed catalog hydration so existing installs receive newly added packaged seed exercises without overwriting user-created exercises or creating duplicate canonical names.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system` Fit components and tokens, kotlinx-coroutines, kotlinx-datetime, SQLDelight, and existing workout/exercise use cases.

**Storage**: Existing SQLDelight local database and in-memory test store. No schema migration is required; seed refresh uses existing exercise catalog and seed import tables.

**Local Data Model**: Reuse `ActiveSessionState`, `ActiveWorkoutResume`, `WorkoutHomeState`, `ExerciseCatalogItem`, and `ExerciseSeedImport`. Add no new persistent entities.

**Testing**: Shared unit tests for workout home state, template launch conflict suppression, active discard from Train, and seed refresh of missing packaged exercises. Android validation with `:androidApp:installDebug` on the connected Pixel 9 Pro.

**Target Platform**: Android first for executable validation and manual device check. iOS remains covered through shared state and shared Compose UI changes.

**Platform Scope**: Shared UI/state/use-case work only. Android app code remains bootstrap-only; no platform adapter changes are expected.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and local-first workout state.

**Performance Goals**: Train hydration and seed refresh remain quick enough for launch/resume flows. Seed refresh checks existing canonical names in memory and only ingests when packaged seed rows are missing.

**Constraints**: Offline-only behavior; no account/network dependency; all UI uses FitTheme/Fit components; no modal interruption for resume; discard from Train is direct per user request; user-created exercises must not be overwritten.

**Scale/Scope**: One Train-screen active-session layout cleanup, one new Discard action on the existing resume card, and seed-refresh behavior for missing packaged seed rows. Removing old seed rows from existing installs is out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The active workout state gets one clear recovery surface and fewer conflicting actions.
- **Ledger Integrity**: PASS. Discard uses the existing explicit active-workout lifecycle path and does not mutate completed workouts.
- **Session Recovery**: PASS. Discard refreshes app/session/navigation state so stale active-session cards do not survive hydration.
- **Progress Promise**: PASS. No PR/history semantics change; seed refresh improves catalog availability.
- **Local-First Ownership**: PASS. All changes operate on local active-session and exercise catalog data.
- **Shared-First KMP**: PASS. State, UI, and seed behavior stay in shared code.
- **Design System and Accessibility**: PASS. Existing Fit card/buttons and touch target helpers are used; no new style system.
- **Public Release Gates**: PASS. Unit tests plus Android build/install/manual check are planned.

## Project Structure

### Documentation (this feature)

```text
specs/020-active-workout-resume/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── train-active-workout-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── AppState.kt
│   ├── data/exercise/
│   │   └── ExerciseSeedIngestion.kt
│   ├── data/repository/
│   │   ├── InMemoryFoundationStore.kt
│   │   └── SqlFoundationStore.kt
│   ├── domain/usecase/
│   │   └── ExerciseCatalogUseCases.kt
│   ├── ui/components/
│   │   └── ResumeBanner.kt
│   ├── ui/navigation/
│   │   └── AppShell.kt
│   └── ui/workout/
│       ├── WorkoutHomeFlow.kt
│       └── WorkoutHomeStateHolder.kt
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/
    ├── domain/usecase/
    │   └── ExerciseCatalogSeedRefreshTest.kt
    └── ui/workout/
        ├── WorkoutHomeStateHolderTest.kt
        └── WorkoutHomeTemplateLaunchTest.kt

androidApp/
└── src/main/
```

**Structure Decision**: Keep the work in shared code because the behavior is common app state and shared Compose UI. Android is used for validation only.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/train-active-workout-contract.md](./contracts/train-active-workout-contract.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Check

PASS. The design keeps active workout recovery explicit, preserves ledger boundaries, avoids new storage schema, stays shared-first, and uses existing Fit design-system components.

## Complexity Tracking

No constitution violations are planned.
