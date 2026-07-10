# Research: User-Owned Cloud Backup Sync

## Decision: Visible Plain JSON Backup Package

Use a user-visible, restore-capable plain JSON backup package for V1.

**Rationale**: Users can inspect, copy, and move the backup without trusting an app account or hidden provider storage. JSON is straightforward to validate in tests, can carry nested training snapshots, and fits restore better than the existing CSV export format.

**Alternatives considered**:

- CSV export only: good for inspection, insufficient for complete restore and active session state.
- Provider-hidden app storage: convenient for automation, but conflicts with the autonomy goal.
- Encrypted package: improves privacy at rest, but introduces passphrase/key recovery scope that is explicitly out of V1.

## Decision: Platform Document Providers Instead of Provider SDKs

Use user-selected document providers and local files through platform document picker boundaries.

**Rationale**: This keeps the feature provider-agnostic and avoids OAuth, account linking, cloud SDK dependency, and backend scope. The user chooses Google Drive, iCloud Drive, Dropbox, local files, or another provider through the OS.

**Alternatives considered**:

- Google Drive SDK: provider-specific and requires account/OAuth scope.
- Firebase or hosted backend: violates account-free/local-first V1.
- Android/iOS automatic backup only: not visible or portable enough for explicit user control.

## Decision: Whole-Snapshot Conflict Policy

For V1, compare local and backup revisions and ask the user to keep local, restore backup after safety backup, or cancel when both changed.

**Rationale**: Field-level merge is high risk for workout ledger, PR evidence, active session state, and future data contracts. Whole-snapshot decisions are understandable and preserve user control.

**Alternatives considered**:

- Last-writer-wins: risks silent data loss.
- Automatic row-level merge: needs stable mutation logs and conflict semantics that are outside V1.
- Backup-only manual export: misses the requested light-sync behavior.

## Decision: Timestamp-Derived V1 Local Revision

Derive local revision from existing created/updated timestamps and latest domain timestamps initially.

**Rationale**: Existing data already carries enough timestamps to detect ordinary local changes without adding a mutation log. This keeps scope smaller while still leaving a future path for merge-ready mutation tracking.

**Alternatives considered**:

- Add mutation log now: better for future multi-device merge but significantly expands schema and write-path scope.
- Only compare backup file modified time: misses local-only changes and is provider-dependent.

## Decision: Transactional Restore with Safety Backup

Validate backup packages before replacement, create or offer a safety backup, then restore inside one local transaction in FK-safe order.

**Rationale**: Restore is destructive and touches the workout ledger, active session, routines, exercises, preferences, and progress evidence. Transactionality and safety backup are the core protection against partial or accidental loss.

**Alternatives considered**:

- Replace rows section by section without transaction: unacceptable partial failure risk.
- Require users to manually export first: too easy to skip and inconsistent with a high-risk restore path.

## Decision: Profile as the User Control Surface

Add link, sync, backup, restore, status, and conflict controls to Profile.

**Rationale**: Profile already owns local-first status and export/settings behavior. Backup and sync are data ownership actions, not workout logging actions, so they belong outside the fast logging loop.

**Alternatives considered**:

- New top-level Sync tab: adds navigation weight for an infrequent control surface.
- Background-only sync indicator: conflicts with explicit user confirmation and visible ownership.
