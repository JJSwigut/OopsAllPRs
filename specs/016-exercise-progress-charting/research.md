# Research: Exercise Progress Charting

## Decision: Build A Small FitLineChart Instead Of Adding A Charting Library

**Rationale**: The first slice needs one line chart over local points. Compose Canvas can render this with fewer dependencies, less binary/API surface, and tighter FitTheme control.

**Alternatives considered**:

- Third-party Compose chart library: rejected for dependency weight and KMP compatibility risk.
- Text-only trend list: rejected because the user specifically asked for graph/charting.

## Decision: Derive Chart Points From Existing ProgressPoint Data

**Rationale**: Progress points are already generated from completed workouts and include metric, value, reps/weight, source ids, and timestamps. Reusing them avoids persistence changes and keeps the chart tied to ledger-derived progress.

**Alternatives considered**:

- New chart table: rejected as unnecessary duplication.
- Derive directly from completed workouts in UI: rejected because PR/progress derivation already owns metric semantics.

## Decision: Keep Time Range Filters Out Of The First Slice

**Rationale**: Metric selection is more important than date filtering for first value. Range filters add state and empty-state combinations that can wait until chart rendering is proven.

**Alternatives considered**:

- Add 1M/3M/6M/All immediately: deferred to avoid feature bloat.

## Decision: Select A Default Metric Per Exercise

**Rationale**: Users should see a useful chart immediately. Prefer estimated 1RM or best set for weighted movements, bodyweight reps for bodyweight movements, then fall back to any available metric.

**Alternatives considered**:

- Always default to volume: rejected because it is less intuitive for many strength movements.
- Ask the user before showing a chart: rejected as unnecessary friction.

## Decision: Preserve Evidence Through Existing Detail Paths

**Rationale**: The existing Progress evidence view already explains source workout/set availability. Chart rows and selectable points should use that instead of creating a new evidence surface.

**Alternatives considered**:

- Inline source details under the chart: rejected because it crowds the chart and duplicates evidence logic.
