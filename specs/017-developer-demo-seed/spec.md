# Feature Specification: Developer Demo Data Seeding

**Feature Branch**: `codex/017-developer-demo-seed`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Create a developer-only way to seed the app with realistic data for testing features. Seed progress, routines, active workouts, history, PRs, rest timers, and recovery scenarios while keeping release users untouched."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Load Progress Demo Data (Priority: P1)

A developer testing the app can populate a fresh local install with realistic completed workouts so History, Progress, PRs, exercise detail charts, exports, and exercise evidence views are immediately reviewable without manually logging weeks of workouts.

**Why this priority**: Progress and PR validation currently requires historical data, which makes manual QA slow and inconsistent.

**Independent Test**: Start from a fresh developer build, trigger the progress demo seed, then verify History has completed workouts, Progress has multiple exercise groups, a selected exercise chart has multiple points, and PR evidence links resolve to completed workout details.

**Acceptance Scenarios**:

1. **Given** a fresh developer build with only the baseline exercise catalog, **When** the developer loads progress demo data, **Then** completed workouts, progress points, personal records, and chartable exercise history are available locally.
2. **Given** progress demo data already exists, **When** the developer loads the same scenario again, **Then** the app does not create duplicate demo workouts, duplicate routines, or duplicate PR rows.
3. **Given** a release build, **When** the app is opened, **Then** no developer seed control or automatic demo data path is available to a public user.

---

### User Story 2 - Load Routine and Rest Timer Demo Data (Priority: P2)

A developer testing training setup can seed reusable routines that include weighted exercises, bodyweight exercises, planned sets, and rest durations so routine launch, rest configuration, and active logging behavior can be reviewed quickly.

**Why this priority**: Routine and rest timer behavior spans several features and is hard to validate repeatedly from an empty database.

**Independent Test**: Load routine demo data, inspect Train routines, launch one routine, log a set, and verify the active workout carries planned sets and rest configuration.

**Acceptance Scenarios**:

1. **Given** a developer build with seeded exercises, **When** routine demo data is loaded, **Then** at least two reusable routines appear with planned weighted and bodyweight work.
2. **Given** a seeded routine has rest durations, **When** the developer launches it and logs a set, **Then** rest timing behavior can be exercised from the active workout without manually configuring each exercise.

---

### User Story 3 - Load Active Recovery Demo Data (Priority: P3)

A developer testing recovery can create an active in-progress workout with logged and unlogged work so the resume banner, active workout screen, draft state, and cold-start restoration can be validated without recreating state by hand.

**Why this priority**: Recovery is constitutionally core, but manual process-death checks need a repeatable active workout state.

**Independent Test**: Load active recovery demo data, kill and restart the app, and verify the same active workout is restored with logged sets, planned next actions, and resume navigation.

**Acceptance Scenarios**:

1. **Given** no active workout exists, **When** the developer loads active recovery demo data, **Then** an active workout appears with at least one logged set and at least one next set ready to log.
2. **Given** an active workout already exists, **When** the developer tries to load active recovery demo data, **Then** the app avoids silently overwriting the active workout and reports that an active workout already exists.

---

### Edge Cases

- Loading a scenario when baseline exercises have not been seeded should seed or resolve the required baseline exercises before creating demo workout data.
- Loading demo data repeatedly should be idempotent and should not inflate History, Progress, routines, exports, or PR counts.
- Developer demo data must not delete or alter user-created exercises or non-demo workouts without explicit reset behavior.
- Demo data must include bodyweight reps-only work and weighted work so both PR paths remain inspectable.
- Demo data must remain local-only, work offline, and survive app restart like ordinary local app data.
- Active recovery seeding must not replace an existing active workout without an explicit developer reset.
- Release builds must not expose the seed controls, invoke seeding automatically, or include any visible demo-data affordance.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a developer-only way to load named demo data scenarios for manual QA.
- **FR-002**: System MUST keep all demo data local to the device and use the same persistence and domain mutation paths as ordinary app actions.
- **FR-003**: System MUST provide a progress demo scenario containing multiple completed workouts across multiple dates, multiple exercises, weighted sets, bodyweight reps-only sets, PR-eligible sets, and chartable progress points.
- **FR-004**: System MUST provide a routine demo scenario containing reusable routines with planned weighted sets, planned bodyweight sets, and rest durations.
- **FR-005**: System MUST provide an active recovery demo scenario containing an in-progress workout that can be restored after app restart.
- **FR-006**: System MUST make each scenario idempotent so repeated activation does not duplicate existing demo workouts, routines, active sessions, or derived progress records.
- **FR-007**: System MUST preserve user-created exercises, non-demo completed workouts, and non-demo routines when loading demo scenarios.
- **FR-008**: System MUST derive personal records and progress points from seeded completed workouts using the normal PR derivation behavior.
- **FR-009**: System MUST prevent active recovery seeding from silently replacing an existing active workout.
- **FR-010**: System MUST provide clear developer feedback after a scenario load succeeds, is skipped because it already exists, or cannot proceed because an active workout exists.
- **FR-011**: System MUST hide developer seed controls and seed entry points from release/public builds.
- **FR-012**: System MUST include validation evidence for seed idempotency, ledger integrity, PR derivation, chart data availability, bodyweight reps-only logging, routine rest data, and active session recovery.

### Key Entities *(include if feature involves data)*

- **Developer Seed Scenario**: A named developer-only action that creates a specific local test dataset.
- **Demo Dataset Marker**: A stable identifier used to detect previously seeded demo data and keep scenarios idempotent.
- **Demo Completed Workout**: A completed workout created for manual QA that must behave like ordinary completed history.
- **Demo Routine**: A reusable routine created for manual QA with planned sets and rest durations.
- **Demo Active Workout**: An active workout created for recovery testing and restored through the normal session recovery path.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A developer can create chartable progress and PR history from a fresh local install in under 30 seconds without manually logging workouts.
- **SC-002**: Loading the same demo scenario twice leaves the number of demo workouts, demo routines, active workouts, and PR rows unchanged after the second load.
- **SC-003**: Progress demo data produces at least three completed workouts, at least three PR records, and at least one exercise detail chart with four or more points.
- **SC-004**: Routine demo data produces at least two routines, including at least one weighted exercise, one bodyweight exercise, and rest durations.
- **SC-005**: Active recovery demo data restores the same active workout identity and logged set values after app restart or state rehydration.
- **SC-006**: Release builds expose zero visible seed controls and do not invoke developer seeding automatically.
- **SC-007**: Automated tests and Android build validation pass before the feature is considered complete.

## Assumptions

- Developer seed controls may appear in a developer-only section of Profile or an equivalent debug-only developer surface.
- Demo data can use stable names and routine labels to identify prior demo data for idempotency.
- A full destructive database reset is out of scope for this slice unless needed as a private test helper; the production-facing debug action should preserve non-demo user data.
- iOS does not need a native developer UI in this slice, but shared seed behavior must remain platform-neutral and callable from future iOS debug tooling.
- Seed data should reuse the existing baseline exercise catalog rather than introducing a second exercise source.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Normal workout logging remains unchanged; demo data only accelerates developer validation.
- **Ledger Integrity**: Demo workouts and sets must be persisted through normal mutation paths, timestamped, and PR-derived like ordinary user data.
- **Recovery Behavior**: Active recovery seeding explicitly supports cold-start and process-death validation.
- **Progress Promise**: Progress demo data covers weighted PRs, bodyweight reps-only PRs, charts, history, and evidence links.
- **Local-First Ownership**: Seeding is local-only, offline-capable, and avoids future sync blockers by using normal stable identifiers and timestamps.
- **Platform Scope**: Android receives the first developer entry point; shared seed orchestration remains iOS-ready.
- **Design System & Accessibility**: Any visible developer surface uses existing Fit design-system components and remains outside release builds.
- **Release Evidence**: Validation requires common tests, Android build, iOS shared compile, release visibility guard, Material/style guard, and whitespace validation.
