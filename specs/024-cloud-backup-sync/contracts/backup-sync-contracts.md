# Contracts: User-Owned Cloud Backup Sync

## Backup Repository Contract

Shared backup orchestration exposes these behaviors:

- Create a `BackupPackage` from current local state.
- Validate a candidate backup package without mutating local data.
- Produce a `RestorePlan` with local and backup summaries.
- Restore a validated package with safety-backup precondition and transactional replacement.
- Return explicit success or recoverable failure for every operation.

## Sync Repository Contract

Shared sync orchestration exposes these behaviors:

- Load persisted `SyncState`.
- Link or unlink a platform-selected backup reference.
- Compute current `LocalRevision`.
- Compare local revision, last synced revision, and backup metadata.
- Execute local-only write, backup-only restore prompt, both-changed conflict, or no-op clean state.
- Persist last outcome, timestamps, conflict summary, and recoverable errors.

## Platform Document Adapter Contract

Platform adapters provide document access without exposing provider-specific details to shared code.

- `createBackupDocument(suggestedName)`: asks the user to create a writable backup document and returns display metadata plus an opaque access reference.
- `openBackupDocument()`: asks the user to select an existing backup document and returns display metadata plus an opaque access reference.
- `readBackup(reference)`: reads the linked document bytes or text.
- `writeBackup(reference, content)`: writes backup content to the linked document.
- `refreshAccess(reference)`: checks whether persisted access remains valid.

**Failure modes**:

- User canceled.
- Access expired or denied.
- File missing or moved.
- Provider temporarily unavailable.
- Read/write failed.
- Invalid or unsupported content.

## Profile UI State Contract

Profile exposes:

- Linked backup display name and access status.
- Last sync/backup/restore outcome.
- Last local and backup timestamps when known.
- Actions: Link backup file, Backup now, Sync now, Restore from file, Unlink.
- Conflict sheet model with local summary, backup summary, and decisions.
- Restore confirmation model with safety backup status and active-workout warning.
- Plain-file privacy label.

Actions must never block navigation to workout logging. Long operations show in-progress state and return recoverable errors.

## Backup Package Contract

V1 package content is a plain JSON object with:

- `formatVersion`
- `createdAt`
- `deviceId`
- `lastLocalRevision`
- `appSchemaVersion`
- `preferences`
- `exercises`
- `routines`
- `activeSession`
- `completedWorkouts`
- `sets`
- `progress`
- `exportMetadata`

Unknown optional fields may be ignored. Missing required sections fail validation before restore.
