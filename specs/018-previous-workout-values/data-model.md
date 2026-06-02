# Data Model: Previous Workout Values

## PreviousWorkoutValue

Derived value for one set default.

Fields:

- `setIndex`: Zero-based set position within the previous completed exercise.
- `setKind`: Weighted or bodyweight.
- `weight`: Optional canonical weight value. Required only for weighted defaults when valid prior data exists.
- `reps`: Optional reps value. Required for a valid previous-value default.
- `sourceCompletedWorkoutId`: Completed workout that supplied the value.
- `sourceSetId`: Completed set that supplied the value.

Validation:

- `reps` must be positive.
- Weighted values require non-negative weight.
- Bodyweight values are reps-only for this slice; weight is ignored unless future added-load behavior is specified.

## PreviousWorkoutSnapshot

Ordered valid previous values for one exercise from the most recent completed workout containing that exercise.

Fields:

- `exerciseCatalogId`: Exercise identity used for lookup.
- `completedWorkoutId`: Source completed workout.
- `finishedAt`: Source completed workout finish timestamp.
- `values`: Ordered valid `PreviousWorkoutValue` entries.

Relationships:

- Derived from `CompletedWorkout` -> `CompletedExercise` -> logged `ExerciseSet`.
- Not persisted as a separate entity.

Validation:

- Snapshot must ignore invalid sets.
- Empty snapshots are treated the same as no previous workout.

## ExplicitRoutineTarget

Existing `RoutineSetTemplate` field values that represent user-planned work.

Rules:

- `targetWeight` is explicit when non-null.
- `targetReps` is explicit when non-null.
- Explicit fields must not be replaced by previous values.
- Missing fields may be filled from the matching previous value for launch-time active sets.

## PrefilledActiveDraft

Existing active workout set or active UI draft after defaults are resolved.

Source precedence:

1. Explicit active/routine planned value.
2. Previous workout value for the same exercise and set index.
3. Existing safe default for exercise type.

State transitions:

- Created during exercise add or routine launch.
- May be edited by the user before logging.
- Persists through active UX draft recovery when visible.
- Becomes a ledger set only after explicit log confirmation succeeds.

## Non-Mutation Rules

- Completed workouts are read-only source evidence.
- Routines are not rewritten when previous values fill launch-time active sets.
- PRs and progress points are not recalculated until a user logs or edits a set through existing mutation paths.
- Export output must not change solely because previous values were looked up.
