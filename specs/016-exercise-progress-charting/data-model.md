# Data Model: Exercise Progress Charting

## Progress Chart State

Represents chart-ready data for the currently selected exercise.

Fields:

- `selectedMetric`: metric currently displayed.
- `availableMetrics`: metrics that have at least one point for the exercise.
- `points`: chronological chart points for `selectedMetric`.
- `latestValueLabel`: display label for the latest point, if present.
- `emptyMessage`: user-facing message for no points.

Validation:

- `selectedMetric` must be one of `availableMetrics` when metrics exist.
- Points must be sorted by recorded time, then stable point id for ties.
- Empty state is valid when no progress points exist or selected metric has no points.

## Progress Chart Point

Represents one plotted point.

Fields:

- `pointId`: source progress point id.
- `metric`: progress metric.
- `value`: raw numeric value for scaling.
- `valueLabel`: unit-aware/reps-aware display value.
- `recordedAt`: wall-clock timestamp.
- `dateLabel`: compact timeline label.
- `sourceWorkoutId`: source completed workout id.
- `sourceSetId`: optional source set id.

Validation:

- Values must be finite before plotting.
- Bodyweight values display reps without fake weight.
- Weighted values display in the user's selected unit.
- Source ids are preserved for evidence navigation.

## Progress Metric Selection

Represents user-selected metric state for the current exercise.

Fields:

- `exerciseCatalogId`: selected exercise id.
- `metric`: selected metric.

State transitions:

- `NoExercise -> ExerciseSelected(default metric)`
- `ExerciseSelected(metric) -> MetricChanged(new metric)`
- `MetricChanged -> ExerciseSelected(default metric)` when changing exercise

Validation:

- Changing metric must not clear the selected exercise or evidence state.
- Selecting an unavailable metric is ignored or falls back to the default available metric.
