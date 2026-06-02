# Implementation Plan: Previous Workout Values

**Branch**: `codex/019-previous-workout-values` | **Date**: 2026-05-31 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/018-previous-workout-values/spec.md`

## Summary

Add shared previous-workout defaulting so adding an exercise or launching an
under-specified routine can prefill the next set from the most recent completed
workout for that exercise. The implementation will derive values from the
completed workout ledger through existing repositories, apply explicit routine
targets first, fall back to previous completed values where targets are absent,
and then fall back to existing safe defaults. No new storage schema is planned.

## Technical Context

**Language/Version**: Kotlin Multiplatform using the existing Kotlin/Compose project versions

**Primary Dependencies**: Existing shared domain/use-case layer, SQLDelight-backed repositories, kotlinx.datetime, Compose Multiplatform shared UI

**Storage**: SQLDelight local SQLite through existing workout/routine/active UX repositories; no migration planned

**Local Data Model**: Previous values are derived read models from completed workouts. Active workout drafts remain persisted through existing active UX draft rows. Completed workouts, routines, PRs, progress points, and exports are not mutated by lookup.

**Testing**: Kotlin common tests for domain use cases and shared state holders; Android debug compile/build; iOS simulator shared compile; Material/style scan; whitespace check

**Target Platform**: Android first, with shared KMP domain/state behavior compiled for iOS

**Platform Scope**: Derivation and default application stay in shared code. Android receives the first validated runtime path through existing add-exercise and routine-launch flows. iOS needs no platform-specific adapter in this slice.

**Project Type**: Kotlin Multiplatform mobile app

**Performance Goals**: Previous-value lookup must be fast enough for add-exercise and routine-launch interactions; in practice this is an in-memory pass over the local completed workout list currently exposed by the repository.

**Constraints**: Offline-only; no cloud lookup; preserve explicit routine targets; preserve bodyweight reps-only validation; no Material 3/shared styling regression; no new UI unless implementation needs a small label or state indicator

**Scale/Scope**: Single-user local workout history. Applies to exercise add and routine launch only. Time-range filters, analytics predictions, automatic routine rewriting, and previous-value editing preferences are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The feature reduces set-entry taps and does not add a modal or extra confirmation to the logging path.
- **Ledger Integrity**: PASS. Previous values are defaults for active drafts only and do not mutate completed workouts, saved routines, PR evidence, progress points, or exports.
- **Session Recovery**: PASS. Drafts created from previous values use existing active UX draft persistence and recovery.
- **Progress Promise**: PASS. The feature improves last-set context while preserving weighted, bodyweight, fractional, canonical-unit, and PR evidence behavior.
- **Local-First Ownership**: PASS. Lookup is derived locally from offline completed workout data and keeps future sync boundaries stable.
- **Shared-First KMP**: PASS. Domain derivation and use-case wiring stay in shared code; platform code is unchanged.
- **Design System and Accessibility**: PASS. No new user-facing component is planned. If any visible copy is added, it must use existing Fit design-system components and labels.
- **Public Release Gates**: PASS. Automated tests, Android build, iOS compile, Material/style scan, whitespace validation, and deferred manual-device notes are identified.

## Project Structure

### Documentation (this feature)

```text
specs/018-previous-workout-values/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── previous-workout-values-contracts.md
├── validation/
│   └── previous-workout-values-results.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/model/              # previous-value read models if needed
│   ├── domain/usecase/            # resolver plus add-exercise/routine launch wiring
│   ├── domain/repository/         # existing repository contracts only if needed
│   └── ui/workout/                # state-holder behavior if draft persistence needs focus refresh
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/
│   ├── domain/usecase/            # previous-value resolver and lifecycle tests
│   └── ui/workout/                # add-exercise/routine launch state-holder tests
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/
    └── FoundationFixtures.kt      # reusable fixture helpers if needed

androidApp/
└── src/main/                      # unchanged Android bootstrap
```

**Structure Decision**: Use the existing shared KMP domain/use-case and state-holder layout. No new platform module, database migration, or design-system component is required for this slice.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/previous-workout-values-contracts.md](./contracts/previous-workout-values-contracts.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Check

- **Fast-Loop Logging**: PASS. Defaults are applied before the draft is shown.
- **Ledger Integrity**: PASS. Data model defines previous values as derived and non-persisted.
- **Session Recovery**: PASS. Existing active UX draft persistence remains the recovery mechanism.
- **Progress Promise**: PASS. Bodyweight reps-only and canonical weight behavior are explicit in contracts.
- **Local-First Ownership**: PASS. No network or account dependency is introduced.
- **Shared-First KMP**: PASS. All behavior is in shared code; Android/iOS platform adapters are unchanged.
- **Design System and Accessibility**: PASS. No new UI surface is required.
- **Public Release Gates**: PASS. Manual real-device validation is deferred by request and recorded as such; automated gates remain required.

## Complexity Tracking

No constitution violations.
