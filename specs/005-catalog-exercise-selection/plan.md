# Implementation Plan: Catalog Exercise Selection

**Branch**: `005-catalog-exercise-selection` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-catalog-exercise-selection/spec.md`

## Summary

Replace the active workout Quick Add placeholder with a shared, catalog-backed
exercise picker. The slice reuses the existing local exercise repository and
seed ingestion baseline, adds shared picker state for local search and simple
custom exercise creation, wires selection into the active workout state holder,
and keeps Android validated first while compiling shared iOS code.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2;
AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`,
Compose runtime/foundation/UI, kotlinx-coroutines, kotlinx-datetime, existing
`ExerciseRepository`, `SetLoggingUseCases`, and active workout state holder.
No Material 3 in shared UI.

**Storage**: Existing local SQLDelight-backed repository boundary with the
current in-memory implementation. Use `ExerciseRepository.search`, `all`, and
`saveUserExercise`; no schema migration is planned for this slice because
custom exercise persistence already exists at the repository contract level.

**Local Data Model**: Picker session state, catalog result rows, custom
exercise drafts, and add-selection outcomes are shared UI/state models derived
from `ExerciseCatalogItem` and `ExerciseReference`. User-created exercises
retain stable ids, canonical names, timestamps, and `isUserCreated`.

**Testing**: Common tests for default local results, seeded search, bodyweight
selection, custom exercise validation/save/search, cancel/no-op behavior, and
add failure preservation. Android-first validation with
`:shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`, and
`:androidApp:assembleDebug`; iOS validation with
`:shared:compileKotlinIosSimulatorArm64`.

**Target Platform**: Android first for executable validation; iOS remains
first-class through shared picker state and Compose UI boundaries.

**Platform Scope**: Shared code owns picker state, validation, custom exercise
creation, result row composition, active workout add wiring, and tests.
Platform code remains unchanged unless compile fixes are required.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and
local-first workout logging.

**Performance Goals**: Opening Add Exercise should show default local results
immediately for the expected seed list size. Searching and selecting from local
catalog data should keep the user within the 10-second manual outcome target
from active workout to focused exercise block.

**Constraints**: Offline-only; no network/account dependency; no logged set or
PR is created by selection alone; all UI built from FitTheme/design-system
primitives; bodyweight catalog metadata must drive reps-only draft behavior;
cancel and failure paths must preserve active workout drafts and logged rows.

**Scale/Scope**: One active workout, local seeded and user-created exercises,
search results, simple custom exercise creation with name plus
weighted/bodyweight classification. Rich exercise metadata editing, remote
catalogs, cloud sync, and duplicate management remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Add Exercise becomes local search and one-tap
  selection; normal set logging remains one-tap and uninterrupted.
- **Ledger Integrity**: PASS. Selection appends active exercise blocks only;
  logged set rows remain created only through the existing Log action.
- **Session Recovery**: PASS. Picker cancellation/failure preserves active
  workout state; active focus changes only after successful append.
- **Progress Promise**: PASS. Exercise ids and bodyweight metadata remain
  suitable for future PR/history derivation.
- **Local-First Ownership**: PASS. Uses local seed and user-created exercise
  data with no network or account dependency.
- **Shared-First KMP**: PASS. State, validation, and UI remain in shared code;
  Android validates first and iOS compile remains required.
- **Design System and Accessibility**: PASS. UI uses FitTheme/design-system
  primitives and keeps touch targets and readable result rows.
- **Public Release Gates**: PASS. Plan includes shared tests, Android build,
  iOS compile, Material scan continuity, and manual device release gate note.

## Project Structure

### Documentation (this feature)

```text
specs/005-catalog-exercise-selection/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── exercise-picker-contracts.md
├── validation/
│   └── exercise-picker-results.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── AppState.kt
│   ├── domain/usecase/
│   │   └── ExerciseCatalogUseCases.kt
│   ├── ui/exercise/
│   │   ├── ExercisePickerFlow.kt
│   │   ├── ExercisePickerModels.kt
│   │   └── ExercisePickerStateHolder.kt
│   ├── ui/navigation/
│   │   └── AppShell.kt
│   └── ui/workout/
│       └── ActiveWorkoutStateHolder.kt
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/usecase/
│   └── ui/exercise/
├── src/androidMain/
└── src/iosMain/
```

**Structure Decision**: Keep the feature inside existing shared domain and UI
packages. Do not add a navigation library or new module. App-level picker
composites wrap the existing `:design-system` primitives.

## Complexity Tracking

No constitution violations are planned.

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts preserve fast-loop logging, ledger
integrity, local-first ownership, shared-first architecture, and FitTheme-only
UI constraints.
