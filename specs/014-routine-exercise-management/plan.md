# Implementation Plan: Routine & Exercise Management

**Branch**: `codex/014-routine-exercise-management` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/014-routine-exercise-management/spec.md`

## Summary

Add direct routine creation/editing and user-created exercise management without changing the fast active-workout logging path. The implementation will extend the existing shared KMP domain/use-case/state-holder/UI architecture: routines remain `ReusableRoutine` persisted through SQLDelight, routine editing uses shared draft state, routine launch continues through `WorkoutLifecycleUseCases.startFromRoutine`, and user-created exercise edits/archive extend the existing exercise catalog repository.

## Technical Context

**Language/Version**: Kotlin Multiplatform with shared Kotlin/Compose code and Android app bootstrap.

**Primary Dependencies**: Existing Compose Multiplatform UI, SQLDelight typed persistence, kotlinx.coroutines, kotlinx.datetime, and the project `:design-system` Neo-Glass Fit components.

**Storage**: SQLDelight local database via existing `WorkoutDatabase`; no cloud or network storage.

**Local Data Model**: Reuse `ReusableRoutine`, `RoutineExercise`, `RoutineSetTemplate`, `RestConfiguration`, and `ExerciseCatalogItem`. Add shared draft/view state models for routine editing and exercise management. Persist routine edits through existing routine tables. Extend exercise queries/repository use cases for user-created exercise list, update, archive, and id lookup while preserving seeded entries and historical snapshots.

**Testing**: Kotlin common tests for use cases/state holders; Android unit tests for SQLDelight persistence; Android debug assemble for app validation; iOS simulator shared compile for boundary validation.

**Target Platform**: Android first for implementation and manual validation; iOS remains first-class through shared domain/state/UI and compile-safe platform boundaries.

**Platform Scope**: No new platform adapters are required. All feature behavior is shared code. Android app bootstrap only participates through existing `AppState` wiring.

**Project Type**: Kotlin Multiplatform mobile app.

**Performance Goals**: Routine and exercise management screens should stay responsive for the current local exercise catalog size and keep search/update interactions within one UI frame for ordinary lists. Active workout logging must not gain extra steps.

**Constraints**: Offline-only, local-first, no Material 3 shared UI, no mutation of completed workout ledger records, no editing/deleting seeded exercises in this slice, no routine draft autosave.

**Scale/Scope**: One routine builder/editor flow under Train, one exercise management flow under Profile, shared tests and SQL persistence coverage, no folders/program calendar/sharing/supersets.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Management flows are outside active logging and must preserve active workout focus, drafts, and rest timers.
- **Ledger Integrity**: PASS. Routine edits update reusable routine records only; completed workout history and logged sets remain immutable snapshots.
- **Session Recovery**: PASS. No new timer/session model is introduced; active session regression tests will cover management flow non-interference.
- **Progress Promise**: PASS. PR derivation remains based on completed workouts/logged sets, not mutable routine/catalog entries.
- **Local-First Ownership**: PASS. All changes persist locally through SQLDelight and retain stable identifiers/timestamps for future sync.
- **Shared-First KMP**: PASS. Domain, data, state, and UI are shared; Android-first validation and iOS compile are planned.
- **Design System and Accessibility**: PASS. UI uses existing FitTheme/Fit components and compact bottom-reachable controls; Material guard remains part of validation.
- **Public Release Gates**: PASS. Plan includes common tests, SQL tests, Android build, iOS compile, Material guard, whitespace check, and manual milestone notes.

## Project Structure

### Documentation (this feature)

```text
specs/014-routine-exercise-management/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── routine-exercise-management-contracts.md
├── tasks.md
└── validation/
    └── routine-exercise-management-results.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/
│   ├── kotlin/com/jjswigut/oopsallprs/domain/model/
│   ├── kotlin/com/jjswigut/oopsallprs/domain/repository/
│   ├── kotlin/com/jjswigut/oopsallprs/domain/usecase/
│   ├── kotlin/com/jjswigut/oopsallprs/data/repository/
│   ├── kotlin/com/jjswigut/oopsallprs/ui/routine/
│   ├── kotlin/com/jjswigut/oopsallprs/ui/exercise/
│   ├── kotlin/com/jjswigut/oopsallprs/ui/navigation/
│   └── sqldelight/com/jjswigut/oopsallprs/db/
├── src/commonTest/
└── src/androidUnitTest/

androidApp/
└── src/main/
```

**Structure Decision**: Implement the feature in shared domain/data/state/UI with SQLDelight persistence changes only where needed for exercise management queries. Android remains bootstrap-only; iOS remains compile-safe because no platform-specific API is introduced.

## Complexity Tracking

No constitution violations or justified complexity exceptions.

## Phase 0: Research

Research completed in [research.md](research.md).

## Phase 1: Design & Contracts

Design artifacts:

- [data-model.md](data-model.md)
- [contracts/routine-exercise-management-contracts.md](contracts/routine-exercise-management-contracts.md)
- [quickstart.md](quickstart.md)

## Constitution Check Post-Design

- **Fast-Loop Logging**: PASS. Routine/exercise management is presented outside the active set confirmation loop; active session regression tests are specified.
- **Ledger Integrity**: PASS. Contracts explicitly preserve completed workout snapshots and restrict exercise edits to user-created catalog records.
- **Session Recovery**: PASS. Quickstart and tasks include active workout non-regression validation.
- **Progress Promise**: PASS. PR/history/export behavior is not rederived from mutable routine draft data.
- **Local-First Ownership**: PASS. Repository/use-case contracts remain local with stable ids/timestamps.
- **Shared-First KMP**: PASS. All new behavior is shared, with Android and iOS validation gates.
- **Design System and Accessibility**: PASS. UI contract requires Fit components/tokens and compact thumb-friendly controls.
- **Public Release Gates**: PASS. Automated and manual gates are listed in quickstart and tasks.
