# Data Model: Mistake Recovery and Editing

## Active Logged Set Correction

Represents an explicit edit to an already logged active-workout set.

- **Identity**: existing set id and exercise instance id.
- **Mutable fields**: reps and optional canonical weight, constrained by set kind.
- **Preserved fields**: set id, exercise instance id, set kind, position, `loggedAt`, `createdAt`.
- **Updated fields**: `updatedAt`.
- **Validation**:
  - Weighted sets require valid reps and valid canonical weight.
  - Bodyweight sets require valid reps and may omit weight.
  - Invalid edits return failure and do not change the stored set.

## Active Logged Set Deletion

Represents removal of one logged set from an active workout.

- **Identity**: active workout id and set id.
- **Effect**: set row is removed from active workout storage.
- **Derived state**: visible logged rows, set counts, next draft position, and inline PR feedback refresh.
- **Validation**:
  - Target workout must be active.
  - Target set must exist and be logged.
  - Not-found targets return recoverable failure.

## Undo Last Set

Shortcut action that resolves to the most recent logged set in an active workout.

- **Identity**: active workout id.
- **Resolution rule**: highest `loggedAt`; if tied, latest updated/position order from current active workout.
- **Effect**: uses the same deletion behavior as Active Logged Set Deletion.
- **Validation**: no logged sets means no-op failure or disabled UI state.

## Active Workout Discard

Confirmed removal of the current active workout.

- **Identity**: active workout id.
- **Effect**: active workout rows, active exercise rows, set rows, drafts, active UX, active session state, and rest state are cleared.
- **Preserved data**: completed workouts, templates, exercises, PR history derived from completed workouts.
- **Validation**: confirmation must be active before destructive execution from UI.

## Template Deletion

Confirmed removal of a reusable routine/template.

- **Identity**: routine/template id.
- **Effect**: routine row, routine exercises, and routine planned-set templates are removed.
- **Preserved data**: completed source workout, completed history, PRs, active workouts not launched from this deletion action.
- **Validation**: not-found deletion returns recoverable failure.

## Completed Workout Deletion

Confirmed removal of a completed workout from local history.

- **Identity**: completed workout id.
- **Effect**: completed workout row and associated completed/source set data are removed or hidden from all completed-history queries and exports.
- **Derived state**: PR and progress projections are rebuilt from remaining completed workouts.
- **Preserved data**: saved templates created from the workout remain independent snapshots.
- **Validation**:
  - If the completed workout is selected in History, selection clears after deletion.
  - Repeated deletion returns recoverable not-found failure.

## UI Confirmation State

Shared UI state for destructive operations.

- **Fields**: target id, title, message, confirm label, cancel label, action type.
- **Transitions**:
  - Idle -> Pending confirmation when user requests destructive operation.
  - Pending -> Idle unchanged when user cancels.
  - Pending -> Executing -> Idle success when destructive operation succeeds.
  - Pending -> Idle with error when destructive operation fails.
