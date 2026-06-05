# Routine Grouping Contracts

## Routine Editor State Contract

### Group Adjacent Exercises

**Given** a routine draft with at least two adjacent exercises
**When** the state holder groups a contiguous range
**Then** each exercise in the range receives the same group id and group position
**And** exercises outside the range remain ungrouped or retain their existing independent group
**And** group labels are derived from group size.

### Remove Group

**Given** a routine draft contains a grouped exercise
**When** the state holder removes that group
**Then** all exercises with the same group id become ungrouped
**And** planned sets, set targets, rest settings, and exercise order remain unchanged.

### Remove Exercise From Group

**Given** a routine draft contains a grouped pair
**When** one exercise is removed
**Then** the remaining exercise becomes ungrouped
**And** no one-exercise group remains.

## Persistence Contract

**Given** a grouped routine is saved
**When** routines are reloaded from local persistence
**Then** group ids, group positions, exercise order, planned sets, and rest settings match the saved routine.

**Given** an existing routine has no group metadata
**When** it is loaded after migration
**Then** it is treated as ungrouped and remains launchable.

## Launch Contract

**Given** a routine has a superset or circuit
**When** the routine is launched
**Then** active workout exercises include display group context
**And** active set logging APIs behave the same as ungrouped routine launches.

## Non-Regression Contract

**Given** grouped routine support is enabled
**When** completed history, PR derivation, previous workout defaults, and export snapshots are generated
**Then** their existing data contracts remain unchanged unless they intentionally ignore routine grouping.
