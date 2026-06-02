# Data Model: Progress and PR Dashboard V1

## PrDashboardState

Display state for the Progress destination.

**Fields**:
- `recentRows`
- `exerciseGroups`
- `selectedExerciseId`
- `selectedEvidence`
- `errorMessage`

**Rules**:
- Derived from local personal records, progress points, and completed workouts.
- Does not mutate underlying workout or progress data.
- Empty state appears when there are no personal records.

## PrRow

Display-ready personal record row.

**Fields**:
- `recordId`
- `exerciseCatalogId`
- `exerciseName`
- `recordKind`
- `valueLabel`
- `improvementLabel`
- `sourceWorkoutId`
- `sourceSetId`
- `achievedAt`

**Rules**:
- Recent rows sort by `achievedAt` descending.
- Bodyweight reps rows never require weight.
- Weighted rows display in the user's selected unit without mutating canonical
  storage.
- Source ids must remain stable for evidence lookup.

## ExercisePrGroup

All records and trend points for one exercise.

**Fields**:
- `exerciseCatalogId`
- `exerciseName`
- `records`
- `trendPoints`

**Rules**:
- Records group by exercise and then record kind.
- Weighted-only sections are omitted when absent.
- Bodyweight-only exercises remain valid groups.

## PrEvidence

Source completed workout/set context for a PR.

**Fields**:
- `recordId`
- `sourceWorkoutId`
- `sourceSetId`
- `workoutSummary`
- `sourceExerciseName`
- `sourceSetLabel`
- `isAvailable`
- `errorMessage`

**Rules**:
- Available evidence must match the record's source workout and set ids.
- Missing evidence returns a safe unavailable state.
- Evidence is read-only.

## TrendPointRow

Display-ready progress point for one exercise/metric over time.

**Fields**:
- `pointId`
- `exerciseCatalogId`
- `metric`
- `valueLabel`
- `sourceWorkoutId`
- `sourceSetId`
- `recordedAt`

**Rules**:
- Points sort chronologically within an exercise group.
- Weight values respect the selected display unit.
- Points with missing source set ids remain displayable but cannot open set
  evidence.

## State Transitions

```text
Completed workouts
  -> rebuild PR/progress data
Progress opened
  -> dashboard state hydrates from local records and completed evidence
Recent PR selected
  -> evidence lookup by source workout/set id
Exercise selected
  -> exercise group detail with record rows and trend points
Missing evidence
  -> unavailable evidence state
```
