# Implementation Plan: Workout Logging UX V1

**Branch**: `006-workout-logging-ux` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-workout-logging-ux/spec.md`

## Summary

Reset the core workout logging UX around a focused full-screen active workout
mode. The slice replaces the current card-stack/demo feel with compact logging
surfaces, dense exercise search, reps-first bodyweight logging, next-set
defaults, inline failure handling, recoverable drafts/focus, and inline PR
feedback. Implementation stays in shared KMP state/UI, uses the existing
Neo-Glass design system, validates Android first, and keeps iOS viable through
shared Compose boundaries.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2;
AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`,
Compose runtime/foundation/UI, kotlinx-coroutines, kotlinx-datetime,
SQLDelight-backed repository boundaries, existing exercise catalog, workout
lifecycle, set ledger, and progress/PR use cases. No new UI framework or
Material 3 dependency is planned.

**Storage**: Existing SQLDelight workout/session store remains the source for
active workouts and confirmed set rows. This slice should add typed local
active-workout UX persistence for focus and editable draft values because the
current `ActiveSessionState` only stores route/rest/session metadata. Do not
persist drafts as `ExerciseSet` rows, and do not use stringly JSON blobs when
typed SQLDelight rows can represent the state directly.

**Local Data Model**: Active workout UX session, active set drafts, exercise
blocks, compact set input fields, logged set rows, exercise picker session, and
inline PR feedback. Confirmed sets stay ledger-backed with `loggedAt`; PR
feedback can be recomputed from logged rows plus stored personal records.

**Testing**: Common tests for Train primary action state, full-screen active
mode state, draft defaulting, draft/focus persistence and recovery,
bodyweight reps-only logging, weighted validation, failed persistence retry,
picker cancellation, picker selection focus handoff, and inline PR feedback.
Android-first validation with `:shared:testDebugUnitTest`,
`:shared:compileDebugKotlinAndroid`, and `:androidApp:assembleDebug`; iOS
validation with `:shared:compileKotlinIosSimulatorArm64`. Material scan
remains required for shared UI.

**Target Platform**: Android first for executable validation and manual review
on a Pixel-class device. iOS remains first-class through shared state models,
shared Compose UI, and platform-adapter-only boundaries.

**Platform Scope**: Shared code owns UX state, reducers/state holders,
validation, PR feedback derivation, draft persistence contracts, and Compose
screen composition. Android and iOS code should remain bootstrap/adapters only
unless a platform compile fix is required.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and
local-first workout logging.

**Performance Goals**: Starting an empty workout should reveal Add Exercise in
under 5 seconds in manual review. Adding a seeded exercise should complete in
under 10 seconds. Logging three repeated weighted sets after exercise selection
should complete in under 30 seconds. Active workout hydration should preserve
the existing <=2s recovery budget for expected workout sizes.

**Constraints**: Offline-only; no account/network dependency; no top-level
navigation competing with active logging; no nested cards inside exercise
blocks; no logged set appears before persistence succeeds; bodyweight sets
require reps only by default; all visual styling uses `FitTheme` tokens and
design-system primitives; controls must remain usable with keyboard, status
bar, navigation bar, reduced motion, haptics disabled, and screen readers.

**Scale/Scope**: Train primary state, focused active workout mode, exercise
block logging layout, dense Add Exercise picker, bodyweight reps-only path,
recoverable focus/drafts, inline PR feedback, and milestone manual review.
Full template management, rest timer UI, finished workout review, Progress
dashboard redesign, export UI, cloud sync, and account features remain out of
scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The feature exists to reduce logging friction:
  one visible next action, dense picker rows, next-set defaults, and
  stepper-first set input.
- **Ledger Integrity**: PASS. Confirmed sets remain ledger rows, drafts are
  separate typed UX state, failure paths preserve drafts, and no set is shown
  logged before persistence succeeds.
- **Session Recovery**: PASS. The plan adds explicit active workout UX
  persistence for focus and drafts and keeps elapsed session context anchored
  in existing wall-clock session state.
- **Progress Promise**: PASS. Inline PR feedback is planned for weighted and
  bodyweight logged sets, using shared derivation rules without blocking
  continued logging.
- **Local-First Ownership**: PASS. Exercise search, custom exercise creation,
  workout state, drafts, and PR feedback are local-only and sync-ready through
  stable ids/timestamps.
- **Shared-First KMP**: PASS. Domain/state/UI stay in shared code; Android is
  validated first while iOS compile remains a required gate.
- **Design System and Accessibility**: PASS. The plan uses the Neo-Glass
  design-system primitives and tokens, large touch targets, readable type,
  reduced-motion paths, haptic alternatives, and screen-reader labels.
- **Public Release Gates**: PASS. Plan includes common tests, Android build,
  iOS compile, Material scan, and milestone manual device verification for
  Train, Active Workout, Add Exercise, keyboard, bodyweight, PR, and recovery.

## Project Structure

### Documentation (this feature)

```text
specs/006-workout-logging-ux/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- ux-wireframes.md
|-- contracts/
|   `-- workout-logging-ux-contracts.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- AppState.kt
|   |-- domain/
|   |   |-- model/
|   |   |   `-- ActiveSessionState.kt
|   |   |-- repository/
|   |   |   `-- FoundationRepositories.kt
|   |   `-- usecase/
|   |       |-- SetLoggingUseCases.kt
|   |       `-- ActivePrFeedbackUseCase.kt
|   |-- data/repository/
|   |   |-- InMemoryFoundationStore.kt
|   |   `-- SqlWorkoutRepository.kt
|   `-- ui/
|       |-- components/
|       |   `-- ResumeBanner.kt
|       |-- designsystem/
|       |   `-- DesignSystemBridge.kt
|       |-- exercise/
|       |   |-- ExercisePickerFlow.kt
|       |   |-- ExercisePickerModels.kt
|       |   `-- ExercisePickerStateHolder.kt
|       |-- navigation/
|       |   |-- AppShell.kt
|       |   `-- AppNavigationStateHolder.kt
|       `-- workout/
|           |-- ActiveWorkoutFlow.kt
|           |-- ActiveWorkoutModels.kt
|           |-- ActiveWorkoutStateHolder.kt
|           |-- ExerciseBlock.kt
|           |-- SetRow.kt
|           |-- CompactSetInput.kt
|           `-- WorkoutHomeFlow.kt
|-- src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
|   |-- Database.sq
|   |-- WorkoutQueries.sq
|   `-- migrations/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|   |-- data/session/
|   |-- domain/usecase/
|   |-- ui/exercise/
|   |-- ui/navigation/
|   `-- ui/workout/
|-- src/androidMain/
`-- src/iosMain/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

design-system/
`-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/
```

**Structure Decision**: Keep the UX reset in shared app UI/state and existing
domain/data boundaries. Add a typed active-workout UX persistence contract
rather than storing unlogged drafts as ledger rows. Add app-level workout
composition primitives only where they encode product workflow; reusable visual
tokens remain in `:design-system`.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Active workout becomes a focused full-screen mode while logging.
- Exercise blocks use dense rows and one next-set input instead of nested card
  stacks.
- Draft/focus recovery requires typed local UX persistence separate from the
  set ledger.
- Inline PR feedback is derived after successful logging and recomputed during
  hydration rather than blocking the logging path.
- The exercise picker remains search-first and dense, scoped to active
  workout selection.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/workout-logging-ux-contracts.md](./contracts/workout-logging-ux-contracts.md)
- [quickstart.md](./quickstart.md)
- [ux-wireframes.md](./ux-wireframes.md)

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts keep the slice fast-loop-first,
ledger-safe, recovery-aware, local-first, shared-first, design-system-only, and
backed by automated plus manual release evidence. No unresolved clarification
or constitution violation remains.
