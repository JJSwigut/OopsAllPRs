# Tasks: Progress and PR Dashboard V1

**Input**: Design documents from `/specs/008-progress-pr-dashboard/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/progress-pr-dashboard-contracts.md`

**Tests**: Required. This slice changes PR/progress derivation, local state restoration, evidence lookup, and shared UI behavior. Add common tests before implementation and validate Android first while keeping iOS compile green.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Phase 1: Setup

**Purpose**: Confirm the generated feature package is complete and ready for implementation.

- [X] T001 Verify Spec Kit artifacts for feature 008 have no unresolved placeholders in `specs/008-progress-pr-dashboard/spec.md`, `specs/008-progress-pr-dashboard/plan.md`, `specs/008-progress-pr-dashboard/research.md`, `specs/008-progress-pr-dashboard/data-model.md`, `specs/008-progress-pr-dashboard/contracts/progress-pr-dashboard-contracts.md`, and `specs/008-progress-pr-dashboard/quickstart.md`
- [X] T002 Confirm `AGENTS.md` points to `specs/008-progress-pr-dashboard/plan.md`

---

## Phase 2: Foundational

**Purpose**: Shared derivation and display infrastructure that all Progress stories depend on.

**CRITICAL**: No Progress user story should ship until these are complete.

- [X] T003 [P] Add common derivation tests for weighted best set, bodyweight reps, estimated one-rep max, and volume records in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationRecordKindsTest.kt`
- [X] T004 [P] Add common display model tests for canonical weight-unit display without mutating stored kilograms in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressDisplayModelsTest.kt`
- [X] T005 Extend PR derivation for `ESTIMATED_ONE_REP_MAX` and `VOLUME` while preserving source workout/set ids in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationUseCase.kt`
- [X] T006 Create Progress dashboard display projections for PR rows, exercise groups, evidence, and trend rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`
- [X] T007 Update `ProgressStateHolder` dependencies and state shape for records, points, completed workouts, selected exercise, and selected evidence in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T008 Wire the updated Progress state holder through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/di/AppModule.kt`

**Checkpoint**: PR derivation and Progress state projections are available to all stories.

---

## Phase 3: User Story 1 - See Recent PRs First (Priority: P1)

**Goal**: Opening Progress shows recent PRs in reverse chronological order with clear weighted/bodyweight/e1RM/volume labels.

**Independent Test**: Seed completed workouts with multiple PR types, refresh Progress, and verify the first screen has newest PRs with plain-language labels and an empty state when no PRs exist.

### Tests for User Story 1

- [X] T009 [P] [US1] Add common state-holder tests for recent PR ordering and empty state in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolderTest.kt`
- [X] T010 [P] [US1] Add common UI model tests for bodyweight reps-only and weighted value labels in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressDisplayModelsTest.kt`

### Implementation for User Story 1

- [X] T011 [US1] Implement recent PR row projection and empty-state data in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T012 [US1] Replace the Progress placeholder screen with a PR-first dashboard using design-system components in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`
- [X] T013 [US1] Connect Progress dashboard callbacks in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: Recent PR dashboard works independently.

---

## Phase 4: User Story 2 - Browse Records by Exercise (Priority: P2)

**Goal**: User can browse exercises that have records and inspect all record types for one exercise.

**Independent Test**: Seed records for at least two exercises, refresh Progress, select an exercise, and verify only that exercise's PRs appear with correct labels.

### Tests for User Story 2

- [X] T014 [P] [US2] Add common tests for exercise grouping and selection state in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolderTest.kt`

### Implementation for User Story 2

- [X] T015 [US2] Implement exercise grouping and selected exercise details in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`
- [X] T016 [US2] Add exercise group list and selected exercise detail UI in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`

**Checkpoint**: Exercise record browsing works independently.

---

## Phase 5: User Story 3 - Inspect Source Evidence (Priority: P3)

**Goal**: User can open a PR and see the completed workout/set that produced it, with a clear fallback when evidence is missing.

**Independent Test**: Seed one valid-source PR and one missing-source PR, open both evidence views, and verify the valid source shows the source workout/set while the missing source does not fabricate evidence.

### Tests for User Story 3

- [X] T017 [P] [US3] Add common evidence lookup and missing-source fallback tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressEvidenceTest.kt`

### Implementation for User Story 3

- [X] T018 [US3] Implement source evidence lookup from completed workouts in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T019 [US3] Add evidence detail UI and close navigation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`

**Checkpoint**: Source evidence is traceable and missing evidence is explicit.

---

## Phase 6: User Story 4 - Understand Progress Trends (Priority: P4)

**Goal**: User can see simple chronological trend context for a selected exercise without adding chart dependencies.

**Independent Test**: Seed progress points for one exercise out of chronological insertion order, select that exercise, and verify trend rows display oldest-to-newest.

### Tests for User Story 4

- [X] T020 [P] [US4] Add common trend ordering tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressTrendPointTest.kt`

### Implementation for User Story 4

- [X] T021 [US4] Implement trend point row projection in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`
- [X] T022 [US4] Add selected exercise trend context UI in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`

**Checkpoint**: Selected exercise trend context works independently.

---

## Phase 7: Polish and Release Validation

**Purpose**: Cross-cutting quality gates for shared KMP, local-first behavior, and design-system compliance.

- [X] T023 [P] Verify `ProgressFlow` uses FitTheme/design-system primitives and does not reintroduce Material 3 in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`
- [X] T024 Run `./gradlew :shared:testDebugUnitTest`
- [X] T025 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- [X] T026 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T027 Run Material scan: `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts`
- [X] T028 Run whitespace validation: `git diff --check -- .`
- [X] T029 Attempt Android manual gate discovery with `adb devices -l` and `emulator -list-avds`; if no target exists, document the deferred manual gate
- [X] T030 Stage and commit feature 008 changes with message `Implement progress PR dashboard`

---

## Dependencies and Execution Order

- Phase 1 must complete before foundational work.
- Phase 2 blocks all user stories because record derivation and display projections are shared.
- US1 is the MVP and should complete before US2/US3/US4.
- US2 depends on shared exercise grouping from Phase 2 and can be validated without evidence.
- US3 depends on record rows from US1 and completed workout lookup from Phase 2.
- US4 depends on selected exercise state from US2 and progress point projections from Phase 2.
- Phase 7 runs after all selected stories are implemented.

## Parallel Opportunities

- T003 and T004 can be written in parallel.
- T009 and T010 can be written in parallel after Phase 2 test scaffolding exists.
- T014, T017, and T020 touch separate test files and can be prepared in parallel after the foundational models compile.
- T024, T025, T026, T027, and T028 are independent validation commands but should be reported as separate release gates.

## Implementation Strategy

1. Build derivation and projection tests first.
2. Extend PR derivation and Progress state models.
3. Implement the recent PR dashboard MVP.
4. Add exercise detail, evidence detail, and trend rows in priority order.
5. Run Android-first and iOS compile validation.
