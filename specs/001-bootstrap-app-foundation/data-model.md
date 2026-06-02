# Data Model: Bootstrap Oops All PRs App Foundation

## Overview

The model separates reusable plans, active ledger state, completed history,
exercise catalog data, user preferences, and derived progress. SQLDelight tables
may share normalized child rows where useful, but domain concepts remain
explicit so active logging cannot accidentally mutate routines or history.

## Entities

### ReusableRoutine

Saved workout plan the user can launch later.

**Fields**
- `id`: Stable unique ID.
- `name`: User-visible routine name.
- `createdAt`: Creation timestamp.
- `updatedAt`: Last routine edit timestamp.
- `sourceCompletedWorkoutId`: Optional completed workout used to create it.
- `exercises`: Ordered `RoutineExercise` list.
- `archivedAt`: Optional timestamp for soft deletion.

**Relationships**
- Has many `RoutineExercise`.
- May be created from one `CompletedWorkout`.

**Validation**
- Name must be non-blank after trimming.
- Exercise order must be contiguous and deterministic.
- Routine planned sets must not have `loggedAt`.

### RoutineExercise

Exercise planned inside a reusable routine.

**Fields**
- `id`: Stable unique ID.
- `routineId`: Parent routine ID.
- `exerciseCatalogId`: Reference to `ExerciseCatalogItem`.
- `displayNameSnapshot`: Exercise name at time of routine creation.
- `position`: Zero-based order in routine.
- `plannedSets`: Ordered `RoutineSetTemplate` list.

**Validation**
- `position` must be unique within a routine.
- Must reference an existing catalog item or preserve enough snapshot data if a
  user-created exercise is later archived.

### RoutineSetTemplate

Planned set values copied into an active workout when a routine starts.

**Fields**
- `id`: Stable unique ID.
- `routineExerciseId`: Parent routine exercise ID.
- `position`: Zero-based order.
- `targetWeightKg`: Optional canonical added load.
- `targetReps`: Optional target reps.
- `setKind`: `WEIGHTED` or `BODYWEIGHT`.

**Validation**
- Bodyweight set templates may omit `targetWeightKg`.
- If present, `targetWeightKg` must be >= 0.
- If present, `targetReps` must be > 0.

### ActiveWorkout

Workout currently being logged.

**Fields**
- `id`: Stable unique ID.
- `startedAt`: Wall-clock start instant.
- `routineId`: Optional source routine ID.
- `routineSnapshotName`: Optional source routine name at start.
- `status`: `ACTIVE`.
- `exercises`: Ordered `ActiveExercise` list.
- `createdAt`: Creation timestamp.
- `updatedAt`: Last mutation timestamp.

**Relationships**
- May come from one `ReusableRoutine`.
- Has many `ActiveExercise`.
- Has one `ActiveSessionState` pointer.

**Validation**
- At most one active workout may be current for a user profile.
- Active workout mutation paths must update `updatedAt`.
- Active workout start must set `startedAt`.

### ActiveExercise

Exercise inside an active workout.

**Fields**
- `id`: Stable unique ID.
- `activeWorkoutId`: Parent active workout ID.
- `exerciseCatalogId`: Reference to catalog item.
- `displayNameSnapshot`: Exercise name displayed in workout.
- `position`: Zero-based order.
- `sets`: Ordered `ExerciseSet` list.

**Validation**
- `position` must be unique within active workout.
- Removing an exercise must also remove unlogged sets; logged-set behavior must
  follow product rules for active workout edits.

### ExerciseSet

Planned or logged set in an active or completed workout.

**Fields**
- `id`: Stable unique ID.
- `exerciseInstanceId`: Parent active/completed exercise ID.
- `position`: Zero-based order.
- `setKind`: `WEIGHTED` or `BODYWEIGHT`.
- `weightKg`: Optional canonical added load; required for weighted logged sets.
- `reps`: Optional reps while planned; required when logged.
- `loggedAt`: Optional timestamp; non-null means confirmed workout evidence.
- `createdAt`: Creation timestamp.
- `updatedAt`: Last edit timestamp.
- `editedAt`: Optional timestamp for explicit logged-set edits.

**Validation**
- Logged bodyweight sets require `reps > 0` and may have `weightKg = null` or
  `0.0` to represent no added load.
- Logged weighted sets require `weightKg >= 0` and `reps > 0`.
- A set must not be shown as logged until `loggedAt` is persisted.
- Editing a logged set preserves `loggedAt` and updates only allowed values plus
  edit metadata.

### CompletedWorkout

Historical workout created when an active workout is finished.

**Fields**
- `id`: Stable unique ID; may reuse active workout ID or link via source ID.
- `startedAt`: Start instant.
- `finishedAt`: Finish instant.
- `durationMs`: Derived duration.
- `routineId`: Optional source routine ID.
- `exercises`: Ordered completed exercise list.
- `createdAt`: Creation timestamp.

**Relationships**
- Has many completed exercise instances and logged `ExerciseSet` rows.
- Source evidence for `PersonalRecord` and `ProgressPoint`.

**Validation**
- Completed workouts contain only sets with non-null `loggedAt`.
- Finished duration must be based on wall-clock `finishedAt - startedAt`.
- Logged set tuples from the active workout must be preserved exactly at finish.

### ActiveSessionState

Recoverable pointer and timing state for the active workout.

**Fields**
- `activeWorkoutId`: Optional active workout ID.
- `startedAt`: Wall-clock workout start instant.
- `restEndsAt`: Optional wall-clock instant when rest ends.
- `restStartedAt`: Optional instant when current rest began.
- `restOriginSetId`: Optional set that started the rest.
- `lastOpenedRoute`: Optional route/screen state safe for process recovery.
- `updatedAt`: Last session state write.

**Validation**
- `restEndsAt` is the source of truth for rest remaining.
- Clearing active workout must clear active rest state.
- Rest notification schedule/cancel must follow this state.

### ExerciseCatalogItem

Selectable exercise from baseline seed data or user creation.

**Fields**
- `id`: Stable unique ID.
- `canonicalName`: Unique normalized exercise name.
- `displayName`: User-visible name.
- `muscleGroup`: Primary muscle group or comma-separated groups.
- `equipment`: Equipment classification.
- `movementPattern`: Movement classification.
- `exerciseType`: Training type classification.
- `experienceLevel`: Difficulty classification.
- `bodyRegion`: Body region classification.
- `isBodyweight`: Derived or explicit boolean.
- `isUserCreated`: Boolean.
- `createdAt`: Creation timestamp.
- `updatedAt`: Last edit timestamp.
- `archivedAt`: Optional soft-delete timestamp.
- `sourceSeedVersion`: Optional seed version for seeded rows.
- `userNotes`: Optional user notes.

**Validation**
- `canonicalName` must be unique across active catalog items.
- Seed rows require all classification fields.
- User-created exercises must not be overwritten by seed ingestion.
- Bodyweight classification must be available to logging and PR logic.

### ExerciseSeedImport

Record of seed ingestion for audit and repeated runs.

**Fields**
- `id`: Stable unique ID or seed version.
- `sourceName`: Seed source identifier.
- `sourceHash`: Hash of seed content.
- `importedAt`: Import timestamp.
- `rowCount`: Number of accepted rows.
- `rejectedRowCount`: Number of rejected rows.
- `warnings`: Optional validation summary.

**Validation**
- Re-running the same seed import must be idempotent for seed-owned rows.
- Import must report malformed rows without corrupting valid seed data.

### UserPreferences

Local user settings that affect foundation behavior.

**Fields**
- `weightUnit`: `KILOGRAMS` or `POUNDS`.
- `dateFormat`: User date display preference.
- `defaultRestSeconds`: Default rest duration.
- `androidAutoBackupAllowed`: Documented release setting.
- `createdAt`: Creation timestamp.
- `updatedAt`: Last preference edit timestamp.

**Validation**
- Weight unit changes must not mutate canonical stored evidence.
- Default rest duration must be within product-defined bounds.

### PersonalRecord

Best known performance for an exercise and comparison bucket.

**Fields**
- `id`: Stable unique ID.
- `exerciseCatalogId`: Exercise reference.
- `recordKind`: `WEIGHT_FOR_REPS`, `BODYWEIGHT_REPS`, `ESTIMATED_ONE_REP_MAX`, or `VOLUME`.
- `reps`: Optional reps bucket.
- `weightKg`: Optional canonical weight/load value.
- `value`: Numeric comparison value for record kind.
- `sourceWorkoutId`: Completed workout evidence.
- `sourceSetId`: Source logged set.
- `achievedAt`: Timestamp or workout date.
- `createdAt`: Derivation timestamp.

**Validation**
- Every PR must trace to a completed workout and logged set where applicable.
- Bodyweight rep PRs compare reps, not missing weight.
- Unit conversion must affect display only, not stored evidence.

### ProgressPoint

Historical point used for progression views.

**Fields**
- `id`: Stable unique ID.
- `exerciseCatalogId`: Exercise reference.
- `sourceWorkoutId`: Completed workout evidence.
- `sourceSetId`: Optional set evidence.
- `metric`: `BEST_SET`, `ESTIMATED_ONE_REP_MAX`, `VOLUME`, or `BODYWEIGHT_REPS`.
- `value`: Numeric value for charting.
- `weightKg`: Optional canonical weight.
- `reps`: Optional reps.
- `recordedAt`: Workout or set timestamp.

**Validation**
- Progress points must be rebuildable from completed workout evidence.
- Values must not include unlogged planned sets.

### ExportSnapshot

Generated local export artifact metadata.

**Fields**
- `id`: Stable unique ID.
- `exportType`: `WORKOUTS`, `PERSONAL_RECORDS`, or future supported type.
- `createdAt`: Export creation timestamp.
- `weightUnit`: Display unit used in export.
- `rowCount`: Number of exported data rows.
- `formatVersion`: Export schema version.

**Validation**
- Export rows must quote CSV fields safely.
- Export must include bodyweight reps-only sets.
- Export generation must not require network access.

## State Transitions

### Workout Lifecycle

```text
ReusableRoutine --start--> ActiveWorkout --finish--> CompletedWorkout
                                    |
                                    +--discard--> deleted/archived active state

CompletedWorkout --save as routine--> ReusableRoutine
```

Rules:
- Starting from a routine clones planned exercise/set structure.
- Finishing keeps only logged sets.
- Discarding removes active workout state and rest timer state.
- Completed workout history remains source evidence for PR/progress.

### Set Lifecycle

```text
PlannedSet --confirm--> LoggedSet --edit logged set--> LoggedSet
PlannedSet --remove before finish--> removed
```

Rules:
- Confirmation persists before the app treats the set as logged.
- Logged edits preserve `loggedAt` and audit edit metadata.
- Planned unlogged sets do not enter completed history.

### Active Session Recovery

```text
NoActiveSession --start/resume--> ActiveSessionState
ActiveSessionState --app restart--> hydrate from local activeWorkoutId + wall-clock anchors
ActiveSessionState --finish/discard--> NoActiveSession
```

Rules:
- `startedAt` and `restEndsAt` are wall-clock anchors.
- Remaining rest and elapsed time are recalculated after restart.
- Rest notification schedule must match `restEndsAt`.

