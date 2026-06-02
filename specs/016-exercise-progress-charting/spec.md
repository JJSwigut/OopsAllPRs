# Feature Specification: Exercise Progress Charting

**Feature Branch**: `codex/016-exercise-progress-charting`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Add charting so a user can see a graph of progress for any given exercise. Use existing progress points and PR data, keep the scope focused, and implement it in the existing Progress exercise detail."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - See An Exercise Progress Chart (Priority: P1)

As a lifter reviewing an exercise, I want a clear chart of that exercise's progress so I can understand whether I am improving without reading a long list of trend rows.

**Why this priority**: Progress is the product promise, and a chart is the fastest way to make improvement visible.

**Independent Test**: Complete workouts that generate progress points for one exercise, open Progress, select that exercise, and verify a chart appears with the expected metric, values, and timeline.

**Acceptance Scenarios**:

1. **Given** an exercise with at least two progress points for the default metric, **When** the user opens the exercise detail from Progress, **Then** the chart renders a visible line with chronological points and latest value context.
2. **Given** an exercise with one progress point, **When** the user opens the exercise detail, **Then** the chart renders a stable single-point state rather than an empty or broken graph.
3. **Given** an exercise with no progress points, **When** the user opens the exercise detail, **Then** the screen explains that chart data appears after completed workouts.

---

### User Story 2 - Switch Progress Metric (Priority: P2)

As a lifter, I want to choose the progress metric shown on the chart so weighted, bodyweight, volume, and estimated strength progress can each be inspected.

**Why this priority**: Different movements progress differently; bodyweight reps and weighted strength need different metrics.

**Independent Test**: Open an exercise with multiple metric types, switch metrics, and verify the chart and row list use only points for the selected metric.

**Acceptance Scenarios**:

1. **Given** an exercise with best set and estimated one-rep-max points, **When** the user switches metrics, **Then** the chart updates to the selected metric and keeps the selected exercise.
2. **Given** a bodyweight exercise, **When** the user opens its chart, **Then** bodyweight reps are available and do not require a weight value.

---

### User Story 3 - Inspect Chart Evidence (Priority: P3)

As a lifter, I want chart points to connect back to logged workout evidence so I can trust the progress claim.

**Why this priority**: A chart that cannot be traced back to logged work weakens ledger trust.

**Independent Test**: Select an exercise with progress points, tap or select a chart point/row, and verify the existing source/evidence detail opens for that point when source data is available.

**Acceptance Scenarios**:

1. **Given** a chart point with a source set, **When** the user selects that point or its linked trend row, **Then** the existing source workout/set evidence view opens.
2. **Given** source evidence is no longer available locally, **When** the user selects the point, **Then** the app shows a local-unavailable message without crashing.

### Edge Cases

- A selected metric has no points for the exercise; the chart must show an empty state and keep metric controls usable.
- A selected metric has exactly one point; the chart must remain visible and stable.
- Bodyweight points have reps and no weight; labels must not display fake weight.
- Weighted points need unit-aware display based on the user's selected unit.
- Multiple points can share a workout date; ordering must remain deterministic.
- Deleted history can make source evidence unavailable; the chart must not imply unavailable data still exists.
- The chart must fit compact Pixel-class screens without overlapping the resume banner or bottom navigation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST show a visual progress chart in the existing per-exercise Progress detail when progress points exist.
- **FR-002**: System MUST support at least best set, estimated one-rep-max, volume, and bodyweight reps metrics when those points exist for the exercise.
- **FR-003**: System MUST default to the most useful available metric for the selected exercise without requiring extra input.
- **FR-004**: System MUST allow users to switch the selected chart metric from the exercise detail.
- **FR-005**: System MUST render stable empty, single-point, and multi-point chart states.
- **FR-006**: System MUST keep chart labels unit-aware for weighted values and reps-only for bodyweight values.
- **FR-007**: System MUST keep chart points chronological and deterministic when timestamps tie.
- **FR-008**: System MUST connect selectable chart/trend points to existing local evidence when source data is available.
- **FR-009**: System MUST use the existing Fit design system and avoid adding a heavy external charting dependency for the first slice.
- **FR-010**: System MUST preserve existing PR list, exercise selection, source evidence, export, and active workout behavior.

### Key Entities *(include if feature involves data)*

- **ProgressChartState**: The selected exercise's chart-ready state, including selected metric, available metrics, points, empty copy, and latest value.
- **ProgressChartPoint**: A visual point derived from a progress point, including normalized chart coordinates, display labels, metric, source workout/set ids, and recorded time.
- **ProgressMetricSelection**: User-controlled selection of the metric displayed for the currently selected exercise.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can open an exercise chart from Progress in no more than two taps from the Progress tab.
- **SC-002**: Chart state tests cover empty, single-point, multi-point, metric switching, and bodyweight reps-only cases.
- **SC-003**: The chart renders without overlapping Progress content, active workout resume banner, or bottom navigation on a Pixel 9 Pro viewport.
- **SC-004**: Shared unit tests, Android debug build, iOS simulator shared compile, Material guard, and whitespace validation pass before completion.

## Assumptions

- The first implementation slice uses existing `ProgressPoint` and `PersonalRecord` data only; no new persistence tables are required.
- Time range filters can be deferred until the base chart and metric switching are stable.
- Point tap selection may be implemented through chart point controls, linked trend rows, or both, as long as evidence remains reachable.
- Advanced analytics such as estimated trend lines, predicted PRs, bodyweight tracking, and plateaus are out of scope.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Charting lives in Progress review surfaces and does not add friction to active set logging.
- **Ledger Integrity**: Chart points derive from persisted progress points and link back to source evidence without mutating logged sets.
- **Recovery Behavior**: The feature does not modify active session state; active workout resume banner must remain stable while viewing charts.
- **Progress Promise**: This directly improves the PR/progress reward loop and keeps claims inspectable.
- **Local-First Ownership**: Charts use local progress data only and work offline.
- **Platform Scope**: Shared Compose/chart state is Android-validated first and iOS-compile-safe.
- **Design System & Accessibility**: Charting uses FitTheme tokens, readable labels, touch targets, and non-color-only point/value text.
- **Release Evidence**: Automated chart tests and Android visual smoke evidence are required.
