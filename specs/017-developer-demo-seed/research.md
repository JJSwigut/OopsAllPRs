# Research: Developer Demo Data Seeding

## Decision: Use Normal Use Cases Instead of Direct Database Writes

**Rationale**: Demo data is most useful when it exercises the same ledger, active session, routine, rest, and PR derivation paths as real user actions. Direct SQL inserts would be faster but could create test data that cannot happen in the product.

**Alternatives considered**:
- Direct SQL fixture insert: rejected because it bypasses set validation, session state, and PR rebuild behavior.
- Static database file import: rejected because it is fragile across schema changes and harder to keep local user data safe.

## Decision: Gate Developer Tools Through AppState Construction

**Rationale**: Shared UI can stay platform-neutral by accepting nullable developer seed state. Android can pass a debug flag at app creation time, while release and iOS default to no developer tooling.

**Alternatives considered**:
- Android-only debug activity: rejected for this slice because it would not validate shared Profile behavior or future iOS reuse.
- Always-present hidden gesture: rejected because hidden production entry points are harder to reason about for release safety.

## Decision: Idempotency Through Reserved Demo Names and Deterministic Timestamps

**Rationale**: Existing domain models do not have arbitrary metadata tags. Reserved `Demo:` routine names and deterministic completed-workout timestamps allow the seed use case to detect prior demo data without adding schema fields or weakening routine use-case validation.

**Alternatives considered**:
- Add database marker table: rejected because this feature should not require a migration.
- Delete and recreate all local data before seeding: rejected because it risks user-created test data and hides preservation problems.

## Decision: Start With Three Scenarios

**Rationale**: Progress demo, routine demo, and active recovery demo cover the features that are hardest to validate manually from an empty install while avoiding a broad sample-data framework.

**Alternatives considered**:
- One giant "seed everything" action only: rejected because active recovery seeding must avoid overwriting existing workouts and developers need targeted scenarios.
- Comprehensive fake account dataset: rejected as feature bloat before cloud sync or import/export tooling exists.

## Decision: Release Guard Is Visibility-Based for This Slice

**Rationale**: The code can live in shared common source for reuse and testability, but release builds must not construct or expose developer seed state. This satisfies the product risk while preserving KMP architecture.

**Alternatives considered**:
- Separate debug-only shared source set: rejected because it adds Gradle source-set complexity and does not improve user-visible release safety for this slice.
