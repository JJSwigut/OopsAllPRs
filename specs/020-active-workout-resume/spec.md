# Feature Specification: Active Workout Resume

**Feature Branch**: `020-active-workout-resume`

**Created**: 2026-06-01

**Status**: Draft

**Input**: User description: "When an active workout exists, remove redundant Start workout actions from the Train home page, keep only the lower active-workout resume card, add a Discard action on that card so users can intentionally discard the active workout, and eliminate the error path where Start workout reports that an active workout is already in progress. Also verify whether curated exercise CSV updates apply only on fresh seed or update existing installs."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Resume or discard active workout from one card (Priority: P1)

A lifter who already has an active workout sees one clear active-workout card on the Train screen. They can resume the workout or intentionally discard it from that card, without seeing a separate Start workout button or an error that another workout is already active.

**Why this priority**: This removes conflicting calls to action from the primary workout entry screen and keeps the active-session recovery path obvious during a workout.

**Independent Test**: Start a workout, return to the Train screen, and verify that the only active-workout action surface is the bottom active-workout card with Resume and Discard actions.

**Acceptance Scenarios**:

1. **Given** an active workout exists, **When** the user opens Train, **Then** the Train content does not show a Start workout button.
2. **Given** an active workout exists, **When** the user opens Train, **Then** the lower active-workout card shows Resume and Discard actions.
3. **Given** an active workout exists, **When** the user taps Resume on the lower active-workout card, **Then** the active workout opens.
4. **Given** an active workout exists, **When** the user taps Discard on the lower active-workout card, **Then** the active workout is removed and the Train screen returns to the no-active-workout state.
5. **Given** an active workout exists, **When** the user interacts with templates on Train, **Then** the app does not show an "active workout already in progress" error on the home screen.

---

### User Story 2 - Existing installs receive new seed exercises (Priority: P2)

A user who already has an exercise catalog on their device receives newly added foundational seed exercises after updating the app, while their custom exercises remain intact.

**Why this priority**: The curated seed catalog is user-visible product content. Existing testers and users should not need to wipe app data to see newly added exercises such as Dead Hang.

**Independent Test**: Simulate an existing catalog missing a newly added seed exercise, hydrate the app, and verify that the missing seed exercise appears without removing user-created exercises.

**Acceptance Scenarios**:

1. **Given** the local catalog already contains exercises but is missing a current seed exercise, **When** the app hydrates, **Then** the missing seed exercise is added to the catalog.
2. **Given** the local catalog contains a user-created exercise, **When** seed refresh runs, **Then** the user-created exercise remains unchanged.
3. **Given** the local catalog already contains all current seed exercises, **When** the app hydrates, **Then** no duplicate seed exercises are created.

### Edge Cases

- If no active workout exists, the Train screen still shows the ordinary Start workout path.
- If templates exist while a workout is active, template rows remain visible for management, but starting another workout from them must not create a visible conflict error.
- If discarding fails, the active-workout card remains available and the user can retry or resume.
- If the app restarts after discard, no active workout resume card appears for the discarded workout.
- If a seed exercise name matches a user-created exercise, the user-created exercise takes precedence and is not overwritten.
- If an existing seed exercise already exists, seed refresh must not create a second catalog entry for the same canonical name.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When an active workout exists, the Train screen MUST hide all Start workout actions from the main Train content.
- **FR-002**: When an active workout exists, the lower active-workout card MUST include a Resume action and a Discard action.
- **FR-003**: The Resume action on the lower active-workout card MUST open the active workout.
- **FR-004**: The Discard action on the lower active-workout card MUST discard the active workout and refresh Train so no stale resume state remains.
- **FR-005**: The Train screen MUST NOT show an "active workout already in progress" error as a result of hidden or unavailable start actions.
- **FR-006**: When an active workout exists, template list actions MUST NOT start a second workout or surface a home-screen conflict error.
- **FR-007**: When no active workout exists, the Train screen MUST preserve the ordinary Start workout and template-start behavior.
- **FR-008**: Existing installs MUST receive seed exercises that are present in the packaged seed catalog but missing locally.
- **FR-009**: Seed refresh MUST preserve user-created exercises, including user-created exercises whose canonical names match seed exercises.
- **FR-010**: Seed refresh MUST avoid duplicate canonical exercise names.

### Key Entities

- **Active Workout**: The currently resumable workout session, including its identity, start time, and recovery state.
- **Active Workout Resume Card**: The persistent Train-screen card that presents active-workout status and recovery actions.
- **Exercise Catalog Item**: A seeded or user-created exercise available for selection.
- **Seed Catalog**: The packaged foundational exercise list used to initialize or refresh local seeded exercises.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: In an active-workout state, users see exactly one active-workout action surface on Train: the lower active-workout card.
- **SC-002**: In an active-workout state, users can resume or discard the active workout from Train in one tap.
- **SC-003**: In an active-workout state, tapping visible Train controls produces no "active workout already in progress" message.
- **SC-004**: Existing catalogs missing current seed exercises receive those exercises after app hydration without duplicate canonical names.
- **SC-005**: User-created exercises remain present and unmodified after seed refresh.

## Assumptions

- Discard from the lower active-workout card is intentional and direct, matching the requested "discard directly from there" behavior.
- Existing seed exercises may be refreshed or supplemented, but user-created exercises are never overwritten.
- The normal active-workout overlay can keep its existing discard confirmation behavior; this feature only changes the Train-screen resume card.
- Seed exercises removed from the packaged curated list do not need to be removed from existing installs in this slice.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Reduces conflicting actions on Train and keeps resume as the obvious next action during an active workout.
- **Ledger Integrity**: Discard remains an explicit active-workout lifecycle action; completed history and user-created exercises must not be silently mutated.
- **Recovery Behavior**: Train must reflect active-session state after hydration, discard, and app restart.
- **Progress Promise**: Discarding an active workout removes uncompleted active work only; completed PR/history behavior is unchanged. Seed refresh improves exercise availability.
- **Local-First Ownership**: All behavior works offline against local catalog and active-session state.
- **Platform Scope**: Shared UI/state behavior applies to Android and iOS; Android remains the first manual verification target.
- **Design System & Accessibility**: Resume and Discard actions use existing Fit design-system components, touch targets, and labels.
- **Release Evidence**: Requires shared state tests, Android unit/build validation, and manual verification on the connected Android device.
