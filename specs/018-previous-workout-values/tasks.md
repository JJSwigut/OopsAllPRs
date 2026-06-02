# Tasks: Previous Workout Values

**Input**: Design documents from `/specs/018-previous-workout-values/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: Required. This feature changes active workout defaults, routine launch, ledger-derived context, and recovery-sensitive drafts.

**Organization**: Tasks are grouped by user story so add-exercise defaults can land as the MVP, followed by routine launch precedence and cross-cutting ledger/recovery validation.

## Phase 1: Setup

**Purpose**: Establish feature artifacts and validation evidence files.

- [X] T001 Confirm `.specify/feature.json` and `AGENTS.md` point to `specs/018-previous-workout-values/`
- [X] T002 Create `specs/018-previous-workout-values/validation/previous-workout-values-results.md` with automated and deferred manual validation sections
- [X] T003 [P] Review existing active workout defaulting in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T004 [P] Review existing add-exercise and routine launch mutation paths in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/SetLoggingUseCases.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`

---

## Phase 2: Foundational

**Purpose**: Add the shared previous-value resolver that all stories use.

- [X] T005 [P] Add previous-value read models and resolution helpers in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/PreviousWorkoutValues.kt`
- [X] T006 [P] Add resolver tests for most-recent completed workout selection, set ordering, invalid-set fallback, and bodyweight reps-only behavior in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCaseTest.kt`
- [X] T007 Implement `PreviousWorkoutDefaultsUseCase` using `WorkoutRepository.completedWorkouts()` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCase.kt`
- [X] T008 Wire `PreviousWorkoutDefaultsUseCase` into app construction in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/di/AppModule.kt`, and `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/FoundationFixtures.kt`

**Checkpoint**: Previous-value derivation exists in shared code and is independently tested.

---

## Phase 3: User Story 1 - Add an Exercise With Last Values Ready (Priority: P1)

**Goal**: Adding an exercise to an active workout creates a visible next-set draft from the latest completed workout for that exercise.

**Independent Test**: Complete a workout, start a new empty workout, add the same exercise, and verify the visible draft is prefilled from prior completed values.

### Tests for User Story 1

- [X] T009 [P] [US1] Add active add-exercise previous weighted default coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPreviousValuesTest.kt`
- [X] T010 [P] [US1] Add active add-exercise bodyweight reps-only previous default coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPreviousValuesTest.kt`
- [X] T011 [P] [US1] Add no-history fallback coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPreviousValuesTest.kt`

### Implementation for User Story 1

- [X] T012 [US1] Extend `ActiveWorkoutStateHolder.addExercise()` to seed and persist a previous-value draft after successful add in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T013 [US1] Ensure previous-value drafts preserve focus and active UX draft persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T014 [US1] Run previous-value resolver and active add-exercise tests and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`

**Checkpoint**: User Story 1 is functional and independently testable.

---

## Phase 4: User Story 2 - Launch a Routine Without Losing Planned Targets (Priority: P2)

**Goal**: Routine launch keeps explicit planned targets and uses previous values only for missing target fields.

**Independent Test**: Launch routines with explicit targets, partial targets, and missing targets after completed history exists, then verify target precedence.

### Tests for User Story 2

- [X] T015 [P] [US2] Add explicit routine target precedence tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutRoutineLaunchTest.kt`
- [X] T016 [P] [US2] Add missing and partial routine target fill tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutRoutineLaunchTest.kt`
- [X] T017 [P] [US2] Add routine non-mutation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutRoutineLaunchTest.kt`

### Implementation for User Story 2

- [X] T018 [US2] Inject previous-value defaults into `WorkoutLifecycleUseCases` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T019 [US2] Apply previous values only to missing routine-launched set fields in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T020 [US2] Preserve explicit routine target values and saved routine rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T021 [US2] Run routine launch previous-value tests and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`

**Checkpoint**: User Story 2 is functional and independently testable.

---

## Phase 5: User Story 3 - Preserve Units, Ledger, and Recovery (Priority: P3)

**Goal**: Previous-value defaults stay canonical, recover correctly, and do not mutate completed ledger data or exports before logging.

**Independent Test**: Use previous-value drafts across weighted/bodyweight cases, recover active state, and verify history/routine/export data is unchanged unless a set is explicitly logged.

### Tests for User Story 3

- [X] T022 [P] [US3] Add active draft recovery coverage for previous-value drafts in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPreviousValuesTest.kt`
- [X] T023 [P] [US3] Add ledger/export non-mutation coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutLedgerIntegrityTest.kt`
- [X] T024 [P] [US3] Add canonical weight and bodyweight validation coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCaseTest.kt`

### Implementation for User Story 3

- [X] T025 [US3] Verify previous-value draft persistence uses existing `PersistedSetDraft` canonical weight fields in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T026 [US3] Ensure previous-value lookup does not trigger PR/progress/export writes in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PreviousWorkoutDefaultsUseCase.kt`
- [X] T027 [US3] Run recovery and ledger integrity tests and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`

**Checkpoint**: User Story 3 is functional and independently testable through automated gates.

---

## Phase 6: Polish and Cross-Cutting Validation

**Purpose**: Complete validation and documentation without requiring manual real-device validation in this turn.

- [X] T028 Run `./gradlew :shared:testDebugUnitTest --tests "*PreviousWorkout*"` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T029 Run targeted active workout and routine regression tests from `specs/018-previous-workout-values/quickstart.md` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T030 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T031 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T032 Run the Material/style guard from `specs/018-previous-workout-values/quickstart.md` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T033 Run `git diff --check -- .` and record results in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T034 Mark manual Android smoke as deferred by user request in `specs/018-previous-workout-values/validation/previous-workout-values-results.md`
- [X] T035 Update this task list in `specs/018-previous-workout-values/tasks.md` so completed tasks are checked before commit

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on setup and blocks user stories.
- **US1 Add Exercise (Phase 3)**: Depends on foundational resolver.
- **US2 Routine Launch (Phase 4)**: Depends on foundational resolver; can be developed independently from US1 wiring.
- **US3 Ledger/Recovery (Phase 5)**: Depends on US1/US2 integration points.
- **Polish (Phase 6)**: Depends on all attempted user stories.

### User Story Dependencies

- **US1**: MVP because it improves the fastest empty-workout/add-exercise path.
- **US2**: Adds routine launch behavior while preserving routine intent.
- **US3**: Validates recovery, canonical units, and non-mutation across the completed feature.

### Parallel Opportunities

- T003 and T004 can run in parallel.
- T005 and T006 can start in parallel if tests encode the intended model contract first.
- T009-T011 can be written in parallel.
- T015-T017 can be written in parallel.
- T022-T024 can be written in parallel.

## Implementation Strategy

### MVP First

1. Complete setup and foundational resolver.
2. Implement US1 add-exercise previous defaults.
3. Run US1 tests and Android compile.

### Incremental Delivery

1. Add US2 routine launch precedence.
2. Add US3 recovery/non-mutation coverage.
3. Run all automated gates and document deferred manual smoke.

### Risk Controls

- Do not add a database migration unless existing repositories cannot expose the needed completed history.
- Do not mutate saved routines when applying previous values.
- Do not add UI prompts or settings in this slice.
- Keep all logic in shared code so iOS remains first-class.
