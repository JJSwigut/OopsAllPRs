# Feature Specification: Durable Local Persistence

**Feature Branch**: `codex/009-local-persistence`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Implement durable SQLDelight-backed local persistence for the Oops All PRs foundation. Replace the in-memory Sql*Repository delegates with real SQLDelight repositories for active workouts, completed workouts, exercise catalog/seed imports, routines/templates, preferences, personal records, progress points, active workout UX sessions, and set drafts. Preserve offline-only behavior, canonical kg storage, session recovery, workout ledger integrity, PR source traceability, and Android-first validation while keeping iOS supported through shared repository boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recover Active Workout After Restart (Priority: P1)

As a lifter in the middle of a workout, I want my active workout, logged sets, focused logging state, set drafts, and rest/session state to recover after the app process is restarted so I can keep logging without rebuilding the workout.

**Why this priority**: Session recovery is a constitutional requirement and the biggest trust gap while repositories are still memory-backed.

**Independent Test**: Start a workout, add an exercise, log a set, save a set draft and active UX session, save session/rest state, recreate repository instances against the same local database, and verify the active workout and recovery state reload exactly once.

**Acceptance Scenarios**:

1. **Given** an active workout with one logged weighted set and one draft, **When** the app restarts with the same local database, **Then** the active workout, logged set, draft, focused exercise, and session route are restored.
2. **Given** an active workout with rest timer anchors, **When** recovery runs after process restart, **Then** elapsed/rest behavior is calculated from stored wall-clock instants rather than countdown deltas.
3. **Given** an active workout is discarded, **When** the app restarts, **Then** no active workout, session state, active UX state, or set drafts remain for that workout.

---

### User Story 2 - Preserve Completed Workout Ledger (Priority: P2)

As a lifter reviewing history, I want completed workouts, templates created from them, and PR source evidence to survive restarts so my workout ledger remains trustworthy.

**Why this priority**: Completed history and PR evidence are the durable audit trail behind Oops All PRs.

**Independent Test**: Complete a workout, derive PRs, save the completed workout as a template, recreate repositories against the same local database, and verify history rows, template launch data, progress rows, and PR evidence all point to the original workout/set ids.

**Acceptance Scenarios**:

1. **Given** a completed workout with weighted and bodyweight sets, **When** repositories are recreated, **Then** the completed workout reloads with exercise names, positions, reps, weights, and logged timestamps intact.
2. **Given** PRs were derived from a completed workout, **When** Progress is opened after restart, **Then** recent PRs, exercise records, trends, and source evidence still reference the original source workout and set ids.
3. **Given** a template is created from a completed workout, **When** the app restarts, **Then** the template remains launchable and produces a new active workout without mutating the completed source workout.

---

### User Story 3 - Persist Catalog, Preferences, And Exports (Priority: P3)

As a local-first user, I want exercise seed data, custom exercises, unit preference, and export snapshots to be stored locally so the app remains useful offline across launches.

**Why this priority**: Catalog and preference data make the app usable without repeated setup, and export behavior is part of user data ownership.

**Independent Test**: Seed exercises, create a user exercise, change weight unit, export data, recreate repositories against the same local database, and verify catalog, preference, and export output remain consistent.

**Acceptance Scenarios**:

1. **Given** baseline exercises are seeded and a user-created exercise exists, **When** seed ingestion runs again after restart, **Then** duplicate canonical seed rows are avoided and the user-created exercise is preserved.
2. **Given** the user selects pounds or kilograms, **When** the app restarts, **Then** the selected display unit reloads while stored set/PR weights remain canonical kilograms.
3. **Given** workouts, routines, exercises, and PRs exist, **When** an export is requested after restart, **Then** export rows reflect the durable local database and record an export snapshot.

### Edge Cases

- App process dies after an active workout is created but before any set is logged.
- App process dies after a set is confirmed; the set must not be lost or duplicated.
- App process dies after workout finish; completed workout data must remain and active session state must be cleared.
- A bodyweight set has reps and no weight; recovery/export/PR rows must not invent a weight.
- Fractional weighted sets are stored in canonical kilograms and display converted units without mutating storage.
- Seed ingestion runs repeatedly against the same database.
- A user-created exercise has the same canonical name as a future seed row.
- A completed workout source set is missing due to database corruption or an old migration; Progress must show missing evidence rather than fabricating it.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Android app MUST construct and use SQLDelight-backed repositories for normal runtime data instead of the in-memory store.
- **FR-002**: The system MUST persist and reload active workouts, active exercises, planned sets, logged sets, and workout status from the local database.
- **FR-003**: The system MUST persist and reload active session state, including active workout id, start instant, rest anchors, rest origin set id, last opened route, and updated instant.
- **FR-004**: The system MUST persist and reload active workout UX session state and set drafts.
- **FR-005**: The system MUST persist and reload completed workouts with exercise snapshots, set tuples, logged timestamps, and source active workout ids.
- **FR-006**: Finishing a workout MUST persist the completed workout and clear active session/UX/draft state without deleting data needed to reconstruct the completed ledger.
- **FR-007**: Discarding a workout MUST remove the active workout, its active exercise/set data, session state, active UX session, and set drafts without creating a completed workout.
- **FR-008**: The system MUST persist and reload reusable routines/templates, routine exercises, routine set templates, archived state, and source completed workout references.
- **FR-009**: The system MUST persist and reload exercise catalog seed rows, seed import reports, archived state, and user-created exercises.
- **FR-010**: Seed ingestion MUST avoid duplicate canonical exercise names and MUST preserve user-created exercises over seed rows.
- **FR-011**: The system MUST persist and reload user weight unit preference, default rest seconds, and backup-allowed preference fields.
- **FR-012**: All persisted weights MUST remain canonical kilograms; display unit conversion MUST happen at repository/use-case/UI boundaries without mutating stored values.
- **FR-013**: The system MUST persist and reload personal records and progress points with source workout/set ids for evidence traceability.
- **FR-014**: Rebuilding PRs MUST replace durable PR/progress rows atomically enough that callers do not observe a partially rebuilt set after the operation completes.
- **FR-015**: The system MUST export workouts, routines, exercises, and personal records from durable local database state and record export snapshots.
- **FR-016**: Repository mutations MUST continue returning explicit success/failure results for validation, conflict, and not-found cases.
- **FR-017**: The shared repository implementation MUST remain usable from Android and iOS through platform database drivers; Android is the first runtime target.
- **FR-018**: Existing in-memory test fixtures MAY remain available for fast domain tests, but production app construction MUST not use them for normal persistence.

### Key Entities *(include if feature involves data)*

- **Local Database**: User-owned offline data store containing workouts, routines, catalog, preferences, PRs, progress points, exports, sessions, and active UX state.
- **Active Workout Ledger**: Active workout, exercise, and set rows that represent in-progress work and recovery state.
- **Completed Workout Ledger**: Completed workout rows and durable source exercise/set data used by History, Templates, Export, and Progress evidence.
- **Exercise Catalog Store**: Seeded and user-created exercise rows keyed by canonical names.
- **Preference Store**: Singleton user settings for units, rest defaults, and backup allowance.
- **Progress Store**: Personal record and progress point rows with source ids.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A logged active workout with at least one set, one draft, and session/rest state can be fully restored after repository recreation in automated tests.
- **SC-002**: A completed workout, derived PRs, and a template created from that workout can be reloaded after repository recreation with original source workout/set ids intact.
- **SC-003**: Re-running seed ingestion after restart does not increase seeded exercise count for duplicate canonical seed rows and does not overwrite a user-created exercise.
- **SC-004**: Unit preference survives repository recreation, while persisted weighted set and PR values remain canonical kilograms.
- **SC-005**: Android unit tests, Android debug build, iOS simulator compile, Material scan, and whitespace validation all pass for the persistence slice.
- **SC-006**: Manual Android device/emulator discovery is attempted; if no target is available, the milestone manual persistence gate is explicitly deferred.

## Assumptions

- SQLDelight remains the durable local persistence layer because schema, drivers, and dependencies already exist.
- The first implementation can keep fast in-memory fixtures for domain-level unit tests while Android runtime uses the durable database path.
- Existing schema can be extended with migrations where current tables cannot reconstruct completed workout ledger data safely.
- Cloud sync remains out of scope, but stable ids, timestamps, explicit mutation paths, and source references must stay sync-ready.
- No new user-facing UI is required beyond the app using durable data underneath existing screens.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Logging interactions stay unchanged; persistence happens beneath existing one-tap/stepper workflows.
- **Ledger Integrity**: Confirmed sets, completed workouts, templates, PRs, progress points, exports, and source ids become durable and auditable across process death.
- **Recovery Behavior**: Active session, route, rest anchors, active workout, drafts, and focused logging state have explicit cold-start behavior.
- **Progress Promise**: PR records, trends, source evidence, bodyweight reps-only behavior, fractional weights, and canonical unit storage are preserved.
- **Local-First Ownership**: Feature is offline-only local storage and strengthens future export/backup/sync boundaries.
- **Platform Scope**: Android runtime is implemented and validated first; shared database/repository code remains usable by iOS through the native SQLDelight driver.
- **Design System & Accessibility**: No new UI surfaces are planned; existing design-system-only screens continue to consume durable state.
- **Release Evidence**: Requires SQL repository tests, recovery tests, ledger tests, export/preference tests, Android build, iOS compile, Material scan, whitespace check, and manual device discovery.
