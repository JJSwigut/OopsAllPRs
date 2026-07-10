# Tasks: Routine Circuits & Supersets

**Input**: Design documents from `/specs/024-routine-circuits-supersets/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

## Phase 1: Setup

- [X] T001 Verify existing ignore files cover Kotlin/Gradle outputs in `.gitignore`
- [X] T002 [P] Review routine launch and editor tests for fixture patterns in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/`

## Phase 2: Foundational

- [X] T003 Add routine grouping domain metadata in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/RoutineModels.kt`
- [X] T004 Add active workout group display metadata in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSelection.kt`
- [X] T005 Add SQLDelight routine exercise grouping columns and migration in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/`
- [X] T006 Update SQL mappers for routine and active launch group metadata in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`

## Phase 3: User Story 1 - Group Routine Exercises (Priority: P1)

**Goal**: Users can group adjacent routine exercises as supersets or circuits while creating or editing routines.

**Independent Test**: Create or edit a routine draft, group adjacent exercises, save/reopen it, and verify group membership and labels persist.

- [X] T007 [P] [US1] Add routine grouping draft models and helpers in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineEditorModels.kt`
- [X] T008 [US1] Add group and ungroup state-holder operations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T009 [P] [US1] Add routine editor grouping tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineBuilderCreateTest.kt`
- [X] T010 [P] [US1] Add routine persistence grouping tests in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlRoutineManagementPersistenceTest.kt`

## Phase 4: User Story 2 - Launch Grouped Routines (Priority: P2)

**Goal**: Group labels carry into active workouts without changing set logging behavior.

**Independent Test**: Launch grouped routines and verify active exercise group context plus unchanged logging and finish behavior.

- [X] T011 [US2] Carry routine group context through routine launch in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T012 [US2] Surface group labels in active workout display models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T013 [US2] Render compact group labels in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ExerciseBlock.kt`
- [X] T014 [P] [US2] Add grouped launch tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TemplateLaunchSeparationTest.kt`

## Phase 5: User Story 3 - Edit Or Remove Groups (Priority: P3)

**Goal**: Users can remove groups or remove grouped exercises without losing planned sets.

**Independent Test**: Remove a group and remove one exercise from a grouped pair; verify exercises and sets remain valid and one-exercise groups are cleaned up.

- [X] T015 [US3] Normalize group metadata after exercise removal in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T016 [P] [US3] Add group removal tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineBuilderEditTest.kt`

## Final Phase: Polish & Cross-Cutting Concerns

- [X] T017 [P] Add old-routine compatibility coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineSeparationTest.kt`
- [X] T018 [P] Run focused routine, launch, persistence, and ledger regression tests
- [X] T019 Update `specs/024-routine-circuits-supersets/quickstart.md` with validation results if command availability differs locally

## Dependencies

- Phase 1 before all implementation.
- Phase 2 before all user stories.
- US1 before US2 and US3 because launch and edit behavior depend on group metadata.
- US2 and US3 can proceed independently after US1.

## Parallel Opportunities

- T002 can run independently of T001.
- T009 and T010 can be written in parallel after T007/T008 stabilize.
- T014 can be written while UI display work proceeds.
- T016 and T017 can be written independently after group metadata exists.

## Implementation Strategy

1. Deliver the foundational data shape and migration.
2. Complete US1 as the MVP: grouping in drafts and persisted routines.
3. Add US2 launch/display carryover without changing logging semantics.
4. Add US3 cleanup/removal behavior and regression coverage.
