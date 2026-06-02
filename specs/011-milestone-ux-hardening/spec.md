# Feature Specification: Milestone Validation and UX Hardening

**Feature Branch**: `codex/011-milestone-ux-hardening`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Create feature 011 milestone validation and UX hardening. Use the existing emulator/device setup to manually validate the core Oops All PRs loop across Train, active workout logging, exercise picker, keyboard entry, finish workout, History, templates, Progress, and Profile/export. Focus on fixing real device/emulator issues: overlap, thumb reach, excessive glow, touch target shape/ripple mismatch, keyboard behavior, accessibility labels/state descriptions, reduced-motion behavior, and any navigation dead ends. Treat this as release-readiness polish, not new product scope."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Validate the Core Logging Loop (Priority: P1)

A lifter can start or resume a workout, add exercises, log weighted and bodyweight sets, correct numeric values with the keyboard, and finish the workout without blocked controls, hidden next actions, or visual feedback that fights the button shape.

**Why this priority**: The app's product promise depends on the active workout loop feeling fast and reliable on an actual phone. New features are lower value until this path is trustworthy.

**Independent Test**: Starting from the Train destination on a Pixel-class Android target, a tester can start a fresh workout, add one seeded weighted exercise and one bodyweight exercise, log at least three sets, use direct numeric entry once, finish the workout, and capture evidence that every next action remains visible and reachable.

**Acceptance Scenarios**:

1. **Given** the app is opened with no active workout, **When** the tester starts a workout and adds a seeded exercise, **Then** the exercise picker remains scrollable, searchable, and dismissible, and the selected exercise appears in the active workout with the next logging action visible.
2. **Given** a weighted exercise has a next-set input, **When** the tester taps into reps or weight and types a replacement value, **Then** the numeric keyboard appears, the first typed character replaces the prior value, and the logged set reflects the corrected value after confirmation.
3. **Given** a bodyweight exercise has been added, **When** the tester logs sets, **Then** reps are the only required value and no weight entry is required in the basic logging path.
4. **Given** an active workout contains confirmed sets, **When** the tester finishes the workout, **Then** the app shows a completed summary or history path and does not leave the tester trapped in the active workout state.

---

### User Story 2 - Validate Cross-Destination Continuity (Priority: P2)

A returning user can move from a completed workout into History, save or launch a template, inspect Progress/PR evidence, and use Profile export without navigation dead ends or inconsistent local data.

**Why this priority**: History, templates, progress, and export prove the logged data is useful after the workout. They also close several previously deferred milestone gates.

**Independent Test**: After completing a workout during the P1 flow, a tester can view the workout in History, create or launch a template where available, inspect Progress for PR evidence, and trigger Profile export while screenshots and notes confirm continuity.

**Acceptance Scenarios**:

1. **Given** a completed workout exists, **When** the tester opens History, **Then** the completed workout is discoverable with readable set details and no overlap with system bars or navigation.
2. **Given** a completed workout can be used as a template source, **When** the tester saves or launches a template, **Then** the flow returns to a clear Train or active-workout state without copying completed timestamps as new logged sets.
3. **Given** logged sets may create records, **When** the tester opens Progress, **Then** PR evidence is readable, source information is inspectable, and bodyweight records remain reps-only.
4. **Given** local data exists, **When** the tester opens Profile and exports data, **Then** the export action reports a successful handoff or a recoverable platform-level limitation without mutating local workout history.

---

### User Story 3 - Validate Accessibility and Interaction Polish (Priority: P3)

A user relying on larger touch targets, reduced motion, state descriptions, or haptic alternatives can operate the main app loop without ambiguous controls or inaccessible visual-only feedback.

**Why this priority**: Public app-store quality requires accessible behavior, and the same checks also reveal thumb reach, shape, glow, and motion defects that affect gym use.

**Independent Test**: A tester reviews Train, active workout, picker, History, Progress, and Profile surfaces for touch target size, labels, state descriptions, reduced-motion behavior, non-haptic alternatives, and visual feedback shape consistency.

**Acceptance Scenarios**:

1. **Given** a primary control is tapped, **When** the interaction feedback appears, **Then** highlight/ripple/glow follows the same rounded shape as the visible control and does not render as a square artifact.
2. **Given** reduced motion is enabled in app preferences or the platform environment, **When** the tester navigates and interacts with major controls, **Then** motion is minimized without hiding state changes.
3. **Given** a screen reader inspects top-level destinations and primary actions, **When** controls receive focus, **Then** labels and states describe the action or current value without relying on visual-only cues.

### Edge Cases

- A newly created emulator or device shows a platform System UI warning while the app itself remains responsive.
- The exercise list is longer than one viewport and must remain scrollable even before search is used.
- The keyboard opens while the active workout input sits near the bottom of the screen.
- A workout is already active when the user attempts to launch a template.
- Export is requested on a target with no share destination available.
- Profile settings, completed workouts, templates, and PRs are reviewed after app restart.
- Bodyweight exercises, fractional weight values, display unit changes, and locale decimal input remain valid during manual flows.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The milestone pass MUST validate the core flow from Train to active logging to workout finish on a Pixel-class Android target and record screenshots or notes for each major step.
- **FR-002**: The active logging UI MUST keep the next primary action visible and reachable while adding exercises, logging sets, editing numeric values, and finishing a workout.
- **FR-003**: The exercise picker MUST support scrolling through the full seeded catalog, searching, cancellation, and selecting recently or commonly used exercises without requiring a search for normal discovery.
- **FR-004**: Numeric set entry MUST use a numeric keyboard where available and MUST replace the selected value on first typed input instead of appending to an old value.
- **FR-005**: Bodyweight logging MUST remain reps-only in the basic logging path and MUST not require a weight value to confirm a set.
- **FR-006**: The finish workout path MUST be discoverable from the active workout surface and MUST lead to a completed-workout state that can be found again from History.
- **FR-007**: Navigation between Train, History, Progress, and Profile MUST avoid dead ends, trapped overlays, or states where the bottom navigation cannot recover the user.
- **FR-008**: Visual interaction feedback MUST match the visible shape of the control, avoid square highlight artifacts, and avoid excessive glow that overlaps neighboring content.
- **FR-009**: Primary workout controls MUST meet the app's touch target expectations and favor one-handed reach, especially for repeated logging and picker/search actions.
- **FR-010**: The milestone pass MUST verify top-level and primary workout controls have meaningful accessibility labels and state descriptions.
- **FR-011**: Reduced-motion and haptic-disabled preferences MUST provide clear non-haptic feedback and MUST not remove essential state changes.
- **FR-012**: History, template, Progress, and Profile/export flows MUST preserve local ledger integrity: manual review and hardening fixes must not silently mutate completed sets, PR sources, templates, or export snapshots.
- **FR-013**: The feature MUST update existing validation notes and task checkboxes for previously deferred manual gates that are completed by this milestone pass.
- **FR-014**: The feature MUST preserve offline-only behavior and must not add cloud, account, analytics, or network dependencies.

### Key Entities *(include if feature involves data)*

- **Milestone Validation Run**: A dated evidence set for manual review of the app's core flows, including target device/emulator, app build, scenarios attempted, screenshots, and findings.
- **UX Finding**: A concrete observed issue with location, reproduction steps, severity, expected behavior, and resolution status.
- **Hardening Fix**: A scoped change that resolves a UX finding without expanding product scope or changing ledger semantics.
- **Validation Evidence**: Screenshots, notes, and command results proving a flow was exercised and whether it passed or failed.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A tester completes the start-workout, add-exercise, log weighted set, log bodyweight set, direct numeric entry, and finish-workout flow on a Pixel-class Android target without encountering an unrecoverable navigation state.
- **SC-002**: Manual evidence shows no primary Train, active workout, picker, History, Progress, or Profile controls overlap the status bar, navigation bar, keyboard, or each other.
- **SC-003**: At least 90% of repeated workout logging actions in the milestone pass are reachable from the lower half of the screen or from an already-focused bottom sheet/input area.
- **SC-004**: Exercise picker review confirms the tester can browse beyond the first visible catalog section without search and can still use search to narrow results.
- **SC-005**: Visual review finds no square highlight artifacts or excessive glow bleed on primary buttons, segmented controls, toggles, or bottom navigation.
- **SC-006**: Existing automated Android/shared/iOS validation gates still pass after hardening fixes.
- **SC-007**: Previously deferred manual gates for workout logging and history/template flows are either completed with evidence or explicitly reclassified with a remaining reason.

## Assumptions

- The app is far enough along that this feature should prioritize hardening existing flows over adding new capabilities.
- Android remains the first runtime validation target; iOS remains first-class through shared behavior and compile validation.
- A Pixel-class emulator is acceptable for milestone evidence when a physical device is not actively connected.
- Platform System UI warnings from a fresh emulator are documented separately from app defects unless they block app operation.
- Existing seed exercises, local persistence, History, Progress, and Profile/export functionality are the baseline under review.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: The feature directly improves quick gym-side logging by validating thumb reach, next-action visibility, picker behavior, numeric entry, and finish flow.
- **Ledger Integrity**: Hardening fixes must not create, edit, or delete confirmed sets except through the normal user action being tested; completed workout, PR, template, and export evidence remain auditable.
- **Recovery Behavior**: Manual review includes app restart/resume where relevant and ensures active or completed state remains discoverable.
- **Progress Promise**: Progress and PR evidence are included in the milestone pass, including bodyweight reps-only records and display-unit behavior.
- **Local-First Ownership**: The pass validates offline local behavior and Profile/export handoff without introducing accounts, sync, or network dependencies.
- **Platform Scope**: Android is validated first on device/emulator; iOS remains covered through shared boundaries and compile validation.
- **Design System & Accessibility**: Fixes must use the Fit design system and verify touch targets, shape-consistent feedback, labels/state descriptions, reduced motion, haptic alternatives, and adaptive layout.
- **Release Evidence**: Requires updated manual validation records, screenshots or notes, Android/shared/iOS automated gates, Material/design-system scan, whitespace check, and completed or reclassified deferred gate tasks.
