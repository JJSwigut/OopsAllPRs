# Foundation Contracts: Bootstrap Oops All PRs App Foundation

These contracts describe observable behavior that implementation tasks must
preserve. They are not public network APIs. They define the domain, persistence,
platform, seed, export, and validation boundaries for the mobile app foundation.

## Workout Ledger Contract

### Start Empty Workout

**Input**
- User requests a fresh workout.

**Required behavior**
- Create a local active workout with stable ID and `startedAt`.
- Set active session pointer to the new workout.
- No network or account dependency.

**Failure behavior**
- Return an explicit failure result and do not set active session pointer if the
  workout cannot be persisted.

### Start Workout From Routine

**Input**
- Routine ID.

**Required behavior**
- Load the routine.
- Create a separate active workout with cloned exercise/set plan.
- Planned sets do not have `loggedAt`.
- Original routine remains unchanged.

**Failure behavior**
- If routine does not exist or clone cannot persist, return explicit failure and
  leave active session state unchanged.

### Confirm Set

**Input**
- Active workout ID.
- Exercise instance ID.
- Set ID.
- Reps.
- Optional added load in canonical kilograms.
- Set kind: weighted or bodyweight.
- Logged timestamp.

**Required behavior**
- Persist the set before presenting logged state.
- Weighted sets require non-negative load and positive reps.
- Bodyweight sets require positive reps and optional added load.
- Logged set stores stable ID, order, reps, load/bodyweight representation, and
  `loggedAt`.
- Confirmation may start/update rest timer state after set persistence.

**Failure behavior**
- Failed persistence must not update UI state to logged.
- Failure must be inspectable by caller.

### Edit Logged Set

**Input**
- Logged set ID.
- New reps and optional load.

**Required behavior**
- Reject edits to unlogged planned sets through this path.
- Preserve original `loggedAt`.
- Update edited values and edit metadata.
- Rebuild or invalidate derived PR/last-set data for the affected exercise.

### Finish Workout

**Input**
- Active workout ID.
- Finished timestamp.

**Required behavior**
- Persist completed workout with only logged sets.
- Preserve each logged set tuple: ID, reps, load/bodyweight representation,
  order, and `loggedAt`.
- Calculate duration from wall-clock start/finish.
- Clear active session pointer and rest timer state.
- Make completed workout available as history and as source for routines.

### Discard Workout

**Input**
- Active workout ID.

**Required behavior**
- Remove or archive active workout according to implementation choice.
- Clear active session pointer and rest timer state.
- Do not alter completed history or reusable routines.

## Session Recovery Contract

### Persisted State

The active session store must expose:
- Active workout ID or null.
- Workout `startedAt` wall-clock instant.
- Rest `endsAt` wall-clock instant or null.
- Optional route/screen recovery state safe to restore.

### Hydration

On app start:
- If no active workout ID exists, show no active session.
- If active workout ID exists and workout exists, hydrate active workout from
  local persistence.
- Elapsed time is `now - startedAt`.
- Rest remaining time is `restEndsAt - now` clamped to zero.
- Expired rest timers are cleared and may show completion notification behavior
  according to platform policy.

### Timer Notification Adapters

Platform adapters must support:
- Schedule rest-complete notification for `restEndsAt`.
- Cancel notification on skip, finish, or discard.
- No notification scheduling in shared code without adapter boundary.

## Exercise Seed Contract

### Seed Source

Baseline seed source is the current OopsAllPRs `exercises.csv` with header:

```text
Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region
```

### Ingestion

Seed ingestion must:
- Validate header and required fields.
- Parse quoted CSV fields safely.
- Normalize canonical exercise names for duplicate detection.
- Classify bodyweight exercises so reps-only logging is available.
- Import valid seed-owned rows idempotently.
- Report malformed rows and duplicate conflicts.
- Preserve user-created exercises and user notes.

### Failure behavior

- A malformed row must not abort the whole import unless the header/schema is
  invalid.
- Seed import failure must be explicit and must not leave partial corruption.

## Unit and Bodyweight Contract

### Canonical Weight

- Canonical storage unit is kilograms.
- User display/input units may be pounds or kilograms.
- Unit conversion occurs at input, display, and export boundaries.
- Changing user unit must not mutate stored workout evidence.

### Locale Decimal Input

- Numeric input must accept `.` and `,` decimal separators where relevant.
- Invalid numeric input fails validation without changing persisted values.

### Bodyweight Logging

- Bodyweight sets are valid with reps alone.
- Added load is optional and stored as canonical kilograms when present.
- Bodyweight reps-only sets are included in history, exports, PR derivation, and
  progress data.

## PR and Progress Contract

### Source of Truth

- Completed logged sets are the authoritative source for PR/progress evidence.
- Materialized PR or progress tables must be rebuildable from completed workout
  evidence.
- Every PR that represents a logged set must link to source workout and set.

### PR Types

Required foundation support:
- Weighted best for reps.
- Bodyweight reps best.
- Estimated one-rep-max data where applicable.
- Volume/progress points from logged evidence.

### Recalculation

PR/progress data must be recalculated or invalidated when:
- A logged set is confirmed.
- A logged set is edited.
- A completed workout is deleted or restored.
- Exercise catalog identity or canonical naming changes.

## Export Contract

### Export Data

Foundation must expose export-ready data for:
- Completed workouts.
- Logged sets.
- Exercises.
- Reusable routines.
- Personal records and progress source references.

### CSV Rules

- CSV values containing commas, quotes, or newlines must be quoted and escaped.
- Export uses the user's selected display unit while retaining canonical source
  data locally.
- Bodyweight reps-only sets must appear in workout exports.
- Export generation must work offline.

## Platform Boundary Contract

Shared code owns:
- Domain models.
- Validation.
- Unit conversion.
- Seed parsing.
- Repository interfaces.
- Use cases.
- Active session state machine.
- PR/progress derivation.

Platform code owns:
- SQLDelight driver creation.
- Settings storage adapter.
- Rest notification scheduling.
- File sharing/export handoff.
- Haptics.
- Permissions.
- App bootstrap.

Shared code must not import Android-only or iOS-only APIs outside expect/actual
or equivalent platform interfaces.

## Validation Contract

Minimum automated validation:
- Start/resume active workout.
- Confirm weighted set and bodyweight reps-only set.
- Edit logged set without losing `loggedAt`.
- Finish preserves logged tuples and strips unlogged planned sets.
- Active workout and rest timer recover after simulated process death.
- Seed import validates baseline CSV and preserves user-created exercises.
- Unit conversion round-trips canonical kilograms.
- PR derivation creates weighted and bodyweight PRs with source references.
- Export includes bodyweight and weighted logged sets.

Minimum milestone/release manual validation:
- Fresh install, start empty workout, add exercise, log first set in <=30s.
- Restart app during active workout and verify recovery.
- Finish workout and verify history evidence.
- Review backup/permissions/export behavior.

