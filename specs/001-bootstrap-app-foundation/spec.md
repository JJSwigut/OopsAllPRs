# Feature Specification: Bootstrap Oops All PRs App Foundation

**Feature Branch**: `001-bootstrap-app-foundation`

**Created**: 2026-05-29

**Status**: Draft

**Input**: User description: "Bootstrap the fresh Oops All PRs app foundation as a Kotlin Multiplatform, local-first, Android-first and iOS-ready workout logger. Include shared domain models for templates, active workouts, completed workouts, exercises, sets, PR history, bodyweight reps-only logging, canonical weight units, SQLDelight local persistence, exercise seed ingestion from the existing app seed list, and validation tests for workout ledger integrity and session recovery."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Start and Resume a Local Workout (Priority: P1)

A lifter opens the app at the gym and can immediately start an empty workout,
start from a reusable routine, or resume an active workout without network
access. The app keeps the active workout identity and timing state so the user
can leave, lock the phone, or restart the app and continue from the same place.

**Why this priority**: This is the minimum viable foundation for fast-loop
logging. No later workout, PR, history, or export feature matters if a user
cannot start and reliably resume a local workout.

**Independent Test**: Create a fresh install state, start a workout, add at
least one exercise, simulate app restart, and verify the active workout,
elapsed time, and available next action are restored.

**Acceptance Scenarios**:

1. **Given** a fresh local app state with seeded exercises, **When** the user starts an empty workout, **Then** an active workout exists locally with no network dependency and is ready for exercise entry.
2. **Given** an active workout exists, **When** the app is restarted, **Then** the same active workout is restored with its elapsed time based on wall-clock time.
3. **Given** a reusable routine exists, **When** the user starts from that routine, **Then** the app creates a separate active workout while leaving the reusable routine unchanged.

---

### User Story 2 - Log Durable Sets Quickly (Priority: P1)

A lifter records sets with minimal friction using controls suited to gym-side
use. Weighted sets support fractional loads and unit preferences. Bodyweight
movements can be logged with reps alone. A confirmed set becomes part of the
workout ledger only after it is durably recorded.

**Why this priority**: Set logging is the core loop of the app. The foundation
must establish ledger rules before any UI polish or analytics builds on top.

**Independent Test**: Start an active workout, add a weighted exercise and a
bodyweight exercise, confirm sets for both, restart the app, and verify each
logged set keeps its values and timestamp.

**Acceptance Scenarios**:

1. **Given** a weighted exercise in an active workout, **When** the user confirms a set with fractional weight and reps, **Then** the set is stored with its canonical value, displayable in the user's preferred unit, and marked logged with a timestamp.
2. **Given** a bodyweight exercise in an active workout, **When** the user confirms a reps-only set, **Then** the set is valid, logged, exportable, and eligible for PR comparison.
3. **Given** a logged set, **When** the user edits the set values, **Then** the app uses an explicit edit path and preserves the original logged status.

---

### User Story 3 - Build Reusable Routines from Real Workouts (Priority: P2)

A lifter can create a reusable routine from a completed workout, launch that
routine later, or skip routines entirely and add exercises during a fresh
workout. Routine editing and active workout logging remain distinct so logging
actions never accidentally rewrite reusable plans.

**Why this priority**: Routines make repeated workouts fast, but they must not
compromise the active logging ledger or force a planning-heavy flow.

**Independent Test**: Finish a workout, create a routine from it, start from
that routine, modify the active workout, and verify the original routine still
matches the saved plan.

**Acceptance Scenarios**:

1. **Given** a completed workout, **When** the user saves it as a reusable routine, **Then** the routine contains planned exercises and sets without logged timestamps.
2. **Given** a reusable routine, **When** the user starts a workout from it, **Then** the app creates a new active workout instance separate from the routine.
3. **Given** an active workout created without a routine, **When** the user adds exercises on the fly, **Then** those additions affect only that active workout unless the user explicitly saves a routine later.

---

### User Story 4 - Seed Exercise Choices and Preserve User Ownership (Priority: P2)

A lifter has a comprehensive default exercise list available on first launch
and can rely on those choices across routines, active workouts, history, and
progress. Seed ingestion validates the current app's exercise seed list and
does not overwrite user-created exercises.

**Why this priority**: Fast logging depends on a useful exercise picker. The
foundation must support seed data without blocking future improvements or user
customization.

**Independent Test**: Load the baseline seed list, verify required exercise
classification fields are present, create a user exercise, re-run seed ingestion,
and verify the user exercise remains intact.

**Acceptance Scenarios**:

1. **Given** a fresh install, **When** seed ingestion runs, **Then** the baseline exercise list is available for search and selection.
2. **Given** a malformed seed row, **When** seed ingestion validates the data, **Then** the row is rejected or reported without corrupting the valid seed list.
3. **Given** a user-created exercise with the same name as a seed candidate, **When** seed ingestion runs, **Then** the app preserves user ownership and prevents unintended replacement.

---

### User Story 5 - Establish PR and Progress Foundations (Priority: P3)

A lifter's logged work can produce inspectable personal records and progress
history. PR data is derived from completed and logged sets, handles bodyweight
and weighted work, and remains traceable back to workout history.

**Why this priority**: PRs are part of the app's core premise, but they depend
on the ledger, units, routines, and seed exercise foundations being in place.

**Independent Test**: Complete workouts containing weighted and bodyweight sets,
then verify PR records and progress points are generated from logged work and
can be traced to the source workouts.

**Acceptance Scenarios**:

1. **Given** a completed workout with a new weighted best, **When** progress data is calculated, **Then** the PR record identifies the exercise, reps, weight, date, and source workout.
2. **Given** a completed bodyweight workout with a new rep best, **When** progress data is calculated, **Then** the PR record recognizes the reps-only improvement.
3. **Given** a user reviews progress history, **When** they inspect a PR, **Then** the underlying workout evidence remains available.

### Edge Cases

- App is fully offline during first workout creation, set logging, routine launch, and progress review.
- App restarts after a workout starts but before any sets are logged.
- App restarts after multiple sets are logged and a rest timer is running.
- User starts from a routine, modifies the active workout, then discards the workout.
- User finishes a workout with some planned sets unlogged.
- User logs bodyweight movements with reps only and no added load.
- User logs fractional weights and later changes display units.
- User enters decimal values using locale-specific decimal separators.
- Seed data contains duplicate names, missing classifications, or malformed rows.
- User-created exercises overlap with seed exercise names.
- Future sync is not implemented, but identifiers and mutation records must not block it.
- Android is the first target to validate, while iOS compatibility cannot be broken by shared foundation choices.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support a local active workout lifecycle that includes starting empty, starting from a reusable routine, resuming, finishing, and discarding.
- **FR-002**: System MUST keep reusable routines separate from active workout instances and completed workout history, even if the storage model shares underlying concepts.
- **FR-003**: System MUST allow users to create reusable routines from completed workouts.
- **FR-004**: System MUST allow users to add exercises during an active workout without requiring a pre-existing routine.
- **FR-005**: System MUST persist active workout identity and timing state so the active workout can be restored after app restart or process death.
- **FR-006**: System MUST record every confirmed set with stable identity, exercise association, weight or bodyweight representation, reps, position, and logged timestamp.
- **FR-007**: System MUST not present a set as logged until the set has been durably recorded.
- **FR-008**: System MUST provide an explicit edit path for logged sets that preserves logged status and keeps the edit auditable.
- **FR-009**: System MUST discard unlogged planned sets when finishing a workout while preserving all logged set tuples.
- **FR-010**: System MUST treat bodyweight reps-only sets as valid logged work, PR-eligible history, and exportable data.
- **FR-011**: System MUST store weights in a canonical unit and support display/input conversion for the user's selected unit.
- **FR-012**: System MUST support fractional weights and locale decimal input for numeric set values.
- **FR-013**: System MUST ingest the current OopsAllPRs baseline exercise seed list and validate required classification fields before exposing exercises to users.
- **FR-014**: System MUST prevent seed ingestion from overwriting or deleting user-created exercises without explicit user action.
- **FR-015**: System MUST support user-created exercises with enough classification data to participate in routines, active workouts, history, and progress.
- **FR-016**: System MUST derive PR and progress records from logged workout evidence and preserve traceability to source workouts.
- **FR-017**: System MUST support weighted PRs, bodyweight rep PRs, and unit-converted PR display without changing stored evidence.
- **FR-018**: System MUST provide local export-ready data for workouts, sets, exercises, routines, and PR history.
- **FR-019**: System MUST keep the foundation usable without network connectivity and MUST NOT require account creation or cloud services.
- **FR-020**: System MUST use stable identifiers, timestamps, and explicit mutation paths that keep future cloud sync feasible.
- **FR-021**: System MUST define validation evidence for ledger integrity, session recovery, seed ingestion, bodyweight logging, canonical units, and PR derivation.
- **FR-022**: System MUST preserve iOS product viability through shared domain behavior and platform adapter boundaries while validating Android first.

### Key Entities *(include if feature involves data)*

- **Reusable Routine**: A saved workout plan the user can launch later; contains planned exercises and planned sets, and is not the same as an active workout instance.
- **Active Workout**: A workout currently being logged; has stable identity, started time, exercises, sets, rest/elapsed timing state, and local recovery state.
- **Completed Workout**: Historical workout evidence produced when the user finishes an active workout; contains only logged sets and completion metadata.
- **Exercise**: A selectable movement from seed data or user creation; includes canonical name and classification such as bodyweight or weighted behavior.
- **Exercise Set**: A planned or logged unit of work for an exercise; logged sets have reps, optional load, order, and logged timestamp.
- **Personal Record**: A best performance derived from logged workout evidence; supports weighted and bodyweight achievements and links back to source history.
- **Progress Point**: A historical data point used to show strength or volume change over time.
- **Exercise Seed Row**: A baseline exercise definition imported into the app after validation.
- **User Preferences**: Local settings such as weight unit, date formatting, and backup/export-related choices.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can start an empty workout, add an exercise, and confirm the first set from a fresh local install in 30 seconds or less during manual validation.
- **SC-002**: After app restart or process death, an active workout with logged sets restores the same workout identity and all logged set values in 2 seconds or less on a supported Android test device.
- **SC-003**: Ledger integrity tests demonstrate that finishing a workout preserves 100% of logged set tuples and removes unlogged planned sets from completed history.
- **SC-004**: Bodyweight validation demonstrates that a reps-only set can be logged, restored, exported, and recognized as PR-eligible.
- **SC-005**: Unit conversion validation demonstrates round-trip preservation of canonical stored weight values for pounds, kilograms, and fractional loads.
- **SC-006**: Seed ingestion validation accepts the complete current baseline seed list, reports malformed rows, and preserves user-created exercises across repeated ingestion.
- **SC-007**: PR derivation validation demonstrates traceable PR records for at least one weighted exercise and one bodyweight exercise.
- **SC-008**: Foundation validation runs without network access for workout start, set logging, finish, history lookup, seed access, and PR lookup.
- **SC-009**: Android-first validation passes while shared-domain tests and adapter boundaries show no Android-only dependency in shared foundation behavior.
- **SC-010**: Milestone release review confirms local data ownership, backup behavior, export readiness, accessibility basics, and release artifact integrity are documented.

## Assumptions

- Android is the first execution and validation target; iOS remains a first-class target through shared behavior and platform adapter design.
- Cloud sync, accounts, and remote backup services are out of scope for this feature, but future sync compatibility is a design constraint.
- Android Auto Backup is allowed, with final backup behavior documented before public release.
- The current OopsAllPRs exercise seed list is the baseline source. A replacement source requires licensing review, coverage comparison, and migration planning.
- Templates may be modeled as a separate entity or as routine-like workout records during planning, as long as the user workflows remain distinct and safe.
- Derived PR and last-set data may be materialized or computed during planning, as long as the source workout evidence remains authoritative.
- Manual device verification is required at milestone or release gates, not for every pull request unless a later plan marks it required.
- The existing design-system module will supply UI components and theme tokens; this foundation wires minimal shared flows through it rather than inventing new component APIs.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Establishes the local foundation for starting, resuming, adding exercises, and confirming sets quickly; normal active logging must remain stepper-first or one-tap-first.
- **Ledger Integrity**: Defines confirmed sets as durable, timestamped evidence; finish behavior must preserve logged tuples and discard unlogged planned sets.
- **Recovery Behavior**: Requires active workout identity and timers to restore after restart/process death using wall-clock timing.
- **Progress Promise**: Includes PR history, weighted PRs, bodyweight rep PRs, last-set/progress foundations, and traceability to source workouts.
- **Local-First Ownership**: Requires offline usability, local storage, export-ready data, backup documentation, and sync-compatible identifiers/mutations without implementing sync.
- **Platform Scope**: Delivers Android validation first while preserving iOS product viability through shared behavior and platform adapter boundaries.
- **Design System & Accessibility**: This foundation does not design the full active UI, but all future UI built on it must use the single Neo-Glass token-driven design system (constitution v3.0.0, Principle VII; not Material 3) and satisfy touch, TalkBack, dynamic type, reduced motion, haptics, and adaptive layout gates.
- **Release Evidence**: Requires automated validation for ledger integrity, recovery, seed ingestion, bodyweight logging, unit conversion, PR derivation, offline behavior, and milestone/release manual checks.
