# Tasks: Exercise Progress Charting

**Input**: Design documents from `/specs/016-exercise-progress-charting/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Required for chart state generation, metric switching, bodyweight labels, and evidence preservation.

## Phase 1: Setup

**Purpose**: Confirm feature artifacts and current Progress implementation.

- [X] T001 Confirm spec, plan, research, data model, contracts, quickstart, checklist, and validation artifacts exist in `specs/016-exercise-progress-charting/`
- [X] T002 Confirm AGENTS.md and `.specify/feature.json` point to `specs/016-exercise-progress-charting/`
- [X] T003 Inspect existing Progress models, state holder, flow, and tests in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/` and `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/`

---

## Phase 2: Foundational

**Purpose**: Chart view models and reusable design-system chart primitive.

- [X] T004 [P] Add chart state/model tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressChartModelTest.kt`
- [X] T005 [P] Add metric selection state-holder tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressChartStateHolderTest.kt`
- [X] T006 Add chart view models and builders in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressChartModels.kt`
- [X] T007 Add reusable token-driven chart primitive in `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/FitLineChart.kt`

---

## Phase 3: User Story 1 - See An Exercise Progress Chart (Priority: P1) MVP

**Goal**: Exercise detail shows a visible progress chart for selected exercise points.

**Independent Test**: Select an exercise with two progress points and verify chart state has chronological points and latest value context.

### Tests for User Story 1

- [X] T008 [US1] Run chart model tests for empty, single-point, and multi-point states in `ProgressChartModelTest.kt`

### Implementation for User Story 1

- [X] T009 [US1] Add chart state to `ProgressExerciseGroup` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressModels.kt`
- [X] T010 [US1] Render chart section in exercise detail in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`

---

## Phase 4: User Story 2 - Switch Progress Metric (Priority: P2)

**Goal**: User can switch the selected metric and chart/trend rows update for the selected exercise.

**Independent Test**: Select an exercise with multiple metrics, switch metrics, and verify selected exercise remains open with filtered chart and rows.

### Tests for User Story 2

- [X] T011 [US2] Run metric selection tests in `ProgressChartStateHolderTest.kt`

### Implementation for User Story 2

- [X] T012 [US2] Add selected metric state and metric-switch action in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt`
- [X] T013 [US2] Wire metric selector callback through `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt` and `ProgressFlow.kt`

---

## Phase 5: User Story 3 - Inspect Chart Evidence (Priority: P3)

**Goal**: Chart/trend points preserve source evidence navigation.

**Independent Test**: Select a chart/trend point and confirm existing evidence detail opens or unavailable-source copy appears.

### Tests for User Story 3

- [X] T014 [P] [US3] Add chart point source id assertions in `ProgressChartModelTest.kt`

### Implementation for User Story 3

- [X] T015 [US3] Preserve source ids in chart point models in `ProgressChartModels.kt`
- [X] T016 [US3] Keep existing trend row evidence actions available in `ProgressFlow.kt`

---

## Phase 6: Polish & Validation

**Purpose**: Run gates, record evidence, and close tasks.

- [X] T017 Run `./gradlew :shared:testDebugUnitTest`
- [X] T018 Run `./gradlew :androidApp:assembleDebug`
- [X] T019 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T020 Run Material guard against shared/design-system sources
- [X] T021 Run `git diff --check -- .`
- [X] T022 Record manual Android chart smoke outcome or deferred note in `specs/016-exercise-progress-charting/validation/exercise-progress-charting-results.md`

## Dependencies & Execution Order

- Phase 1 has no dependencies.
- Phase 2 blocks chart UI integration.
- US1 is MVP and should complete before metric switching.
- US2 depends on US1 chart state.
- US3 depends on chart point source ids from US1/US2.
- Polish depends on all implemented stories.

## Parallel Opportunities

- T004 and T005 can be written in parallel.
- T006 and T007 touch different modules but should coordinate on chart point shape.
- US3 source-id assertions can be added while UI metric wiring is underway.

## Implementation Strategy

1. Build chart state/model tests first.
2. Add chart-ready models from existing progress points.
3. Add reusable `FitLineChart`.
4. Render chart in selected exercise detail.
5. Add metric selection state and UI.
6. Validate source evidence remains reachable.
7. Run automated and manual validation gates.
