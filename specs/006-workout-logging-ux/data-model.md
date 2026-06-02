# Data Model: Workout Logging UX V1

## ActiveWorkoutUxSession

Recoverable UI state for the active workout logging mode.

**Fields**:
- `activeWorkoutId`
- `focusedExerciseInstanceId`
- `focusedDraftId`
- `updatedAt`

**Rules**:
- Exists only while an active workout exists.
- Focus must point to an existing exercise/draft when possible.
- If focus points to a removed exercise, recovery falls back to the first
  exercise block.
- Clearing or finishing the active workout clears this state.

## PersistedSetDraft

Typed local draft state for the next editable set on an exercise.

**Fields**:
- `draftId`
- `activeWorkoutId`
- `exerciseInstanceId`
- `position`
- `setKind`
- `reps`
- `weight`
- `updatedAt`

**Rules**:
- Drafts are not logged sets and must not appear in the ledger.
- A draft may be persisted with incomplete values so user work survives
  interruptions.
- A confirmed set removes or replaces the draft for that exercise with the next
  default draft.
- Bodyweight drafts may have null weight.
- Weighted drafts may have null weight while editing, but validation blocks
  confirmation until weight is valid.

## ActiveWorkoutView

Display-ready active workout state.

**Fields**:
- `workoutId`
- `startedAt`
- `elapsedMillis`
- `exerciseBlocks`
- `focus`
- `primaryAction`
- `errorMessage`

**Rules**:
- Derived from durable active workout rows plus persisted UX state.
- Must always expose one obvious next action: add exercise, log set, retry, or
  resume draft.
- Does not duplicate top-level navigation state.

## ExerciseBlockState

An active exercise grouped with logged history and next-set input.

**Fields**:
- `exerciseInstanceId`
- `exerciseCatalogId`
- `displayName`
- `isBodyweight`
- `position`
- `loggedRows`
- `draft`
- `prFeedback`
- `inlineError`

**Rules**:
- Blocks sort by position.
- Long names wrap within the block without displacing controls.
- Logged rows are compact rows, not nested cards.
- The focused block receives the strongest visual emphasis.

## SetInputDraft

Editable next-set state rendered inside an exercise block.

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
- Reps must be positive before confirmation.
- Weighted sets require non-negative weight before confirmation.
- Bodyweight sets require reps only in the default path.
- Pending drafts suppress duplicate confirmation.
- Failure returns to editable state with values preserved.

## LoggedSetRow

Compact display projection of a confirmed set.

**Fields**:
- `setId`
- `position`
- `setKind`
- `reps`
- `weight`
- `loggedAt`
- `prFeedback`

**Rules**:
- Must always map to a persisted set with `loggedAt`.
- Appears only after persistence succeeds.
- PR feedback is attached inline when derived for that set.

## PrFeedback

Non-blocking progress message attached to a logged set.

**Fields**:
- `setId`
- `exerciseCatalogId`
- `kind`
- `label`
- `previousValue`
- `newValue`

**Rules**:
- Derived after successful set confirmation.
- Must support weighted records and bodyweight reps records.
- Must be recomputable during active workout hydration.
- Does not block the next set draft from appearing.

## ExercisePickerSession

Search/add state scoped to the active workout.

**Fields**:
- `activeWorkoutId`
- `query`
- `results`
- `customDraft`
- `isOpen`
- `isSaving`
- `errorMessage`

**Rules**:
- Search reads local catalog data only.
- Selection closes the picker only after append succeeds.
- Cancel leaves workout blocks, drafts, and focus unchanged.
- Custom creation captures only the minimum metadata needed to log: name and
  weighted/bodyweight classification.

## State Transitions

```text
No active workout
  -> start empty
Active workout empty
  -> add exercise
Exercise selected
  -> exercise block focused with default draft
Draft edited
  -> draft persisted as UX state
Log set tapped
  -> draft pending
Persist success
  -> logged row shown, PR feedback derived, next draft defaulted
Persist failure
  -> draft restored with retry/error
App restart
  -> active workout, logged rows, focus, and drafts hydrate from local state
Finish/discard
  -> active UX state cleared
```
