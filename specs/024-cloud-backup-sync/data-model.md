# Data Model: User-Owned Cloud Backup Sync

## BackupPackage

Represents a complete restore-capable user-owned data snapshot.

**Fields**:

- `formatVersion`: backup package format number.
- `createdAt`: package creation instant.
- `deviceId`: stable local device identifier used for conflict context.
- `lastLocalRevision`: revision summary at backup time.
- `appSchemaVersion`: local app database/schema version represented by the snapshot.
- `preferences`: user preferences and ownership/profile settings needed for restore.
- `exercises`: seeded references where useful plus user-created exercise records.
- `routines`: routines/templates and routine exercise configuration.
- `activeSession`: active workout/session/draft/rest state needed for migration.
- `completedWorkouts`: completed workout summaries.
- `sets`: logged set rows and source timestamps.
- `progress`: PR/progress evidence and source relationships.
- `exportMetadata`: useful export history metadata where it helps preserve Profile context.

**Validation Rules**:

- Required metadata must be present before restore.
- Unsupported future format versions are rejected with a recoverable error.
- Backup schema version must be supported by restore migration rules.
- Source identifiers and relationships must resolve before replacement begins.

## SyncState

Represents the locally persisted link and last known sync outcome.

**Fields**:

- `linkedBackupDisplayName`
- `providerReference`
- `providerReferenceKind`
- `lastBackupRevision`
- `lastBackupTimestamp`
- `lastLocalRevision`
- `lastLocalTimestamp`
- `lastOutcome`
- `lastError`
- `lastConflictSummary`

**Validation Rules**:

- Provider reference is opaque to shared code and interpreted only by platform adapters.
- Last outcome must distinguish clean, local-written, backup-changed, conflict, unavailable, failed, and unlinked states.
- Errors are recoverable and must not imply data mutation unless an operation completed successfully.

## LocalRevision

Represents a monotonic summary of user-data changes for V1 sync decisions.

**Fields**:

- `revisionValue`
- `latestUserDataTimestamp`
- `summaryCounts`

**Validation Rules**:

- Must include changes across preferences, exercises, routines, active session, completed workouts, sets, and progress evidence.
- Must be deterministic for the same local data.

## SnapshotSummary

Human-readable counts and timestamps used in restore and conflict prompts.

**Fields**:

- `workoutCount`
- `setCount`
- `routineCount`
- `customExerciseCount`
- `progressRecordCount`
- `hasActiveWorkout`
- `latestWorkoutTimestamp`
- `latestUpdatedTimestamp`

**Validation Rules**:

- Counts must be derived from the backup/local snapshot being summarized.
- Summary must be safe to show before destructive restore.

## RestorePlan

Represents a validated pending restore operation.

**Fields**:

- `backupSummary`
- `localSummary`
- `requiresActiveWorkoutWarning`
- `safetyBackupStatus`
- `validationStatus`
- `warnings`

**Validation Rules**:

- Cannot execute until validation passes.
- Cannot execute destructive replacement unless safety backup has succeeded or the platform handoff has been explicitly attempted and surfaced.
- Must warn when replacing active workout/session state.

## ConflictDecision

Represents the user-selected outcome for both-changed sync state.

**States**:

- `KeepLocalOverwriteBackup`
- `RestoreBackupAfterSafetyCopy`
- `Cancel`

**Validation Rules**:

- Keep-local writes the linked backup and updates sync metadata only after write succeeds.
- Restore-backup creates/offers a safety backup before replacement.
- Cancel does not mutate either copy.

## State Transitions

```text
Unlinked
  -> LinkedClean
  -> LocalChanged
  -> BackupChanged
  -> ConflictDetected
  -> SyncInProgress
  -> RestorePending
  -> ErrorRecoverable

ConflictDetected
  -> LinkedClean (keep local succeeds)
  -> RestorePending (restore backup selected)
  -> ConflictDetected (cancel)

RestorePending
  -> RestoreInProgress
  -> LinkedClean (restore succeeds)
  -> ErrorRecoverable (validation/safety/restore fails)
```
