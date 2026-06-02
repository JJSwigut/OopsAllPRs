---
description: "Task list for Completed Workout History and Templates"
---

# Tasks: Completed Workout History and Templates

**Input**: Design documents from `/specs/007-history-templates/`

**Scope**: Add completed workout summary/detail, useful History list/detail,
save-completed-as-template, and Train template launch while keeping completed
history, reusable templates, and active workouts separate.

**Tests**: Required for this feature. History summaries, bodyweight completed
rows, PR markers, template save validation, template launch separation, active
session conflicts, Android-first validation, and iOS shared compile all require
explicit evidence.

## Phase 1: Setup

**Purpose**: Create validation tracking before implementation starts.

- [X] T001 Create `specs/007-history-templates/validation/history-templates-results.md` with sections for common tests, Android build, iOS compile, Material scan, manual Pixel review, screenshots, and deferred gates

---

## Phase 2: Foundational

**Purpose**: Shared projection and lifecycle foundations used by every story.

**Checkpoint**: No user story work should begin until this phase is complete.

### Tests for Foundational Work

- [X] T002 [P] Add completed workout summary projection tests for duration, mixed weighted/bodyweight rows, and set counts in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/CompletedWorkoutSummaryModelTest.kt`
- [X] T003 [P] Add completed workout PR marker projection tests from personal records in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/CompletedWorkoutPrMarkerTest.kt`
- [X] T004 [P] Add template launch ledger separation tests proving planned sets launch without copied `loggedAt` values in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TemplateLaunchSeparationTest.kt`
- [X] T005 [P] Add active-workout conflict tests for empty start and template launch in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActiveWorkoutConflictTest.kt`

### Implementation for Foundational Work

- [X] T006 Add completed workout summary, history row, template row, and PR marker display models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryModels.kt`
- [X] T007 Add mapping helpers from completed workouts and personal records to summary/detail rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryModels.kt`
- [X] T008 Update workout lifecycle start paths to reject silent second active workouts in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T009 Ensure finish-workout refreshes derived PR evidence for completed history summaries in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineUseCases.kt`
- [X] T010 Wire any new routine/progress dependencies into `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/di/AppModule.kt`

---

## Phase 3: User Story 1 - Review a Finished Workout (Priority: P1) MVP

**Goal**: After finishing an active workout, show a completed workout summary with duration, exercises, logged sets, bodyweight rows, and PR markers.

**Independent Test**: Finish a mixed weighted/bodyweight workout and verify the summary matches the logged ledger data.

### Tests for User Story 1

- [X] T011 [P] [US1] Add finish-to-summary state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolderSummaryTest.kt`
- [X] T012 [P] [US1] Add app-shell finish routing tests for presenting the completed summary in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellFinishSummaryTest.kt`

### Implementation for User Story 1

- [X] T013 [US1] Extend `HistoryStateHolder` with selected completed workout summary state in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolder.kt`
- [X] T014 [US1] Update active workout finish wiring to present the completed summary after successful finish in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T015 [US1] Render completed workout summary/detail content in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`
- [X] T016 [US1] Record US1 validation evidence in `specs/007-history-templates/validation/history-templates-results.md`

**Checkpoint**: User Story 1 is the MVP. The user can finish a workout and inspect what was saved.

---

## Phase 4: User Story 2 - Browse Workout History (Priority: P2)

**Goal**: History shows completed workouts in a useful list and opens read-only details for each workout.

**Independent Test**: Complete two workouts, open History, verify reverse chronological rows, and open both details.

### Tests for User Story 2

- [X] T017 [P] [US2] Add History ordering, empty-state, and detail selection tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolderListTest.kt`

### Implementation for User Story 2

- [X] T018 [US2] Extend `HistoryStateHolder` refresh/select/back behavior for list and detail in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolder.kt`
- [X] T019 [US2] Update History list, empty state, detail navigation, and long-content scrolling in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`
- [X] T020 [US2] Wire History row selection callbacks through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T021 [US2] Record US2 validation evidence in `specs/007-history-templates/validation/history-templates-results.md`

**Checkpoint**: History is independently useful for completed workout review.

---

## Phase 5: User Story 3 - Save a Workout as a Template (Priority: P3)

**Goal**: Save a completed workout as a named reusable template without mutating completed history.

**Independent Test**: Open a completed workout detail, save it as a named template, and verify the completed workout remains unchanged.

### Tests for User Story 3

- [X] T022 [P] [US3] Add template name validation and save-from-completed tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolderTemplateSaveTest.kt`
- [X] T023 [P] [US3] Add completed-history immutability tests after template save in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TemplateSaveSeparationTest.kt`

### Implementation for User Story 3

- [X] T024 [US3] Extend `RoutineStateHolder` with template list state, save draft state, validation, and save-from-completed action in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T025 [US3] Add save-template controls and validation feedback to completed workout detail in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`
- [X] T026 [US3] Wire save-template callbacks and template refresh through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T027 [US3] Record US3 validation evidence in `specs/007-history-templates/validation/history-templates-results.md`

**Checkpoint**: Completed workouts can become reusable local templates.

---

## Phase 6: User Story 4 - Launch a Template From Train (Priority: P4)

**Goal**: Train shows saved templates and launches one into active logging with planned sets but no copied logged rows.

**Independent Test**: Save a template, launch it from Train, and verify the active workout has planned set defaults with null `loggedAt`.

### Tests for User Story 4

- [X] T028 [P] [US4] Add Train template list and launch state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeTemplateLaunchTest.kt`
- [X] T029 [P] [US4] Add launched-template bodyweight planned-set tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeTemplateBodyweightTest.kt`

### Implementation for User Story 4

- [X] T030 [US4] Extend `WorkoutHomeStateHolder` with template rows and launch-template action in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolder.kt`
- [X] T031 [US4] Render compact template launch rows on Train without hiding Start workout in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [X] T032 [US4] Wire template launch through hydration and active workout presentation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T033 [US4] Record US4 validation evidence in `specs/007-history-templates/validation/history-templates-results.md`

**Checkpoint**: The finish-save-launch loop is complete.

---

## Final Phase: Polish & Validation

**Purpose**: Cross-story quality gates and release evidence.

- [X] T034 Run `./gradlew :shared:testDebugUnitTest` and record results in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T035 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T036 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T037 Run the Material scan from `specs/007-history-templates/quickstart.md` and record results in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T038 Install the Android debug app on a Pixel-class device or emulator, complete the manual milestone from `specs/007-history-templates/quickstart.md`, and record screenshots/notes in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T039 Verify History, summary, template save, and Train template UI uses FitTheme/design-system boundaries in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [ ] T040 Verify touch targets, TalkBack labels/state descriptions, reduced-motion behavior, and non-haptic alternatives for the new review/template surfaces, then record findings in `specs/007-history-templates/validation/history-templates-results.md`
- [X] T041 Verify export-ready completed workout and template data still appears in existing export coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/export/ExportSnapshotTest.kt`
- [X] T042 Update `specs/007-history-templates/tasks.md` with completed checkboxes and any deferred manual gates before commit

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 Setup**: No dependencies.
- **Phase 2 Foundational**: Depends on setup and blocks all user stories.
- **US1 MVP**: Depends on completed summary projections and finish PR evidence.
- **US2**: Depends on summary projections and can start after US1 summary state exists.
- **US3**: Depends on completed detail and existing routine save use case.
- **US4**: Depends on template list state and existing routine launch use case.
- **Final Validation**: Depends on all selected story phases.

### User Story Dependencies

- **US1 (P1)**: MVP and first implementation target.
- **US2 (P2)**: Builds on the same summary projection and makes History useful independently.
- **US3 (P3)**: Builds on completed detail and routine save behavior.
- **US4 (P4)**: Builds on saved templates and active workout lifecycle launch behavior.

### Within Each User Story

- Tests are written first and must fail before implementation.
- Display models precede state holder changes.
- State holder behavior precedes Compose rendering.
- AppShell wiring happens after state holder APIs exist.
- Story validation notes are recorded before moving to the next phase checkpoint.

## Parallel Opportunities

- T002, T003, T004, and T005 can run in parallel.
- T011 and T012 can run in parallel after foundational models exist.
- T022 and T023 can run in parallel after completed detail state exists.
- T028 and T029 can run in parallel after template rows exist.
- Final validation commands T034-T037 can run independently once implementation is complete, but record results in order.

## Parallel Example: User Story 1

```text
Task: "T011 Add finish-to-summary state tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolderSummaryTest.kt"
Task: "T012 Add app-shell finish routing tests for presenting the completed summary in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellFinishSummaryTest.kt"
```

## Parallel Example: User Story 3

```text
Task: "T022 Add template name validation and save-from-completed tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolderTemplateSaveTest.kt"
Task: "T023 Add completed-history immutability tests after template save in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/TemplateSaveSeparationTest.kt"
```

## MVP Strategy

Deliver US1 first: finishing a workout produces a trustworthy completed summary
with weighted/bodyweight logged rows and PR markers. This is independently
valuable and provides the projection needed by later History and template
stories.

## Incremental Delivery

1. Complete Foundational + US1 to close finish-to-summary.
2. Add US2 to make History list/detail useful.
3. Add US3 to save completed workouts as templates.
4. Add US4 to launch templates from Train into active logging.
5. Run validation gates and record manual deferrals if no device is available.

## Notes

- Feature 011 completed T038 on Pixel-class AVD `OopsAllPRs_Pixel_9_Pro`.
  T040 remains deferred for a dedicated TalkBack/reduced-motion/haptic-disabled
  accessibility pass.
