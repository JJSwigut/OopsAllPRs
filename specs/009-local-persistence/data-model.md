# Data Model: Durable Local Persistence

## ActiveWorkout Row

- **Fields**: id, startedAt, routineId, routineSnapshotName, status, createdAt, updatedAt
- **Relationships**: Owns ActiveExercise rows by active workout id; owns ExerciseSet rows by workout id.
- **Validation**: Only one workout may have `ACTIVE` status. Discarded workouts are removed. Completed workouts keep source rows for ledger reconstruction.
- **State transitions**: ACTIVE -> COMPLETED on finish; ACTIVE -> removed on discard.

## ActiveExercise Row

- **Fields**: id, activeWorkoutId, exerciseCatalogId, displayNameSnapshot, isBodyweight, position
- **Relationships**: Belongs to ActiveWorkout; owns ExerciseSet rows by exercise instance id.
- **Validation**: Position is zero or greater. Display snapshot is preserved for history even if catalog names change.

## ExerciseSet Row

- **Fields**: id, workoutId, exerciseInstanceId, position, setKind, weightKg, reps, loggedAt, createdAt, updatedAt, editedAt
- **Relationships**: Belongs to active/completed source workout and exercise instance.
- **Validation**: Logged weighted sets require reps and weight. Logged bodyweight sets require reps and may have null weight. Stored weight is canonical kilograms.

## ActiveSessionState Row

- **Fields**: singletonId, activeWorkoutId, startedAt, restEndsAt, restStartedAt, restOriginSetId, lastOpenedRoute, updatedAt
- **Relationships**: References active workout when one exists.
- **Validation**: Singleton row only. Rest calculations use wall-clock instants.

## ActiveWorkoutUxSession Row

- **Fields**: activeWorkoutId, focusedExerciseInstanceId, focusedDraftId, updatedAt
- **Relationships**: Belongs to active workout.
- **Validation**: Cleared on finish/discard.

## ActiveSetDraft Row

- **Fields**: draftId, activeWorkoutId, exerciseInstanceId, position, setKind, reps, weightKg, updatedAt
- **Relationships**: Belongs to active workout and active exercise.
- **Validation**: Draft values may be partial; stored weight remains canonical kilograms.

## CompletedWorkout Row

- **Fields**: id, sourceActiveWorkoutId, startedAt, finishedAt, durationMs, routineId, createdAt
- **Relationships**: Reconstructs exercises/sets from preserved source active workout rows.
- **Validation**: Created only through finish path. Does not include unlogged planned sets in completed summaries.

## ReusableRoutine, RoutineExercise, RoutineSetTemplate Rows

- **Fields**: routine identity/name/source/archives; exercise snapshots; planned set target weight/reps/kind.
- **Relationships**: Routine owns exercises; routine exercise owns set templates.
- **Validation**: Routine name cannot be blank. Archived routines are hidden from normal lists.

## ExerciseCatalogItem Row

- **Fields**: id, canonicalName, displayName, classification fields, isBodyweight, isUserCreated, timestamps, archivedAt, sourceSeedVersion, userNotes
- **Relationships**: Referenced by workouts/routines/progress by stable id.
- **Validation**: Canonical name is unique. Seed rows do not overwrite user-created rows.

## ExerciseSeedImport Row

- **Fields**: id, sourceName, sourceHash, importedAt, rowCount, rejectedRowCount, warnings
- **Validation**: Import report is stored after seed ingestion completes.

## UserPreferences Row

- **Fields**: singletonId, weightUnit, dateFormat, defaultRestSeconds, androidAutoBackupAllowed, createdAt, updatedAt
- **Validation**: Singleton row; default unit may be pounds; persisted weights remain kg.

## PersonalRecord Row

- **Fields**: id, exerciseCatalogId, recordKind, reps, weightKg, value, sourceWorkoutId, sourceSetId, achievedAt, createdAt
- **Relationships**: Source ids point to completed workout/set evidence.
- **Validation**: Rebuild replaces durable rows as one operation.

## ProgressPoint Row

- **Fields**: id, exerciseCatalogId, sourceWorkoutId, sourceSetId, metric, value, weightKg, reps, recordedAt
- **Relationships**: Source ids point to source workout/set where available.
- **Validation**: Chronological order is preserved on reads.

## ExportSnapshot Row

- **Fields**: id, exportType, createdAt, weightUnit, rowCount, formatVersion
- **Validation**: Created when export succeeds.
