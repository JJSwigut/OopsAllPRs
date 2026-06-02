# Implementation Plan: Active Workout Logging Loop

**Branch**: `004-active-workout-loop` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-active-workout-loop/spec.md`

## Summary

Implement the first real active-workout logging experience on top of the
completed KMP foundation, Neo-Glass design system, and app navigation shell.
The slice adds shared state models for exercise blocks, editable set drafts,
inline roller fields, add-exercise focus handoff, one-tap confirmed logging,
bodyweight reps-only support, non-destructive inline errors, and recovery of
focused exercise/set draft state.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2;
AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`,
Compose runtime/foundation/UI, kotlinx-coroutines, kotlinx-datetime,
SQLDelight-backed repository boundaries, existing exercise catalog and set
logging use cases. No Material 3 in shared UI.

**Storage**: Existing local SQLDelight/in-memory foundation store. Reuse active
workout and session repositories. Add a small active workout draft/focus state
model in shared; persist through the session repository boundary where possible
without mutating confirmed ledger rows.

**Local Data Model**: Exercise blocks, set row drafts, logged rows,
roller-field values, focus state, and add-exercise selection results are shared
UI/state models derived from `ActiveWorkout`, `ActiveExercise`, and
`ExerciseSet`. Confirmed sets continue to be stored as ledger rows with
`loggedAt`.

**Testing**: Common tests for draft defaults, bodyweight validation,
one-tap-confirm behavior, duplicate-save suppression, inline error preservation,
add-exercise focus, and recovery state. Android-first validation with
`:shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`, and
`:androidApp:assembleDebug`; iOS validation with
`:shared:compileKotlinIosSimulatorArm64`.

**Target Platform**: Android first for executable validation; iOS remains
first-class through shared models/state/UI and adapter boundaries.

**Platform Scope**: Shared code owns logging models, reducer/state holder,
draft validation, exercise block composition, roller field composition, and
session/focus recovery. Platform code remains unchanged for this slice unless
compile fixes are required.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and
local-first workout logging.

**Performance Goals**: One-tap log path should update visible state immediately
after repository success and prevent duplicate taps while pending. Active
workout hydration should remain within the prior <=2s cold-start session target
for expected workout sizes.

**Constraints**: Offline-only; no network/account dependency; all UI built from
FitTheme/design-system primitives; bodyweight reps-only logging valid; weighted
sets require non-negative weight; logged rows mutate only through explicit edit
paths; no modal in the normal logging path.

**Scale/Scope**: One active workout, multiple exercise blocks, logged history
per block, one editable next-set draft per block, add-exercise from existing
catalog search, and focus recovery. Rest timer UI, finish/discard, PR
celebration, charts, and export UI remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. One-tap log and inline edit are primary flows;
  direct numeric entry is fallback only.
- **Ledger Integrity**: PASS. Logged rows appear only after persistence success;
  duplicate pending saves are blocked; failures preserve unlogged drafts.
- **Session Recovery**: PASS. Focused exercise/set and draft values are planned
  as recoverable active session state.
- **Progress Promise**: PASS. Confirmed set tuples remain suitable for PR
  derivation; this slice does not implement PR celebration.
- **Local-First Ownership**: PASS. Feature is local-only and uses existing
  stable ids/timestamps.
- **Shared-First KMP**: PASS. Models, reducer, validation, and UI live in
  shared code; Android validates first and iOS compile remains required.
- **Design System and Accessibility**: PASS. UI uses FitTheme/design-system
  components and keeps large controls, semantics, reduced motion, and haptic
  alternatives.
- **Public Release Gates**: PASS. Plan includes shared tests, Android build,
  iOS compile, Material scan continuity, and milestone manual device note.

## Project Structure

### Documentation (this feature)

```text
specs/004-active-workout-loop/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── active-workout-contracts.md
├── validation/
│   └── active-workout-results.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/usecase/
│   │   └── SetLoggingUseCases.kt
│   └── ui/workout/
│       ├── ActiveWorkoutStateHolder.kt
│       ├── ActiveWorkoutFlow.kt
│       ├── ActiveWorkoutModels.kt
│       ├── ExerciseBlock.kt
│       ├── SetRow.kt
│       └── RollerField.kt
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/usecase/
│   └── ui/workout/
├── src/androidMain/
└── src/iosMain/
```

**Structure Decision**: Keep this as an app-level shared UI/state feature in
`shared/src/commonMain/.../ui/workout`. Do not add a new module or navigation
library. New app-level composites wrap `:design-system` primitives rather than
expanding the design system itself.

## Complexity Tracking

No constitution violations are planned.

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts preserve fast-loop logging, ledger
integrity, recovery, local-first ownership, shared-first architecture, and
FitTheme-only UI constraints.
