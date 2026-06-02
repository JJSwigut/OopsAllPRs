# Tasks: Milestone Validation and UX Hardening

**Input**: Design documents from `/specs/011-milestone-ux-hardening/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md), [research.md](./research.md), [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md)

**Tests**: This is a validation-driven hardening slice. Add or update focused common tests when behavior changes, and record emulator/device evidence for every manual gate.

**Organization**: Tasks are grouped by user story so the core logging loop can be validated first, followed by cross-destination continuity and accessibility/interaction polish.

## Phase 1: Setup

**Purpose**: Prepare validation evidence files and confirm the current branch context.

- [X] T001 Create `specs/011-milestone-ux-hardening/validation/milestone-results.md` with sections for target, environment notes, automated gates, scenario results, UX findings, fixes, screenshots, and deferred-gate updates
- [X] T002 Create `specs/011-milestone-ux-hardening/validation/screenshots/.gitkeep` so manual evidence has a stable repository location
- [X] T003 [P] Record the existing deferred manual gates from `specs/002-neo-glass-design-system/tasks.md`, `specs/006-workout-logging-ux/tasks.md`, and `specs/007-history-templates/tasks.md` in `specs/011-milestone-ux-hardening/validation/milestone-results.md`

---

## Phase 2: Foundational Validation Baseline

**Purpose**: Establish the runnable Android baseline and identify app defects separately from emulator/device defects.

- [X] T004 Build the Android debug APK with `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record the result in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T005 Install and launch `androidApp/build/outputs/apk/debug/androidApp-debug.apk` on a Pixel-class emulator/device and record target details in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T006 Capture the initial Train or current app state screenshot to `specs/011-milestone-ux-hardening/validation/screenshots/baseline-01-launch.png` and reference it from `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T007 [P] Review baseline active logging state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolderTest.kt` and add coverage only if a manual finding exposes an untested state transition
- [X] T008 [P] Review baseline picker state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxStateTest.kt` and add coverage only if a manual finding exposes missing browse/search/recent behavior
- [X] T009 [P] Review baseline finish/history routing tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellFinishSummaryTest.kt` and add coverage only if a manual finding exposes a routing defect

**Checkpoint**: Android runtime target is available, baseline evidence is recorded, and app/environment blockers are separated.

---

## Phase 3: User Story 1 - Validate the Core Logging Loop (Priority: P1)

**Goal**: Validate and harden Train, active workout logging, exercise picker, numeric keyboard entry, bodyweight reps-only logging, and finish workflow.

**Independent Test**: Complete the core logging checklist from `specs/011-milestone-ux-hardening/quickstart.md` on a Pixel-class target and record screenshots/notes.

### Tests for User Story 1

- [X] T010 [P] [US1] Add or update numeric replacement behavior tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerFieldModelTest.kt` if direct entry appends instead of replacing during manual validation
- [X] T011 [P] [US1] Add or update picker browse/recent behavior tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerUxStateTest.kt` if the seeded catalog cannot be browsed beyond the first visible section
- [X] T012 [P] [US1] Add or update finish discoverability/routing tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellFinishSummaryTest.kt` if the active workout finish path is hidden or traps the user

### Implementation for User Story 1

- [X] T013 [US1] Run the core logging manual flow and capture screenshots under `specs/011-milestone-ux-hardening/validation/screenshots/core-*.png`
- [X] T014 [US1] Record weighted logging, direct numeric entry, bodyweight reps-only logging, keyboard overlap, and finish-flow findings in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T015 [US1] Fix active logging layout, bottom action reach, keyboard avoidance, or finish visibility defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`
- [X] T016 [US1] Fix set input direct-entry or compact logging defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/CompactSetInput.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerField.kt`, or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/RollerFieldModels.kt`
- [X] T017 [US1] Fix exercise picker browse/search/recent/selection defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt` or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerStateHolder.kt`
- [X] T018 [US1] Rebuild, reinstall, rerun the affected core flow, and record fixed evidence in `specs/011-milestone-ux-hardening/validation/milestone-results.md`

**Checkpoint**: Core logging flow is complete or each remaining issue has an explicit blocker and owner.

---

## Phase 4: User Story 2 - Validate Cross-Destination Continuity (Priority: P2)

**Goal**: Validate and harden History, templates, Progress, Profile/export, and app restart continuity after a completed workout.

**Independent Test**: Use the completed workout from US1 or local seed/test data to navigate History, template save/launch, Progress, and Profile/export without dead ends.

### Tests for User Story 2

- [X] T019 [P] [US2] Add or update History/template continuity tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolderSummaryTest.kt` or `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeTemplateLaunchTest.kt` if manual review finds a continuity defect
- [X] T020 [P] [US2] Add or update Progress evidence tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressEvidenceTest.kt` if manual review finds missing or misleading PR evidence
- [X] T021 [P] [US2] Add or update Profile export state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolderTest.kt` if manual review finds export or unit state defects

### Implementation for User Story 2

- [X] T022 [US2] Run the History/template/Progress/Profile manual flow and capture screenshots under `specs/011-milestone-ux-hardening/validation/screenshots/continuity-*.png`
- [X] T023 [US2] Record cross-destination navigation, template, PR evidence, Profile/export, and restart findings in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T024 [US2] Fix History or template continuity defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolder.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`, or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt`
- [X] T025 [US2] Fix Progress evidence or layout defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt` or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T026 [US2] Fix Profile/export continuity defects found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt` or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T027 [US2] Rebuild, reinstall, rerun affected continuity checks, and record fixed evidence in `specs/011-milestone-ux-hardening/validation/milestone-results.md`

**Checkpoint**: Post-workout continuity is validated or any remaining limitation is explicitly documented.

---

## Phase 5: User Story 3 - Validate Accessibility and Interaction Polish (Priority: P3)

**Goal**: Validate and harden touch targets, labels/state descriptions, reduced motion, haptic alternatives, shape-consistent feedback, and glow behavior.

**Independent Test**: Review the major surfaces and design-system controls on the Pixel-class target and record accessibility/interaction findings.

### Tests for User Story 3

- [X] T028 [P] [US3] Add or update design-system component math tests in `design-system/src/commonTest/kotlin/com/jjswigut/oopsallprs/ds/component/ComponentMathTest.kt` if a shape, size, or touch target token defect is found
- [X] T029 [P] [US3] Add or update navigation accessibility state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppDestinationTest.kt` or `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppNavigationStateHolderTest.kt` if labels/states are defective

### Implementation for User Story 3

- [X] T030 [US3] Run the accessibility and interaction polish checklist and capture screenshots under `specs/011-milestone-ux-hardening/validation/screenshots/polish-*.png`
- [X] T031 [US3] Record touch target, TalkBack/state description, reduced-motion, haptic-disabled, shape feedback, and glow findings in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T032 [US3] Fix reusable shape, press feedback, glow, or touch target defects found in `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/FitButton.kt`, `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/FitSegmentedControl.kt`, `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/FitTabBar.kt`, `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/FitTextField.kt`, or `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/foundation/Pressable.kt`
- [X] T033 [US3] Fix flow-specific accessibility labels/state descriptions found in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/accessibility/FoundationAccessibility.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt`, or `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T034 [US3] Rebuild, reinstall, rerun affected polish checks, and record fixed evidence in `specs/011-milestone-ux-hardening/validation/milestone-results.md`

**Checkpoint**: Accessibility and interaction polish issues are fixed or explicitly deferred with rationale.

**Accessibility note**: The 018 follow-up completed the Pixel-class emulator
hierarchy check for labels, selected state, switch state, touch reach, and
shape/glow polish. A live TalkBack audio pass plus OS-level reduced-motion and
haptic-disabled verification remains a pre-release manual gate because
UIAutomator hierarchy evidence cannot prove spoken output or device setting
behavior.

---

## Phase 6: Polish and Cross-Cutting Validation

**Purpose**: Close validation gates, update deferred tasks, and verify the branch.

- [X] T035 [P] Update `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md` with a reference to the 011 milestone evidence and remaining status
- [X] T036 [P] Update `specs/007-history-templates/validation/history-templates-results.md` with a reference to the 011 milestone evidence and remaining status
- [X] T037 [P] Update `specs/002-neo-glass-design-system/validation/build-and-test-results.md` with a reference to the 011 on-device visual tuning evidence and remaining status
- [X] T038 Update `specs/006-workout-logging-ux/tasks.md`, `specs/007-history-templates/tasks.md`, and `specs/002-neo-glass-design-system/tasks.md` checkboxes for gates completed by this feature
- [X] T039 Run `./gradlew :shared:testDebugUnitTest` and record results in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T040 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T041 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record results in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T042 Run the Material/design-system scan from `specs/011-milestone-ux-hardening/quickstart.md` and record results in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T043 Run `git diff --check -- .` and record results in `specs/011-milestone-ux-hardening/validation/milestone-results.md`
- [X] T044 Update this task list in `specs/011-milestone-ux-hardening/tasks.md` so completed tasks are checked and remaining manual limitations are documented before commit
- [X] T045 Stage and commit feature 011 changes with message `Harden milestone UX validation`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational Validation Baseline (Phase 2)**: Depends on setup and blocks user-story validation.
- **US1 Core Logging (Phase 3)**: Depends on baseline and is the MVP.
- **US2 Cross-Destination Continuity (Phase 4)**: Depends on baseline and should use data created by US1 when possible.
- **US3 Accessibility and Interaction Polish (Phase 5)**: Depends on baseline and can run after or alongside US2.
- **Polish (Phase 6)**: Depends on all attempted user-story phases.

### User Story Dependencies

- **US1**: First priority because it validates the active workout loop.
- **US2**: Can start after baseline, but most useful after US1 creates a completed workout.
- **US3**: Can start after baseline and should be rechecked after any US1/US2 UI fixes.

### Parallel Opportunities

- T003 can run after T001.
- T007-T009 can be reviewed in parallel.
- T010-T012 can be prepared in parallel if manual findings require tests.
- T019-T021 can be prepared in parallel if manual findings require tests.
- T028-T029 can be prepared in parallel if manual findings require tests.
- T035-T037 can be updated in parallel once milestone evidence exists.

## Implementation Strategy

### MVP First

1. Complete setup and baseline launch evidence.
2. Complete US1 core logging validation and fix only concrete defects.
3. Rebuild and rerun the affected core flow.

### Incremental Delivery

1. Add US2 cross-destination continuity using the completed workout from US1.
2. Add US3 accessibility/interaction polish.
3. Run all automated gates and update older deferred validation files.

### Risk Controls

- Do not add product scope during hardening.
- Keep fixes in the smallest owning module.
- If emulator environment issues recur, document them as environment findings and continue only when app interaction remains possible.
- Never mark an older deferred gate complete unless 011 evidence covers the same scenario.
