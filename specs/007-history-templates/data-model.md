# Data Model: Completed Workout History and Templates

## CompletedWorkoutSummary

Display-ready read-only projection for finish summary and History detail.

**Fields**:
- `workoutId`
- `startedAt`
- `finishedAt`
- `durationMs`
- `exerciseCount`
- `setCount`
- `prCount`
- `exercises`

**Rules**:
- Derived from a completed workout and its completed exercises.
- Includes logged sets only.
- Does not expose mutation actions for completed workout data.
- Must be restorable from local completed history after app restart.

## CompletedExerciseSummary

Display-ready exercise group within a completed workout.

**Fields**:
- `completedExerciseId`
- `exerciseCatalogId`
- `displayName`
- `position`
- `setRows`

**Rules**:
- Sorts by completed exercise position.
- Keeps display name snapshot from the completed workout.
- Supports bodyweight and weighted set rows.

## CompletedSetSummary

Display-ready row for a logged completed set.

**Fields**:
- `setId`
- `position`
- `setKind`
- `reps`
- `weight`
- `loggedAt`
- `prMarker`

**Rules**:
- Must map to a logged set with `loggedAt`.
- Bodyweight rows may have null weight and remain valid.
- PR markers are display evidence only and must not mutate the logged set.

## HistoryListItem

Compact row shown in the History tab.

**Fields**:
- `workoutId`
- `finishedAt`
- `durationLabel`
- `exerciseCount`
- `setCount`
- `hasPr`
- `title`

**Rules**:
- Sorts by `finishedAt` descending.
- Opens the matching `CompletedWorkoutSummary`.
- Must be useful when there are no PRs.

## TemplateSaveDraft

Transient state for naming a reusable template from a completed workout.

**Fields**:
- `completedWorkoutId`
- `name`
- `isSaving`
- `errorMessage`

**Rules**:
- Rejects blank or whitespace-only names.
- Preserves entered name after validation or persistence failure.
- Creates at most one template per successful save action.

## TemplateListItem

Compact template row available from Train.

**Fields**:
- `templateId`
- `name`
- `exerciseCount`
- `setTargetCount`
- `sourceCompletedWorkoutId`

**Rules**:
- Sorts newest or most relevant first according to available template metadata.
- Launches through the routine lifecycle path.
- Duplicate names are allowed because ids are stable.

## TemplateLaunchResult

Outcome of launching a template into a new active workout.

**Fields**:
- `templateId`
- `activeWorkoutId`
- `startedAt`
- `errorMessage`

**Rules**:
- Success creates a new active workout only when no active workout conflict is
  unresolved.
- Active set rows created from planned targets must have null `loggedAt`.
- Completed workout ids, completed exercise ids, and PR markers are not copied.

## State Transitions

```text
Active workout with logged sets
  -> finish workout
Completed workout summary
  -> open History detail
Completed workout detail
  -> enter template name
Template save draft
  -> reusable template created
Train with templates
  -> launch template
Active workout from template
  -> planned sets ready, no logged rows copied

App restart
  -> completed history and templates hydrate from local data
```
