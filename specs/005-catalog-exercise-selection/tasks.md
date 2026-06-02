---
description: "Task list for Catalog Exercise Selection"
---

# Tasks: Catalog Exercise Selection

**Input**: Design documents from `/specs/005-catalog-exercise-selection/`

**Scope**: Replace active workout Quick Add with shared catalog-backed exercise
selection, seeded local search, bodyweight metadata preservation, simple
custom exercise creation, non-destructive cancel/failure behavior, and
validation across Android and iOS shared compile.

## Phase 1: Setup

- [X] T001 Create `specs/005-catalog-exercise-selection/validation/exercise-picker-results.md` with sections for unit tests, Android build, iOS compile, Material scan, and manual validation

---

## Phase 2: Foundational

- [X] T002 [P] Add exercise catalog use cases in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCases.kt`
- [X] T003 [P] Add picker result and custom draft models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerModels.kt`
- [X] T004 Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt` for open/search/select/create/cancel state
- [X] T005 Wire `ExerciseCatalogUseCases` and `ExercisePickerStateHolder` into `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`

---

## Phase 3: User Story 1 - Select a Seeded Exercise During a Workout (Priority: P1)

**Independent Test**: Start an active workout, open Add Exercise, search a
seeded exercise, select it, and confirm the focused exercise block appears
without mutating existing workout drafts or logged rows.

- [X] T006 [P] [US1] Add catalog search/default-results use case tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCasesTest.kt`
- [X] T007 [P] [US1] Add seeded selection and focus handoff tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolderTest.kt`
- [X] T008 [US1] Implement search result rendering and selection callbacks in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`
- [X] T009 [US1] Replace active workout Quick Add placeholder with picker open/select wiring in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

---

## Phase 4: User Story 2 - Preserve Bodyweight Logging From Catalog Metadata (Priority: P1)

**Independent Test**: Select a bodyweight catalog exercise during an active
workout and confirm the resulting draft logs reps-only without requiring
weight.

- [X] T010 [P] [US2] Add bodyweight picker selection tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerBodyweightTest.kt`
- [X] T011 [US2] Ensure catalog rows map bodyweight classification into `ExerciseReference` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerModels.kt`
- [X] T012 [US2] Ensure selected bodyweight exercises create reps-only active workout drafts through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`

---

## Phase 5: User Story 3 - Create a Missing Exercise Locally (Priority: P2)

**Independent Test**: Search for a missing exercise, create it locally with a
name and classification, append it to the active workout, and confirm it
appears in later searches.

- [X] T013 [P] [US3] Add custom exercise creation use case tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogCreationTest.kt`
- [X] T014 [P] [US3] Add picker custom creation flow tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerCustomExerciseTest.kt`
- [X] T015 [US3] Implement custom exercise validation and persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCases.kt`
- [X] T016 [US3] Implement custom exercise form state and create action in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`
- [X] T017 [US3] Render no-results and custom creation UI in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`

---

## Phase 6: User Story 4 - Keep Picker State Recoverable and Non-Destructive (Priority: P2)

**Independent Test**: Open picker, search/cancel or force an add failure, and
confirm active workout exercise blocks, logged rows, and drafts remain
unchanged.

- [X] T018 [P] [US4] Add cancel/no-op behavior tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerCancelTest.kt`
- [X] T019 [P] [US4] Add add failure preservation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFailureTest.kt`
- [X] T020 [US4] Preserve picker state and active workout state on cancel/failure in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`
- [X] T021 [US4] Add dismiss/error rendering paths in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`

---

## Final Phase: Polish & Validation

- [X] T022 Run `./gradlew :shared:testDebugUnitTest` and record results in `specs/005-catalog-exercise-selection/validation/exercise-picker-results.md`
- [X] T023 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/005-catalog-exercise-selection/validation/exercise-picker-results.md`
- [X] T024 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/005-catalog-exercise-selection/validation/exercise-picker-results.md`
- [X] T025 Run the shared Material scan from `specs/005-catalog-exercise-selection/quickstart.md` and record results in `specs/005-catalog-exercise-selection/validation/exercise-picker-results.md`
- [X] T026 Update `specs/005-catalog-exercise-selection/tasks.md` so completed tasks are checked and any manual device gate deferral is documented

## Dependencies & Execution Order

- Phase 1 before all other work.
- Phase 2 before user story work.
- US1 and US2 are both P1 and can be validated independently after Phase 2.
- US3 depends on the foundational use case and picker state.
- US4 depends on picker open/search/select behavior.
- Final validation depends on selected story phases.

## Parallel Opportunities

- T002 and T003 can run in parallel.
- T006, T007, T010, T013, T014, T018, and T019 touch separate test files and
  can run in parallel once foundational APIs exist.

## MVP Strategy

1. Complete setup/foundation.
2. Deliver US1 seeded search and select.
3. Deliver US2 bodyweight classification preservation.
4. Add US3 custom local exercise creation.
5. Add US4 cancel/failure hardening.
6. Run platform validation.
