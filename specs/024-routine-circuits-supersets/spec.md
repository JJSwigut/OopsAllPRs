# Feature Specification: Routine Circuits & Supersets

**Feature Branch**: `024-routine-circuits-supersets`

**Created**: 2026-06-05

**Status**: Draft

**Input**: User description: "When I create a routine, I often do circuits or supersets. Add support for grouping routine exercises into circuits or supersets while preserving fast workout logging."

## Clarifications

### Session 2026-06-05

- Q: How should circuits and supersets be modeled for the first release? -> A: Ordered routine groups; 2 exercises are labeled superset, 3 or more are labeled circuit, and active workout logging remains exercise-row based.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Group Routine Exercises (Priority: P1)

A lifter creating or editing a routine can mark adjacent exercises as a superset or circuit so the saved routine reflects how they actually train.

**Why this priority**: The primary gap is in routine planning. Users need to capture that multiple exercises belong together before they launch the routine.

**Independent Test**: Create a routine with bench press, row, and curls; group bench press and row; save and reopen the routine; verify the two grouped exercises remain together with the correct group label and curls remains ungrouped.

**Acceptance Scenarios**:

1. **Given** a routine draft has at least two exercises, **When** the user groups two adjacent exercises, **Then** the draft shows those exercises as one superset and leaves other exercises ungrouped.
2. **Given** a routine draft has at least three adjacent exercises, **When** the user groups three or more exercises, **Then** the draft shows those exercises as one circuit.
3. **Given** a saved routine has grouped exercises, **When** the user reopens it for editing, **Then** group membership, group order, and exercise order are preserved.

---

### User Story 2 - Launch Grouped Routines (Priority: P2)

A lifter launches a routine with supersets or circuits and can see the grouping context during the active workout without losing the normal fast logging path.

**Why this priority**: Grouping only matters if the gym-side view carries enough context to follow the plan.

**Independent Test**: Launch a routine containing one superset and one circuit, verify active workout exercises appear in routine order with group labels, log sets normally, finish the workout, and verify the completed workout ledger remains ordered and complete.

**Acceptance Scenarios**:

1. **Given** a routine includes a superset, **When** the user launches it, **Then** the active workout labels the grouped exercises as a superset while preserving each exercise's planned sets and rest settings.
2. **Given** a routine includes a circuit, **When** the user launches it, **Then** the active workout labels the grouped exercises as a circuit while preserving routine order.
3. **Given** the user logs sets inside a grouped routine, **When** each set persists, **Then** logging, rest timers, PR feedback, and finish behavior remain unchanged from ungrouped routines.

---

### User Story 3 - Edit Or Remove Groups (Priority: P3)

A lifter can revise a routine when their program changes by removing grouping or regrouping exercises without deleting exercises or planned sets.

**Why this priority**: Grouping is planning metadata. Users should be able to fix it without recreating the routine.

**Independent Test**: Edit a saved routine with a superset, remove the group, save, relaunch, and verify all exercises and sets remain present but no group label appears.

**Acceptance Scenarios**:

1. **Given** a saved routine has a grouped pair, **When** the user removes the group and saves, **Then** the exercises remain in the routine as ungrouped exercises with their planned sets intact.
2. **Given** a draft has a group, **When** the user removes one grouped exercise, **Then** the remaining exercise is no longer presented as a one-exercise group.
3. **Given** a user edits group membership, **When** they cancel the editor, **Then** the saved routine remains unchanged.

### Edge Cases

- A user tries to create a group with fewer than two exercises.
- A user removes an exercise from a superset or circuit.
- A user reorders or deletes exercises inside a grouped routine draft.
- A routine has multiple separate groups.
- Existing routines created before this feature have no group metadata.
- A completed workout is saved as a routine and starts with no group metadata unless the user edits it.
- The app is offline, killed, or restarted after grouped routines have been saved.
- Exports, history, PRs, previous values, and completed workout details remain readable when routines gain grouping metadata.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to group two or more adjacent routine exercises while creating or editing a routine.
- **FR-002**: A group containing exactly two exercises MUST be labeled as a superset.
- **FR-003**: A group containing three or more exercises MUST be labeled as a circuit.
- **FR-004**: Users MUST be able to remove grouping from grouped routine exercises without removing those exercises or their planned sets.
- **FR-005**: Saving a routine MUST persist group membership, group order, exercise order, planned sets, set kinds, weights, reps, durations, and rest settings.
- **FR-006**: Editing a routine MUST preserve existing group metadata unless the user explicitly changes or removes it.
- **FR-007**: Removing exercises from a draft MUST leave no one-exercise group behind.
- **FR-008**: Launching a grouped routine MUST carry group labels and membership into the active workout while preserving current set logging behavior.
- **FR-009**: Group metadata MUST NOT mutate completed workout history, logged sets, PR derivation, previous workout defaults, or exports.
- **FR-010**: Existing routines with no group metadata MUST remain launchable and editable as ungrouped routines.
- **FR-011**: Group creation, removal, save, and launch MUST work offline and persist locally.
- **FR-012**: Group controls and labels MUST use the shared Neo-Glass Fit design system and remain usable on compact Android screens.
- **FR-013**: Validation failures MUST be shown before persistence succeeds and MUST NOT create partially saved groups.

### Key Entities *(include if feature involves data)*

- **Routine Exercise Group**: Planning metadata that identifies adjacent routine exercises as one superset or circuit through a stable group id and ordered position.
- **Grouped Routine Exercise**: A routine exercise that optionally belongs to a routine exercise group while retaining its own planned sets, rest, exercise catalog reference, and display snapshot.
- **Active Workout Group Context**: The group label and group id carried from a launched routine into active workout exercises for display only.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can group two routine exercises and save the routine in under 20 seconds during manual validation.
- **SC-002**: Saved grouped routines reopen with 100% of group membership, group labels, exercise order, planned sets, and rest settings preserved in automated tests.
- **SC-003**: Launching a grouped routine carries group labels into active workout state without increasing the number of taps required to log a planned set.
- **SC-004**: Existing ungrouped routines remain launchable and editable in automated tests.
- **SC-005**: Removing a group preserves all exercises and planned sets in automated tests.
- **SC-006**: Completed workout history, PR derivation, previous values, and export tests pass unchanged after group metadata is introduced.

## Assumptions

- This feature models grouping as routine planning metadata, not as a new exercise type or a separate workout lifecycle.
- Grouping is limited to adjacent exercises in routine order for the first release.
- Drag-and-drop grouping is out of scope; a deterministic state-holder API and compact controls are sufficient for the first slice.
- Group-specific rest, round count, automatic exercise rotation, and per-round completion are out of scope.
- Completed workouts do not need to persist group metadata in this slice; active workout group context is used only to help users follow a launched routine.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Group labels add context but do not change the one-tap set logging path or require modal interaction during active workouts.
- **Ledger Integrity**: Grouping is routine metadata; logged sets and completed workout records remain independent, durable, and auditable.
- **Recovery Behavior**: Saved grouped routines persist locally; active workout recovery carries display group context without changing timer anchors or set persistence rules.
- **Progress Promise**: PRs, history, last-set values, bodyweight behavior, unit conversion, and previous workout defaults remain based on logged sets.
- **Local-First Ownership**: Group metadata is local, offline-capable, timestamped through routine updates, and shaped with stable ids for future sync.
- **Platform Scope**: Domain, persistence, state, and shared UI live in shared code; Android is validated first and iOS remains compile-safe through shared KMP boundaries.
- **Design System & Accessibility**: Group labels and controls use FitTheme and design-system components, with compact touch targets and clear accessible text.
- **Release Evidence**: Automated tests cover model mapping, state-holder grouping, routine persistence, launch carryover, old-routine compatibility, and ledger regressions.
