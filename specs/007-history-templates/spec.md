# Feature Specification: Completed Workout History and Templates

**Feature Branch**: `codex/007-history-templates`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Build feature 007 completed workout history and reusable templates. After finishing a workout, the user should be able to review a completed workout summary with duration, exercises, logged sets, bodyweight reps-only sets, and PR markers. The History tab should show completed workouts in a useful list with detail views. The user should be able to save a completed workout as a reusable template with a local name, see templates from Train, launch a template into a new active workout with planned sets but no logged timestamps, and keep templates separate from completed history. Keep everything local-only/offline-first, Android implemented and validated first, iOS first-class through shared KMP state and Compose boundaries, and use the existing design-system module only."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Review a Finished Workout (Priority: P1)

A lifter finishes an active workout and immediately sees what was recorded: duration, exercises, completed sets, bodyweight reps-only work, and any PR markers.

**Why this priority**: Finishing is the handoff from fast logging to durable history. If the user cannot inspect what was saved, the ledger feels incomplete.

**Independent Test**: A tester can complete a workout with one weighted exercise and one bodyweight exercise, finish it, and review a completed workout summary that matches the logged sets.

**Acceptance Scenarios**:

1. **Given** an active workout has logged weighted and bodyweight sets, **When** the user finishes the workout, **Then** the app shows a completed workout summary with duration, exercises, and all logged sets.
2. **Given** a completed workout contains a bodyweight reps-only set, **When** the summary is reviewed, **Then** the set is displayed without requiring or inventing a weight value.
3. **Given** a logged set produced PR feedback during the workout, **When** the completed workout summary is shown, **Then** the PR marker remains visible in the relevant set context.
4. **Given** the completed workout had unlogged planned sets, **When** the summary is shown, **Then** only logged sets appear in completed history.

---

### User Story 2 - Browse Workout History (Priority: P2)

A lifter opens History and can scan recent completed workouts, then open a useful detail view for any workout.

**Why this priority**: History is the durable proof that the app recorded real work. It also becomes the source for template creation and later progress review.

**Independent Test**: A tester can complete two workouts, open History, see both workouts in reverse chronological order, and open each detail without losing completed data.

**Acceptance Scenarios**:

1. **Given** completed workouts exist, **When** the user opens History, **Then** workouts are listed with date, duration, exercise count, set count, and PR indicator when applicable.
2. **Given** the user selects a history item, **When** the detail opens, **Then** the completed workout detail shows the same exercises and logged sets as the finish summary.
3. **Given** no completed workouts exist, **When** the user opens History, **Then** the empty state clearly indicates that finished workouts will appear there without blocking Train.
4. **Given** the app restarts after workouts have been completed, **When** History is opened, **Then** the completed workout list and details are restored from local data.

---

### User Story 3 - Save a Workout as a Template (Priority: P3)

A lifter can turn a completed workout into a reusable template by naming it locally.

**Why this priority**: Templates make repeated training faster without asking the user to pre-plan before the first workout.

**Independent Test**: A tester can finish a workout, save it as a named template, and see the new template available from Train without changing the completed workout.

**Acceptance Scenarios**:

1. **Given** a completed workout detail is open, **When** the user chooses to save it as a template and enters a valid name, **Then** a reusable template is created locally from the completed exercises and logged set values.
2. **Given** a completed workout is saved as a template, **When** the user returns to History, **Then** the completed workout remains unchanged and inspectable.
3. **Given** the user enters a blank template name, **When** they attempt to save, **Then** the app shows a validation error and does not create a template.
4. **Given** the user already has a template with the same name, **When** another completed workout is saved using that name, **Then** the app allows the save and treats the templates as separate local records.

---

### User Story 4 - Launch a Template From Train (Priority: P4)

A lifter can start a new active workout from a saved template, with planned sets ready to log but no logged timestamps copied from history.

**Why this priority**: Launching a template closes the repeat-workout loop and keeps future sessions fast.

**Independent Test**: A tester can save a completed workout as a template, launch it from Train, and confirm the new active workout has the template exercises and planned set defaults but no completed logged rows.

**Acceptance Scenarios**:

1. **Given** one or more templates exist, **When** the user opens Train, **Then** the templates are visible or reachable without leaving the primary workout-starting flow.
2. **Given** the user launches a template and no active workout exists, **When** the active workout opens, **Then** it contains the template exercises and planned set targets ready for logging.
3. **Given** a template was created from logged sets, **When** it is launched, **Then** the new active workout does not copy logged timestamps, completed history ids, or PR markers from the source workout.
4. **Given** an active workout already exists, **When** the user attempts to launch a template, **Then** the app offers resume/finish/discard choices instead of silently creating a second active session.

### Edge Cases

- Completed workout summaries remain useful when a workout has one exercise, many exercises, no PRs, only bodyweight sets, or mixed weighted and bodyweight work.
- Workouts with zero logged sets are either not offered for template creation or save as an empty template only through an explicit non-default path.
- Very long exercise names and many logged sets remain readable without overlapping controls or hiding primary actions.
- Template creation failure preserves the entered name and does not duplicate templates.
- Launching a template after app restart uses local template data and works without network access.
- Deleting or editing completed history is out of scope for this feature and must not be implied by the UI.
- Existing workout history, PR history, exercise catalog entries, custom exercises, exports, and active workout state are not changed except through explicit finish, save-template, or launch-template actions.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST show a completed workout summary after a workout is successfully finished.
- **FR-002**: A completed workout summary MUST include workout duration, finish time, exercise names, logged set count, and per-exercise logged set details.
- **FR-003**: Completed workout summaries MUST preserve bodyweight reps-only sets without inventing or requiring a weight value.
- **FR-004**: Completed workout summaries MUST show PR markers for sets that were identified as personal records in the active workout or completed workout evidence.
- **FR-005**: Completed history MUST include only logged sets and MUST exclude unlogged planned sets.
- **FR-006**: The History tab MUST list completed workouts in reverse chronological order.
- **FR-007**: Each History list row MUST expose enough information to identify the workout without opening it: date or relative date, duration, exercise count, set count, and PR presence.
- **FR-008**: Users MUST be able to open a completed workout detail from History.
- **FR-009**: Completed workout details MUST match the completed workout summary data for the same workout.
- **FR-010**: History list and detail data MUST restore from local storage after app restart.
- **FR-011**: Users MUST be able to save a completed workout as a reusable template with a user-entered local name.
- **FR-012**: Template names MUST reject blank or whitespace-only input.
- **FR-013**: Saving a completed workout as a template MUST copy exercise order, exercise identity snapshots, and logged set values as future planned set targets.
- **FR-014**: Saving a completed workout as a template MUST NOT mutate the completed workout record.
- **FR-015**: Reusable templates MUST remain distinct from completed workout history, even when created from completed workouts.
- **FR-016**: Train MUST show or provide direct access to saved templates without making template launch compete with the primary empty-workout start action.
- **FR-017**: Users MUST be able to launch a saved template into a new active workout when no active workout exists.
- **FR-018**: Launching a template MUST create planned sets ready to log and MUST NOT copy logged timestamps, completed workout ids, completed exercise ids, or PR markers into the active workout.
- **FR-019**: Launching a template MUST preserve bodyweight reps-only planned set behavior for bodyweight exercises.
- **FR-020**: The app MUST prevent silent creation of a second active workout when launching a template while another active workout exists.
- **FR-021**: Template creation and launch failures MUST be visible, retryable, and non-destructive.
- **FR-022**: The feature MUST work fully offline using local completed workout, template, exercise, and PR data.
- **FR-023**: The UI MUST support one-handed review and template launch on phone-sized screens with readable text, large touch targets, and no overlapping primary controls.
- **FR-024**: The feature MUST preserve export-ready completed workout and template data for future user-controlled export flows.

### Key Entities *(include if feature involves data)*

- **Completed Workout Summary**: The post-finish view of a completed workout, including timing, exercise count, set count, logged set details, and PR markers.
- **Completed Workout List Item**: A compact History row representing one completed workout with enough scan data to choose it.
- **Completed Workout Detail**: The full read-only view of a completed workout, equivalent to the finish summary and restorable from local history.
- **Reusable Template**: A named local workout template created from completed workout evidence or existing template data.
- **Template Exercise**: An ordered exercise entry within a template, including display name snapshot, catalog identity, bodyweight classification, and planned set targets.
- **Template Launch**: The creation of a new active workout from a reusable template without copied logged timestamps or completed history identity.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: After finishing a workout with at least two exercises, a tester can verify the completed workout summary matches the logged sets in under 15 seconds.
- **SC-002**: A tester can complete two workouts, open History, and find both workouts in reverse chronological order without using instructions.
- **SC-003**: A tester can open a completed workout detail and confirm bodyweight reps-only sets display without a required weight field.
- **SC-004**: A tester can save a completed workout as a named template in under 20 seconds from the completed workout detail.
- **SC-005**: A tester can launch a saved template from Train and reach the active workout logger with planned sets ready in under 10 seconds.
- **SC-006**: Automated validation proves launched templates contain no copied logged timestamps or completed-history identifiers.
- **SC-007**: After app restart, completed history and saved templates remain visible and launchable without network access.
- **SC-008**: Android build, shared behavior tests, iOS shared compile, and milestone manual review evidence are recorded for the feature.

## Assumptions

- A completed workout can be saved as a template multiple times, and duplicate template names are allowed because templates have stable identities.
- The template created from a completed workout uses the completed logged set values as planned targets for the next workout.
- Template editing, template deletion, completed workout deletion, and completed workout editing are out of scope for this slice.
- Template launch is blocked by an existing active workout unless the user explicitly resolves the active session first.
- PR markers shown in completed summaries can be derived from existing PR evidence or recomputed from completed workout data when needed.
- Android remains the first manual validation target; iOS remains first-class through shared domain, state, and Compose boundaries.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Templates shorten repeat workout start, and Train must keep empty-start primary while making template launch quickly reachable.
- **Ledger Integrity**: Completed workout details are read-only; template creation copies planned targets without mutating logged history; template launch never copies logged timestamps into new active work.
- **Recovery Behavior**: History, completed summaries, template creation state, and template availability must survive restart using local durable data.
- **Progress Promise**: Completed summaries and History preserve PR markers and make progress evidence inspectable in context.
- **Local-First Ownership**: History and templates work offline from local data, preserve export-ready user-owned records, and avoid account or cloud assumptions.
- **Platform Scope**: Android is validated first while business rules, state, and shared UI remain viable for iOS.
- **Design System & Accessibility**: History, summary, template naming, and Train template launch use the shared token-driven design system with large touch targets, readable dynamic type, assistive labels, reduced motion, and non-haptic alternatives.
- **Release Evidence**: Automated tests must cover completed summary mapping, history restore, template save validation, template launch separation, bodyweight reps-only preservation, and no copied logged timestamps; milestone manual review covers finish summary, History, save-template, and launch-template flows.
