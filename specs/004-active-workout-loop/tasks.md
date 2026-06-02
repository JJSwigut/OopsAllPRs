---
description: "Task list for the Active Workout Logging Loop"
---

# Tasks: Active Workout Logging Loop

**Input**: Design documents from `/specs/004-active-workout-loop/`

**Scope**: Implement the active workout logging MVP: exercise blocks, editable
set drafts, inline roller fields, one-tap logging, bodyweight reps-only support,
add-exercise focus handoff, non-destructive errors, and shared validation.

## Phase 1: Setup

- [X] T001 Create `specs/004-active-workout-loop/validation/active-workout-results.md` with sections for unit tests, Android build, iOS compile, Material scan, and manual validation

---

## Phase 2: Foundational

- [X] T002 [P] [US1] Add active workout view/draft/logged row models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T003 [P] [US2] Add roller field value/range helpers in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerFieldModels.kt`
- [X] T004 Add active workout lookup helpers to `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T005 Add one-tap set draft confirmation support to `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`

---

## Phase 3: User Story 1 - Log a Set With One Tap (Priority: P1)

**Independent Test**: Start an active workout with an exercise and draft row,
tap Log, and confirm the row appears logged only after persistence succeeds.

- [X] T006 [P] [US1] Add draft default and one-tap success tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolderTest.kt`
- [X] T007 [P] [US1] Add duplicate pending confirm suppression test in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPendingTest.kt`
- [X] T008 [US1] Implement `ExerciseBlock` composition in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ExerciseBlock.kt`
- [X] T009 [US1] Implement `SetRow` composition in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/SetRow.kt`
- [X] T010 [US1] Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt` to render exercise blocks and one-tap log actions

---

## Phase 4: User Story 2 - Edit Reps and Weight Inline (Priority: P1)

**Independent Test**: Tap/step and drag/roll reps or weight, then log and
confirm the persisted tuple uses the edited values.

- [X] T011 [P] [US2] Add roller clamp/step/direct-entry tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerFieldModelTest.kt`
- [X] T012 [US2] Implement `RollerField` composition in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerField.kt`
- [X] T013 [US2] Wire reps and weight draft updates through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/SetRow.kt`

---

## Phase 5: User Story 3 - Add Exercises While Working Out (Priority: P1)

**Independent Test**: Add a weighted exercise and a bodyweight exercise during
an active workout and log both, including bodyweight reps-only.

- [X] T014 [P] [US3] Add add-exercise focus and bodyweight reps-only tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutAddExerciseTest.kt`
- [X] T015 [US3] Add catalog-backed exercise add helpers to `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T016 [US3] Wire add-exercise placeholder actions from `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`

---

## Phase 6: User Story 4 - Recover the Active Logging Position (Priority: P2)

**Independent Test**: Rehydrate state from an active workout and saved focus
snapshot and confirm the same exercise/draft is focused.

- [X] T017 [P] [US4] Add focus recovery tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutRecoveryTest.kt`
- [X] T018 [US4] Add focus snapshot/restore support to `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`

---

## Phase 7: User Story 5 - Keep Errors Non-Destructive (Priority: P2)

**Independent Test**: Invalid input and persistence failures keep the draft
editable and unlogged with an inline error.

- [X] T019 [P] [US5] Add invalid weighted/bodyweight draft tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutErrorTest.kt`
- [X] T020 [US5] Ensure draft validation and failure handling preserve values in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`

---

## Final Phase: Polish & Validation

- [X] T021 Run `./gradlew :shared:testDebugUnitTest` and record results in `specs/004-active-workout-loop/validation/active-workout-results.md`
- [X] T022 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/004-active-workout-loop/validation/active-workout-results.md`
- [X] T023 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/004-active-workout-loop/validation/active-workout-results.md`
- [X] T024 Run the shared Material scan from `specs/004-active-workout-loop/quickstart.md` and record results in `specs/004-active-workout-loop/validation/active-workout-results.md`
- [X] T025 Update `specs/004-active-workout-loop/tasks.md` so completed tasks are checked and any manual device gate deferral is documented

## Dependencies & Execution Order

- Phase 1 before all other work.
- Phase 2 before story work.
- US1 and US2 are both P1; implement US1 first because US2 edits its set row.
- US3 depends on foundational state and can run after US1.
- US4 and US5 depend on the state holder behavior from US1-US3.
- Final validation depends on all selected story phases.

## Parallel Opportunities

- T002 and T003 can run in parallel.
- T006 and T007 can run in parallel after T005.
- T011, T014, T017, and T019 touch separate test files and can run in parallel
  after their target APIs exist.

## MVP Strategy

1. Complete setup/foundation.
2. Deliver US1 one-tap logging.
3. Add US2 inline edits.
4. Add US3 add-exercise/bodyweight support.
5. Add recovery and error hardening.
6. Run platform validation.
