# Research: Catalog Exercise Selection

## Decision: Reuse the existing local exercise repository

**Rationale**: The foundation already has `ExerciseRepository.search`, `all`,
and `saveUserExercise`, plus seed ingestion and user-created exercise
preservation. Reusing that boundary keeps the feature local-first and
sync-ready without schema or architecture churn.

**Alternatives considered**:
- Add a picker-only in-memory list: rejected because it would bypass seed
  ingestion and user-created exercise persistence.
- Add remote exercise lookup: rejected because cloud/network features are out
  of scope and would violate local-only behavior.

## Decision: Add a small domain use case for catalog search and creation

**Rationale**: The picker should not know how to canonicalize names or create
domain catalog items. A dedicated use case keeps validation, timestamps, stable
ids, and repository calls testable in shared code.

**Alternatives considered**:
- Put creation directly in the state holder: rejected because it couples UI
  state with domain object construction.
- Extend `SetLoggingUseCases`: rejected because exercise catalog management is
  distinct from set ledger operations.

## Decision: Preserve active workout state until append succeeds

**Rationale**: The picker is nested inside an active workout. Failed saves or
cancel actions must not mutate blocks, drafts, or logged rows. The existing
`ActiveWorkoutStateHolder.addExercise` already returns explicit
success/failure, so the picker can close only on success.

**Alternatives considered**:
- Optimistically append before persistence: rejected because it would violate
  workout ledger integrity and failure semantics.

## Decision: Treat custom exercise creation as minimal metadata

**Rationale**: A missing exercise should be quick to add during a workout.
Capturing only display name and weighted/bodyweight classification is enough to
log correctly now and avoids forcing richer catalog editing into the gym flow.

**Alternatives considered**:
- Full exercise metadata form: rejected for this slice because it slows active
  logging and is not required for set validation.

## Decision: Keep picker UI in shared Compose

**Rationale**: Android is validated first, but iOS is a first-class target.
Shared picker state and UI protect the product from an Android-only path and
match the existing navigation/active workout architecture.

**Alternatives considered**:
- Android-only picker: rejected as a shared-first KMP violation.
