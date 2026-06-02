# Feature Specification: Routine & Exercise Management

**Feature Branch**: `codex/014-routine-exercise-management`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Add the missing ability to create routines and manage exercises. Routines should be creatable from scratch, editable after they are saved, launchable from Train, and able to carry planned exercises, sets, bodyweight behavior, canonical weights, and rest settings. Exercise management should let users create, review, edit, and archive user-created exercises locally without disrupting fast workout logging."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create A Routine From Scratch (Priority: P1)

A lifter wants to set up a reusable routine before training, without first completing a workout. They can name the routine, add exercises from the catalog, configure planned sets, save it, and launch it from Train.

**Why this priority**: This closes the largest routine gap. Users already can launch templates and save from history; they also need a direct planning path.

**Independent Test**: Create a routine named "Push", add a weighted exercise and a bodyweight exercise, configure planned sets and rest, save, return to Train, launch the routine, and verify the active workout contains the planned exercises, sets, set kinds, weights, reps, and rest values.

**Acceptance Scenarios**:

1. **Given** the user is on Train with no routine draft open, **When** they create a routine, name it, add two exercises, configure planned sets, and save, **Then** the routine appears in the Train routine list and can be launched.
2. **Given** a routine draft contains a bodyweight exercise, **When** the user adds planned sets, **Then** those sets allow reps without requiring a weight.
3. **Given** a routine draft contains a weighted exercise, **When** the user saves planned sets, **Then** weights are stored canonically and displayed in the user's selected unit.

---

### User Story 2 - Edit Existing Routines (Priority: P2)

A lifter can update a saved routine as their program changes. They can rename it, add or remove exercises, update planned sets, and change rest settings without altering completed workout history.

**Why this priority**: Training plans change. Editing routines prevents users from deleting and recreating templates, while keeping the workout ledger immutable.

**Independent Test**: Start with an existing routine, edit the name, remove one exercise, add another, change set targets and rest duration, save, then launch and verify the active workout reflects the edited routine while completed workout history is unchanged.

**Acceptance Scenarios**:

1. **Given** a saved routine exists, **When** the user edits its name and saves, **Then** the routine list shows the updated name.
2. **Given** a saved routine has planned exercises, **When** the user removes one exercise and adds another, **Then** launching the routine uses the edited exercise list.
3. **Given** a routine came from a completed workout, **When** the user edits the routine, **Then** the source completed workout and history details remain unchanged.

---

### User Story 3 - Manage User-Created Exercises (Priority: P3)

A lifter can review custom exercises they created, correct mistakes, and archive ones they no longer want to see in pickers. Seeded exercises remain stable baseline catalog entries.

**Why this priority**: Custom exercise creation already exists in the picker, but users need a way to clean up names, bodyweight classification, and stale custom exercises.

**Independent Test**: Create a custom exercise, find it in exercise management, rename it, change bodyweight classification before it is used, confirm it appears in search with updated metadata, archive it, and verify it no longer appears in default picker results while historical workouts that already used it still display their snapshots.

**Acceptance Scenarios**:

1. **Given** a user-created exercise exists, **When** the user renames it, **Then** future search results use the new display name and existing logged workout snapshots remain unchanged.
2. **Given** a user-created exercise has not been used in logged history, **When** the user changes its bodyweight classification, **Then** future workout and routine set defaults reflect the updated classification.
3. **Given** a user-created exercise exists, **When** the user archives it, **Then** it is hidden from normal exercise selection and existing history remains readable.

---

### User Story 4 - Preserve Fast Logging From Management Flows (Priority: P4)

A lifter can enter routine or exercise management without losing an active workout, active draft, rest timer, or current Train context.

**Why this priority**: Planning features must not damage the gym-side logging experience.

**Independent Test**: Start an active workout with a draft and a running rest timer, open routine or exercise management, cancel or save changes, return to the active workout, and verify the focused draft, rest remaining time, and resume banner are still valid.

**Acceptance Scenarios**:

1. **Given** an active workout is running, **When** the user opens routine management and cancels, **Then** the active workout remains resumable with its focus and rest timer intact.
2. **Given** an active workout is running, **When** the user creates or edits a custom exercise outside the active picker, **Then** no unconfirmed set is logged, deleted, or mutated.
3. **Given** the app restarts after a routine draft is saved or canceled, **When** the user returns, **Then** active workout recovery works exactly as before this feature.

### Edge Cases

- Routine names are blank, duplicate, very long, or contain leading/trailing spaces.
- A routine draft has no exercises, or an exercise has no planned sets.
- Planned weighted sets have missing, zero, fractional, negative, or locale-formatted weights.
- Planned bodyweight sets have reps only, optional added load, or invalid negative load.
- Rest is disabled or set to zero for a routine exercise.
- A user archives a custom exercise that is currently in a routine.
- A user edits a custom exercise name after it has been used in completed history.
- Existing routines created before this feature have no draft metadata beyond their existing exercises and planned sets.
- Exercise search returns no results while building a routine.
- The app is offline, killed, or restarted while active workout state exists and management flows were recently used.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create a reusable routine from Train without first completing a workout.
- **FR-002**: Users MUST be able to name and rename routines, with blank names rejected and whitespace trimmed.
- **FR-003**: Users MUST be able to add catalog exercises and user-created exercises to a routine.
- **FR-004**: Users MUST be able to remove exercises from a routine draft before saving.
- **FR-005**: Users MUST be able to configure planned routine sets with set kind, reps, and optional canonical weight.
- **FR-006**: Bodyweight routine sets MUST allow reps-only planned sets and MUST NOT require a weight.
- **FR-007**: Weighted routine sets MUST preserve canonical weights and reject invalid negative weights.
- **FR-008**: Users MUST be able to configure or disable per-exercise rest for routine exercises.
- **FR-009**: Saving a routine MUST persist the complete edited routine locally and make it launchable from Train.
- **FR-010**: Editing a routine MUST update that routine without mutating completed workout history or logged set records.
- **FR-011**: Launching a created or edited routine MUST produce an active workout with the routine's current exercise order, planned sets, set kinds, weights, reps, bodyweight classification, and rest configuration.
- **FR-012**: Users MUST be able to cancel routine creation or editing without saving partial changes.
- **FR-013**: Users MUST be able to view user-created exercises separately from seeded baseline exercises.
- **FR-014**: Users MUST be able to create custom exercises from management flows and from the existing exercise picker.
- **FR-015**: Users MUST be able to rename user-created exercises.
- **FR-016**: Users MUST be able to archive user-created exercises so they no longer appear in normal picker and routine-builder search results.
- **FR-017**: Seeded exercises MUST remain protected baseline entries; this feature MUST NOT require editing or deleting seeded exercises.
- **FR-018**: Existing workouts, completed history, PRs, exports, and routine snapshots MUST remain readable after exercise edits or archives.
- **FR-019**: Routine and exercise management MUST be local-only/offline-first and shaped for future sync with stable identifiers and explicit mutation timestamps.
- **FR-020**: Opening, saving, or canceling routine and exercise management MUST NOT clear active workout focus, drafts, session recovery, or running rest timers.
- **FR-021**: Management controls MUST use the shared Neo-Glass Fit design system and remain usable with one hand on compact Android screens.
- **FR-022**: Validation failures MUST be shown before persistence succeeds and MUST NOT create partially saved routines or exercises.

### Key Entities *(include if feature involves data)*

- **Routine Draft**: A user-editable in-memory representation of a new or existing routine, including name, exercises, planned sets, rest settings, and validation errors.
- **Routine Exercise Draft**: A planned exercise in a routine draft, including catalog reference, display snapshot, bodyweight classification, order, rest configuration, and planned set drafts.
- **Routine Set Draft**: A planned set target with set kind, optional canonical weight, optional reps, and position.
- **Managed Exercise**: A catalog entry shown in exercise management, including display name, source type, bodyweight classification, archived state, and usage restrictions.
- **Exercise Edit Draft**: A validation state for creating or editing a user-created exercise.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create and save a two-exercise routine from scratch in under 90 seconds during manual validation.
- **SC-002**: A created or edited routine launches with 100% of planned exercise order, set kind, reps, weights, and rest settings preserved in automated tests.
- **SC-003**: Editing a routine leaves completed workout history and PR derivation unchanged in automated tests.
- **SC-004**: A user-created exercise can be created, renamed, searched, and archived locally in automated tests.
- **SC-005**: Archived user-created exercises are hidden from normal selection while historical snapshots remain readable in automated tests.
- **SC-006**: Active workout focus, draft state, and rest recovery still pass existing validation after management flows are used.
- **SC-007**: Routine and exercise management screens use design-system components and avoid Material 3 shared UI references.

## Assumptions

- Routine management lives under Train for this feature rather than adding a new top-level tab.
- Exercise management lives under Profile or a Profile-launched management surface because it is catalog maintenance, not gym-side logging.
- Reordering can be delivered through move-up/move-down controls or stable insertion order; drag-and-drop is not required for the first slice.
- Seeded exercise edits are out of scope to avoid migration and licensing ambiguity; users can create their own variant if they need different metadata.
- Routine draft autosave is out of scope; canceling a draft intentionally drops unsaved changes.
- Custom exercise bodyweight classification may be edited for future usage; historical active and completed workouts keep their display and set-kind snapshots.
- This feature does not add folders, routine sharing, program calendars, supersets, warm-up rules, or cloud sync.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Management flows are outside the one-tap logging path and must preserve active workout resume, focus, drafts, and rest timers.
- **Ledger Integrity**: Routine edits must not mutate completed workouts or logged sets; exercise edits must not rewrite historical display snapshots.
- **Recovery Behavior**: Active session recovery remains unchanged and must be validated after opening management flows.
- **Progress Promise**: PR history and completed workout evidence remain derived from immutable logged sets, not mutable routine or catalog drafts.
- **Local-First Ownership**: Routine and custom exercise changes are local, offline-capable, timestamped, and shaped for future sync.
- **Platform Scope**: Domain, repository, state holder, and shared Compose UI live in shared code; Android is validated first and iOS remains compile-safe through shared boundaries.
- **Design System & Accessibility**: UI uses FitTheme and design-system components only, with thumb-friendly controls, readable text, touch targets, TalkBack labels where supported, reduced-motion compatibility, and no Material 3 shared dependency.
- **Release Evidence**: Automated tests cover routine creation/edit/launch, exercise create/edit/archive, SQL persistence, session non-regression, and design-system guard; manual milestone validation covers compact Android navigation and routine creation ergonomics.
