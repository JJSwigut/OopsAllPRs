# Data Model: Active Workout Resume

## Active Workout

Represents a resumable workout session.

- **Key fields**: workout id, started-at time, updated-at time, active-session recovery metadata.
- **Validation**: At most one active workout can exist. Discard requires a valid active workout id.
- **State transitions**:
  - Active -> discarded: active workout rows, active UX state, and session reference are cleared.
  - Active -> presented: active workout overlay opens without changing ledger state.

## Active Workout Resume Card

Represents the Train-screen recovery surface.

- **Key fields**: active workout id, display title, elapsed text, optional rest status.
- **Actions**:
  - Resume opens the active workout.
  - Discard removes the active workout and refreshes Train state.
- **Validation**: Only visible when a resumable active workout exists.

## Exercise Catalog Item

Represents a selectable exercise.

- **Key fields**: id, canonical name, display name, classification strings, logging mode, user-created flag, seed version.
- **Validation**: Canonical names are unique among visible catalog items.
- **Seed rule**: User-created items must not be overwritten by seed refresh.

## Seed Catalog

Represents the packaged CSV baseline.

- **Key fields**: exercise rows with canonical names and required classifications.
- **Refresh rule**: If any packaged seed canonical name is missing locally, seed ingestion runs and inserts or updates seed-owned rows while preserving user-created rows.
- **Out of scope**: Archiving seed-owned rows that were removed from the packaged CSV.
