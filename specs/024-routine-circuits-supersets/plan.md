# Implementation Plan: Routine Circuits & Supersets

**Branch**: `024-routine-circuits-supersets` | **Date**: 2026-06-05 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/024-routine-circuits-supersets/spec.md`

## Summary

Add routine exercise grouping so users can save adjacent routine exercises as supersets or circuits. Implement this as shared routine metadata with stable group ids, persist it locally with SQLDelight, carry group context into launched active workouts for display, and keep set logging, rest timers, completed history, PRs, previous values, and exports unchanged.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; Swift 5; Xcode 26.5 project.

**Primary Dependencies**: Existing `:shared`, `:androidApp`, `:design-system`, SQLDelight, kotlinx.coroutines, kotlinx.datetime, shared Compose UI, Fit design system components.

**Storage**: Existing SQLDelight local database. Add nullable routine-exercise group metadata and migration tests; do not change completed workout tables.

**Local Data Model**: `ReusableRoutine` and `RoutineExercise` gain optional grouping metadata. Active workout exercise references gain optional group display context. Completed workouts remain ungrouped ledger records.

**Testing**: Shared common tests for models/use cases/state holders plus Android SQLDelight migration/persistence tests. Run focused routine tests and relevant shared test suites.

**Target Platform**: Android-first KMP mobile app with iOS compile-safe shared boundaries.

**Platform Scope**: Shared domain, persistence, state holder, and Compose UI. No Android-only adapters expected. iOS host should require no platform changes.

**Project Type**: Kotlin Multiplatform mobile app.

**Performance Goals**: Group metadata must not add taps to active set logging; routine launch and editor state updates remain instant for typical routines under 50 exercises.

**Constraints**: Offline-first local persistence; no mutation of completed workouts; no group-specific rest or automatic rotation in this slice; no parallel styling outside Fit design system.

**Scale/Scope**: Routine builder, routine launch, active workout display context, SQL persistence, and tests. Export schema, completed workout grouping, drag-and-drop grouping, group rounds, and sync conflict behavior are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Active logging remains exercise-row based; grouping adds labels/context only.
- **Ledger Integrity**: PASS. Completed workout and set ledgers are not modified by routine grouping.
- **Session Recovery**: PASS. Active workout recovery carries existing exercises plus display group context and does not alter timers.
- **Progress Promise**: PASS. PRs, previous values, history, and exports remain derived from logged sets.
- **Local-First Ownership**: PASS. Group metadata persists locally with stable ids and no network dependency.
- **Shared-First KMP**: PASS. Domain, persistence, state, and UI live in shared code; no Android-only shortcut is planned.
- **Design System and Accessibility**: PASS. UI labels/controls use existing Fit components and compact accessible text.
- **Public Release Gates**: PASS. Plan includes automated tests for persistence, launch carryover, old routines, and ledger regressions.

## Project Structure

### Documentation (this feature)

```text
specs/024-routine-circuits-supersets/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── routine-grouping-contracts.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/data/db/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/
├── src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/
└── src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/
```

**Structure Decision**: Use the existing shared-first KMP structure. Add group metadata at the routine model and SQL mapping boundaries, then surface it through routine editor and active workout shared UI/state.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/routine-grouping-contracts.md](./contracts/routine-grouping-contracts.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Check

PASS. The design keeps grouping as local routine metadata, preserves active logging mechanics, avoids completed-ledger mutation, and requires focused automated tests for persistence and launch carryover.

## Complexity Tracking

No constitution violations are planned.
