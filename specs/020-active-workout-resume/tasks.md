# Tasks: Active Workout Resume

**Input**: Design documents from `/specs/020-active-workout-resume/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Behavior-changing work requires shared unit tests and Android validation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Phase 1: Setup

**Purpose**: Verify current files and active feature context.

- [X] T001 Review current Train active-workout UI and seed refresh code in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCases.kt`

---

## Phase 2: Foundational

**Purpose**: Establish reusable state operations required by the user stories.

- [X] T002 Add Train-screen active-workout discard state operation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolder.kt`
- [X] T003 Update resume-card API to expose a Discard action in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt`

---

## Phase 3: User Story 1 - Resume or discard active workout from one card (Priority: P1)

**Goal**: When an active workout exists, Train shows no Start workout or top Resume button and uses the lower card for Resume and Discard.

**Independent Test**: Start a workout, hydrate Train, verify home state can discard directly and template launch conflicts do not create a visible home error.

### Tests for User Story 1

- [X] T004 [P] [US1] Add direct active discard state-holder test in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeStateHolderTest.kt`
- [X] T005 [P] [US1] Update template active-session conflict test in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeTemplateLaunchTest.kt`

### Implementation for User Story 1

- [X] T006 [US1] Remove active-session Start workout/top Resume actions from `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [X] T007 [US1] Disable template row start behavior while active without hiding edit/delete in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt`
- [X] T008 [US1] Wire ResumeBanner Discard through app state refresh/navigation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

---

## Phase 4: User Story 2 - Existing installs receive new seed exercises (Priority: P2)

**Goal**: Existing catalogs receive missing packaged seed exercises without duplicate canonical names or overwriting user-created exercises.

**Independent Test**: Seed a partial catalog, run ensure-seeded with a larger CSV, and verify the missing seed appears while user-created rows are preserved.

### Tests for User Story 2

- [X] T009 [P] [US2] Add seed refresh tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogSeedRefreshTest.kt`

### Implementation for User Story 2

- [X] T010 [US2] Change seed ensure logic to ingest when packaged canonical names are missing in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/ExerciseCatalogUseCases.kt`
- [X] T011 [US2] Preserve existing seed IDs during seed refresh in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlFoundationStore.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/InMemoryFoundationStore.kt`

---

## Phase 5: Polish & Cross-Cutting Concerns

**Purpose**: Validate the full feature and update device build.

- [X] T012 Run shared Android unit tests with `./gradlew :shared:testDebugUnitTest`
- [X] T013 Install and launch on Pixel 9 Pro with `./gradlew :androidApp:installDebug` and `adb shell monkey`
- [X] T014 Update task completion statuses in `specs/020-active-workout-resume/tasks.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on setup.
- **User Story 1 (Phase 3)**: Depends on foundational tasks.
- **User Story 2 (Phase 4)**: Depends on setup; can be implemented independently of US1 after repository behavior is understood.
- **Polish (Phase 5)**: Depends on selected user stories complete.

### User Story Dependencies

- **User Story 1 (P1)**: MVP and highest priority.
- **User Story 2 (P2)**: Independent seed-refresh behavior; should be included before final install because the user observed missing exercises.

### Parallel Opportunities

- T004 and T005 can be authored independently.
- T009 can be authored independently from the Train UI tests.
- UI wiring and seed refresh implementation touch different files after foundational review.

## Implementation Strategy

1. Complete setup/foundational review.
2. Implement and test US1 active-workout Train cleanup.
3. Implement and test US2 seed refresh.
4. Run shared unit tests and install/launch on the connected Pixel 9 Pro.
