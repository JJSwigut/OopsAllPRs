# Research: Routine & Exercise Management

## Decision: Keep Routine Management Under Train

**Rationale**: Train already owns starting empty workouts, launching templates, and deleting templates. Adding create/edit actions there keeps routine management close to its launch context without adding a new top-level destination.

**Alternatives considered**:

- New Routine tab: rejected for scope and because the top-level shell is intentionally compact.
- Profile-only routine management: rejected because routines are part of the training workflow, not app settings.

## Decision: Use Shared Draft State Instead Of Persisted Draft Autosave

**Rationale**: Routine draft autosave would introduce recovery and conflict semantics that are not needed for the first management slice. Canceling a draft should drop unsaved changes. Saved routines remain durable through explicit save.

**Alternatives considered**:

- SQL-backed routine draft table: deferred until users need long-running planning drafts.
- Mutating routine rows as the user edits: rejected because it makes cancel unsafe.

## Decision: Protect Seeded Exercises, Edit User-Created Only

**Rationale**: Seeded exercises are baseline data and may be refreshed or migrated later. Editing them now would require source/version semantics and could make future seed upgrades ambiguous. User-created exercises can be safely renamed or archived with explicit timestamps.

**Alternatives considered**:

- Edit all exercises: rejected for migration risk.
- Duplicate seeded exercise before editing: deferred; users can create a custom variant manually.

## Decision: Preserve Historical Snapshots

**Rationale**: Completed workouts and active workout exercise references already snapshot display names and bodyweight classification. Exercise management should affect future picker/routine usage only, not rewrite logged history.

**Alternatives considered**:

- Backfill historical names after catalog edit: rejected because it violates ledger integrity and PR evidence inspectability.

## Decision: Add Repository Operations Rather Than A Separate Catalog Store

**Rationale**: Existing `ExerciseRepository` already owns catalog query and custom creation. Extending it with update/archive operations keeps local-first boundaries simple and shared.

**Alternatives considered**:

- New `ExerciseManagementRepository`: rejected as duplicate ownership for the same table.
