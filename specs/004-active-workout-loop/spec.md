# Feature Specification: Active Workout Logging Loop

**Feature Branch**: `004-active-workout-loop`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Implement the active workout logging loop for Oops All PRs on top of the shared navigation shell and Neo-Glass design system. Include exercise blocks, set rows, one-tap logging from target or last-set values, bodyweight reps-only logging, inline RollerField editing for reps and weight, add-exercise during workout, logged-only-after-persistence behavior, non-destructive inline errors, and active session recovery for focused exercise/set state. Keep Android validated first and iOS first-class through shared Compose/state boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log a Set With One Tap (Priority: P1)

A lifter opens an active workout and logs the next set with one tap because the
set row is pre-filled from the planned target or last known set values. The set
does not appear logged until persistence confirms.

**Why this priority**: This is the product's core loop. Oops All PRs must be
faster than a notes app while preserving workout ledger integrity.

**Independent Test**: Start an active workout with an exercise and a pre-filled
set row, tap Log once, and confirm the row becomes logged only after persistence
success with a timestamp and no modal interruption.

**Acceptance Scenarios**:

1. **Given** an active workout has an exercise with a next set row, **When** the user taps Log, **Then** the set is persisted and the row shows logged with `loggedAt`.
2. **Given** persistence fails, **When** the user taps Log, **Then** the row remains unlogged and shows an inline error without losing the entered values.
3. **Given** the prior completed set had usable values, **When** the next set row appears, **Then** weight and reps default from that prior context unless the routine target overrides them.

### User Story 2 - Edit Reps and Weight Inline (Priority: P1)

A lifter adjusts reps or weight in place without opening a modal. Inline
`RollerField` controls support tap-step changes, drag-roll changes, and direct
numeric fallback for correction.

**Why this priority**: Values often change between sets; editing must be fast
and not interrupt the workout rhythm.

**Independent Test**: On an active set row, tap the reps field top/bottom half
to adjust by one step, drag the weight field to jump values, and confirm the
updated values are used by one-tap logging.

**Acceptance Scenarios**:

1. **Given** a reps field is focused, **When** the user taps above or below its center, **Then** reps adjust by one step within valid bounds.
2. **Given** a weight field is edited, **When** the user drags and releases, **Then** the value snaps to the configured increment and remains ready to log.
3. **Given** direct numeric entry is used, **When** the value is valid, **Then** it updates the same set row without changing logged history.

### User Story 3 - Add Exercises While Working Out (Priority: P1)

A lifter can add an exercise during an active workout and immediately log sets
for it. Bodyweight exercises allow reps-only logging by default.

**Why this priority**: Real workouts change on the fly. The active loop must
support spontaneous exercise additions without forcing routine editing.

**Independent Test**: From the active workout, add a weighted exercise and a
bodyweight exercise, then log a weighted set and a reps-only bodyweight set.

**Acceptance Scenarios**:

1. **Given** the user searches the exercise catalog, **When** they select an exercise, **Then** it is appended to the active workout as a loggable exercise block.
2. **Given** the selected exercise is bodyweight, **When** the first set row appears, **Then** reps are required and weight is optional.
3. **Given** the user creates or selects an exercise during the workout, **When** they return to the active screen, **Then** focus returns to the new exercise block.

### User Story 4 - Recover the Active Logging Position (Priority: P2)

If the app is backgrounded, restarted, or process-killed, the active workout
returns to the focused exercise/set and preserves unlogged edits.

**Why this priority**: Session recovery is a core product promise and active
logging is the highest-risk place to lose user effort.

**Independent Test**: Edit an unlogged set, leave the app, recreate the app
state, and confirm the active workout, focused exercise, focused set, entered
values, and elapsed time recover.

**Acceptance Scenarios**:

1. **Given** the user has an active workout with a focused set row, **When** the app restarts, **Then** the same exercise and set are focused.
2. **Given** the user changed an unlogged set value, **When** the session recovers, **Then** the edited value is still present but not marked logged.
3. **Given** a set was logged before restart, **When** the session recovers, **Then** the logged timestamp and tuple remain unchanged.

### User Story 5 - Keep Errors Non-Destructive (Priority: P2)

When validation or persistence fails, the app explains the issue inline and
preserves the user's in-progress set values.

**Why this priority**: Error handling must protect trust in the workout ledger
without derailing the active workout.

**Independent Test**: Attempt invalid bodyweight, weighted, and persistence
failure cases and confirm no logged row is fabricated or silently changed.

**Acceptance Scenarios**:

1. **Given** reps are missing for any set, **When** the user taps Log, **Then** an inline validation error appears and the set remains unlogged.
2. **Given** a weighted set has an invalid weight, **When** the user taps Log, **Then** an inline validation error appears and prior logged sets are unchanged.
3. **Given** persistence fails after valid input, **When** the failure returns, **Then** the set row remains editable with its values intact.

### Edge Cases

- Bodyweight movements are valid with reps only; added load is optional.
- Fractional weights are accepted according to the configured increment.
- Negative reps, zero reps, and invalid decimal values are rejected inline.
- Duplicate taps on Log while save is pending must not create duplicate sets.
- Removing or editing unlogged rows must not mutate logged rows.
- Empty active workouts can add exercises on the fly.
- Restart during pending save recovers the last confirmed ledger state and any
  local unlogged draft values that were persisted as active session draft state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Active workout must render exercise blocks containing editable next-set rows and logged set history.
- **FR-002**: Each next-set row must support one-tap logging using target or last-set defaults.
- **FR-003**: A set must be marked logged only after persistence succeeds and must include `loggedAt`.
- **FR-004**: Duplicate log taps while a save is pending must be ignored or disabled to prevent duplicate ledger entries.
- **FR-005**: Inline reps editing must support step changes, drag/roll changes, valid bounds, and direct numeric fallback.
- **FR-006**: Inline weight editing must support canonical unit conversion, fractional increments, valid bounds, and direct numeric fallback.
- **FR-007**: Bodyweight sets must allow reps-only logging and must not require a weight.
- **FR-008**: Add-exercise flow must let users select from the exercise catalog during an active workout and append the selected exercise to the active workout.
- **FR-009**: Returning from add-exercise must focus the newly added exercise block.
- **FR-010**: Active session recovery must restore active workout id, focused exercise, focused set, draft set values, elapsed time source, and logged/unlogged row state.
- **FR-011**: Validation and persistence failures must show non-destructive inline errors and preserve entered values.
- **FR-012**: All active logging UI must use the Neo-Glass design system and meet touch target, screen reader, reduced-motion, and haptic-alternative expectations.

### Key Entities *(include if feature involves data)*

- **ExerciseBlock**: active exercise instance, display name, bodyweight flag,
  logged set history, current editable set row, focus state.
- **SetRowDraft**: editable set kind, reps, weight, unit display, position,
  validation state, pending-save state.
- **LoggedSetRow**: persisted set id, reps, weight, kind, position, logged
  timestamp, edit affordance.
- **RollerField**: inline numeric editor for reps or weight with step, range,
  display unit, and direct-entry state.
- **ActiveWorkoutFocus**: focused exercise id, focused set id or draft id,
  scroll target, updated timestamp.
- **ExerciseSelectionResult**: selected catalog exercise and insertion position.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A repeat set can be logged from the active screen with one tap and no modal.
- **SC-002**: A bodyweight set can be logged with reps only.
- **SC-003**: A failed persistence attempt never displays a set as logged.
- **SC-004**: Restarting the app during an active workout restores the focused exercise/set and unlogged draft values.
- **SC-005**: Adding an exercise during a workout returns the user to the active screen focused on the new exercise.
- **SC-006**: Android build and shared tests pass, and iOS shared compile remains healthy.

## Assumptions

- The navigation shell and design system from features 002 and 003 are already
  available.
- This feature implements active workout logging behavior and app-level
  composites, not a complete rest timer, finish/discard flow, or PR celebration.
- Direct numeric entry is an escape hatch; the normal editing path remains
  inline tap/roll controls.
- Draft recovery may use existing session storage or a small local extension,
  but confirmed workout ledger semantics remain unchanged.

## Constitution Alignment *(mandatory)*

- **I (Fast-loop logging first)**: one-tap logging and inline editing avoid
  modal interruption.
- **II (Ledger)**: rows become logged only after persistence succeeds; failures
  preserve drafts and logged tuples.
- **III (Session recovery)**: focused exercise/set and draft values recover
  after restart.
- **IV (Progress promise)**: logged sets preserve enough evidence for later PR
  derivation.
- **V (Local-first ownership)**: all behavior is local/offline and sync-ready
  through stable ids and explicit mutation paths.
- **VI (Shared-first KMP)**: logging state and shared UI live in shared code
  with Android validated first and iOS kept compile-ready.
- **VII (Design system and accessibility)**: active logging UI uses only
  FitTheme/design-system components and accessibility-friendly controls.
- **VIII (Public release gates)**: tests and platform validation are required;
  manual device verification can be milestone-gated when hardware is available.
