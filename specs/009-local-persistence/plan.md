# Implementation Plan: Durable Local Persistence

**Branch**: `codex/009-local-persistence` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/009-local-persistence/spec.md`

## Summary

Move the app's normal runtime data path from `InMemoryFoundationStore` delegates
to SQLDelight-backed repositories. The feature adds a shared SQL store that
implements the existing repository interfaces, wires Android runtime through
the platform database driver, preserves in-memory fixtures for fast domain
tests, and validates restart recovery for active workouts, completed ledger
data, routines, exercise catalog, preferences, PR/progress rows, active UX
state, drafts, and exports.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight runtime/coroutines, Android SQLDelight driver, native SQLDelight driver, JDBC SQLite driver for Android unit tests, kotlinx-datetime, kotlinx-coroutines, existing repository/use-case interfaces.

**Storage**: SQLDelight local SQLite database via `WorkoutDatabase` and platform drivers. Existing `.sq` schema and queries are extended where needed for completed workout reconstruction, deletion cleanup, preferences, export snapshots, and list/detail reads.

**Local Data Model**: Active workout rows, active exercises, exercise sets, active session singleton, active workout UX singleton-by-workout, set drafts, completed workouts, routines/templates, exercise catalog/seed imports, user preferences, personal records, progress points, and export snapshots. Completed workouts reuse the source active workout's persisted exercise/set rows unless schema gaps require explicit completed exercise/set rows.

**Testing**: Android unit tests for SQLDelight repository persistence/recovery using an in-memory SQLite driver; common tests remain for domain behavior and in-memory fixtures. Existing gates continue: `:shared:testDebugUnitTest`, `:shared:compileDebugKotlinAndroid`, `:androidApp:assembleDebug`, `:shared:compileKotlinIosSimulatorArm64`, Material scan, and `git diff --check`.

**Target Platform**: Android runtime first. iOS remains first-class through shared SQL store and existing native SQLDelight driver compile validation.

**Platform Scope**: Shared code owns SQL repository implementation, mapping, validation return behavior, and use-case wiring. Android passes a `PlatformDatabaseDriverFactory` into shared app bootstrap. iOS compile remains adapter-only for this slice.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI.

**Performance Goals**: Hydrate active session and top-level screen state within the existing <=2s local startup budget. Repository operations should be local single-device SQLite operations suitable for ordinary personal workout volumes.

**Constraints**: Offline-only; no cloud/account/network dependency; repository mutations return explicit `FoundationResult`; confirmed sets do not appear logged before persistence succeeds; all persisted weights remain canonical kilograms; no new user-facing UI; no Material 3 reintroduction.

**Scale/Scope**: Single-user local workout database, baseline exercise seed list, active/completed workout ledger, templates, progress rows, preferences, export snapshots, restart recovery. Cloud sync, encryption, import/restore UI, and multi-device conflict resolution remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Existing logging UI/interaction stays unchanged; persistence moves under the same use cases.
- **Ledger Integrity**: PASS. This feature directly makes confirmed active sets, completed workouts, PR source ids, templates, and exports durable and auditable.
- **Session Recovery**: PASS. Active session, rest anchors, route, active workout, drafts, and active UX state are core acceptance criteria.
- **Progress Promise**: PASS. PR/progress rows remain source-traceable, bodyweight reps-only and canonical weight storage are explicitly covered.
- **Local-First Ownership**: PASS. Feature is offline local persistence with stable ids/timestamps and no network dependency.
- **Shared-First KMP**: PASS. SQL store and mappings live in shared code; Android only supplies the driver; iOS remains covered by native driver compile.
- **Design System and Accessibility**: PASS. No new UI is introduced; existing design-system screens consume persisted data.
- **Public Release Gates**: PASS. Plan includes repository/recovery tests, Android build, iOS compile, Material scan, whitespace validation, export sanity, and manual device discovery.

## Project Structure

### Documentation (this feature)

```text
specs/009-local-persistence/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- local-persistence-contracts.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- App.kt
|   |-- AppState.kt
|   |-- data/repository/
|   |   |-- InMemoryFoundationStore.kt
|   |   |-- SqlExerciseRepository.kt
|   |   |-- SqlFoundationStore.kt
|   |   |-- SqlProgressRepository.kt
|   |   |-- SqlRoutineRepository.kt
|   |   |-- SqlSetLedgerRepository.kt
|   |   `-- SqlWorkoutRepository.kt
|   |-- domain/repository/FoundationRepositories.kt
|   `-- platform/PlatformAdapters.kt
|-- src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
|   |-- Database.sq
|   |-- ExerciseQueries.sq
|   |-- ProgressQueries.sq
|   |-- RoutineQueries.sq
|   |-- SetQueries.sq
|   |-- WorkoutQueries.sq
|   `-- migrations/
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
|-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/
|-- src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/
`-- src/commonTest/kotlin/com/jjswigut/oopsallprs/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt
```

**Structure Decision**: Implement a shared `SqlFoundationStore` that owns SQLDelight mapping and implements repository interfaces. Keep the thin `Sql*Repository` classes as typed adapters around the SQL store to minimize dependency churn in use-case wiring. Keep `InMemoryFoundationStore` for fast tests only.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Use SQLDelight as the durable local store because schema, generated database, and platform drivers already exist.
- Add a single shared SQL store and keep the existing repository interfaces.
- Keep completed workout reconstruction ledger-safe by preserving source active exercise/set rows when finishing.
- Use Android unit tests with in-memory SQLite for durable restart-style assertions.
- Keep in-memory fixtures for targeted domain tests, while production `App` bootstrap uses platform SQL drivers.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/local-persistence-contracts.md](./contracts/local-persistence-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. Phase 0 and Phase 1 artifacts keep the slice local-first,
ledger-safe, session-recovery-focused, shared-first, and backed by automated
plus manual release evidence. No unresolved clarification or complexity
violation remains.
