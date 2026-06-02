# Tasks: Timed Exercise PRs

**Input**: Design documents from `/specs/019-timed-exercise-prs/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required. This feature changes active workout logging, local persistence, active draft recovery, exercise catalog classification, routines, PR derivation, Progress, History, and export output.

**Organization**: Tasks are grouped by user story so timed set logging can land as the MVP, followed by PR/progress visibility, routines/previous values, and cross-surface review/export behavior.

## Phase 1: Setup

**Purpose**: Confirm feature artifacts and prepare validation evidence.

- [X] T001 Confirm `.specify/feature.json` and `AGENTS.md` point to `specs/019-timed-exercise-prs/`
- [X] T002 Create `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md` with automated and manual validation sections
- [X] T003 [P] Review current set model, draft model, and SQLDelight schema in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSet.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ActiveWorkoutUxState.kt`, and `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq`
- [X] T004 [P] Review current active logging, routine, history, progress, and export surfaces in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/`

---

## Phase 2: Foundational

**Purpose**: Add shared timed data, persistence, and catalog classification that block all stories.

- [X] T005 [P] Add timed logging mode and duration fields to domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseCatalog.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSelection.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSet.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ActiveWorkoutUxState.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/RoutineModels.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ProgressModels.kt`
- [X] T006 Extend SQLDelight schema, queries, and migration for timed duration/catalog fields in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq`, `SetQueries.sq`, `WorkoutQueries.sq`, `RoutineQueries.sq`, `ExerciseQueries.sq`, and `migrations/3.sqm`
- [X] T007 Update in-memory and SQL repository mappings for timed sets, active drafts, routine targets, catalog logging mode, PRs, progress points, and exports in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/InMemoryFoundationStore.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`
- [X] T008 Add seed classification for hold-style timed exercises in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseCatalog.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseSeedIngestion.kt`, and `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/exercise/ExerciseSeedTimedClassificationTest.kt`
- [X] T009 Wire logging mode through picker and custom exercise creation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerModels.kt`, `ExercisePickerStateHolder.kt`, `ExercisePickerFlow.kt`, `ExerciseManagementModels.kt`, `ExerciseManagementStateHolder.kt`, and `ExerciseManagementFlow.kt`
- [X] T010 Update shared test fixtures for timed references and seed rows in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/FoundationFixtures.kt`

**Checkpoint**: Timed data shape, local persistence, and catalog classification exist in shared code.

---

## Phase 3: User Story 1 - Log a Timed Exercise Set (Priority: P1) MVP

**Goal**: A user can add a timed exercise, enter or time a positive duration, log it without reps/weight, and recover timed draft state.

**Independent Test**: Start or resume a workout, add a timed exercise, record a completed hold duration, finish the workout, and verify the completed history shows the exercise and time value without requiring reps or weight.

### Tests for User Story 1

- [X] T011 [P] [US1] Add timed set validation and confirm-set coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TimedSetLoggingUseCaseTest.kt`
- [X] T012 [P] [US1] Add active timed draft recovery coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutTimedLoggingTest.kt`
- [X] T013 [P] [US1] Add timed input model coverage for duration formatting and increments in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/TimedSetInputModelTest.kt`

### Implementation for User Story 1

- [X] T014 [US1] Extend set logging and edit use cases for optional duration in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/SetLoggingUseCases.kt`
- [X] T015 [US1] Add timed draft state actions, validation, persistence, and running timer anchors in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T016 [US1] Add duration-aware active workout view models and default drafts in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T017 [US1] Add timed active logging UI controls in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/CompactSetInput.kt`, `SetRow.kt`, `ExerciseBlock.kt`, and `ActiveWorkoutFlow.kt`
- [X] T018 [US1] Wire timed logging callbacks through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: User Story 1 is functional and independently testable.

---

## Phase 4: User Story 2 - Track Time-Based PRs (Priority: P2)

**Goal**: Timed sets produce longest-duration PRs with source evidence, and ties do not create duplicate PRs.

**Independent Test**: Complete two workouts for the same timed exercise with increasing durations and verify the later workout marks a new time PR with source evidence.

### Tests for User Story 2

- [X] T019 [P] [US2] Add time PR derivation tests for improvements, ties, and source evidence in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TimedPersonalRecordDerivationTest.kt`
- [X] T020 [P] [US2] Add active time PR feedback tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActivePrFeedbackUseCaseTest.kt`
- [X] T021 [P] [US2] Add time PR evidence display tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressEvidenceTest.kt`

### Implementation for User Story 2

- [X] T022 [US2] Extend PR derivation for `PersonalRecordKind.TIME` and `ProgressMetric.TIME` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationUseCase.kt`
- [X] T023 [US2] Extend active PR feedback labels for timed sets in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActivePrFeedbackUseCase.kt`
- [X] T024 [US2] Add duration labels and evidence text for time records in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryModels.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`, and `ProgressChartModels.kt`

**Checkpoint**: User Story 2 is functional and independently testable.

---

## Phase 5: User Story 3 - Use Timed Exercises in Routines and Previous Values (Priority: P3)

**Goal**: Timed exercises can be saved into routines, launched from routines, and prefilled from previous timed durations when targets are blank.

**Independent Test**: Create or launch a routine containing a timed exercise, leave its duration target blank, and verify the active workout pre-fills the last completed duration while keeping the saved routine unchanged.

### Tests for User Story 3

- [X] T025 [P] [US3] Add timed previous-value resolver coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCaseTest.kt`
- [X] T026 [P] [US3] Add timed routine launch precedence and non-mutation coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TimedRoutineLaunchTest.kt`
- [X] T027 [P] [US3] Add routine editor timed target coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineTimedEditorTest.kt`

### Implementation for User Story 3

- [X] T028 [US3] Extend previous-value models and resolver for timed durations in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/PreviousWorkoutValues.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCase.kt`
- [X] T029 [US3] Apply timed routine target/default precedence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineUseCases.kt`
- [X] T030 [US3] Add timed routine editor state and UI fields in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineEditorModels.kt`, `RoutineStateHolder.kt`, `RoutineEditorFlow.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: User Story 3 is functional and independently testable.

---

## Phase 6: User Story 4 - Review Timed Progress (Priority: P4)

**Goal**: History, Progress, charts, and exports show duration values for timed exercises.

**Independent Test**: Complete timed exercise workouts that produce time PRs, open Progress for that exercise, and verify the latest PR and trend use duration labels.

### Tests for User Story 4

- [X] T031 [P] [US4] Add history timed duration display tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryTimedDisplayTest.kt`
- [X] T032 [P] [US4] Add progress timed chart/label tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressTimedDisplayTest.kt`
- [X] T033 [P] [US4] Add timed export coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/export/TimedExportTest.kt`

### Implementation for User Story 4

- [X] T034 [US4] Render timed labels in History and active logged rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryModels.kt`, `HistoryFlow.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/SetRow.kt`
- [X] T035 [US4] Render timed Progress cards, chart metric labels, and trend labels in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`, `ProgressChartModels.kt`, and `ProgressFlow.kt`
- [X] T036 [US4] Include timed duration columns and labels in workout and PR exports in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/InMemoryFoundationStore.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt`

**Checkpoint**: User Story 4 is functional and independently testable.

---

## Phase 7: Polish and Cross-Cutting Validation

**Purpose**: Run gates, record evidence, and close tasks.

- [X] T037 Add timed demo seed data or fixture helpers in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCase.kt` and `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/FoundationFixtures.kt`
- [X] T038 Run `./gradlew :shared:testDebugUnitTest --tests "*Timed*"` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T039 Run `./gradlew :shared:testDebugUnitTest --tests "*PersonalRecordDerivationTest" --tests "*Progress*" --tests "*Routine*" --tests "*PreviousWorkout*" --tests "*Export*" --tests "*Seed*"` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T040 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T041 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T042 Run the Material/style guard from `specs/019-timed-exercise-prs/quickstart.md` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T043 Run `git diff --check -- .` and record results in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T044 Record manual Pixel validation as completed or deferred in `specs/019-timed-exercise-prs/validation/timed-exercise-prs-results.md`
- [X] T045 Update this task list in `specs/019-timed-exercise-prs/tasks.md` so completed tasks are checked before reporting completion

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on setup and blocks user stories.
- **US1 Timed Logging (Phase 3)**: Depends on foundational models and persistence.
- **US2 Time PRs (Phase 4)**: Depends on timed logged sets from US1.
- **US3 Routines/Previous Values (Phase 5)**: Depends on foundational timed templates and timed set defaults; can be implemented after US1.
- **US4 Review/Export (Phase 6)**: Depends on US1 and US2 data shape.
- **Polish (Phase 7)**: Depends on all attempted stories.

### User Story Dependencies

- **US1**: MVP because timed sets must exist before PRs, routines, or charts can be trusted.
- **US2**: Builds the product promise on top of timed ledger data.
- **US3**: Adds repeated-workout ergonomics and routine parity.
- **US4**: Makes timed progress inspectable across History, Progress, and exports.

### Parallel Opportunities

- T003 and T004 can run in parallel.
- T005 and T008 can start in parallel with T006 after model shape is agreed.
- T011-T013 can be written in parallel.
- T019-T021 can be written in parallel.
- T025-T027 can be written in parallel.
- T031-T033 can be written in parallel.

## Implementation Strategy

### MVP First

1. Complete setup and foundational timed schema/model work.
2. Implement US1 timed logging and recovery.
3. Run US1 tests and Android compile.

### Incremental Delivery

1. Add US2 time PR derivation and active feedback.
2. Add US3 routine/default support.
3. Add US4 History/Progress/export display.
4. Run all automated gates and document manual Pixel status.

### Risk Controls

- Keep duration storage canonical in milliseconds.
- Do not weaken weighted or bodyweight validation.
- Do not mutate saved routines when applying previous timed values.
- Keep timer recovery anchored to persisted wall-clock instants.
- Keep UI styling inside FitTheme/design-system components.
