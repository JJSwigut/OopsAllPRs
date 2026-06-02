# Tasks: Bootstrap Oops All PRs App Foundation

**Input**: Design documents from `/specs/001-bootstrap-app-foundation/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/foundation-contracts.md](./contracts/foundation-contracts.md), [quickstart.md](./quickstart.md)

**Tests**: Required for this feature. The specification requires validation evidence for workout ledger integrity, session recovery, bodyweight reps-only logging, canonical weight units, seed ingestion, export readiness, offline behavior, shared/platform boundaries, and PR derivation.

**Organization**: Tasks are grouped by user story so each story can be implemented and validated independently after setup and foundation phases.

**Design System Note**: The existing OopsAllPRs design-system module supplies UI components and theme tokens. UI tasks in this feature must integrate that module from shared code rather than creating new component APIs.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the fresh Kotlin Multiplatform project skeleton, Android executable target, iOS-ready structure, and seed-data baseline.

- [X] T001 Create the Gradle project include graph for `shared`, `androidApp`, and `iosApp` in `settings.gradle.kts`
- [X] T002 Create root plugin aliases and repository configuration for the KMP build in `build.gradle.kts`
- [X] T003 [P] Define Kotlin, Compose Multiplatform, AGP, SQLDelight, coroutines, datetime, Koin, UUID, and Vico versions in `gradle/libs.versions.toml`
- [X] T004 Configure KMP targets, source sets, resources, SQLDelight database package, and shared test dependencies in `shared/build.gradle.kts`
- [X] T005 Configure the Android application target, namespace, min/target SDK, Compose, and dependency on `shared` in `androidApp/build.gradle.kts`
- [X] T006 [P] Create the Android manifest, backup policy configuration reference, and main activity declaration in `androidApp/src/main/AndroidManifest.xml`
- [X] T007 [P] Create the Android entry point and Compose bootstrap host in `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`
- [X] T008 [P] Create a minimal iOS app host that consumes the shared framework in `iosApp/iosApp/OopsAllPRsApp.swift` and `iosApp/iosApp/ContentView.swift`
- [X] T009 Copy the existing baseline exercise seed list into `shared/src/commonMain/resources/exercises.csv`
- [X] T010 [P] Create project setup documentation with build commands and platform prerequisites in `README.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Establish shared primitives, persistence boundaries, adapters, and test harnesses that every user story needs.

**Critical**: No user story work should begin until this phase is complete.

- [X] T011 Define stable ID, timestamp, ordering, and mutation result primitives in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/FoundationPrimitives.kt`
- [X] T012 [P] Define explicit domain failure types for workout, set, routine, seed, export, and PR operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/validation/FoundationErrors.kt`
- [X] T013 [P] Implement canonical kilogram storage, pounds/kilograms conversion, and round-trip rounding rules in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/Weight.kt`
- [X] T014 [P] Implement locale-aware decimal parsing for workout inputs in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/validation/DecimalInputParser.kt`
- [X] T015 Create SQLDelight tables for workouts, workout exercises, sets, session state, routines, exercises, seed imports, preferences, PRs, progress points, and exports in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq`
- [X] T016 Create the initial SQLDelight migration and mapper utilities matching the schema in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/migrations/1.sqm` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/db/SqlMappers.kt`
- [X] T017 [P] Define repository interfaces for workouts, routines, exercises, preferences, sessions, PRs, and exports in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt`
- [X] T018 Define `expect` platform interfaces for database driver creation, local settings, rest notifications, file export handoff, haptics, and clock access in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/platform/PlatformAdapters.kt`
- [X] T019 Implement Android `actual` platform adapters for SQLDelight driver creation, settings, notification scheduling, file export handoff, haptics, and clock access in `shared/src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/AndroidPlatformAdapters.kt`
- [X] T020 Implement compileable iOS `actual` platform adapters for SQLDelight driver creation, settings, notification scheduling, file export handoff, haptics, and clock access in `shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/IosPlatformAdapters.kt`
- [X] T021 [P] Create shared test fixtures for clocks, temporary databases, exercise CSV rows, seeded catalog rows, workouts, routines, and set data in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/FoundationFixtures.kt`
- [X] T022 [P] Add seed-bootstrap and shared platform-boundary tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/platform/FoundationBoundaryTest.kt`
- [X] T023 [P] Implement a quoted-field-safe baseline exercise CSV parser and row validator in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCsvParser.kt`
- [X] T024 [P] Implement first-run exercise seed ingestion bootstrap and searchable seed catalog availability in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCatalogInitializer.kt`

**Checkpoint**: Shared infrastructure is ready for story implementation.

---

## Phase 3: User Story 1 - Start and Resume a Local Workout (Priority: P1) MVP

**Goal**: A lifter can start an empty workout, start from an existing routine, or resume an active workout after process recreation without network access.

**Independent Test**: Start a workout from fresh local state, add at least one exercise shell, simulate restart, and verify the same active workout ID, elapsed time, and available next action are restored.

### Tests for User Story 1

- [X] T025 [P] [US1] Add contract tests for start-empty, start-from-routine failure handling, discard, explicit persistence failure behavior, and rest notification schedule/cancel behavior in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleContractTest.kt`
- [X] T026 [P] [US1] Add SQLDelight persistence tests for active workout identity, session pointer, elapsed wall-clock recovery, rest timer recovery, and rest notification anchors in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/session/ActiveSessionPersistenceTest.kt`
- [X] T027 [P] [US1] Add Android startup recovery tests for hydrating an existing active session into the app shell in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/session/AndroidSessionRecoveryTest.kt`

### Implementation for User Story 1

- [X] T028 [P] [US1] Implement active workout and active exercise domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ActiveWorkout.kt`
- [X] T029 [P] [US1] Implement active session state and wall-clock timer domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ActiveSessionState.kt`
- [X] T030 [US1] Implement SQLDelight queries for active workout creation, routine cloning lookup, session pointer writes, rest timer writes, and active hydration in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/WorkoutQueries.sq`
- [X] T031 [US1] Implement workout and session SQL repositories with explicit success/failure results in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlWorkoutRepository.kt`
- [X] T032 [US1] Implement start-empty, start-from-routine, restore-active-session, discard-active-workout, update-rest-timer, and rest notification schedule/cancel use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T033 [US1] Implement active-session hydration, elapsed/rest remaining calculations, and rest notification reconciliation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/session/ActiveSessionCoordinator.kt`
- [X] T034 [US1] Wire startup session hydration into shared app state in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`
- [X] T035 [US1] Implement shared start/resume workout state holder using foundation use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolder.kt`
- [X] T036 [US1] Integrate the existing design system module into a minimal shared Compose workout home flow with start-empty, resume, and start-from-routine actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [X] T037 [US1] Record US1 validation evidence for offline start and process-recovery timing in `specs/001-bootstrap-app-foundation/validation/us1-session-recovery.md`

**Checkpoint**: User Story 1 is functional and independently testable.

---

## Phase 4: User Story 2 - Log Durable Sets Quickly (Priority: P1)

**Goal**: A lifter can add exercises and confirm weighted or bodyweight sets durably, with canonical units, locale decimal input, and explicit logged-set editing.

**Independent Test**: Start an active workout, add a weighted exercise and a bodyweight exercise, confirm sets for both, restart, and verify values, timestamps, and logged status are preserved.

### Tests for User Story 2

- [X] T038 [P] [US2] Add validation tests for weighted sets, bodyweight reps-only sets, fractional loads, invalid reps, and missing weight rules in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/validation/SetValidationTest.kt`
- [X] T039 [P] [US2] Add SQLDelight ledger tests for confirm-set durability, edit-logged-set preservation of `loggedAt`, and restart hydration of logged set tuples in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/workout/SetLedgerPersistenceTest.kt`
- [X] T040 [P] [US2] Add unit conversion and locale decimal input tests for pounds, kilograms, fractional loads, comma decimals, and invalid numeric input in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/model/WeightInputTest.kt`

### Implementation for User Story 2

- [X] T041 [P] [US2] Implement exercise set, set kind, logged status, and edit metadata domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSet.kt`
- [X] T042 [P] [US2] Implement exercise catalog reference and active exercise selection models used by set logging in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSelection.kt`
- [X] T043 [US2] Implement SQLDelight queries for active exercise insertion, planned set insertion, set confirmation, logged-set editing, and active workout set hydration in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/SetQueries.sq`
- [X] T044 [US2] Implement set ledger repository methods with no UI logged state before successful persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlSetLedgerRepository.kt`
- [X] T045 [US2] Implement add-exercise, confirm-set, edit-logged-set, and hydrate-active-sets use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/SetLoggingUseCases.kt`
- [X] T046 [US2] Integrate canonical unit conversion and decimal parsing into set logging use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WeightInputUseCases.kt`
- [X] T047 [US2] Implement shared active workout logging state holder with explicit loading, success, and failure states in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T048 [US2] Integrate existing design system controls into a minimal shared Compose active workout flow for exercise selection, reps input, weight input, bodyweight reps-only logging, confirm-set, and edit-set in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`
- [X] T049 [US2] Record US2 validation evidence for durable weighted/bodyweight logging, restart recovery, and unit round-trip behavior in `specs/001-bootstrap-app-foundation/validation/us2-set-ledger.md`

**Checkpoint**: User Story 2 is functional and independently testable.

---

## Phase 5: User Story 3 - Build Reusable Routines from Real Workouts (Priority: P2)

**Goal**: A lifter can finish a workout, save it as a reusable routine, launch it later, and modify active workouts without mutating the reusable plan.

**Independent Test**: Finish a workout, create a routine from it, start from the routine, modify the active workout, and verify the routine remains unchanged.

### Tests for User Story 3

- [X] T050 [P] [US3] Add routine separation tests for completed-workout-to-routine creation, routine cloning without `loggedAt`, and active workout mutation isolation in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineSeparationTest.kt`
- [X] T051 [P] [US3] Add finish-workout ledger tests proving logged tuples are preserved and unlogged planned sets are stripped from completed history in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/workout/FinishWorkoutLedgerTest.kt`
- [X] T052 [P] [US3] Add SQLDelight tests for routine ordering, routine set templates, completed workout history, and source completed workout links in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/routine/RoutinePersistenceTest.kt`

### Implementation for User Story 3

- [X] T053 [P] [US3] Implement reusable routine, routine exercise, routine set template, and completed workout domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/RoutineModels.kt`
- [X] T054 [US3] Implement SQLDelight queries for completed workout creation, routine creation, routine reads, routine cloning, and routine source links in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/RoutineQueries.sq`
- [X] T055 [US3] Implement routine and completed workout repositories with explicit clone and save-from-history operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlRoutineRepository.kt`
- [X] T056 [US3] Implement finish-workout, save-completed-workout-as-routine, list-routines, and launch-routine use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineUseCases.kt`
- [X] T057 [US3] Add shared routine list and save-as-routine state holder in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T058 [US3] Integrate existing design system controls into a minimal shared Compose routine flow for list, launch, and save-from-completed actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineFlow.kt`
- [X] T059 [US3] Record US3 validation evidence for routine launch, save-from-workout, and active/routine separation in `specs/001-bootstrap-app-foundation/validation/us3-routines.md`

**Checkpoint**: User Story 3 is functional and independently testable.

---

## Phase 6: User Story 4 - Seed Exercise Choices and Preserve User Ownership (Priority: P2)

**Goal**: A lifter receives the baseline exercise catalog on first launch, malformed seed rows are reported, and user-created exercises are preserved across repeated ingestion.

**Independent Test**: Load the seed list, verify required classifications, create a user exercise, re-run ingestion, and verify user data is unchanged.

### Tests for User Story 4

- [X] T060 [P] [US4] Expand CSV parser tests for quoted fields, commas, malformed rows, header validation, duplicate canonical names, and required classifications in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCsvParserTest.kt`
- [X] T061 [P] [US4] Expand seed ingestion tests for baseline row acceptance, idempotent re-import, malformed-row reporting, and user-created exercise preservation in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseSeedIngestionTest.kt`
- [X] T062 [P] [US4] Add exercise catalog tests for search, classification availability, bodyweight detection, and seed/user ownership conflicts in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogTest.kt`

### Implementation for User Story 4

- [X] T063 [P] [US4] Implement exercise catalog item, seed import report, and user-created exercise domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseCatalog.kt`
- [X] T064 [US4] Harden the baseline CSV parser with full malformed-row diagnostics and duplicate canonical-name reporting in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCsvParser.kt`
- [X] T065 [US4] Implement seed hashing, canonical name normalization, idempotent seed upsert, malformed-row reporting, and user-owned conflict preservation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseSeedIngestion.kt`
- [X] T066 [US4] Implement SQLDelight queries for seed import audit rows, user-created exercise rows, ownership conflicts, and exercise search in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ExerciseQueries.sq`
- [X] T067 [US4] Complete exercise catalog repository and search/create use cases for seeded and user-created exercises in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlExerciseRepository.kt`
- [X] T068 [US4] Extend the first-run seed initializer with user-ownership preservation and repeated-ingestion reporting in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCatalogInitializer.kt`
- [X] T069 [US4] Add shared exercise search and user-created exercise state holder in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`
- [X] T070 [US4] Integrate existing design system controls into a minimal shared Compose exercise picker flow for search and user-created exercise actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`
- [X] T071 [US4] Record US4 validation evidence for baseline seed count, malformed-row reporting, bodyweight classification, and user exercise preservation in `specs/001-bootstrap-app-foundation/validation/us4-seed-ingestion.md`

**Checkpoint**: User Story 4 is functional and independently testable.

---

## Phase 7: User Story 5 - Establish PR and Progress Foundations (Priority: P3)

**Goal**: Logged completed workouts produce traceable weighted and bodyweight PRs, progress points, and export-ready local data.

**Independent Test**: Complete weighted and bodyweight workouts, derive PR/progress data, and trace each PR back to source workout and set evidence.

### Tests for User Story 5

- [X] T072 [P] [US5] Add PR derivation tests for weighted best-for-reps, bodyweight reps best, estimated one-rep-max, volume points, and source workout/set references in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationTest.kt`
- [X] T073 [P] [US5] Add progress persistence tests proving materialized PR/progress data is rebuildable from completed logged sets in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/progress/ProgressPersistenceTest.kt`
- [X] T074 [P] [US5] Add export tests for completed workouts, logged sets, exercises, routines, PRs, bodyweight reps-only rows, display-unit conversion, and CSV quoting in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/export/ExportSnapshotTest.kt`

### Implementation for User Story 5

- [X] T075 [P] [US5] Implement personal record, progress point, export snapshot, and export row domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ProgressModels.kt`
- [X] T076 [US5] Implement SQLDelight queries for personal records, progress points, source references, export metadata, and completed workout evidence lookup in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ProgressQueries.sq`
- [X] T077 [US5] Implement PR derivation service for weighted sets, bodyweight reps-only sets, estimated one-rep-max, volume, and source traceability in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationUseCase.kt`
- [X] T078 [US5] Implement progress repository with rebuild and invalidation entry points for set confirmation, logged-set edits, and completed workout changes in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlProgressRepository.kt`
- [X] T079 [US5] Implement export-ready data service for workouts, sets, exercises, routines, PRs, display-unit conversion, and CSV escaping in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/export/ExportService.kt`
- [X] T080 [US5] Add shared PR/progress state holder for weighted and bodyweight records in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T081 [US5] Integrate existing design system controls into a minimal shared Compose progress and PR evidence flow in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`
- [X] T082 [US5] Record US5 validation evidence for weighted PRs, bodyweight PRs, traceability, and export-ready data in `specs/001-bootstrap-app-foundation/validation/us5-progress-prs.md`

**Checkpoint**: User Story 5 is functional and independently testable.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Verify the full foundation against release gates and clean up cross-story integration.

- [X] T083 [P] Integrate the existing Neo-Glass design-system module's `FitTheme` token/theme entry point in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/designsystem/DesignSystemBridge.kt`
- [X] T084 [P] Add shared accessibility semantics, minimum touch-target checks, and reduced-motion hooks around existing design system components in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/accessibility/FoundationAccessibility.kt`
- [X] T085 Add a foundation integration test that covers start, add exercise, confirm weighted set, confirm bodyweight set, finish, derive PR, and export offline in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/integration/FoundationOfflineFlowTest.kt`
- [X] T086 Add a restore performance validation helper for the <=2 second active workout recovery target in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/session/SessionRestorePerformanceTest.kt`
- [X] T087 Document Android Auto Backup behavior, permissions, export behavior, and release artifact checks in `docs/release/foundation-release-gates.md`
- [X] T088 Run `./gradlew :shared:build`, `./gradlew :androidApp:assembleDebug`, the configured iOS shared framework compile task, `./gradlew testDebugUnitTest`, and `./gradlew check`, then record results in `specs/001-bootstrap-app-foundation/validation/final-build-results.md`
- [X] T089 Defer the quickstart milestone manual validation flow as a release gate and record the deferral in `specs/001-bootstrap-app-foundation/validation/milestone-manual-gate.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- Phase 1 Setup has no dependencies.
- Phase 2 Foundational depends on Phase 1 and blocks every user story.
- Phase 3 US1 and Phase 4 US2 are the MVP P1 stories and can start after Phase 2.
- Phase 5 US3 and Phase 6 US4 are P2 stories and can start after Phase 2, but US3 benefits from US1/US2 and US4 benefits from the foundational schema and app startup.
- Phase 7 US5 depends on completed-workout evidence from US3 and logged-set evidence from US2.
- Phase 8 Polish depends on all desired user stories for the milestone.

### User Story Dependencies

- US1 Start and Resume a Local Workout: depends only on Phase 2.
- US2 Log Durable Sets Quickly: depends on Phase 2 and can run alongside US1 once active workout interfaces are stable.
- US3 Build Reusable Routines from Real Workouts: depends on US1 lifecycle and US2 logged-set ledger behavior.
- US4 Seed Exercise Choices and Preserve User Ownership: depends on Phase 2 seed bootstrap and can run alongside US1/US2 for user-owned exercise refinements.
- US5 Establish PR and Progress Foundations: depends on US2 logged sets and US3 completed workout evidence.

### Within Each User Story

- Write and run story tests first; they should fail before implementation.
- Implement domain models before repositories and use cases.
- Implement SQLDelight queries before SQL repositories.
- Implement shared use cases before shared state holders and design-system-backed Compose flows; Android app code only hosts shared flows.
- Record validation evidence before considering the story complete.

---

## Parallel Opportunities

- Setup tasks T003, T006, T007, T008, and T010 can run in parallel after T001/T002 ownership is clear.
- Foundational tasks T012, T013, T014, T017, T021, T022, T023, and T024 touch separate files and can run in parallel.
- US1 tests T025, T026, and T027 can run in parallel.
- US2 tests T038, T039, and T040 can run in parallel.
- US3 tests T050, T051, and T052 can run in parallel.
- US4 tests T060, T061, and T062 can run in parallel.
- US5 tests T072, T073, and T074 can run in parallel.
- Shared UI integration tasks and validation documentation within each story can run in parallel after shared use cases are stable.

## Parallel Example: User Story 1

```text
Task: "T025 [P] [US1] Add contract tests for start-empty, start-from-routine failure handling, discard, and explicit persistence failure behavior in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleContractTest.kt"
Task: "T026 [P] [US1] Add SQLDelight persistence tests for active workout identity, session pointer, elapsed wall-clock recovery, and rest timer recovery in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/session/ActiveSessionPersistenceTest.kt"
Task: "T027 [P] [US1] Add Android startup recovery tests for hydrating an existing active session into the app shell in shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/session/AndroidSessionRecoveryTest.kt"
```

## Parallel Example: User Story 2

```text
Task: "T038 [P] [US2] Add validation tests for weighted sets, bodyweight reps-only sets, fractional loads, invalid reps, and missing weight rules in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/validation/SetValidationTest.kt"
Task: "T039 [P] [US2] Add SQLDelight ledger tests for confirm-set durability, edit-logged-set preservation of loggedAt, and restart hydration of logged set tuples in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/workout/SetLedgerPersistenceTest.kt"
Task: "T040 [P] [US2] Add unit conversion and locale decimal input tests for pounds, kilograms, fractional loads, comma decimals, and invalid numeric input in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/model/WeightInputTest.kt"
```

## Parallel Example: User Story 4

```text
Task: "T060 [P] [US4] Add CSV parser tests for quoted fields, commas, malformed rows, header validation, duplicate canonical names, and required classifications in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseCsvParserTest.kt"
Task: "T061 [P] [US4] Add seed ingestion tests for baseline row acceptance, idempotent re-import, malformed-row reporting, and user-created exercise preservation in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseSeedIngestionTest.kt"
Task: "T062 [P] [US4] Add exercise catalog tests for search, classification availability, bodyweight detection, and seed/user ownership conflicts in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogTest.kt"
```

---

## Implementation Strategy

### MVP First

1. Complete Phase 1 Setup.
2. Complete Phase 2 Foundational.
3. Complete Phase 3 US1 and Phase 4 US2.
4. Stop and validate offline start/resume plus durable weighted/bodyweight set logging.
5. Only then add routines, seed ownership, and PR/progress layers.

### Incremental Delivery

1. Setup plus foundation: app builds, shared/platform boundaries exist, database schema is typed, and baseline seed ingestion is available before exercise search.
2. US1: active workout lifecycle and recovery.
3. US2: durable set ledger, bodyweight reps-only logging, units, and logged-set editing.
4. US3: completed workouts and routines.
5. US4: robust seed ingestion and user exercise ownership.
6. US5: PR/progress derivation and export-ready local data.
7. Polish: build checks, manual milestone validation, backup/export/release documentation.

### Validation Gates

- Every behavior-changing story must have failing tests before implementation and passing tests before checkpoint.
- Shared foundation code must pass the platform-boundary test before Android integration is considered complete.
- Final milestone requires `./gradlew :shared:build`, `./gradlew :androidApp:assembleDebug`, `./gradlew testDebugUnitTest`, and `./gradlew check` results recorded in the validation folder.
