# Contracts: User-Owned Cloud Backup Sync

## Backup Repository Contract

Shared backup orchestration exposes these behaviors:

- Create a `BackupPackage` from current local state.
- Validate a candidate backup package without mutating local data.
- Produce a `RestorePlan` with local and backup summaries.
- Restore a validated package with safety-backup precondition and transactional replacement.
- `restore(pkg: BackupPackage, expectedLocalRevision: String? = null)` accepts an optional canonical local snapshot identity. When supplied, the repository must compare it with the current local snapshot inside the same transaction as replacement, before destructive writes. A mismatch returns `FoundationError.Conflict` without replacing data, clearing the existing link, or advancing agreed baselines. The nullable default preserves existing direct callers; both coordinator restore paths must supply the guard.
- Return explicit success or recoverable failure for every operation.

## Sync Repository Contract

Shared sync orchestration exposes these behaviors:

- Load persisted `SyncState`.
- Link or unlink a platform-selected backup reference.
- Compute current `LocalRevision`.
- Compare local revision, last synced revision, and backup metadata.
- Execute local-only write, backup-only restore prompt, both-changed conflict, or no-op clean state.
- Persist last outcome, timestamps, conflict summary, and recoverable errors.

### Agreed Baselines And Pending Conflicts

- `lastLocalRevision` and `lastBackupRevision` describe the last successful synchronization, not the latest observed revisions. Reading a changed backup must not advance them.
- `BACKUP_CHANGED` and `CONFLICT` remain pending across repeated checks, cancellation, provider errors, and coordinator/repository recreation. This also protects conflict rows written by older versions that advanced their revisions too early.
- Both automatic sync and `Backup now` compare full snapshot content, including preferences, drafts, session state, configurations, and export metadata. Only an explicit keep-local resolution may overwrite a pending remote change. With no conflict, manual backup always writes a fresh snapshot; automatic sync may return `CLEAN` without writing.
- Snapshot revisions are versioned SHA-256 content identities, computed locally rather than trusted from the incoming file's `lastLocalRevision`. Envelope metadata and derived summaries are excluded; nested domain timestamps, positions, and ordered configuration measures/effort kinds remain significant. Unordered entity collections are canonicalized by stable keys.
- Older persisted timestamp/count baselines require an explicit one-time choice before writing. Matching old revision labels cannot establish equal content. Cancellation, errors, and relaunch retain this review requirement.
- Outbound saves acknowledge the identity of the written package, not a newer local snapshot sampled after provider I/O. New edits must remain eligible for a subsequent backup.
- Restore preserves separate remote and local baselines: the incoming file's content identity and the repository-captured restored local identity. Retained immutable configurations may legitimately make these differ. Their presence must not trigger an immediate automatic overwrite of the source file.
- The SQL restore transaction must include the guard, data replacement, previous-link clearing, and restored-local-identity capture. Failure to capture that identity must roll back replacement and link clearing. Post-commit sync-metadata save/relink failures remain partial success: restored data remains, a warning requests relinking, and automatic sync must not use the old file. Cancellation after commit leaves the restored data unlinked.
- Coordinator mutations are serialized per instance. Automatic writes re-read remote content after package preparation and stop if it changed. Both file restore and conflict restore export a local safety snapshot, retain that exact package, and pass its canonical identity to the repository. They must not substitute a later/current revision or the package's legacy wire revision label.
- The coordinator retains an early post-export comparison to reject edits made during provider I/O before entering restore. This is an early rejection only, not an atomicity guarantee: the mandatory repository guard covers edits after that comparison. Guard failure is returned as `FoundationError.Conflict`, is never automatically replayed with another revision, and does not acknowledge restore or advance baselines. A returned failure while saving conflict status must not replace the original conflict error.
- These contracts do not provide provider-level compare-and-swap or atomic multi-device sync. Concurrent cloud writers still require platform/provider support; the local restore transaction requirement does not make remote document operations atomic.

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
