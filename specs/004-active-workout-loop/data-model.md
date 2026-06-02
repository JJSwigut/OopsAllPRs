# Data Model: Active Workout Logging Loop

## ActiveWorkoutView

Display-ready active workout state.

**Fields**:
- `workoutId`
- `startedAt`
- `exerciseBlocks`
- `focus`
- `isAddingExercise`
- `errorMessage`

**Rules**:
- Exists only for an active workout.
- Logged rows come from persisted `ExerciseSet` values.
- Draft rows are unlogged and editable.

## ExerciseBlock

Active exercise plus logged history and next draft.

**Fields**:
- `exerciseInstanceId`
- `exerciseCatalogId`
- `displayName`
- `isBodyweight`
- `position`
- `loggedRows`
- `draft`
- `inlineError`

**Rules**:
- Bodyweight blocks default to `SetKind.BODYWEIGHT`.
- Weighted blocks default to `SetKind.WEIGHTED`.
- Logged rows sort by position and preserve `loggedAt`.

## SetRowDraft

Editable next-set row.

**Fields**:
- `draftId`
- `exerciseInstanceId`
- `position`
- `setKind`
- `reps`
- `weight`
- `isPending`
- `inlineError`

**Rules**:
- Reps must be positive before logging.
- Weighted sets require non-negative weight.
- Bodyweight sets allow null weight.
- Pending drafts ignore duplicate confirm requests.

## LoggedSetRow

Confirmed ledger row projected for display.

**Fields**:
- `setId`
- `position`
- `setKind`
- `reps`
- `weight`
- `loggedAt`

**Rules**:
- Must always have `loggedAt`.
- Mutated only through explicit logged-set edit paths outside this slice.

## RollerFieldState

Inline numeric editor state.

**Fields**:
- `value`
- `range`
- `step`
- `unitLabel`
- `isDirectEntry`
- `directEntryText`
- `errorMessage`

**Rules**:
- Tap-step and drag changes clamp to range.
- Direct entry must parse successfully before updating the draft.

## ActiveWorkoutFocus

Recoverable focus target for active logging.

**Fields**:
- `exerciseInstanceId`
- `draftId`
- `updatedAt`

**Rules**:
- Focus must point to an existing exercise when possible.
- Adding an exercise moves focus to the new block.
- Removing unavailable focus falls back to the first block.

## ExerciseSelectionResult

Catalog selection result used by add-exercise.

**Fields**:
- `exerciseCatalogId`
- `displayName`
- `isBodyweight`

**Rules**:
- Selection appends a new exercise block to the active workout.
- The new block receives the next contiguous position.
