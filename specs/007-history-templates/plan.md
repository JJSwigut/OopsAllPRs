# Implementation Plan: Completed Workout History and Templates

**Branch**: `codex/007-history-templates` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/007-history-templates/spec.md`

## Summary

Complete the post-workout loop by making finished workouts inspectable and
reusable. The feature adds a completed workout summary/detail model, upgrades
History from a count placeholder into a useful list and detail flow, lets users
save completed workouts as named reusable templates, and exposes template launch
from Train while preserving the separation between completed history, reusable
templates, and new active workouts. Implementation stays local-first, shared
KMP-first, Android-validated first, and design-system-only.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, Compose runtime/foundation/UI, kotlinx-coroutines, kotlinx-datetime, existing workout lifecycle, routine, progress, exercise catalog, and repository boundaries. No new UI framework or Material 3 dependency is planned.

**Storage**: Use existing local completed workout and routine repository boundaries. The current domain already supports completed workout lookup/listing, saving a completed workout as a reusable routine, and starting from a routine; this feature should avoid schema work unless implementation discovers a missing persisted field required for summary/detail fidelity.

**Local Data Model**: Completed workout summary projections, history list rows, completed workout detail state, template save draft/result state, Train template list rows, and template launch result state. Completed workouts remain immutable read models for this feature; reusable templates remain separate routine records.

**Testing**: Common tests for completed summary mapping, History list ordering/detail selection, bodyweight reps-only summary display, PR marker preservation/derivation, template name validation, completed-workout-to-template separation, template launch into active planned sets, no copied logged timestamps, active-session conflict handling, and restart restoration. Android-first validation with `:shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`, and `:androidApp:assembleDebug`; iOS validation with `:shared:compileKotlinIosSimulatorArm64`. Material scan remains required for shared UI.

**Target Platform**: Android first for executable validation and manual review on a Pixel-class device. iOS remains first-class through shared domain behavior, shared state holders, shared Compose UI, and platform-adapter-only boundaries.

**Platform Scope**: Shared code owns projections, state holders, validation, template launch orchestration, and Compose screens. Android and iOS code should stay bootstrap/adapters only unless compile fixes are required.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and local-first workout logging.

**Performance Goals**: Completed summary should appear immediately after finish from local data. History with ordinary personal workout volume should hydrate within the existing <=2s local startup budget. Launching a saved template from Train should reach active logging in under 10 seconds during manual review.

**Constraints**: Offline-only; no account/network dependency; completed history is read-only in this slice; template names reject blank input; duplicate template names are allowed; template launch must not copy logged timestamps or completed-history identities; bodyweight planned sets remain reps-first; all visual styling uses `FitTheme` tokens and design-system primitives.

**Scale/Scope**: Completed summary after finish, History list/detail, save completed as template, Train template list/launch, validation evidence. Template edit/delete, completed workout edit/delete, export UI, cloud sync, account features, and a full Progress dashboard redesign remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Template launch improves repeat-workout start speed, and Train keeps empty-start primary while making templates quickly reachable.
- **Ledger Integrity**: PASS. Completed history remains immutable/read-only for this slice. Saving a template copies planned targets only, and launching a template creates new active set rows without copied logged timestamps.
- **Session Recovery**: PASS. History and template state are restored from durable local data after restart. Template launch respects existing active-session recovery and conflict handling.
- **Progress Promise**: PASS. Completed summaries and History preserve PR markers and make record evidence inspectable in workout context.
- **Local-First Ownership**: PASS. All behavior uses local completed workout, routine, exercise, and progress data without network/account dependency and preserves export-ready records.
- **Shared-First KMP**: PASS. Domain/state/UI live in shared code. Android is validated first while iOS shared compile remains a required gate.
- **Design System and Accessibility**: PASS. History, summary, template naming, and Train template launch use Neo-Glass design-system tokens/components, large touch targets, readable type, screen-reader labels, reduced-motion paths, and non-haptic alternatives.
- **Public Release Gates**: PASS. Plan includes common tests, Android build, iOS compile, Material scan, and milestone manual device verification for finish summary, History, save-template, and template launch.

## Project Structure

### Documentation (this feature)

```text
specs/007-history-templates/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- history-templates-contracts.md
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
|   |   |   `-- RoutineModels.kt
|   |   |-- repository/
|   |   |   `-- FoundationRepositories.kt
|   |   `-- usecase/
|   |       |-- RoutineUseCases.kt
|   |       `-- WorkoutLifecycleUseCases.kt
|   `-- ui/
|       |-- history/
|       |   |-- HistoryFlow.kt
|       |   |-- HistoryModels.kt
|       |   `-- HistoryStateHolder.kt
|       |-- navigation/
|       |   `-- AppShell.kt
|       |-- routine/
|       |   `-- RoutineStateHolder.kt
|       `-- workout/
|           |-- WorkoutHomeFlow.kt
|           `-- WorkoutHomeStateHolder.kt
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|   |-- domain/usecase/
|   |-- ui/history/
|   |-- ui/routine/
|   `-- ui/workout/
|-- src/androidMain/
`-- src/iosMain/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

design-system/
`-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/
```

**Structure Decision**: Reuse existing completed workout and routine domain
models. Add display projections and state holders in shared UI modules instead
of changing storage for read-only summary/list/detail behavior. Keep template
launch through `WorkoutLifecycleUseCases.startFromRoutine` so completed history
and active workout creation remain separated at the domain boundary.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Treat completed workout summary and History detail as the same read-only
  projection.
- Use completed workout evidence to build templates; duplicate template names
  are allowed because stable ids disambiguate records.
- Launch templates through the existing routine-to-active-workout lifecycle so
  planned sets are copied without logged timestamps.
- Keep Train empty-start primary and expose templates as a compact launch list.
- Defer template editing/deletion and completed workout editing/deletion.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/history-templates-contracts.md](./contracts/history-templates-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts keep the slice fast-loop-first,
ledger-safe, recovery-aware, progress-aware, local-first, shared-first,
design-system-only, and backed by automated plus manual release evidence. No
unresolved clarification or constitution violation remains.
