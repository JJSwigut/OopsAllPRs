# Research: Routine Circuits & Supersets

## Decision: Model Groups As Routine Metadata

**Decision**: Store grouping on routine exercises via optional group id and group position, with labels derived from group size: two exercises are a superset, three or more are a circuit.

**Rationale**: This keeps routine grouping close to the feature that owns it, avoids introducing a new workout lifecycle, and preserves completed workout ledger semantics. It also gives future sync a stable metadata shape without forcing completed-history migration now.

**Alternatives considered**:

- Separate group entity table: More normalized, but unnecessary for the first slice and adds joins/migration surface.
- New exercise type for supersets/circuits: Incorrect domain model because grouped exercises still retain independent sets, rest, PRs, and history.
- Persist group metadata on completed workouts: Higher ledger/export blast radius and not required for the first user value.

## Decision: Restrict First Release To Adjacent Exercises

**Decision**: A group can contain only adjacent routine exercises in routine order.

**Rationale**: Supersets and circuits are normally followed as contiguous plan blocks. This keeps UI, validation, launch mapping, and accessibility straightforward.

**Alternatives considered**:

- Allow non-adjacent grouping: More flexible but confusing in an ordered workout view and harder to validate.
- Require drag-and-drop grouping: Higher UI cost; state-holder operations are enough for the first release.

## Decision: Preserve Active Workout Logging Semantics

**Decision**: Launching a grouped routine carries group display context into active workout exercises, but logging, rest timer origin, PR feedback, previous values, and finish behavior stay unchanged.

**Rationale**: The constitution prioritizes fast logging and ledger integrity. The fastest low-risk value is helping the user see which exercises belong together while keeping proven logging behavior intact.

**Alternatives considered**:

- Auto-advance through group rounds: Useful later but changes active workout behavior and needs separate acceptance criteria.
- Group-specific rest: Useful later but conflicts with current per-exercise rest behavior and would expand the feature scope.

## Decision: Migration Adds Nullable Metadata

**Decision**: Add nullable grouping columns to routine exercises and map missing values as ungrouped.

**Rationale**: Existing routines remain valid, migration is reversible in behavior, and no backfill is needed.

**Alternatives considered**:

- Create a routine group table: Better if groups need names or settings soon, but not justified for derived superset/circuit labels.
- Encode groups in JSON: Avoids schema shape now but weakens typed persistence and migration tests.
