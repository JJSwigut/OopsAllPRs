# Implementation Plan: Progress and PR Dashboard V1

**Branch**: `codex/008-progress-pr-dashboard` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/008-progress-pr-dashboard/spec.md`

## Summary

Make Progress a PR-first dashboard instead of a placeholder count. The feature
adds display-ready PR rows, exercise grouping, source evidence lookup, and
simple trend context from local progress points. It reuses completed workout
ledger data and existing progress repositories, preserves source workout/set
traceability, supports weighted/bodyweight/e1RM/volume records, and renders the
experience in shared Compose with the existing design system.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, Compose runtime/foundation/UI, kotlinx-coroutines, kotlinx-datetime, local progress repository, completed workout repository, PR derivation use case, History completed workout projections, and existing weight unit domain utilities. No new charting or Material 3 dependency is planned.

**Storage**: Use existing local progress records/points and completed workout ledger repositories. Extend PR derivation if needed to materialize estimated one-rep max and volume records from completed sets; avoid schema changes unless current persisted progress models cannot represent required evidence.

**Local Data Model**: PR dashboard state, PR row, exercise PR group, PR source evidence, trend point display row, and selected exercise/evidence state. Display projections are derived from `PersonalRecord`, `ProgressPoint`, completed workouts, and exercise display snapshots.

**Testing**: Common tests for PR row labels, recent ordering, weighted/bodyweight/e1RM/volume records, unit display without canonical mutation, exercise grouping, source evidence lookup, missing evidence fallback, trend ordering, state restoration from local repositories, and non-mutation of completed history/templates/active workouts. Android-first validation with `:shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`, and `:androidApp:assembleDebug`; iOS validation with `:shared:compileKotlinIosSimulatorArm64`. Material scan remains required.

**Target Platform**: Android first for executable validation and manual review on a Pixel-class device. iOS remains first-class through shared state models, shared Compose UI, and platform-adapter-only boundaries.

**Platform Scope**: Shared code owns PR derivation, display projections, state holders, evidence lookup, and Compose screen composition. Android and iOS code should remain bootstrap/adapters only unless compile fixes are required.

**Project Type**: Kotlin Multiplatform mobile app with shared Compose UI and local-first workout progress review.

**Performance Goals**: Progress should hydrate from local records within the existing <=2s local startup budget. Recent PRs should be visible immediately after opening Progress. Evidence lookup should use local completed workout data and return in ordinary tap-response time for expected personal workout volumes.

**Constraints**: Offline-only; no account/network dependency; Progress is read-only in this slice; opening Progress must not mutate workout history, templates, active sessions, or exercise data; source evidence must not be fabricated; all visual styling uses `FitTheme` tokens and design-system primitives; no new charting library in V1.

**Scale/Scope**: Recent PR list, exercise record grouping, source evidence lookup, simple trend point context, weighted/bodyweight/e1RM/volume records, validation evidence. Editing/deleting PRs, full charts, goal setting, cloud sync, account features, and export UI remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Progress is read-only and does not interrupt active logging. It reinforces the reward loop after completed workouts.
- **Ledger Integrity**: PASS. PR rows and evidence derive from completed set ledger data and preserve source workout/set ids without mutating logged tuples.
- **Session Recovery**: PASS. Progress dashboard state hydrates from durable local progress/completed workout data after restart; no timer behavior is introduced.
- **Progress Promise**: PASS. The feature directly centers PRs, weighted/bodyweight records, e1RM, volume, source evidence, and plain-language improvements.
- **Local-First Ownership**: PASS. All records, trends, and evidence work offline from local data and remain compatible with export/sync-ready records.
- **Shared-First KMP**: PASS. Domain/state/UI stay in shared code; Android is validated first while iOS compile remains a required gate.
- **Design System and Accessibility**: PASS. Progress UI uses Neo-Glass design-system tokens/components, large touch targets, readable type, screen-reader labels, reduced-motion paths, and non-haptic alternatives.
- **Public Release Gates**: PASS. Plan includes common tests, Android build, iOS compile, Material scan, and milestone manual device verification for recent PRs, exercise grouping, evidence, empty state, bodyweight, units, and trends.

## Project Structure

### Documentation (this feature)

```text
specs/008-progress-pr-dashboard/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- progress-pr-dashboard-contracts.md
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
|   |   |   `-- ProgressModels.kt
|   |   |-- repository/
|   |   |   `-- FoundationRepositories.kt
|   |   `-- usecase/
|   |       `-- PersonalRecordDerivationUseCase.kt
|   `-- ui/
|       |-- history/
|       |   `-- HistoryModels.kt
|       |-- navigation/
|       |   `-- AppShell.kt
|       `-- progress/
|           |-- ProgressFlow.kt
|           |-- ProgressModels.kt
|           `-- ProgressStateHolder.kt
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|   |-- domain/usecase/
|   `-- ui/progress/
|-- src/androidMain/
`-- src/iosMain/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

design-system/
`-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/
```

**Structure Decision**: Reuse existing `PersonalRecord`, `ProgressPoint`, and
completed workout models. Add display projections/state in `ui/progress` and
source evidence lookup through existing completed workout repository behavior.
Extend derivation logic for missing record kinds rather than adding UI-only
fake records.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Progress is PR-first: recent records before trend context.
- Exercise grouping is derived from existing personal records and completed
  workout exercise snapshots.
- Source evidence uses completed workout/set ids and may reuse History
  completed workout projections.
- V1 trend context is a simple ordered local list, not a charting dependency.
- PR derivation should materialize weighted, bodyweight, estimated one-rep max,
  and volume record kinds from completed ledger data.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/progress-pr-dashboard-contracts.md](./contracts/progress-pr-dashboard-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts keep the slice progress-first,
ledger-safe, local-first, shared-first, design-system-only, and backed by
automated plus manual release evidence. No unresolved clarification or
constitution violation remains.
