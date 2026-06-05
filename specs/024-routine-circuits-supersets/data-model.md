# Data Model: Routine Circuits & Supersets

## Routine Exercise Group

Planning metadata represented by shared fields on routine exercises.

**Fields**:

- `groupId`: Stable identifier shared by exercises in the same routine group; absent means ungrouped.
- `groupPosition`: Ordered position of the group block inside the routine; absent means ungrouped.

**Validation rules**:

- A valid group contains at least two routine exercises.
- Grouped exercises must be adjacent in routine exercise order.
- Group label is derived from group size: size 2 -> superset, size >= 3 -> circuit.
- Removing exercises must collapse one-exercise groups back to ungrouped.

## Grouped Routine Exercise

Existing routine exercise plus optional group metadata.

**Fields**:

- Existing exercise catalog id, display snapshot, routine position, planned sets, and rest configuration.
- Optional group metadata from Routine Exercise Group.

**Relationships**:

- Belongs to one reusable routine.
- Optionally belongs to one routine exercise group inside that routine.
- Retains independent planned sets and rest configuration.

## Active Workout Group Context

Display-only context copied when launching a routine.

**Fields**:

- `groupId`: Optional group identifier.
- `groupLabel`: Optional display label derived from launched routine group size.
- `groupPosition`: Optional group block order.

**Validation rules**:

- Active workout group context must not affect set identity, logging, PR derivation, rest origin, or finish behavior.
- Existing active workout recovery must tolerate absent group context.

## State Transitions

```text
Ungrouped routine exercises
  -> group adjacent exercises
Grouped routine exercises
  -> remove group
Ungrouped routine exercises

Grouped routine exercises
  -> save routine
Persisted grouped routine
  -> launch routine
Active workout with display group context
  -> finish workout
Completed workout ledger without grouping mutation
```

## Persistence Impact

- Add nullable group metadata to routine exercise persistence.
- Existing rows with null metadata are treated as ungrouped.
- Completed workout, set ledger, progress, and export persistence remain unchanged.
