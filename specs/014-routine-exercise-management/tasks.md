# Tasks: Routine & Exercise Management

**Input**: Design documents from `/specs/014-routine-exercise-management/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Required. This feature changes shared state holders, local persistence, routine launch behavior, exercise catalog mutation, and active-session non-regression.

## Phase 1: Setup

**Purpose**: Confirm feature artifacts and validation targets are ready.

- [X] T001 Confirm spec, plan, research, data model, contracts, quickstart, and validation placeholder exist in `specs/014-routine-exercise-management/`
- [X] T002 Confirm AGENTS.md points to `specs/014-routine-exercise-management/plan.md`

---

## Phase 2: Foundational

**Purpose**: Shared repository/use-case operations and UI models that block all user stories.

- [X] T003 Extend exercise repository contract with list user-created, find by id, update user-created, and archive user-created operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt`
- [X] T004 Update in-memory exercise repository behavior for user-created update/archive and active-only search in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/InMemoryFoundationStore.kt`
- [X] T005 Update SQLDelight exercise queries for id lookup, user-created list, update, and archive in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/ExerciseQueries.sq`
- [X] T006 Update SQL repository mapping and exercise mutation operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T007 Extend exercise catalog use cases for management create/edit/archive operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCases.kt`
- [X] T008 Extend routine use cases for create/update routine save operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineUseCases.kt`

---

## Phase 3: User Story 1 - Create A Routine From Scratch (Priority: P1) MVP

**Goal**: Users can create a routine from Train, add exercises, configure planned sets/rest, save, and launch it.

**Independent Test**: Create a two-exercise routine with weighted and bodyweight planned sets, save, launch, and verify active workout planned data.

### Tests for User Story 1

- [X] T009 [P] [US1] Add common routine create-save-launch tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineBuilderCreateTest.kt`
- [X] T010 [P] [US1] Add SQL routine persistence create/update test in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlRoutineManagementPersistenceTest.kt`

### Implementation for User Story 1

- [X] T011 [US1] Add routine editor draft/view models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineEditorModels.kt`
- [X] T012 [US1] Implement create routine draft state-holder actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T013 [US1] Add routine editor UI for create, exercise list, set targets, rest controls, cancel, and save in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineEditorFlow.kt`
- [X] T014 [US1] Add Train entry point and route routine editor overlay in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T015 [US1] Wire routine editor exercise selection through existing picker result models or shared catalog search in `RoutineStateHolder.kt` and `AppShell.kt`

---

## Phase 4: User Story 2 - Edit Existing Routines (Priority: P2)

**Goal**: Users can edit saved routine names, exercises, sets, and rest without mutating completed history.

**Independent Test**: Edit an existing routine, save, launch, and verify completed workout history remains unchanged.

### Tests for User Story 2

- [X] T016 [P] [US2] Add common routine edit/history immutability tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineBuilderEditTest.kt`

### Implementation for User Story 2

- [X] T017 [US2] Implement edit existing routine draft loading and save update behavior in `RoutineStateHolder.kt`
- [X] T018 [US2] Add edit actions to Train routine rows and preserve delete behavior in `WorkoutHomeFlow.kt` and `AppShell.kt`
- [X] T019 [US2] Ensure edited routine launch reflects new planned sets/rest through `WorkoutLifecycleUseCases.kt` tests and existing launch path

---

## Phase 5: User Story 3 - Manage User-Created Exercises (Priority: P3)

**Goal**: Users can create, rename, search, and archive user-created exercises while seeded exercises remain read-only.

**Independent Test**: Create a custom exercise, edit it, find it in search, archive it, and verify it is hidden from normal selection while history snapshots remain readable.

### Tests for User Story 3

- [X] T020 [P] [US3] Add common exercise management state-holder tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExerciseManagementStateHolderTest.kt`
- [X] T021 [P] [US3] Add Android SQL exercise update/archive tests in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlExerciseManagementPersistenceTest.kt`

### Implementation for User Story 3

- [X] T022 [US3] Add exercise management models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExerciseManagementModels.kt`
- [X] T023 [US3] Implement exercise management state holder in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExerciseManagementStateHolder.kt`
- [X] T024 [US3] Add exercise management UI flow in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExerciseManagementFlow.kt`
- [X] T025 [US3] Wire Profile entry point and management overlay in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T026 [US3] Ensure archived user-created exercises stay hidden from picker/search by repository and use-case behavior in `ExerciseCatalogUseCases.kt`

---

## Phase 6: User Story 4 - Preserve Fast Logging From Management Flows (Priority: P4)

**Goal**: Routine/exercise management does not clear active workout state, focus, drafts, rest timers, or resume banners.

**Independent Test**: Start an active workout with a draft/rest timer, open and cancel management flows, and verify recovery state remains valid.

### Tests for User Story 4

- [X] T027 [P] [US4] Add active session non-regression tests around management flows in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/ManagementFlowSessionRegressionTest.kt`

### Implementation for User Story 4

- [X] T028 [US4] Keep routine and exercise overlays independent of active workout state in `AppShell.kt`
- [X] T029 [US4] Refresh routine/exercise lists after saves without rehydrating or clearing active workout state in `AppState.kt` and relevant state holders

---

## Phase 7: Polish & Validation

**Purpose**: Finish validation evidence and guardrails.

- [X] T030 Update validation results in `specs/014-routine-exercise-management/validation/routine-exercise-management-results.md`
- [X] T031 Run `./gradlew :shared:testDebugUnitTest`
- [X] T032 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- [X] T033 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T034 Run Material guard against shared/design-system sources
- [X] T035 Run `git diff --check -- .`
- [X] T036 Record manual Android smoke outcome or defer note in `specs/014-routine-exercise-management/validation/routine-exercise-management-results.md`

## Dependencies & Execution Order

- Phase 1 has no dependencies.
- Phase 2 blocks all user stories.
- US1 is MVP and should complete before US2 because edit reuses create draft behavior.
- US3 can proceed after Phase 2 and is independent of routine UI except shared catalog search behavior.
- US4 depends on management flows from US1 and US3.
- Polish depends on all desired stories being complete.

## Parallel Opportunities

- T003-T006 affect different repository/query files but should be coordinated before compile.
- T009 and T010 can be written in parallel.
- T020 and T021 can be written in parallel.
- US3 can be implemented in parallel with US2 after the shared exercise use cases are available.

## Implementation Strategy

1. Extend exercise/routine domain and persistence foundations.
2. Deliver US1 as MVP: create routine, save, launch.
3. Add US2 editing on the same routine editor state.
4. Add US3 exercise management in Profile.
5. Validate US4 active-workout non-regression.
6. Run validation gates and record results.
