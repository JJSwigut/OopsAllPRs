# Implementation Plan: Timed Exercise PRs

**Branch**: `019-timed-exercise-prs` | **Date**: 2026-06-01 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/019-timed-exercise-prs/spec.md`

## Summary

Add first-class duration logging for hold-style exercises such as planks, wall
sits, and dead hangs. The implementation will extend the shared ledger model
with a timed set kind and canonical duration storage, migrate SQLDelight tables
for completed sets, active drafts, routine targets, and exercise classification,
then wire shared logging, recovery, PR derivation, History, Progress, routine,
previous-value, export, and developer seed paths. Android is validated first;
iOS remains first-class through shared KMP domain, persistence contracts, and
shared Compose UI boundaries.

## Technical Context

**Language/Version**: Kotlin Multiplatform using the existing Kotlin/Compose project versions

**Primary Dependencies**: Existing shared domain/use-case layer, SQLDelight-backed repositories, kotlinx.datetime, Compose Multiplatform shared UI, Fit design-system components

**Storage**: SQLDelight local SQLite with an additive schema migration for set durations, active timed drafts, routine duration targets, and timed exercise classification

**Local Data Model**: Add `SetKind.TIMED`, canonical `durationMs` storage on set/draft/template records, timed exercise classification in the exercise catalog/reference model, and time PR/progress enum values. Existing weighted/bodyweight data remains valid with null duration fields.

**Testing**: Kotlin common tests for validation, set logging, routine launch/defaults, PR derivation, history/progress display models, export rows, active UX recovery, and seed classification; Android debug build; iOS simulator shared compile; Material/style scan; whitespace check; manual Pixel validation for the timed logging loop

**Target Platform**: Android first, with shared KMP domain/state/UI behavior compiled for iOS

**Platform Scope**: Domain rules, repositories, SQLDelight mappings, state holders, shared Compose screens, and display formatting stay in shared code. Android receives the first runtime validation through the existing app shell and active workout flows. iOS requires no separate behavior, only adapter compatibility with the migrated shared database and shared UI.

**Project Type**: Kotlin Multiplatform mobile app

**Performance Goals**: Timed set logging and display must stay in the active workout fast path. Duration confirmation should be one tap after a duration exists, PR derivation should remain bounded by local completed workout history, and Progress/History formatting should not add perceptible delay to screen load.

**Constraints**: Offline-only; no cloud sync; no Material 3 reintroduction in shared UI; preserve existing weighted, bodyweight, rest-timer, routine, history, Progress, unit, export, and previous-value behavior; timer-like state must use wall-clock anchors if a live timed-set counter is running

**Scale/Scope**: Single-user local workout history. In scope: hold-style duration exercises, duration PRs, routine duration targets, previous timed defaults, seed classification, developer demo support, and export. Out of scope: shortest-time goals, intervals, circuits, rounds, pace scoring, cloud conflict resolution, and background notifications for timed sets.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Timed logging adds a duration-specific path that keeps confirmation in the active card and allows direct correction without a modal for the normal flow.
- **Ledger Integrity**: PASS. Logged timed sets use the same confirmed-set ledger with positive duration validation, durable timestamps, explicit edit/delete paths, and source-linked PR evidence.
- **Session Recovery**: PASS. Active timed drafts and any running timed-set counter are persisted through active UX state with wall-clock anchors, matching the constitution's timer rule.
- **Progress Promise**: PASS. Time PRs extend PR derivation, active feedback, History evidence, Progress records, and trend points while preserving non-timed PR behavior.
- **Local-First Ownership**: PASS. Timed data is local SQLDelight data, exportable, offline-available, and modeled with stable IDs/timestamps for future sync.
- **Shared-First KMP**: PASS. Business rules, validation, persistence mappings, state, formatting, and shared UI live in `shared`; Android-only code remains platform integration only.
- **Design System and Accessibility**: PASS. New timed controls use FitTheme tokens/components, large touch targets, readable time labels, TalkBack/VoiceOver labels, reduced-motion-safe state, and non-color-only PR feedback.
- **Public Release Gates**: PASS. Plan identifies common tests, Android build, iOS compile, SQLDelight migration validation, export sanity checks, style scans, and manual device validation.

## Project Structure

### Documentation (this feature)

```text
specs/019-timed-exercise-prs/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── timed-exercise-prs-contracts.md
├── validation/
│   └── timed-exercise-prs-results.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/model/              # SetKind, ExerciseSet, RoutineSetTemplate, PR/progress/timed draft models
│   ├── domain/usecase/            # set logging, previous defaults, PR derivation, routines, export wiring
│   ├── domain/validation/         # positive duration and timed/non-timed validation rules
│   ├── data/repository/           # SQLDelight mappings and migration-safe persistence
│   ├── data/export/               # timed workout, PR, and routine export rows
│   ├── data/exercise/             # timed seed classification and catalog mapping
│   ├── dev/                       # timed demo seed data
│   └── ui/
│       ├── workout/               # timed active logging card, timer/correction state, recovery
│       ├── routine/               # timed routine targets and exercise editor support
│       ├── history/               # duration rows and PR evidence labels
│       └── progress/              # time PR hero/detail and duration trend labels
├── src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
│   ├── Database.sq                # additive duration/catalog fields
│   ├── SetQueries.sq              # timed set persistence
│   ├── RoutineQueries.sq          # timed template persistence
│   ├── ProgressQueries.sq         # time PR/progress persistence through enum values
│   └── migrations/3.sqm           # migration from current schema
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/
    ├── domain/usecase/            # timed validation, PR derivation, routine defaults
    ├── data/                      # SQLDelight migration/export/recovery tests
    ├── ui/workout/                # timed logging/recovery state-holder tests
    ├── ui/history/                # duration and evidence display tests
    └── ui/progress/               # time PR/trend display tests

androidApp/
└── src/main/                      # unchanged Android bootstrap except adapter compatibility if needed
```

**Structure Decision**: Use the existing shared KMP domain/use-case/state/UI and SQLDelight layout. This feature needs an additive local schema migration and shared UI updates, but no new module or platform-specific feature fork.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/timed-exercise-prs-contracts.md](./contracts/timed-exercise-prs-contracts.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Check

- **Fast-Loop Logging**: PASS. Contracts require a one-tap log once duration exists and an in-card correction path.
- **Ledger Integrity**: PASS. Data model keeps timed sets in the same logged-set lifecycle and makes duration edits explicit.
- **Session Recovery**: PASS. Active timed drafts include duration state and optional running timer anchors for restart recovery.
- **Progress Promise**: PASS. Time PRs, progress points, evidence, and trend formatting are defined with longest-duration rules and no duplicate tie PRs.
- **Local-First Ownership**: PASS. All data is stored in local SQLDelight rows and included in export output.
- **Shared-First KMP**: PASS. Contracts and data model keep rules in shared code with Android-first validation and iOS compile coverage.
- **Design System and Accessibility**: PASS. UI contracts require FitTheme, touch targets, accessibility labels, and reduced-motion-safe behavior.
- **Public Release Gates**: PASS. Quickstart defines automated, migration, export, Android, iOS, style, whitespace, and manual-device evidence.

## Complexity Tracking

No constitution violations.
