# Feature Specification: User-Owned Cloud Backup Sync

**Feature Branch**: `024-cloud-backup-sync`

**Created**: 2026-06-04

**Status**: Draft

**Input**: User description: "Add provider-agnostic light sync without Firebase, app accounts, or a hosted backend. The app stays local-first, but Profile gains a user-linked backup file stored through platform document providers such as Google Drive, iCloud Drive, Dropbox, or local files. V1 uses a visible portable plain backup package, manual plus launch/resume sync checks, and user-confirmed conflict resolution."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Link and Create a Portable Backup (Priority: P1)

As a local-first lifter, I want to link a visible backup file from Profile and create a complete backup on demand so I can keep a portable copy of my training data in a storage location I control.

**Why this priority**: A trustworthy, user-controlled backup is the minimum useful slice and directly supports local-first ownership without accounts or a hosted service.

**Independent Test**: From Profile, link a user-selected backup location, create a backup, restart the app, and verify Profile still shows the linked location and can read the backup metadata.

**Acceptance Scenarios**:

1. **Given** no backup is linked, **When** the user chooses Link backup file and selects a writable file location, **Then** Profile shows the linked display name and backup actions become available.
2. **Given** a linked location and local workouts, routines, exercises, preferences, active session state, and progress evidence, **When** the user chooses Backup now, **Then** a complete readable backup package is written and Profile shows the last successful backup time.
3. **Given** the app is offline, **When** the user links or writes a local or provider-backed document, **Then** the app performs the operation without any app account, app backend, or provider-specific sign-in screen.
4. **Given** a backup file exists outside the app, **When** the user inspects it through their chosen storage provider, **Then** it is recognizable as an Oops All PRs backup and labeled as readable by anyone with access to that file.

---

### User Story 2 - Restore from a Backup Safely (Priority: P2)

As a user moving to a new device or recovering data, I want to restore from a selected backup file with a safety copy before replacement so I can regain my training history without accidental loss.

**Why this priority**: Backup is incomplete without restore, and restore is a high-risk data replacement path that must protect the current local ledger.

**Independent Test**: Start with an empty local profile, restore from a backup containing completed workouts, routines, custom exercises, preferences, active session state, and PR evidence, then verify all areas hydrate correctly.

**Acceptance Scenarios**:

1. **Given** an empty local profile, **When** the user chooses Restore from file and confirms the selected backup, **Then** the app imports the complete training snapshot and returns Profile to a successful restore state.
2. **Given** existing local data, **When** the user starts a restore, **Then** the app creates or offers a safety backup of the current local data before any destructive replacement happens.
3. **Given** an active local workout exists, **When** the selected backup would replace active workout state, **Then** the confirmation clearly warns that the current active workout will be replaced.
4. **Given** a restore fails during validation or replacement, **When** the user returns to the app, **Then** existing local data remains intact and Profile shows a recoverable error.

---

### User Story 3 - Sync and Resolve Conflicts Explicitly (Priority: P3)

As a user who edits data across launches or devices, I want Profile to detect whether local data or the linked backup changed and ask before overwriting either copy so I stay in control of conflicts.

**Why this priority**: Light sync must be useful beyond one-off backup while avoiding hidden automation that could overwrite training data.

**Independent Test**: Create local-only, backup-only, and both-changed states, trigger Sync now and launch/resume checks, and verify each state produces the expected write, restore prompt, or conflict sheet.

**Acceptance Scenarios**:

1. **Given** only local data changed since the last sync, **When** a sync check runs, **Then** the app writes the linked backup and records a successful sync.
2. **Given** only the linked backup changed since the last sync, **When** a sync check runs, **Then** the app asks the user before restoring the backup.
3. **Given** both local data and the linked backup changed, **When** a sync check runs, **Then** Profile shows local and backup timestamps plus summary counts and offers keep local, restore backup after safety export, or cancel.
4. **Given** a conflict sheet is shown, **When** the user cancels, **Then** neither local data nor the linked backup is modified and Profile records the unresolved state.
5. **Given** the app launches or resumes with a linked backup, **When** the linked file can be read, **Then** the app performs a lightweight check and surfaces any action needed without blocking fast workout logging.

### Edge Cases

- The linked file is moved, deleted, renamed, unavailable, or no longer writable.
- A selected file is malformed, from an unsupported future format, from an incompatible schema, or missing required training data sections.
- The backup represents an older app schema that can be restored through supported migration rules, or it is rejected with a clear message if not supported.
- The current local database has no workouts yet; backup and restore should still handle preferences, seeded/custom exercises, and empty sections.
- Bodyweight, fractional weights, selected units, locale decimal behavior, timed sets, and PR evidence must survive backup and restore without changing meaning.
- The user starts a workout while a sync warning exists; logging remains available and the warning stays recoverable from Profile.
- The platform provider requires renewed access after app restart; Profile must guide the user to relink without losing local data.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to link a user-selected backup file location from Profile.
- **FR-002**: Users MUST be able to create a new backup file or link an existing backup file.
- **FR-003**: The linked backup display name, access reference, last sync metadata, and last outcome MUST persist locally and reload after app restart.
- **FR-004**: Users MUST be able to manually trigger Backup now, Sync now, and Restore from file from Profile.
- **FR-005**: Backup packages MUST include format version, creation time, device identifier, local revision metadata, app schema version, and a full snapshot of user-owned training data.
- **FR-006**: Backup packages MUST include preferences, custom exercise data, routines, active workout/session/draft state, completed workouts, sets, progress and PR evidence, and useful export metadata.
- **FR-007**: Backup packages MUST be plain, portable, readable files and MUST be labeled as readable by anyone with access to the file or storage account.
- **FR-008**: Existing CSV exports MUST remain available for human inspection and MUST NOT be replaced by the restore-capable backup package.
- **FR-009**: Backup, sync, and restore flows MUST work without app accounts, a hosted backend, provider-specific account integration, or background sync.
- **FR-010**: The app MUST check the linked backup on launch/resume and through explicit Sync now, while keeping the user in control before restoring or overwriting changed data.
- **FR-011**: The app MUST distinguish local-only changes, backup-only changes, both-changed conflicts, unavailable linked files, and clean/no-change states.
- **FR-012**: If only local data changed, the app MUST write the linked backup and update sync metadata.
- **FR-013**: If only the backup changed, the app MUST ask the user before restoring from the backup.
- **FR-014**: If both local data and backup changed, the app MUST show a conflict resolution sheet with timestamps, summary counts, and choices to keep local, restore backup after a safety copy, or cancel.
- **FR-015**: Restore MUST validate the selected backup before replacing local data and MUST avoid partial replacement on failure.
- **FR-016**: Before any destructive restore, the app MUST generate or offer a safety backup of the current local data.
- **FR-017**: Restore confirmation MUST warn when replacing an active local workout/session.
- **FR-018**: Profile MUST show linked location, last sync time, last conflict, and recoverable error status.
- **FR-019**: Platform file access failures MUST be recoverable and MUST NOT mutate local training data.
- **FR-020**: Android and iOS MUST both expose platform document-provider boundaries so shared Profile state can read and write user-selected documents.

### Key Entities *(include if feature involves data)*

- **Backup Package**: A portable restore-capable snapshot of user-owned training data plus format, schema, device, revision, and creation metadata.
- **Backup Link**: The locally persisted user-selected file reference, display name, provider state, and access status.
- **Sync State**: Last known local and backup revisions/timestamps, last outcome, last error, and conflict state.
- **Local Revision**: A monotonic summary of local user-data changes derived from persisted timestamps for V1.
- **Snapshot Summary**: Counts and latest timestamps for key backup areas used in confirmation and conflict UI.
- **Restore Plan**: A validated replacement plan describing the backup to import, safety backup status, warnings, and destructive operation confirmation.
- **Conflict Decision**: The user's selected outcome when local and backup copies both changed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can link a backup file, run Backup now, restart the app, and see the linked file status restored in Profile in automated state tests.
- **SC-002**: Backup serialization round-trips all persisted user-owned training areas in automated tests, including active session state and progress evidence.
- **SC-003**: Restore into an empty profile hydrates preferences, exercises, routines, active session state, completed workouts, sets, and PR/progress evidence in repository tests.
- **SC-004**: Simulated restore failure leaves the prior local data unchanged in automated tests.
- **SC-005**: Conflict tests cover local-only, backup-only, both-changed cancel, keep-local, and restore-backup outcomes.
- **SC-006**: Android document-provider smoke validation proves link, persisted access after restart, read, and write behavior for a user-selected document.
- **SC-007**: iOS document-provider adapter boundaries compile and report recoverable failures through shared Profile state.
- **SC-008**: Profile state tests cover link, Sync now, Backup now, Restore from file, conflict sheet, active-workout warning, and recoverable error messaging.

## Assumptions

- V1 prioritizes user autonomy and portability over invisible automation.
- Plain portable backup is the selected privacy mode for V1; encryption and passphrase management are out of scope.
- Light sync means launch/resume checks plus explicit Sync now, not background sync.
- Provider-specific hidden backup areas and provider SDKs remain out of scope for this feature.
- Multi-device field-level merging is out of scope; V1 uses whole-snapshot restore or whole-backup overwrite choices.
- Local revision metadata can initially be derived from existing created/updated timestamps, with mutation logs deferred until a future full merge feature.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Launch/resume checks must never block starting, resuming, or logging a workout; conflict and restore actions live in Profile.
- **Ledger Integrity**: Backup and restore cover the full workout ledger, set timestamps, PR source evidence, active session state, and safety-copy behavior before destructive restore.
- **Recovery Behavior**: Linked backup state, sync state, active workout/session snapshot, and recoverable provider errors must survive app restart and process death.
- **Progress Promise**: Progress rows, PR evidence, bodyweight, fractional weights, unit preferences, timed sets, and completed workout history remain inspectable after backup and restore.
- **Local-First Ownership**: The feature is offline-capable, user-visible, provider-agnostic, account-free, and keeps backups under explicit user control.
- **Platform Scope**: Shared code owns backup/sync/restore state and business rules; Android and iOS provide document-provider adapters.
- **Design System & Accessibility**: Profile additions use the shared Neo-Glass design system with accessible labels, touch targets, dynamic type, conflict warnings, and non-blocking status rows.
- **Release Evidence**: Requires serialization, restore, transaction/failure, conflict, platform-adapter, Profile state, Android smoke, iOS compile, export sanity, and permissions/backup review evidence.
