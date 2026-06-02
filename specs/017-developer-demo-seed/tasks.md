# Tasks: Developer Demo Data Seeding

**Input**: Design documents from `/specs/017-developer-demo-seed/`

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/)

**Tests**: Required because this changes developer-visible behavior, local persistence paths, active recovery setup, and PR/progress validation data.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the feature surface and shared developer tooling structure.

- [X] T001 Create shared developer seed package in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/`
- [X] T002 [P] Create shared developer seed test package in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/`
- [X] T003 [P] Add validation results document in `specs/017-developer-demo-seed/validation/developer-demo-seed-results.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared models and app-state boundary required before individual scenarios.

- [X] T004 Define developer seed scenario/result/state models in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedModels.kt`
- [X] T005 Implement shared `DeveloperSeedStateHolder` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedStateHolder.kt`
- [X] T006 Wire nullable developer seed tooling into `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`
- [X] T007 Wire developer tooling availability through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt` and `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`

**Checkpoint**: App can represent developer seed availability without showing release controls.

---

## Phase 3: User Story 1 - Load Progress Demo Data (Priority: P1) 🎯 MVP

**Goal**: Seed realistic completed workouts, progress points, and PR evidence from a fresh developer build.

**Independent Test**: Trigger progress demo and verify completed history, PRs, progress groups, and chart points exist without duplicates on a second trigger.

### Tests for User Story 1

- [X] T008 [P] [US1] Add progress demo idempotency and ledger tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCaseTest.kt`
- [X] T009 [P] [US1] Add PR/chart/bodyweight assertions in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCaseTest.kt`

### Implementation for User Story 1

- [X] T010 [US1] Implement exercise resolution and baseline seed fallback in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCase.kt`
- [X] T011 [US1] Implement progress demo scenario through workout lifecycle, set logging, and finish paths in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCase.kt`
- [X] T012 [US1] Refresh History and Progress after seed actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: User Story 1 is independently functional and covered by shared tests.

---

## Phase 4: User Story 2 - Load Routine and Rest Timer Demo Data (Priority: P2)

**Goal**: Seed reusable routines with planned weighted/bodyweight sets and rest durations.

**Independent Test**: Trigger routine demo and verify demo routines are available with rest-aware planned sets.

### Tests for User Story 2

- [X] T013 [P] [US2] Add routine demo idempotency and rest configuration tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCaseTest.kt`

### Implementation for User Story 2

- [X] T014 [US2] Implement routine demo builders with reserved demo routine names in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCase.kt`
- [X] T015 [US2] Refresh Train routines after seed actions in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: User Story 2 is independently functional and covered by shared tests.

---

## Phase 5: User Story 3 - Load Active Recovery Demo Data (Priority: P3)

**Goal**: Seed an active workout suitable for resume and recovery validation.

**Independent Test**: Trigger active recovery demo, hydrate app state, and verify the active workout identity and logged set values restore.

### Tests for User Story 3

- [X] T016 [P] [US3] Add active recovery seed conflict and restore tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCaseTest.kt`

### Implementation for User Story 3

- [X] T017 [US3] Implement active recovery demo scenario without overwriting existing active workouts in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/dev/DeveloperSeedUseCase.kt`
- [X] T018 [US3] Refresh active session and resume banner after active recovery seeding in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: User Story 3 is independently functional and covered by shared tests.

---

## Phase 6: Debug UI and Release Guard

**Purpose**: Expose seed actions in debug Profile only, using design-system components.

- [X] T019 Add developer seed card to Profile UI in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T020 Connect Profile developer callbacks in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T021 [P] Add release visibility guard test or static validation evidence in `specs/017-developer-demo-seed/validation/developer-demo-seed-results.md`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Validate the feature and record evidence.

- [X] T022 Run `./gradlew :shared:testDebugUnitTest`
- [X] T023 Run `./gradlew :androidApp:assembleDebug`
- [X] T024 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T025 Run Material guard `rg -n "androidx\\.compose\\.material3|MaterialTheme|androidx\\.compose\\.material\\." shared design-system androidApp --glob '*.kt'`
- [X] T026 Run `git diff --check -- .`
- [X] T027 Update validation evidence in `specs/017-developer-demo-seed/validation/developer-demo-seed-results.md`
- [X] T028 Mark completed tasks in `specs/017-developer-demo-seed/tasks.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup and blocks all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational and is MVP
- **User Story 2 (Phase 4)**: Depends on Foundational; can be implemented after or alongside US1
- **User Story 3 (Phase 5)**: Depends on Foundational and routine demo helpers
- **Debug UI and Release Guard (Phase 6)**: Depends on at least one implemented scenario
- **Polish (Phase 7)**: Depends on implemented scope

### User Story Dependencies

- **User Story 1 (P1)**: No dependency on US2 or US3
- **User Story 2 (P2)**: No dependency on US1, but shares exercise resolution helpers
- **User Story 3 (P3)**: Uses routine-building helpers from US2 to create planned unlogged sets

### Parallel Opportunities

- T002 and T003 can run in parallel.
- T008 and T009 can be written together in the same test file before implementation.
- T013 and T016 can be drafted independently once shared helper contracts are stable.
- Documentation evidence can be updated while builds run.

## Implementation Strategy

### MVP First

1. Complete shared models/state holder and AppState nullable boundary.
2. Implement User Story 1 progress demo data.
3. Validate progress, PR, and chart data with common tests.
4. Add debug Profile entry point and Android debug flag.

### Incremental Delivery

1. Add routine demo data.
2. Add active recovery demo data.
3. Run Android/iOS/build guards and record validation.
