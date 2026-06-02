# Feature Specification: Timed Exercise PRs

**Feature Branch**: `019-timed-exercise-prs`

**Created**: 2026-06-01

**Status**: Draft

**Input**: User description: "Add timing support so users can track duration-based exercises like planks and wall sits, log time-based sets, and record PRs for time."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log A Timed Exercise Set (Priority: P1)

As a lifter doing a plank, wall sit, dead hang, or similar duration-based movement, I want to record how long I held the set so my workout history reflects the actual work I performed.

**Why this priority**: Duration-based movements are not accurately represented by reps or weight. The app needs one fast path for timed work before time PRs, history, or progress can be trusted.

**Independent Test**: Start or resume a workout, add a duration-based exercise, record a completed hold duration, finish the workout, and verify the completed history shows the exercise and time value without requiring reps or weight.

**Acceptance Scenarios**:

1. **Given** an active workout with a timed exercise selected, **When** the user records a duration and logs the set, **Then** the set is saved as completed work with that duration and without requiring reps or weight.
2. **Given** a timed exercise draft has no duration, **When** the user attempts to log it, **Then** the app prevents the invalid set and keeps the current draft available for correction.
3. **Given** a user logs several timed sets for the same exercise, **When** they review the active workout and completed history, **Then** each logged set shows its duration in a readable minutes/seconds format.

---

### User Story 2 - Track Time-Based PRs (Priority: P2)

As a lifter improving on planks or wall sits, I want the app to identify when I set a new time PR so duration progress feels as rewarding and inspectable as weight or reps progress.

**Why this priority**: The product promise is PR feedback. Timed exercises need first-class PR detection, not only history storage.

**Independent Test**: Complete two workouts for the same timed exercise with increasing durations and verify the later workout marks a new time PR with source evidence.

**Acceptance Scenarios**:

1. **Given** the user has a prior 60-second plank, **When** they log and finish a 75-second plank, **Then** the completed set is marked as a new time PR.
2. **Given** the user has a prior 75-second plank, **When** they log and finish another 75-second plank, **Then** the app does not create a duplicate new PR for tying the current best.
3. **Given** a time PR exists, **When** the user opens PR evidence from History or Progress, **Then** the source workout, exercise, set duration, and achieved date are visible.

---

### User Story 3 - Use Timed Exercises In Routines And Previous Values (Priority: P3)

As a lifter who repeats core or conditioning work, I want timed exercises to work inside routines and reuse my previous duration when no explicit target is set.

**Why this priority**: The app now supports routine shells and previous-value defaults. Timed exercises should follow the same fast-loop pattern instead of becoming a separate workflow.

**Independent Test**: Create or launch a routine containing a timed exercise, leave its duration target blank, and verify the active workout pre-fills the last completed duration while keeping the saved routine unchanged.

**Acceptance Scenarios**:

1. **Given** a routine includes a timed exercise with no explicit target duration, **When** the user launches the routine after a previous completed timed set, **Then** the draft duration defaults to the previous completed duration.
2. **Given** a routine includes an explicit timed target, **When** the user launches it, **Then** the explicit target is shown instead of the previous duration.
3. **Given** a user finishes a timed workout created from a routine, **When** they return to the routine later, **Then** the saved routine is not silently overwritten by the completed duration.

---

### User Story 4 - Review Timed Progress (Priority: P4)

As a lifter reviewing progress, I want timed exercise records and charts to show duration values so I can see whether my holds are getting longer.

**Why this priority**: Timed PRs should be visible in the same History and Progress surfaces as other PR types, but this can follow the core logging and PR path.

**Independent Test**: Complete timed exercise workouts that produce time PRs, open Progress for that exercise, and verify the latest PR and trend use duration labels.

**Acceptance Scenarios**:

1. **Given** a timed exercise has one or more time PRs, **When** the user opens the Progress exercise detail, **Then** the latest record displays the best duration and achieved date.
2. **Given** a timed exercise has multiple completed durations, **When** the user views its trend, **Then** duration points are chronological and formatted consistently.

### Edge Cases

- Timed sets with zero, negative, or missing durations must not be logged as completed work.
- Timed exercises must not require reps or weight unless a future exercise type explicitly supports mixed scoring.
- A timed exercise may be added during an active workout, launched from a routine, or saved into a routine from a completed workout.
- App restart, process death, or backgrounding during an active workout must preserve any unlogged timed draft and all previously logged timed sets.
- Finished workouts must keep logged timed sets and discard unlogged timed drafts, matching the existing workout ledger behavior.
- Deleted history can remove source evidence for a time PR; Progress must show a local-unavailable message rather than crashing or fabricating evidence.
- Duration labels must remain readable for short holds under one minute and long holds over one hour.
- Existing weighted and bodyweight reps-only exercises, PRs, routines, charts, exports, and profile weight-unit settings must continue to behave as they do today.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST allow users to record a completed set whose primary logged value is duration.
- **FR-002**: System MUST support duration-based exercises such as planks and wall sits without requiring reps or weight.
- **FR-003**: System MUST validate that logged timed sets include a positive duration before they can be confirmed.
- **FR-004**: System MUST display timed set durations in active workout, completed history, PR evidence, and Progress views using a readable time format.
- **FR-005**: System MUST preserve timed set ledger integrity: logged durations are durable, timestamped, auditable, and only changed through explicit edit/delete flows.
- **FR-006**: System MUST derive time PRs for timed exercises based on longest completed duration for that exercise.
- **FR-007**: System MUST avoid creating a new time PR when a completed duration only ties the existing best.
- **FR-008**: System MUST link each time PR to source workout evidence, including workout date, exercise name, set duration, and achieved date when source data remains available.
- **FR-009**: Users MUST be able to include timed exercises in reusable routines.
- **FR-010**: System MUST support blank and explicit timed routine targets, preserving explicit targets and filling blank targets from previous completed timed values when available.
- **FR-011**: System MUST keep completed timed workout data local-first and available offline.
- **FR-012**: System MUST preserve active timed workout state across app restart, process death, and ordinary backgrounding.
- **FR-013**: System MUST include timed exercise data in user-owned history and export views in a format that remains understandable outside the app.
- **FR-014**: System MUST preserve existing weighted, bodyweight reps-only, rest timer, routine, history, Progress, PR, and export behavior for non-timed exercises.

### Key Entities *(include if feature involves data)*

- **Timed Set**: A completed or draft workout set where duration is the primary measure of work. Key attributes include duration, set position, exercise, logged timestamp when confirmed, and edit/delete status when changed later.
- **Timed Exercise**: An exercise classified for duration-based logging, such as plank, wall sit, dead hang, or similar holds. It determines whether duration is required and reps/weight are optional or hidden.
- **Time PR**: A personal record representing the longest completed duration for a timed exercise, with source workout/set evidence and achieved date.
- **Timed Routine Target**: An optional planned duration for a timed exercise inside a reusable routine. Blank targets allow previous completed duration defaults; explicit targets override previous values.
- **Timed Progress Point**: A historical duration point used to show timed exercise progress and trend over time.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can add a timed exercise and log one completed timed set from an active workout in under 10 seconds after selecting the exercise.
- **SC-002**: 100% of invalid zero, negative, or missing-duration timed set attempts are blocked before becoming completed workout history.
- **SC-003**: Time PR derivation correctly identifies longest-duration improvements and rejects ties across at least three completed workout scenarios.
- **SC-004**: Timed set recovery tests prove that an active timed draft and logged timed sets remain available after app restart or process recreation.
- **SC-005**: Timed set values appear correctly in active workout, history detail, PR evidence, and Progress exercise detail during manual release validation.
- **SC-006**: Existing weighted and bodyweight regression tests continue to pass with no behavior change for non-timed exercises.

## Assumptions

- Timed exercises are duration-first: reps and weight are not required for the first timed logging slice.
- Time PRs use longest duration as the initial scoring rule. Shortest-time goals, intervals, rounds, circuits, and pace-based scoring are out of scope.
- The first timed exercise examples are planks and wall sits, but the model should support other hold-style movements.
- Timed routine targets are optional by default, matching the current routine-shell direction.
- The active logging path should support direct duration correction as an escape hatch, but should remain thumb-friendly and quick.
- Cloud sync remains out of scope; timed workout data is local-first and future sync-ready through stable records and timestamps.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Timed logging must remain quick and thumb-friendly, with no modal required to confirm a basic timed set.
- **Ledger Integrity**: Timed sets and time PRs must be durable, timestamped, source-evidenced, and edited only through explicit recovery/edit flows.
- **Recovery Behavior**: Active timed drafts, elapsed workout state, logged timed sets, and any active duration entry must survive restart, process death, and backgrounding.
- **Progress Promise**: Time PRs extend the app's PR loop to duration-based work while preserving evidence and Progress visibility.
- **Local-First Ownership**: All timed data works offline, stays local by default, exports clearly, and remains future sync-ready through stable identifiers and timestamps.
- **Platform Scope**: Android is validated first, while domain rules, shared state, and shared UI remain iOS-ready.
- **Design System & Accessibility**: Timed controls use the existing Fit design system, large touch targets, readable labels, TalkBack/VoiceOver descriptions, reduced-motion-safe behavior, and non-color-only PR feedback.
- **Release Evidence**: Planning must include automated tests for logging, validation, PR derivation, routine defaults, recovery, history/progress display, export behavior, Android build health, iOS shared compile, and manual device validation of the timed logging loop.
