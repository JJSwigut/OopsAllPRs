# Tasks: Mistake Recovery and Editing

**Input**: Feature artifacts in `specs/012-mistake-recovery-editing/`
**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/mistake-recovery-contracts.md`

## Phase 1: Domain and Persistence

- [X] T001 Add repository contracts for active logged-set deletion, completed-workout deletion, and template deletion in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt`
- [X] T002 Implement mistake-recovery repository operations in `InMemoryFoundationStore`
- [X] T003 Add SQLDelight queries for logged-set deletion, completed-workout deletion, completed source cleanup, and routine archival
- [X] T004 Implement SQL-backed mistake-recovery repository operations in `SqlFoundationStore`

## Phase 2: Use Cases

- [X] T005 Add logged-set edit-by-id, delete, and undo-last behavior to `SetLoggingUseCases`
- [X] T006 Add template deletion and completed-workout deletion with PR rebuild to `RoutineUseCases`

## Phase 3: State and UI

- [X] T007 Add active-workout edit draft, undo, delete-set, and discard-confirmation state to `ActiveWorkoutStateHolder`
- [X] T008 Update active workout shared Compose UI with logged-row Edit/Delete, bottom edit mode, undo, and discard confirmation
- [X] T009 Add template deletion confirmation state and Train UI actions
- [X] T010 Add completed-workout deletion confirmation state and History UI actions
- [X] T011 Wire AppShell callbacks so edits, deletes, undo, discard, and PR refreshes flow through shared boundaries

## Phase 4: Validation

- [X] T012 Add common use-case tests for logged-set edit/delete/undo and routine/completed-workout deletion
- [X] T013 Add common state-holder tests for active workout, Train template deletion, and History deletion
- [X] T014 Add SQL-backed Android unit tests for the new persistence queries
- [X] T015 Run validation gates and record results in `validation/mistake-recovery-results.md`
