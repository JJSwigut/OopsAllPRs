# Tasks: Routine-Aware Rest Timers

**Input**: Design documents from `/specs/013-rest-timer-routines/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Required. This feature changes active workout logging, local persistence, session recovery, shared UI, preferences, and platform alert boundaries.

## Phase 1: Setup

**Purpose**: Confirm feature artifacts and validation targets are ready.

- [X] T001 Confirm rest-timer plan, data model, contracts, quickstart, and backlog artifacts in `specs/013-rest-timer-routines/`
- [X] T002 Add validation result placeholder in `specs/013-rest-timer-routines/validation/rest-timer-results.md`

---

## Phase 2: Foundational

**Purpose**: Shared rest data, persistence, preferences, and timer lifecycle that block all user stories.

- [X] T003 [P] Add shared rest configuration model in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/RestTimerModels.kt`
- [X] T004 Add rest configuration fields to active and routine exercise models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/ExerciseSelection.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/RoutineModels.kt`
- [X] T005 Extend SQLDelight schema and queries for active/routine exercise rest fields and rest preferences in `shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq`, `SetQueries.sq`, `RoutineQueries.sq`, and `WorkoutQueries.sq`
- [X] T006 Extend repository contracts for rest preferences in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt`
- [X] T007 Update in-memory and SQL repositories for rest model mapping and preference persistence in `InMemoryFoundationStore.kt` and `SqlFoundationStore.kt`
- [X] T008 Add rest lifecycle use-case operations for start, adjust, skip, and origin cleanup in `WorkoutLifecycleUseCases.kt`

---

## Phase 3: User Story 1 - Auto-Start Rest After Logging (Priority: P1)

**Goal**: Logging a set for an exercise with rest enabled auto-starts a recoverable RestBar without blocking logging.

**Independent Test**: Start workout, add exercise, log set, see active rest state and usable logging draft.

### Tests for User Story 1

- [X] T009 [P] [US1] Add common rest auto-start tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutRestTimerTest.kt`
- [X] T010 [P] [US1] Add Android SQL rest persistence tests in `shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlRestTimerPersistenceTest.kt`

### Implementation for User Story 1

- [X] T011 [US1] Add rest view models and active workout view mapping in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutModels.kt`
- [X] T012 [US1] Auto-start rest after confirmed set persistence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutStateHolder.kt`
- [X] T013 [US1] Add RestBar UI to the active workout bottom area in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt`
- [X] T014 [US1] Wire active workout rest callbacks and ticking in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

---

## Phase 4: User Story 2 - Save Rest Durations In Routines (Priority: P2)

**Goal**: Routine exercises carry rest durations into launched active workouts and saved templates get default rest.

**Independent Test**: Create/save routine, launch routine, log different exercises, verify each rest duration.

### Tests for User Story 2

- [X] T015 [P] [US2] Add routine rest carryover tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/RoutineRestConfigurationTest.kt`
- [X] T016 [P] [US2] Add Train/profile rest preference tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileRestPreferenceTest.kt`

### Implementation for User Story 2

- [X] T017 [US2] Apply default rest to added active exercises and saved routines in `SetLoggingUseCases.kt` and `RoutineUseCases.kt`
- [X] T018 [US2] Preserve routine rest configuration during routine launch in `WorkoutLifecycleUseCases.kt`
- [X] T019 [US2] Surface default rest and sound preferences in Profile state/UI via `ProfileStateHolder.kt`, `ProfileFlow.kt`, and `AppShell.kt`

---

## Phase 5: User Story 3 - Control Rest Without Losing Flow (Priority: P3)

**Goal**: Users can add time, subtract time, and skip rest from the active workout loop.

**Independent Test**: Start rest, adjust +15/-15, skip, and verify focus/logging remain available.

### Tests for User Story 3

- [X] T020 [P] [US3] Add common adjust/skip and deleted-origin cleanup tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutRestControlsTest.kt`

### Implementation for User Story 3

- [X] T021 [US3] Implement adjust/skip state-holder actions in `ActiveWorkoutStateHolder.kt`
- [X] T022 [US3] Clear invalid rest origins on delete, undo, finish, and discard in `ActiveWorkoutStateHolder.kt`, `RoutineUseCases.kt`, and `WorkoutLifecycleUseCases.kt`
- [X] T023 [US3] Add active workout RestBar +15/-15/skip callbacks in `ActiveWorkoutFlow.kt` and `AppShell.kt`

---

## Phase 6: User Story 4 - Alert When Rest Ends (Priority: P4)

**Goal**: Rest completion schedules/cancels alerts through platform adapters.

**Independent Test**: Start rest, verify scheduling; skip/finish/discard cancels; Android adapter compiles.

### Tests for User Story 4

- [X] T024 [P] [US4] Add fake scheduler tests for schedule/cancel behavior in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/RestNotificationSchedulerTest.kt`

### Implementation for User Story 4

- [X] T025 [US4] Pass rest scheduler and haptic/sound preference boundaries through `AppState.kt` and Android `MainActivity.kt`
- [X] T026 [US4] Implement Android rest alert scheduling and cancellation in `shared/src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/AndroidPlatformAdapters.kt`
- [X] T027 [US4] Keep iOS rest alert boundary compile-safe in `shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/IosPlatformAdapters.kt`

---

## Phase 7: Polish & Validation

**Purpose**: Finish validation evidence and guardrails.

- [X] T028 Update quickstart or validation notes with Android alert permission behavior in `specs/013-rest-timer-routines/validation/rest-timer-results.md`
- [X] T029 Run `./gradlew :shared:testDebugUnitTest`
- [X] T030 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- [X] T031 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T032 Run Material guard against shared/design-system sources
- [X] T033 Run `git diff --check -- .`

## Dependencies & Execution Order

- Phase 1 has no dependencies.
- Phase 2 blocks all user stories.
- US1 is MVP and should complete before US2-US4 integration work.
- US2 depends on foundational rest model and can be validated after US1.
- US3 depends on active rest state from US1.
- US4 depends on lifecycle scheduling/cancel points from US1 and US3.
- Polish depends on all desired stories being complete.

## Parallel Opportunities

- T003 can run in parallel with documentation setup.
- T009 and T010 can be written in parallel.
- T015 and T016 can be written in parallel.
- T020 and T024 are independent once foundational lifecycle APIs exist.

## Implementation Strategy

1. Build the shared rest model/persistence foundation.
2. Deliver US1 as the MVP: auto-start visible rest after logging.
3. Add routine/default preference carryover.
4. Add adjust/skip and origin cleanup.
5. Add Android alert scheduling behind the platform adapter.
6. Run validation gates and record results.
