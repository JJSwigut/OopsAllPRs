---
description: "Task list for Workout Logging UX V1"
---

# Tasks: Workout Logging UX V1

**Input**: Design documents from `/specs/006-workout-logging-ux/`

**Scope**: Reset the active workout logging loop around a focused full-screen
mode, compact exercise blocks, dense Add Exercise search, bodyweight reps-only
logging, recoverable focus/drafts, inline persistence failures, and inline PR
feedback.

**Tests**: Required for this feature. Active workout, ledger, persistence,
recovery, bodyweight, PR/progression, accessibility, Android-first validation,
and iOS shared compile all require explicit validation evidence.

## Phase 1: Setup

**Purpose**: Create validation tracking before implementation starts.

- [X] T001 Create `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md` with sections for common tests, Android build, iOS compile, Material scan, manual Pixel review, screenshots, and deferred gates

---

## Phase 2: Foundational

**Purpose**: Blocking shared persistence, model, and use-case infrastructure for all user stories.

**Checkpoint**: No user story work should begin until this phase is complete.

### Tests for Foundational Work

- [X] T002 [P] Add active workout UX persistence tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/session/ActiveWorkoutUxPersistenceTest.kt`
- [X] T003 [P] Add compact set input value/formatting tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/CompactSetInputModelTest.kt`
- [X] T004 [P] Add active PR feedback derivation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActivePrFeedbackUseCaseTest.kt`

### Implementation for Foundational Work

- [X] T005 Add typed active workout UX session and set draft tables/queries in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq` and `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/WorkoutQueries.sq`
- [X] T006 Add SQLDelight migration for active workout UX session and set draft tables in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/migrations/2.sqm`
- [X] T007 [P] Add active workout UX domain models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ActiveWorkoutUxState.kt`
- [X] T008 Define active workout UX repository methods in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt`
- [X] T009 Implement active workout UX persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/InMemoryFoundationStore.kt`
- [X] T010 Expose active workout UX persistence through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlWorkoutRepository.kt`
- [X] T011 [P] Add active PR feedback derivation use case in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActivePrFeedbackUseCase.kt`
- [X] T012 Wire active workout UX repository and PR feedback use case into `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`

---

## Phase 3: User Story 1 - Log a Workout Without Thinking (Priority: P1) MVP

**Goal**: Start an empty workout, reach active logging quickly, add an exercise, and log repeated weighted sets with one obvious next action and no decorative UI competing with logging.

**Independent Test**: Start from a clean app state, start a workout, add one weighted exercise, log three weighted sets, and confirm each next action remains visible without instructions.

### Tests for User Story 1

- [X] T013 [P] [US1] Add Train primary action and active-session resume tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolderTest.kt`
- [X] T014 [P] [US1] Add focused active mode and next-action state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellActiveModeTest.kt`
- [X] T015 [P] [US1] Add weighted draft default, pending, success, and retry tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolderTest.kt`

### Implementation for User Story 1

- [X] T016 [US1] Update Train content to prioritize Start workout and remove title/marketing copy in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [X] T017 [US1] Update active workout presentation to focused full-screen mode with hidden top-level navigation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T018 [US1] Extend active workout display models for elapsed context, primary action, compact rows, and PR feedback placeholders in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T019 [US1] Update active workout state holder for persisted draft defaults, focused block state, pending suppression, success refresh, and retry state in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T020 [US1] Implement compact stepper-first set input in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/CompactSetInput.kt`
- [X] T021 [US1] Replace nested set cards with compact logged rows and next-set input in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/SetRow.kt`
- [X] T022 [US1] Update exercise blocks to use dense grouping, focused emphasis, long-name wrapping, and no nested card stacks in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ExerciseBlock.kt`
- [X] T023 [US1] Update active workout screen header, empty state, Add Exercise action, scrolling, and no-title layout in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`
- [X] T024 [US1] Record US1 test results and manual notes in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`

**Checkpoint**: User Story 1 is the MVP. The app can start a workout and log repeated weighted sets in the focused active mode.

---

## Phase 4: User Story 2 - Add Exercises in the Logging Flow (Priority: P2)

**Goal**: Add exercises during active logging through a dense search-first picker that returns directly to the active workout with focus on the new exercise.

**Independent Test**: Start an active workout, open Add Exercise, select a seeded exercise, and confirm the picker closes with the new exercise ready to log while existing drafts/logged rows are unchanged.

### Tests for User Story 2

- [X] T025 [P] [US2] Add dense picker selection and focus handoff tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxStateTest.kt`
- [X] T026 [P] [US2] Add picker cancel/failure draft preservation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxRecoveryTest.kt`

### Implementation for User Story 2

- [X] T027 [US2] Update picker row state for dense inline Add actions and saving/error display in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerModels.kt`
- [X] T028 [US2] Update picker state holder for active-workout-scoped search, selection, custom creation, cancel, and failure preservation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`
- [X] T029 [US2] Update picker UI to search-first dense rows with inline Add actions and keyboard-safe scrolling in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`
- [X] T030 [US2] Ensure active workout add-exercise creates and persists focus/draft for the new block in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T031 [US2] Update active-mode picker presentation and dismissal wiring in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T032 [US2] Record US2 test results and manual picker notes in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`

**Checkpoint**: User Story 2 works independently after foundation: exercise search/add is fast, dense, and non-destructive.

---

## Phase 5: User Story 3 - Bodyweight Logging Stays Reps-First (Priority: P3)

**Goal**: Bodyweight movements log reps only by default and never require weight in the normal path.

**Independent Test**: Add a bodyweight exercise, log three reps-only sets, and confirm no required weight field appears or blocks confirmation.

### Tests for User Story 3

- [X] T033 [P] [US3] Add bodyweight reps-only draft default and confirmation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutBodyweightUxTest.kt`
- [X] T034 [P] [US3] Add bodyweight picker-to-logging flow tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerBodyweightUxTest.kt`

### Implementation for User Story 3

- [X] T035 [US3] Update active workout draft defaulting for bodyweight null-weight reps-only behavior in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T036 [US3] Update compact set input to hide weight controls for default bodyweight flow in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/CompactSetInput.kt`
- [X] T037 [US3] Update set validation to accept bodyweight reps-only and reject only invalid added load when present in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T038 [US3] Update exercise picker custom classification controls to preserve weighted/bodyweight selection clearly in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`
- [X] T039 [US3] Record US3 test results and bodyweight manual notes in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`

**Checkpoint**: Bodyweight reps-only logging is independently testable and does not depend on PR feedback work.

---

## Phase 6: User Story 4 - Recover and Celebrate Progress (Priority: P4)

**Goal**: Recover active logging state after interruption and show inline PR feedback when a logged set improves a record.

**Independent Test**: Start a workout, log a set, edit a draft, restart the app, resume the workout, log a record-setting set, and confirm focus/draft recovery plus inline PR feedback.

### Tests for User Story 4

- [X] T040 [P] [US4] Add persisted focus/draft recovery tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutUxRecoveryTest.kt`
- [X] T041 [P] [US4] Add inline weighted PR feedback tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPrFeedbackTest.kt`
- [X] T042 [P] [US4] Add bodyweight PR feedback and non-PR no-decoration tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutBodyweightPrTest.kt`

### Implementation for User Story 4

- [X] T043 [US4] Hydrate active workout focus and set drafts from typed UX persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T044 [US4] Persist draft edits and focus changes as they occur in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T045 [US4] Derive inline PR feedback after successful confirmation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T046 [US4] Render inline PR markers on logged rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ExerciseBlock.kt`
- [X] T047 [US4] Clear active workout UX persistence on finish or discard in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T048 [US4] Refresh resume state and recovered active mode after app hydration in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`
- [X] T049 [US4] Record US4 test results and recovery/PR manual notes in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`

**Checkpoint**: Recovery and PR feedback are functional without blocking the fast logging loop.

---

## Final Phase: Polish & Validation

**Purpose**: Cross-story quality gates and release evidence.

- [X] T050 Run `./gradlew :shared:testDebugUnitTest` and record results in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T051 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T052 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T053 Run the Material scan from `specs/006-workout-logging-ux/quickstart.md` and record results in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T054 Install the Android debug app on a Pixel-class device or emulator, complete the manual milestone from `specs/006-workout-logging-ux/quickstart.md`, and record screenshots/notes in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T055 Verify no active logging UI uses hardcoded styling outside FitTheme/design-system boundaries in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`
- [X] T056 Verify keyboard/status/navigation-bar overlap scenarios from the wireframes and record findings in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [ ] T057 Verify active logging touch targets, TalkBack labels/state descriptions, reduced-motion behavior, and non-haptic alternatives against the constitution, then record findings in `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- [X] T058 Update `specs/006-workout-logging-ux/tasks.md` with completed checkboxes and any deferred manual gates before commit

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup and blocks all user stories.
- **US1 MVP**: Depends on foundational active UX models, persistence contracts, and PR feedback placeholder models.
- **US2**: Depends on foundational persistence and can start after US1 active mode entry points exist.
- **US3**: Depends on foundational set draft modeling and can run after US1 compact set input exists.
- **US4**: Depends on foundational persistence and the set logging behavior from US1/US3.
- **Final Validation**: Depends on all selected story phases.

### User Story Dependencies

- **US1 (P1)**: MVP and first implementation target.
- **US2 (P2)**: Integrates with active mode and add-exercise state; independently testable with seeded selection.
- **US3 (P3)**: Builds on set input and exercise classification; independently testable with bodyweight exercises.
- **US4 (P4)**: Builds on persisted drafts/focus and confirmed set rows; independently testable with restart and PR scenarios.

### Within Each User Story

- Tests are written first and must fail before implementation.
- Model/state changes precede UI composition changes.
- Persistence/repository work precedes recovery state holder changes.
- State holder behavior precedes Compose rendering.
- Story validation notes are recorded before moving to the next phase checkpoint.

## Parallel Opportunities

- T002, T003, and T004 can run in parallel.
- T007 and T011 can run in parallel after T002-T004 are understood.
- T013, T014, and T015 can run in parallel after foundational APIs exist.
- T025 and T026 can run in parallel after US1 active mode exists.
- T033 and T034 can run in parallel after compact set input APIs exist.
- T040, T041, and T042 can run in parallel after PR feedback models exist.
- Final validation commands T050-T053 can run independently once implementation is complete, but record results in order.

## Parallel Example: User Story 1

```text
Task: "T013 Add Train primary action and active-session resume tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolderTest.kt"
Task: "T014 Add focused active mode and next-action state tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellActiveModeTest.kt"
Task: "T015 Add weighted draft default, pending, success, and retry tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolderTest.kt"
```

## Parallel Example: User Story 2

```text
Task: "T025 Add dense picker selection and focus handoff tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxStateTest.kt"
Task: "T026 Add picker cancel/failure draft preservation tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxRecoveryTest.kt"
```

## Parallel Example: User Story 4

```text
Task: "T040 Add persisted focus/draft recovery tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutUxRecoveryTest.kt"
Task: "T041 Add inline weighted PR feedback tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutPrFeedbackTest.kt"
Task: "T042 Add bodyweight PR feedback and non-PR no-decoration tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutBodyweightPrTest.kt"
```

## MVP Strategy

1. Complete Phase 1 setup and Phase 2 foundational persistence/models.
2. Deliver US1 only: Train primary action, focused active mode, compact weighted set logging.
3. Stop and validate US1 independently before adding picker/bodyweight/recovery scope.
4. Add US2 dense exercise selection.
5. Add US3 reps-only bodyweight logging.
6. Add US4 recovery and inline PR feedback.
7. Run final cross-platform and manual Pixel validation.

## Notes

- [P] tasks touch different files or can be completed without depending on another incomplete task.
- Every user story task includes a [US#] label for traceability.
- Keep confirmed set rows separate from persisted drafts throughout implementation.
- Do not add a second styling system; active logging UI must use FitTheme/design-system primitives.
- Existing uncommitted layout-pass files should be reconciled against this task list rather than blindly preserved.
- Feature 011 completed T054 and T056 on Pixel-class AVD
  `OopsAllPRs_Pixel_9_Pro`; T057 remains deferred for a dedicated
  TalkBack/reduced-motion/haptic-disabled accessibility pass.
