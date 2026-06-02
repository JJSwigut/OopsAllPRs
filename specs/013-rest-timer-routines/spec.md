# Feature Specification: Routine-Aware Rest Timers

**Feature Branch**: `013-rest-timer-routines`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Add rest time support like Hevy, Strong, and FitNotes: routines should be able to carry rest times, logging a set should auto-start rest, and the app should alert with sound when rest is done. Preserve local-first behavior, Android-first validation, and iOS-ready shared boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Auto-Start Rest After Logging (Priority: P1)

A lifter logs a set during an active workout and immediately sees a non-blocking rest timer start with the configured duration for that exercise.

**Why this priority**: The core workout loop is log set, recover, log next set. Rest must support that loop without slowing down set entry.

**Independent Test**: Start an empty workout, add an exercise with a rest duration, log a set, and verify the rest countdown appears, counts down from the configured duration, and does not block logging or editing the next set.

**Acceptance Scenarios**:

1. **Given** an active workout exercise has a 2-minute rest duration, **When** the user logs a set, **Then** a visible rest timer starts at 2 minutes and remains accessible while the user can continue logging.
2. **Given** a rest timer is running, **When** the user logs another set with a different configured rest duration, **Then** the timer restarts from the newly logged set's duration and identifies the latest set as the rest origin.
3. **Given** an exercise has rest disabled, **When** the user logs a set for that exercise, **Then** no rest timer starts automatically.

---

### User Story 2 - Save Rest Durations In Routines (Priority: P2)

A lifter configures rest durations while creating or saving a routine, then launches that routine and gets the same rest behavior during the workout.

**Why this priority**: Routines should reduce setup at the gym. If bench press needs longer rest than curls, the routine should remember that.

**Independent Test**: Save or edit a routine with different rest durations per exercise, launch it, log sets for each exercise, and verify each exercise starts the correct timer.

**Acceptance Scenarios**:

1. **Given** a routine includes bench press with 3 minutes rest and curls with 60 seconds rest, **When** the user launches the routine and logs a bench set, **Then** the rest timer starts at 3 minutes.
2. **Given** the same routine is active, **When** the user logs a curls set, **Then** the rest timer starts at 60 seconds.
3. **Given** a completed workout is saved as a routine, **When** the user accepts default rest values, **Then** the resulting routine includes rest settings for future launches.

---

### User Story 3 - Control Rest Without Losing Flow (Priority: P3)

A lifter can adjust or skip the current rest timer from the active workout screen without navigating away or losing focus.

**Why this priority**: Rest needs to adapt to real gym conditions without becoming a modal interruption.

**Independent Test**: Start a rest timer, add and subtract time, skip it, and verify the active workout remains focused on the current logging area.

**Acceptance Scenarios**:

1. **Given** rest is running, **When** the user taps +15 seconds, **Then** the remaining rest increases by 15 seconds.
2. **Given** rest is running, **When** the user taps -15 seconds, **Then** the remaining rest decreases by 15 seconds without going below zero.
3. **Given** rest is running, **When** the user taps Skip, **Then** the timer clears and no completion alert fires for that skipped rest.

---

### User Story 4 - Alert When Rest Ends (Priority: P4)

A lifter hears or feels an alert when rest completes, including when the app is backgrounded or the device is locked where platform permissions allow.

**Why this priority**: A rest timer only works if the user notices it while actually training.

**Independent Test**: Start a rest timer, background or lock the device, wait for completion, and verify the user receives an audible or haptic alert according to settings and platform permissions.

**Acceptance Scenarios**:

1. **Given** sound alerts are enabled and the device allows notifications, **When** rest reaches zero, **Then** the user receives a completion alert.
2. **Given** sound alerts are disabled but haptics are enabled, **When** rest reaches zero while the app is foregrounded, **Then** the user receives haptic feedback without sound.
3. **Given** platform notification permission is unavailable or denied, **When** rest completes in the background, **Then** the app clearly shows the completed rest state on return without corrupting the active workout.

---

### Edge Cases

- Rest duration is zero, disabled, or removed from an exercise after rest is already running.
- A logged set is edited or deleted while it is the origin of the current rest timer.
- The user finishes or discards a workout while rest is running.
- The app is killed, restarted, backgrounded, or the screen is locked while rest is running.
- The user changes the default rest setting after existing routines already have rest durations.
- The user launches an older routine that has no rest settings.
- Multiple rapid set logs occur before the previous rest timer finishes.
- Sound, vibration, and notification permissions differ across Android and iOS.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to configure a default rest duration for newly added workout and routine exercises.
- **FR-002**: Users MUST be able to configure a rest duration for each exercise in a routine.
- **FR-003**: Users MUST be able to disable automatic rest for any routine or active workout exercise.
- **FR-004**: Launching a routine MUST carry each exercise's rest configuration into the active workout.
- **FR-005**: Saving a completed workout as a routine MUST preserve or assign rest durations so the routine is ready for future launches.
- **FR-006**: Logging a set MUST automatically start rest when the logged exercise has automatic rest enabled.
- **FR-007**: Auto-started rest MUST be wall-clock anchored and recover with the correct remaining time after app restart or process death.
- **FR-008**: Rest MUST be visible from the active workout screen without blocking set logging, exercise selection, editing, deleting, undo, or finishing.
- **FR-009**: Users MUST be able to add time, subtract time, and skip the current rest timer.
- **FR-010**: Skipping, finishing, or discarding MUST cancel any pending rest completion alert.
- **FR-011**: If a new set is logged while rest is running, the active rest timer MUST reflect the latest logged set's rest duration and origin.
- **FR-012**: If the rest-origin set is deleted, the system MUST clear the timer or move it to the latest valid logged set without referencing deleted data.
- **FR-013**: When rest reaches zero, the system MUST alert the user using available sound, vibration, and notification capabilities according to user settings and platform permissions.
- **FR-014**: Users MUST be able to turn rest completion sound on or off independently from the existence of the visual rest timer.
- **FR-015**: Users MUST be able to continue using the app offline; rest timers, routine rest settings, and active rest state MUST remain local.
- **FR-016**: Existing workouts, completed history, templates, PRs, exports, and user-created exercises MUST remain valid when rest settings are introduced.
- **FR-017**: Older routines with no rest duration MUST receive a clear default behavior when launched.
- **FR-018**: Rest timer labels and controls MUST be accessible to screen readers and usable with one hand during an active workout.

### Key Entities *(include if feature involves data)*

- **Rest Configuration**: A duration and enabled state attached to an exercise within a routine or active workout.
- **Active Rest Timer**: A wall-clock anchored timer with start time, end time, origin workout, origin exercise, origin set, and alert state.
- **Rest Alert Preference**: User setting for whether rest completion should make sound, vibrate, both, or visual-only when platform capabilities allow.
- **Default Rest Preference**: User setting that applies to newly added routine and active workout exercises.
- **Routine Exercise**: A reusable exercise inside a routine that now carries its intended rest configuration.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can configure rest for an exercise, log a set, and see the timer start in under 3 seconds during manual validation.
- **SC-002**: Rest recovery after app restart shows remaining time within 2 seconds of wall-clock truth in automated tests.
- **SC-003**: Users can add time, subtract time, or skip rest with one tap from the active workout screen.
- **SC-004**: Launching a routine with at least two exercises starts the correct per-exercise rest duration for each exercise in automated tests.
- **SC-005**: Finishing or discarding a workout leaves no visible resume timer and no pending rest alert in automated tests.
- **SC-006**: Older routines and completed history remain visible and launchable after rest support is introduced.
- **SC-007**: Manual Android validation confirms an audible or haptic rest completion alert when platform permissions allow.

## Assumptions

- Rest duration is per exercise for this feature, not per individual set, because per-exercise rest covers the common Hevy/FitNotes flow and fits the current routine model.
- Warm-up/rest-by-set-type behavior is deferred until set types are introduced.
- A default rest of 2 minutes is acceptable for new weighted exercises unless the user changes it.
- Bodyweight exercises may use the same default rest behavior as weighted exercises unless the user disables or customizes it.
- Existing routines without rest settings will use the current default rest preference on first launch rather than being mutated retroactively.
- Background alerts depend on platform permissions and battery behavior; the app must degrade gracefully when the OS prevents sound or notification delivery.
- Cloud sync conflict behavior is out of scope, but rest data must be shaped so future sync can add conflict handling without changing user-visible meaning.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Auto-start rest removes manual timer setup and keeps controls in the active workout loop without blocking set logging.
- **Ledger Integrity**: Rest origin references must never point to deleted sets, and rest settings must not mutate completed workout history or PR evidence.
- **Recovery Behavior**: Active rest is wall-clock anchored and must recover after backgrounding, screen lock, restart, or process death.
- **Progress Promise**: Rest does not alter PR derivation, bodyweight rules, canonical weight units, or completed ledger values.
- **Local-First Ownership**: Rest settings, active rest state, and alert preferences remain local/offline-first and export-safe for future backup/sync work.
- **Platform Scope**: Android is validated first for notifications/sound; iOS remains first-class through shared timer state and platform alert boundaries.
- **Design System & Accessibility**: Rest UI must use the existing design system, thumb-reachable controls, accessible labels, reduced-motion-aware feedback, and adaptive layout.
- **Release Evidence**: Automated tests must cover routine rest carryover, auto-start, adjust/skip, restart recovery, discard/finish cancellation, old-routine compatibility, and Android alert behavior.
