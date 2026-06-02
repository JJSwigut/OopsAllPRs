# Feature Specification: Previous Workout Values

**Feature Branch**: `codex/019-previous-workout-values`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Previous Workout Values: when launching a routine or adding an exercise, prefill planned and next-set values from the last completed workout for that exercise while preserving explicit routine targets, bodyweight reps-only behavior, canonical units, local-only/offline-first persistence, and Android-first/iOS-ready shared KMP boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add an Exercise With Last Values Ready (Priority: P1)

A lifter adds an exercise during an empty or active workout and immediately sees the next set prefilled from the most recent completed workout for that exercise, so repeated training starts faster without manually re-entering the same weight and reps.

**Why this priority**: Adding an exercise on the fly is one of the fastest workout paths. Previous values reduce taps at the exact moment the user is trying to log.

**Independent Test**: Complete a workout with an exercise, start a new empty workout, add the same exercise, and verify the first next-set draft uses the latest completed values for that exercise.

**Acceptance Scenarios**:

1. **Given** a completed workout contains Bench Press sets of 135 lb x 8 and 135 lb x 7, **When** the user starts a new empty workout and adds Bench Press, **Then** the first Bench Press draft is prefilled with 135 lb x 8.
2. **Given** a completed workout contains Pull Up bodyweight sets of 10 and 8 reps, **When** the user adds Pull Up to a later workout, **Then** the draft is reps-only and prefilled with 10 reps without requiring a load.
3. **Given** no completed workout exists for the added exercise, **When** the user adds that exercise, **Then** the app uses the existing safe defaults for that exercise type.

---

### User Story 2 - Launch a Routine Without Losing Planned Targets (Priority: P2)

A lifter launches a saved routine and receives previous completed values only where the routine does not already provide an explicit planned set target. Deliberate routine targets remain intact.

**Why this priority**: Routines are reusable plans. Previous values should speed up under-specified routines without silently rewriting planned work.

**Independent Test**: Create or seed a routine with one exercise that has explicit planned targets and another exercise or extra set without targets, launch the routine after completed workout history exists, and verify explicit targets remain while missing values use prior completed values.

**Acceptance Scenarios**:

1. **Given** a routine has Squat planned as 185 lb x 5, **When** the user launches the routine after recently completing Squat at 195 lb x 5, **Then** the launched planned set remains 185 lb x 5.
2. **Given** a routine includes a set with missing target values for Row, **When** the user launches it after completing Row at 100 lb x 10, **Then** the missing target values are filled from the previous completed Row set.
3. **Given** a routine has fewer planned sets than the prior completed workout, **When** the launched workout prepares additional next-set drafts after planned sets are logged, **Then** those drafts may use the prior completed set sequence without mutating the saved routine.

---

### User Story 3 - Preserve Units, Ledger, and Recovery (Priority: P3)

A lifter can change display units, restart the app, or review history without previous-value defaults changing completed workouts, routines, PRs, exports, or canonical stored values.

**Why this priority**: Previous values are useful only if they remain trustworthy context rather than silent mutation of the workout ledger.

**Independent Test**: Complete weighted and bodyweight workouts, change unit preference, start new workouts with previous defaults, restart the app, and verify drafts recover while completed history, routines, PRs, and exports remain unchanged.

**Acceptance Scenarios**:

1. **Given** a completed weighted set is stored in canonical units, **When** the user changes display units and adds the exercise later, **Then** the displayed default respects the selected unit while the stored draft keeps canonical meaning.
2. **Given** a previous-value draft is created in an active workout, **When** the app restarts before the set is logged, **Then** the draft recovers through the normal active workout recovery path.
3. **Given** previous values are used to prefill a new draft, **When** the user reviews History, Progress, PR evidence, exports, or saved routines, **Then** no completed or template data has been silently changed.

---

### Edge Cases

- If multiple completed workouts contain the same exercise, the most recently completed workout wins.
- If the most recent completed workout has multiple sets for an exercise, defaults follow the completed set order; when more drafts are needed than prior sets, the last prior completed set may be reused.
- If the last completed set for a weighted exercise has missing or invalid load/reps data, the app must ignore that set and use the next valid prior set or safe defaults.
- Bodyweight exercises use reps-only previous values unless the app later supports optional added load for that movement.
- Previous-value lookup must not consider active unlogged drafts, discarded workouts, deleted completed workouts, archived exercises without current selection, or routine targets as completed evidence.
- Previous values must work offline and must not require cloud sync, account state, or network lookup.
- Locale decimal input and display-unit conversion must remain handled by existing input/unit boundaries rather than stored as display strings.
- Existing workout history, templates, PRs, exports, and user-created exercises must remain unchanged unless the user explicitly logs or edits a set.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST derive previous workout defaults from completed workout ledger data, not active drafts, routine templates, or UI-only state.
- **FR-002**: System MUST select the most recent completed workout containing the requested exercise when deriving previous values.
- **FR-003**: System MUST expose previous values as draft defaults for an exercise added to an active workout when no explicit active target exists.
- **FR-004**: System MUST preserve explicit routine planned set targets when launching a routine.
- **FR-005**: System MUST use previous values to fill missing routine-launched target values where the routine does not provide an explicit value.
- **FR-006**: System MUST derive previous values by set order, using the matching prior set index when available and the last valid prior set when a later draft exceeds the prior set count.
- **FR-007**: System MUST keep bodyweight previous-value drafts reps-only and must not require weight for bodyweight movements.
- **FR-008**: System MUST keep weight defaults canonical internally and display them in the user's selected unit through existing unit formatting behavior.
- **FR-009**: System MUST ignore invalid or incomplete prior sets and fall back to the next valid prior value or the existing safe default.
- **FR-010**: System MUST preserve completed workouts, routine templates, PR history, progress points, export snapshots, and user-created exercises when previous values are applied.
- **FR-011**: System MUST persist and recover active drafts created from previous values through the normal active workout recovery path.
- **FR-012**: System MUST work fully offline using local data and remain compatible with future sync by relying on stable exercise and workout identifiers.
- **FR-013**: System MUST include validation coverage for added-exercise defaults, routine target precedence, missing routine target fills, bodyweight reps-only defaults, unit/canonical behavior, invalid prior-set fallback, ledger non-mutation, and recovery.

### Key Entities *(include if feature involves data)*

- **Previous Workout Value**: A derived default for one exercise set based on a prior completed set's reps and optional load.
- **Previous Workout Snapshot**: The ordered valid sets from the most recent completed workout containing an exercise.
- **Explicit Routine Target**: A routine planned set value provided by the saved routine that must take precedence over derived previous values.
- **Prefilled Active Draft**: An active workout set draft initialized from either an explicit target, a previous value, or existing safe defaults.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After a prior completed workout exists, adding the same exercise to a new workout produces a loggable prefilled draft without additional user input.
- **SC-002**: A routine launch with explicit planned targets preserves 100% of those targets even when newer completed values exist.
- **SC-003**: A routine launch with missing targets fills available missing values from prior completed sets for the same exercise.
- **SC-004**: Bodyweight previous-value drafts can be logged with reps only and no weight validation error.
- **SC-005**: Automated tests prove previous-value application does not mutate completed history, routines, PR derivation inputs, or exportable ledger data.
- **SC-006**: Android build, shared behavior tests, iOS shared compile, Material/style guard, and whitespace validation pass before completion.

## Assumptions

- "Previous workout" means the most recently completed workout that includes the same exercise catalog identity, not the most recent routine or active draft.
- Explicit routine targets take precedence because a saved routine represents deliberate planned work.
- If a routine target has only one field missing, previous values may fill the missing field without replacing the field that was explicitly provided.
- Previous-value lookup is derived on demand for launch/add flows; this feature does not require a new persisted cache.
- Manual real-device validation is deferred by user request for this turn, but automated gates and emulator/device-ready evidence files remain required.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: The feature reduces repeated entry when adding exercises or launching under-specified routines and keeps the next logging action obvious.
- **Ledger Integrity**: Previous values are draft defaults only; completed workouts, routines, PR evidence, and exports are not silently mutated.
- **Recovery Behavior**: Active drafts created from previous values recover through existing session recovery.
- **Progress Promise**: Last-set context becomes more useful while preserving weighted, bodyweight, fractional, unit-converted, and PR evidence semantics.
- **Local-First Ownership**: Lookup is local/offline-only and uses existing stable identifiers and timestamps so future sync remains feasible.
- **Platform Scope**: Android is validated first while derivation, state, and shared Compose boundaries remain Kotlin Multiplatform and iOS-ready.
- **Design System & Accessibility**: Any visible context uses existing Fit design-system components and labels; no new parallel styling is introduced.
- **Release Evidence**: Common tests, Android build, iOS shared compile, Material/style scan, whitespace validation, and deferred manual-device notes are required.
