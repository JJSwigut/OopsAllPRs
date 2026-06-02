# Tasks: Durable Local Persistence

**Input**: Design documents from `/specs/009-local-persistence/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/local-persistence-contracts.md`

**Tests**: Required. This slice changes durable persistence, session recovery, ledger integrity, PR/progress evidence, preferences, export output, and app runtime wiring.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Phase 1: Setup

**Purpose**: Confirm the feature package and SQLDelight baseline are ready.

- [X] T001 Verify Spec Kit artifacts for feature 009 have no unresolved placeholders in `specs/009-local-persistence/spec.md`, `specs/009-local-persistence/plan.md`, `specs/009-local-persistence/research.md`, `specs/009-local-persistence/data-model.md`, `specs/009-local-persistence/contracts/local-persistence-contracts.md`, and `specs/009-local-persistence/quickstart.md`
- [X] T002 Confirm `AGENTS.md` points to `specs/009-local-persistence/plan.md`
- [X] T003 [P] Inspect SQLDelight query coverage in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/WorkoutQueries.sq`, `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/SetQueries.sq`, `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/RoutineQueries.sq`, `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ExerciseQueries.sq`, and `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ProgressQueries.sq`

---

## Phase 2: Foundational

**Purpose**: Shared SQL store infrastructure that all user stories depend on.

**CRITICAL**: No user story can complete until repository construction and SQL mapping exist.

- [X] T004 Add missing SQLDelight queries for active cleanup, completed workout lists, routine children, preferences, export snapshots, and source ledger reads in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/WorkoutQueries.sq`, `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/SetQueries.sq`, `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/RoutineQueries.sq`, and `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ProgressQueries.sq`
- [X] T005 Create SQLDelight test harness for in-memory repository recreation in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStoreTestHarness.kt`
- [X] T006 Implement shared SQL domain mapping and repository interfaces in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T007 Update thin `Sql*Repository` adapters to delegate to `SqlFoundationStore` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlWorkoutRepository.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlSetLedgerRepository.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlRoutineRepository.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlExerciseRepository.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlProgressRepository.kt`
- [X] T008 Wire persistent app construction through `WorkoutDatabase` and platform drivers in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt`, and `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`
- [X] T009 Update dependency injection wiring for SQL store construction in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/di/AppModule.kt`

**Checkpoint**: SQL-backed repositories can compile and be constructed.

---

## Phase 3: User Story 1 - Recover Active Workout After Restart (Priority: P1)

**Goal**: Active workouts, logged sets, drafts, active UX state, and session/rest anchors survive repository recreation.

**Independent Test**: Write active workout data through SQL repositories, recreate repository wrappers against the same database, and verify the active workout and session state reload.

### Tests for User Story 1

- [X] T010 [P] [US1] Add active workout recovery SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlActiveWorkoutRecoveryTest.kt`
- [X] T011 [P] [US1] Add discard cleanup SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlActiveWorkoutRecoveryTest.kt`

### Implementation for User Story 1

- [X] T012 [US1] Implement SQL-backed active workout create/save/load/current/discard in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T013 [US1] Implement SQL-backed set confirm/edit behavior in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T014 [US1] Implement SQL-backed session, active UX, and set draft behavior in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`

**Checkpoint**: Active workout recovery works independently.

---

## Phase 4: User Story 2 - Preserve Completed Workout Ledger (Priority: P2)

**Goal**: Completed workouts, templates, PRs, progress points, and source evidence survive repository recreation.

**Independent Test**: Finish a workout, derive PRs, save a template, recreate repositories, and verify completed history, template data, PRs, progress points, and source ids reload.

### Tests for User Story 2

- [X] T015 [P] [US2] Add completed workout ledger SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlCompletedLedgerPersistenceTest.kt`
- [X] T016 [P] [US2] Add routine/template persistence SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlCompletedLedgerPersistenceTest.kt`
- [X] T017 [P] [US2] Add PR/progress source traceability SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlProgressPersistenceIntegrationTest.kt`

### Implementation for User Story 2

- [X] T018 [US2] Implement SQL-backed finish/completed workout reconstruction in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T019 [US2] Implement SQL-backed routine/template save/load/list in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T020 [US2] Implement SQL-backed personal record and progress point replacement/load in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`

**Checkpoint**: Completed ledger and progress persistence work independently.

---

## Phase 5: User Story 3 - Persist Catalog, Preferences, And Exports (Priority: P3)

**Goal**: Exercise catalog, seed imports, user exercises, unit preference, and exports survive repository recreation.

**Independent Test**: Seed exercises, create a user exercise, change unit, export data, recreate repositories, and verify durable catalog/preference/export behavior.

### Tests for User Story 3

- [X] T021 [P] [US3] Add exercise seed/user exercise SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlCatalogPreferenceExportTest.kt`
- [X] T022 [P] [US3] Add weight-unit preference SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlCatalogPreferenceExportTest.kt`
- [X] T023 [P] [US3] Add export-from-durable-state SQL test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlCatalogPreferenceExportTest.kt`

### Implementation for User Story 3

- [X] T024 [US3] Implement SQL-backed exercise catalog search/list/seed/user-created save in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T025 [US3] Implement SQL-backed preference load/save in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T026 [US3] Implement SQL-backed export output and export snapshot write in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`

**Checkpoint**: Catalog, preference, and export persistence work independently.

---

## Phase 6: Polish and Release Validation

**Purpose**: Cross-cutting quality gates for shared KMP, local-first persistence, and release readiness.

- [X] T027 [P] Verify production app construction no longer uses `InMemoryFoundationStore` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, and `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`
- [X] T028 Run `./gradlew :shared:testDebugUnitTest`
- [X] T029 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- [X] T030 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T031 Run Material scan: `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts`
- [X] T032 Run whitespace validation: `git diff --check -- .`
- [X] T033 Attempt Android manual gate discovery with `~/Library/Android/sdk/platform-tools/adb devices -l` and `~/Library/Android/sdk/emulator/emulator -list-avds`; if no target exists, document the deferred manual gate
- [X] T034 Stage and commit feature 009 changes with message `Implement durable local persistence`

---

## Dependencies and Execution Order

- Phase 1 must complete before foundational SQL implementation.
- Phase 2 blocks all user stories because repository construction and mapping are shared.
- US1 is the MVP and validates the most important local-first recovery behavior.
- US2 depends on source active workout/set reconstruction from US1.
- US3 depends on shared SQL store mapping from Phase 2 and can be validated after US1/US2.
- Phase 6 runs after all selected stories are implemented.

## Parallel Opportunities

- T003 can run independently after setup.
- T010 and T011 share a file and should be coordinated, but can be reasoned about independently before implementation.
- T015, T016, and T017 touch two test files and can be prepared after the harness exists.
- T021, T022, and T023 share a file and should be coordinated.
- T028, T029, T030, T031, and T032 are independent validation commands but should be reported as separate release gates.

## Implementation Strategy

1. Extend SQLDelight query coverage and create the SQL test harness.
2. Implement `SqlFoundationStore` and adapter wiring.
3. Validate active workout restart recovery first.
4. Validate completed ledger, routine, and progress source traceability.
5. Validate catalog, preference, and export persistence.
6. Run Android-first and iOS compile gates before commit.
