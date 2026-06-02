# Contracts: Durable Local Persistence

## Runtime Construction Contract

- Android runtime MUST create `AppState` with a `PlatformDatabaseDriverFactory` backed by the app context.
- Shared app bootstrap MUST create a SQLDelight `WorkoutDatabase` from the driver and wire SQL-backed repository adapters.
- In-memory construction MAY remain available for tests, but normal app runtime MUST not use it.
- iOS compile MUST continue to use the existing native SQLDelight driver boundary.

## Repository Contract

All SQL-backed repository adapters MUST preserve the existing repository
interfaces and return the same domain types as the in-memory implementation.

- `WorkoutRepository`: create/save/load/current/discard/finish active and completed workouts.
- `SessionRepository`: load/save/clear active session singleton.
- `ActiveWorkoutUxRepository`: load/save/clear UX session and set drafts.
- `SetLedgerRepository`: confirm and edit logged sets.
- `RoutineRepository`: load/list/save reusable routines.
- `ExerciseRepository`: search/list/save seed and user-created exercises.
- `PreferencesRepository`: load/save weight unit preference.
- `ProgressRepository`: replace/load personal records and progress points.
- `ExportRepository`: export durable local data and record export snapshots.

## Ledger Integrity Contract

- Confirmed sets are written before `confirmSet` returns success.
- A confirmed set must not be duplicated after repository recreation.
- Finish writes a completed workout row, marks the source workout completed,
  clears session/UX/drafts, and preserves source exercise/set rows.
- Discard removes active workout recovery data and does not create a completed
  workout row.
- Completed workout reads must omit unlogged planned sets.
- PR source evidence must use stored `sourceWorkoutId` and `sourceSetId`; if a
  source row cannot be found, callers must receive missing evidence behavior
  from the Progress layer rather than fabricated details.

## Unit Contract

- `WeightKg.value` is the only persisted weight quantity.
- Unit preference controls display/export formatting only.
- Bodyweight reps-only sets may persist null weight.

## Export Contract

- Export rows come from SQL-backed repository reads.
- Export success records an export snapshot with type, unit, row count, format
  version, and timestamp.
