# Research: Mistake Recovery and Editing

## Decision: Edit active logged sets in place

**Rationale**: The current product has no audit-history UI, and users need fast correction rather than revision browsing. Preserving the set id and original `loggedAt` keeps source evidence stable while `updatedAt` records the correction time.

**Alternatives considered**:

- Add a separate set revision table. Rejected for this slice because it adds schema and UI complexity before sync/audit requirements exist.
- Delete and recreate corrected sets. Rejected because it changes source evidence ids and makes PR/export references harder to reason about.

## Decision: Delete and undo active logged sets by removing set rows before completion

**Rationale**: Active workouts are still in-progress local drafts. If the user removes a set before finishing, the completed ledger should simply exclude it. Undo last set can resolve to the most recent `loggedAt` row and use the same deletion path.

**Alternatives considered**:

- Mark active sets as deleted. Rejected because there is no sync/tombstone consumer yet and hidden rows would complicate active count/draft logic.
- Convert deleted logged sets back to unlogged drafts. Rejected because undo should remove the mistake and leave the normal next draft available.

## Decision: Require confirmation for discard, template delete, and completed workout delete

**Rationale**: These actions can remove many rows or durable history. Confirmation is the lowest-complexity way to preserve trust while keeping destructive actions available.

**Alternatives considered**:

- Immediate destructive action with snackbar undo. Rejected for completed workout/template deletion because the current design system has no durable undo queue, and PR/export rebuild would need rollback support.
- Platform dialogs. Rejected because the shared Compose UI already owns app flows and should remain iOS-compatible.

## Decision: Hard-delete completed workouts locally and rebuild derived PR/progress data

**Rationale**: The app is offline-only and has no sync conflict contract. Hard deletion matches user expectations for removing a mistaken workout and ensures History, Progress, and exports are consistent.

**Alternatives considered**:

- Soft delete/tombstone. Rejected until future sync is introduced; it would need export, restore, and conflict semantics.
- Hide only in UI. Rejected because exports and PR derivation would still contain the mistaken data.

## Decision: Keep saved templates independent from deleted completed workouts

**Rationale**: Templates already snapshot exercises and planned sets. Deleting source history should not silently remove a reusable template the user may still want.

**Alternatives considered**:

- Cascade delete templates from deleted completed workouts. Rejected because it can surprise users and destroys independent reusable data.

## Decision: Use existing PR derivation rebuild path after source data changes

**Rationale**: The current app already rebuilds PRs from completed workouts after finish. Reusing that rule after completed deletion keeps source-of-truth logic centralized.

**Alternatives considered**:

- Incrementally remove affected PRs. Rejected as more error-prone because deleting a workout can reveal older records from remaining history.
