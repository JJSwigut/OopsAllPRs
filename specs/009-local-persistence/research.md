# Research: Durable Local Persistence

## Decision: Use SQLDelight As The Runtime Persistence Layer

**Rationale**: The project already has SQLDelight dependencies, platform
drivers, generated schema, and a `WorkoutDatabase` package. Implementing the
existing repository interfaces against SQLDelight closes the persistence gap
without changing domain/use-case contracts or adding another storage library.

**Alternatives considered**:

- Keep in-memory repositories until later: rejected because local-first and
  session recovery cannot be manually trusted without durable runtime storage.
- Use platform-specific storage directly: rejected because it violates the
  shared-first KMP architecture and would duplicate Android/iOS behavior.
- Add a new ORM or serialization store: rejected because SQLDelight is already
  selected by the foundation and constitution technical direction.

## Decision: Add A Shared `SqlFoundationStore`

**Rationale**: One shared SQL store can implement `WorkoutRepository`,
`SessionRepository`, `ActiveWorkoutUxRepository`, `SetLedgerRepository`,
`RoutineRepository`, `ExerciseRepository`, `PreferencesRepository`,
`ProgressRepository`, and `ExportRepository` while keeping transactional
behavior and mapping helpers in one place. Thin `Sql*Repository` adapters can
delegate to it so existing use-case construction remains clear.

**Alternatives considered**:

- Put full SQL logic in each thin repository: rejected because cross-entity
  operations such as finish/discard/export need shared transaction and mapping
  helpers.
- Replace repository interfaces: rejected because use cases and UI state
  holders already depend on the stable interfaces.

## Decision: Preserve Source Active Rows For Completed Ledger Reconstruction

**Rationale**: The current schema stores active workouts, active exercises, and
exercise sets separately from completed workout summary rows. On finish, marking
the source active workout as `COMPLETED` and keeping its exercise/set rows gives
History, Progress, Templates, and Export enough data to reconstruct the
completed ledger without fabricating set ids or display snapshots.

**Alternatives considered**:

- Delete active rows on finish and store only completed summary rows: rejected
  because completed exercise/set detail would be lost with the current schema.
- Add duplicate completed exercise/set tables immediately: possible later, but
  not required if source active rows are preserved and completed reconstruction
  is covered by tests.

## Decision: Android Unit Tests Use In-Memory SQLite

**Rationale**: SQLDelight's SQLite JDBC driver is already available to
`androidUnitTest`. Tests can create a fresh in-memory database, write data,
recreate repository wrappers against the same driver/database, and assert
restart-like recovery without depending on an emulator.

**Alternatives considered**:

- Only common tests with in-memory fixtures: rejected because they do not prove
  SQL persistence.
- Instrumented device tests: useful later, but too slow and not required for
  this foundation slice.

## Decision: Keep In-Memory Store For Domain Fixtures

**Rationale**: Existing common tests exercise use-case behavior quickly and do
not all need SQL. Keeping the in-memory fixture reduces test churn while the
production runtime path moves to SQLDelight.

**Alternatives considered**:

- Rewrite all tests to SQL immediately: rejected because it increases blast
  radius without improving confidence for pure domain validation.
