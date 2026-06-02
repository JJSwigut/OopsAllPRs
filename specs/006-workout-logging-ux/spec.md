# Feature Specification: Workout Logging UX V1

**Feature Branch**: `006-workout-logging-ux`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Create a UX reset for the core workout logging loop. Design around a fast full-screen active workout logger: start quickly, add exercises quickly, log repeated sets from defaults, handle bodyweight reps-only logging, surface PR feedback inline, keep active logging focused, and produce concrete screen and interaction guidance before more implementation."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Log a Workout Without Thinking (Priority: P1)

A lifter opens the app at the gym, starts an empty workout, adds one exercise, and logs several sets with minimal attention and no decorative UI competing with the next action.

**Why this priority**: This is the product's core loop. If the active workout logger is slow, crowded, or unclear, the rest of the app cannot deliver on Oops All PRs.

**Independent Test**: A tester can start from a clean app state, start a workout, add an exercise, log three weighted sets, and confirm that every next action is visible without needing instructions.

**Acceptance Scenarios**:

1. **Given** no active workout exists, **When** the user opens Train, **Then** the primary available action is starting a workout and no app title, marketing copy, or decorative panel competes with it.
2. **Given** an active workout with one weighted exercise, **When** the user logs a set, **Then** the next set row appears with sensible defaults from the prior set.
3. **Given** the user is in an active workout, **When** they need to log repeated sets, **Then** they can adjust reps or weight with thumb-sized controls and confirm the set without opening a separate editor.
4. **Given** a set is confirmed, **When** persistence succeeds, **Then** the set appears in the completed set list and the next input remains focused on continuing the workout.

---

### User Story 2 - Add Exercises in the Logging Flow (Priority: P2)

A lifter can add exercises during a workout through a search-first picker that feels like a quick command, not a separate app section.

**Why this priority**: Workout sessions change in real time. Adding exercises must be fast enough that the user does not abandon logging or switch to notes.

**Independent Test**: A tester can start an active workout, open Add Exercise, find a seeded exercise, add it, and return to the active logger with that exercise ready for its first set.

**Acceptance Scenarios**:

1. **Given** an active workout is open, **When** the user chooses Add Exercise, **Then** a dense search-first picker appears without exposing bottom navigation or unrelated destinations.
2. **Given** the exercise picker has local matches, **When** the user selects a result, **Then** the picker closes and the chosen exercise is visible in the active workout ready to log.
3. **Given** no local match exists, **When** the user creates a custom exercise, **Then** the exercise is added to the workout and remains available locally for future search.
4. **Given** the user cancels exercise selection, **When** they return to the active workout, **Then** existing drafted and logged sets are unchanged.

---

### User Story 3 - Bodyweight Logging Stays Reps-First (Priority: P3)

A lifter logging bodyweight movements sees reps as the default and only required input, with optional load never blocking a normal reps-only set.

**Why this priority**: Bodyweight exercises are common, and requiring weight breaks the user's stated logging model.

**Independent Test**: A tester can add a bodyweight exercise, log multiple reps-only sets, and confirm that completed sets and PR feedback do not require weight.

**Acceptance Scenarios**:

1. **Given** a bodyweight exercise is added, **When** the exercise block appears, **Then** the next set input shows reps as the primary required value and does not require weight.
2. **Given** a bodyweight set has reps, **When** the user confirms it, **Then** the set logs successfully with no weight value.
3. **Given** a bodyweight exercise supports added load in the future, **When** the user is in the basic logging path, **Then** optional load controls do not appear unless the user explicitly asks for them.

---

### User Story 4 - Recover and Celebrate Progress (Priority: P4)

A lifter can background or restart the app during a session, resume where they left off, and immediately understand when a set improves a personal record.

**Why this priority**: Session recovery protects trust, and PR feedback is the product reward loop.

**Independent Test**: A tester can start a workout, log a set, restart the app, resume the session, log a record-setting set, and see clear inline progress feedback.

**Acceptance Scenarios**:

1. **Given** an active workout exists, **When** the app restarts, **Then** Train exposes a resume affordance and the active logger restores the exercises, completed sets, drafts, and elapsed session context.
2. **Given** a logged set creates or improves a personal record, **When** the set appears in the completed list, **Then** an inline PR marker identifies what improved without blocking further logging.
3. **Given** a logged set does not create a personal record, **When** it is confirmed, **Then** the user is not shown distracting progress decoration.

### Edge Cases

- Starting a workout while a prior active workout exists shows resume/discard/finish choices instead of silently creating a second active session.
- Long exercise names wrap within their row without pushing controls off screen.
- The exercise picker remains usable when the keyboard is visible and never hides the active search field or selected result action.
- Weight input supports fractional plates, canonical display precision, and locale-friendly correction without making steppers ambiguous.
- Bodyweight movements remain loggable and PR-eligible with reps alone.
- Failed persistence leaves the draft intact, shows a clear retry path, and does not display the set as logged.
- App restart, process death, backgrounding, and screen lock preserve active workout state and elapsed workout context.
- Empty seed data or search failure still leaves custom exercise creation available locally.
- Very long workouts with many exercises remain scrollable with the current logging target easy to find.
- Completed workout history, templates, PRs, exports, and user-created exercises are not changed by the UX reset except through explicit workout logging actions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Train MUST prioritize starting or resuming a workout over branding, explanatory copy, decorative sections, or secondary destinations.
- **FR-002**: Train MUST allow starting an empty workout in one primary action from the first visible screen.
- **FR-003**: Train MUST show an active-session resume affordance whenever an unfinished workout exists.
- **FR-004**: Active workout logging MUST use a focused full-screen mode where workout logging is the primary task and top-level navigation is hidden or visually secondary.
- **FR-005**: Active workout logging MUST show a clear next action at all times: add exercise, log set, retry failed set, or resume existing draft.
- **FR-006**: Exercise blocks MUST show exercise name, exercise type, completed sets, and the next set input in a compact scan-friendly layout.
- **FR-007**: The next set input MUST default to the most recent relevant set values for that exercise when available.
- **FR-008**: Users MUST be able to adjust reps with stepper-style controls and direct correction as an escape hatch.
- **FR-009**: Users MUST be able to adjust weighted exercise load with stepper-style controls and direct correction as an escape hatch.
- **FR-010**: Bodyweight exercise logging MUST require reps only in the default path.
- **FR-011**: Optional added load for bodyweight movements MUST stay out of the default reps-only path until explicitly requested by the user.
- **FR-012**: Confirming a set MUST display it as logged only after the app has accepted and stored the set.
- **FR-013**: Failed set confirmation MUST preserve the user's draft values and provide a visible retry path.
- **FR-014**: Add Exercise MUST open a dense search-first picker scoped to the active workout.
- **FR-015**: Exercise search MUST work from local exercise data and user-created exercises without requiring network access.
- **FR-016**: Selecting an exercise MUST return the user to the active workout with the new exercise ready for its first set.
- **FR-017**: Creating a custom exercise from the picker MUST add it to the active workout and make it available in future local search.
- **FR-018**: Canceling the picker MUST leave active workout sets, drafts, and focus unchanged.
- **FR-019**: Active workout state MUST recover after app restart, process death, backgrounding, and screen lock.
- **FR-020**: Logging a set that creates or improves a personal record MUST show inline PR feedback tied to the logged set.
- **FR-021**: PR feedback MUST identify the improvement in plain language without interrupting further logging.
- **FR-022**: The UX MUST support one-handed use with large touch targets, readable text, and no overlapping controls on common phone sizes.
- **FR-023**: The UX MUST remain understandable with reduced motion, haptics disabled, and assistive screen readers enabled.
- **FR-024**: The UX reset MUST preserve existing workout history, templates, PR history, user-created exercises, and exportable data unless the user explicitly logs, finishes, edits, or discards workout data.

### Key Entities *(include if feature involves data)*

- **Active Workout**: The user's current unfinished workout, including start time, elapsed context, exercise blocks, completed sets, drafts, and current focus.
- **Exercise Block**: A loggable exercise instance within an active workout, including display name, bodyweight/weighted classification, completed set rows, and next set draft.
- **Set Draft**: The editable next-set values for an exercise before confirmation, including reps, optional weight, pending state, and validation errors.
- **Logged Set**: A confirmed workout set with durable position, timestamp, reps, optional weight, and PR eligibility.
- **Exercise Picker Result**: A local exercise choice or user-created exercise candidate that can be added to the active workout.
- **PR Feedback**: A non-blocking marker attached to a logged set when that set creates or improves a record.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A first-time tester can start an empty workout and reach the Add Exercise action in under 5 seconds.
- **SC-002**: A tester can add a seeded exercise to an active workout in under 10 seconds without using instructions.
- **SC-003**: A tester can log three repeated weighted sets for one exercise in under 30 seconds after the exercise is added.
- **SC-004**: A tester can log three bodyweight reps-only sets without entering or seeing a required weight field.
- **SC-005**: In manual review on a Pixel-class phone, no primary Train, Active Workout, or Add Exercise controls overlap the status bar, navigation bar, keyboard, or each other.
- **SC-006**: At least 90% of primary logging actions in manual review are completed without the tester backing out of the active workout screen.
- **SC-007**: After app restart during an active session, a tester can resume and continue logging without losing completed sets or current drafts.
- **SC-008**: A record-setting set produces visible inline PR feedback within the logged set context without requiring the user to navigate to Progress.

## Assumptions

- Android remains the first device target for manual validation, with iOS kept first-class through shared behavior and screen structure.
- The existing exercise seed list remains the baseline catalog for this UX slice.
- This feature is a UX reset for the active logging loop, not a broad redesign of History, Progress, Profile, exports, or account/sync features.
- The active workout mode may temporarily hide or visually subordinate top-level navigation to preserve focus.
- Direct numeric entry is allowed as a correction path, but the primary gym-side path should remain stepper-first or one-tap-first.
- PR feedback can be limited to inline set-level markers for this slice; deeper PR dashboards remain part of Progress.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: The spec directly prioritizes one-tap workout start, dense exercise add, defaulted next-set values, and stepper-first logging so the gym-side loop is faster and clearer.
- **Ledger Integrity**: Confirmed sets appear only after successful acceptance; failed confirmation preserves drafts; existing history, templates, PRs, user-created exercises, and exports are not silently changed.
- **Recovery Behavior**: Active workouts, completed sets, drafts, focus, and elapsed context must recover after restart, process death, backgrounding, and screen lock.
- **Progress Promise**: PR feedback is part of the active loop and must work for weighted and bodyweight reps-only sets without blocking continued logging.
- **Local-First Ownership**: Exercise search, custom exercise creation, active workout logging, and recovery must work offline using local data.
- **Platform Scope**: Android is the first validation target, while the UX requirements describe shared behavior and screen structure that must remain viable on iOS.
- **Design System & Accessibility**: The UX must use the shared token-driven design system, support large touch targets, readable text, reduced motion, non-haptic alternatives, and assistive screen reader labels.
- **Release Evidence**: Readiness requires automated coverage for active logging, picker cancellation, recovery, bodyweight reps-only logging, failed persistence, and PR feedback, plus milestone manual device review of Train, Active Workout, and Add Exercise.
