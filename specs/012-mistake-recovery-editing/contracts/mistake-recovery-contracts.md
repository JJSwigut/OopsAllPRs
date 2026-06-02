# Contracts: Mistake Recovery and Editing

## Active Workout Set Recovery Contract

All active logged-set recovery operations return an explicit success/failure result and must not partially mutate state on validation failure.

### Edit Logged Set

- **Input**: active workout id, set id, reps, optional canonical weight, edit timestamp.
- **Preconditions**:
  - Active workout exists and is active.
  - Set exists in that active workout and is logged.
  - New reps/weight satisfy set-kind validation.
- **Success**:
  - Same set id remains visible as logged.
  - `loggedAt` remains unchanged.
  - `updatedAt` reflects edit timestamp.
  - Active workout view refreshes rows, focus, next draft, and inline PR feedback.
- **Failure**:
  - Existing logged row remains unchanged.
  - User-facing state exposes a recoverable error.

### Delete Logged Set

- **Input**: active workout id, set id, deletion timestamp.
- **Preconditions**:
  - Active workout exists and is active.
  - Set exists in that active workout and is logged.
- **Success**:
  - Set no longer appears in active logged rows.
  - Next draft remains usable.
  - Operation survives app restart.
- **Failure**:
  - Existing active workout rows remain unchanged.

### Undo Last Logged Set

- **Input**: active workout id, deletion timestamp.
- **Preconditions**:
  - Active workout has at least one logged set.
- **Success**:
  - Exactly the most recent logged set is deleted.
  - Older logged sets remain unchanged.
- **Failure**:
  - No state changes when there is no logged set to undo.

## Active Workout Discard Contract

- **Input**: active workout id and confirmation acceptance.
- **Preconditions**:
  - User has confirmed discard in the UI.
- **Success**:
  - Active workout, drafts, active UX, session, and rest state are cleared.
  - Train shows no resume state after refresh or restart.
  - Completed history, templates, exercises, and PRs remain unchanged.
- **Failure**:
  - User can retry or cancel; existing active workout remains recoverable.

## Template Deletion Contract

- **Input**: routine/template id and confirmation acceptance.
- **Success**:
  - Template disappears from Train/template lists.
  - Launching the deleted template is no longer possible.
  - Completed workout history remains unchanged.
- **Failure**:
  - Template list remains unchanged and exposes a recoverable error.

## Completed Workout Deletion Contract

- **Input**: completed workout id and confirmation acceptance.
- **Success**:
  - Workout disappears from History list and detail.
  - PR/progress projections are rebuilt from remaining completed workouts.
  - Exports exclude the deleted workout and its sets.
  - Saved templates remain if they were created from the deleted workout.
- **Failure**:
  - History selection and source data remain unchanged.

## UI Contract

- Destructive actions except undo last set require confirmation with a visible cancel action.
- Confirmation copy must identify the target action and consequence.
- New controls must use existing Fit design-system primitives and accessible labels.
- Android manual smoke must cover edit/delete/undo, discard, completed deletion, and template deletion where practical.
