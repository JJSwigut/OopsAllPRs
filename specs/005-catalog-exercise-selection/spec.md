# Feature Specification: Catalog Exercise Selection

**Feature Branch**: `005-catalog-exercise-selection`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Implement catalog-backed exercise selection for active workouts. Replace the Quick Add placeholder with a shared Compose exercise picker that searches the existing seeded exercise catalog, supports selecting weighted and bodyweight exercises, appends the selected exercise to the active workout, moves focus to the new exercise block, preserves offline/local-only behavior, and includes a simple create-custom-exercise path for exercises missing from the seed list. Android should be validated first, with iOS kept first-class through shared state and UI boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Select a Seeded Exercise During a Workout (Priority: P1)

A lifter in an active workout opens Add Exercise, searches the local exercise
catalog, picks an exercise from the seeded list, and immediately gets a new
exercise block ready for logging.

**Why this priority**: The active workout loop is blocked by the current Quick
Add placeholder. Real catalog selection is required before workout logging
feels usable.

**Independent Test**: Can be fully tested by starting an active workout,
opening Add Exercise, searching for an existing seeded exercise, selecting it,
and confirming that the new block appears focused with a ready draft.

**Acceptance Scenarios**:

1. **Given** an active workout is open and the seeded catalog contains "Bench Press", **When** the user searches "bench" and selects Bench Press, **Then** the active workout gains a Bench Press block and focus moves to its next-set draft.
2. **Given** the user opens Add Exercise from an active workout, **When** the catalog has matching results, **Then** results show enough information to distinguish the exercise before selection.
3. **Given** an exercise is selected successfully, **When** the picker closes, **Then** the user remains in the active workout without losing any existing logged sets or drafts.

---

### User Story 2 - Preserve Bodyweight Logging From Catalog Metadata (Priority: P1)

A lifter selects a bodyweight movement from the catalog and gets a reps-only
draft by default, without being forced to enter external load.

**Why this priority**: Bodyweight reps-only logging is a core constitution and
foundation requirement; catalog selection must preserve that behavior.

**Independent Test**: Can be tested by selecting a known bodyweight seeded
exercise during an active workout and confirming the draft logs reps with no
weight.

**Acceptance Scenarios**:

1. **Given** the seeded catalog contains a bodyweight exercise, **When** the user selects it during an active workout, **Then** the created exercise block is marked bodyweight and its next draft does not require weight.
2. **Given** a bodyweight catalog exercise has been added, **When** the user logs reps only, **Then** the set is accepted and appears in logged history.

---

### User Story 3 - Create a Missing Exercise Locally (Priority: P2)

A lifter cannot find an exercise in the seeded catalog, so they create a simple
local exercise and add it to the active workout without leaving the logging
flow.

**Why this priority**: The seed list will never cover every user preference.
Local creation keeps the app useful without requiring cloud accounts or a
network dependency.

**Independent Test**: Can be tested by searching for a missing exercise,
creating it with a name and bodyweight/weighted classification, selecting it,
and confirming it remains searchable locally afterward.

**Acceptance Scenarios**:

1. **Given** a catalog search has no useful match, **When** the user creates a custom weighted exercise with a valid name, **Then** it is saved locally and appended to the active workout.
2. **Given** the user creates a custom bodyweight exercise, **When** it is appended to the active workout, **Then** its draft follows bodyweight reps-only rules.
3. **Given** the user later searches the same custom exercise name, **When** local catalog results load, **Then** the custom exercise appears in the results.

---

### User Story 4 - Keep Picker State Recoverable and Non-Destructive (Priority: P2)

A lifter can open, search, cancel, or encounter add failures without damaging
the active workout already in progress.

**Why this priority**: The picker is used inside an active workout, so errors
must not corrupt the workout ledger or discard drafts.

**Independent Test**: Can be tested by opening the picker, entering a search,
canceling, retrying add after a failure, and confirming existing workout state
is unchanged.

**Acceptance Scenarios**:

1. **Given** the picker is open with a search query, **When** the user cancels, **Then** the active workout remains unchanged and visible.
2. **Given** adding an exercise fails, **When** the error is shown, **Then** no exercise block is appended and existing logged rows and drafts remain intact.
3. **Given** the app is offline, **When** the user searches and selects catalog exercises, **Then** the flow continues using only local data.

### Edge Cases

- Empty query should show a sensible local default list rather than a blank or broken screen.
- No-result searches should offer custom exercise creation without forcing the user to abandon the workout.
- Duplicate exercise names should remain selectable without merging active workout blocks.
- Invalid custom exercise names, including blank or whitespace-only names, should be rejected without closing the picker.
- Catalog selection must not create a logged set; only the later Log action can add ledger rows.
- Existing active workout drafts must survive opening, searching, canceling, and failed adds.
- Offline behavior is mandatory; this feature must not require network, accounts, or remote exercise lookup.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to open an exercise picker from an active workout.
- **FR-002**: The picker MUST search the locally available exercise catalog by user-entered text.
- **FR-003**: The picker MUST provide useful default catalog results when opened with an empty query.
- **FR-004**: Users MUST be able to select a catalog exercise and append it to the active workout.
- **FR-005**: After successful selection, the active workout MUST focus the newly appended exercise block's next-set draft.
- **FR-006**: Selected catalog exercises MUST preserve their bodyweight versus weighted classification in the active workout.
- **FR-007**: Bodyweight selections MUST support reps-only logging without requiring weight.
- **FR-008**: Existing active workout exercise blocks, logged rows, and editable drafts MUST remain unchanged while browsing, canceling, or handling add failures.
- **FR-009**: The picker MUST show a no-results state for searches without useful matches.
- **FR-010**: Users MUST be able to create a simple custom local exercise from the no-results or create path.
- **FR-011**: Custom exercise creation MUST require a non-blank display name and a weighted/bodyweight classification.
- **FR-012**: Created custom exercises MUST be saved locally and be available in later local catalog searches.
- **FR-013**: Selecting or creating an exercise MUST NOT create a logged set or PR by itself.
- **FR-014**: Add failures and validation failures MUST be visible to the user and preserve picker and workout state for retry.
- **FR-015**: The feature MUST work fully offline using local seed and user-created exercise data.
- **FR-016**: The feature MUST remain usable on phone-sized Android screens and shared iOS UI boundaries.

### Key Entities *(include if feature involves data)*

- **Exercise Picker Session**: Temporary picker state for active workout id, query, results, selected mode, no-results state, creation draft, loading flag, and error message.
- **Catalog Exercise Result**: A local exercise option shown in picker results, including display name, stable id, bodyweight classification, and distinguishing metadata.
- **Custom Exercise Draft**: User-entered name and weighted/bodyweight classification for a locally created exercise.
- **Active Workout Exercise Block**: The appended exercise instance in the active workout, derived from a selected catalog result or created custom exercise.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can start from an active workout, search a seeded exercise, select it, and see the focused exercise block in 10 seconds or less during manual validation.
- **SC-002**: A bodyweight exercise selected from the catalog can be logged as reps-only with no validation error.
- **SC-003**: Canceling or failing an add operation leaves the count and content of existing active workout blocks and logged rows unchanged.
- **SC-004**: A custom exercise created locally appears in a later local search without app restart or network access.
- **SC-005**: Automated validation covers seeded search, bodyweight selection, custom exercise creation, cancel/no-op behavior, and add failure preservation.
- **SC-006**: Android build, shared unit tests, iOS shared compile, and Material scan all pass for the feature.

## Assumptions

- The existing seeded exercise list remains the baseline catalog.
- The initial custom exercise path only captures display name and weighted/bodyweight classification; richer metadata can come later.
- Duplicate active workout blocks for the same catalog exercise are allowed because lifters may repeat an exercise in separate supersets or sections.
- The first implementation can show local default results sorted by existing catalog order or display name.
- Cloud sync and remote catalog lookup remain future features and are not part of this slice.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Replaces Quick Add with fast local search and one-tap selection so lifters can add movements without leaving the logging flow.
- **Ledger Integrity**: Selection only appends active exercise blocks; confirmed set ledger rows are created only by explicit Log actions.
- **Recovery Behavior**: Picker open/cancel/failure must preserve active workout state and existing drafts; active workout focus moves only after successful append.
- **Progress Promise**: Bodyweight metadata and exercise identity remain suitable for later PR derivation, history, and progress evidence.
- **Local-First Ownership**: Seeded and custom exercises are local/offline; no accounts, network calls, or remote catalog dependencies.
- **Platform Scope**: Android is validated first while picker state and UI remain in shared code for iOS readiness.
- **Design System & Accessibility**: Picker uses existing FitTheme/design-system primitives with reachable touch targets, readable result rows, and reduced-motion-safe behavior.
- **Release Evidence**: Requires shared tests, Android build, iOS shared compile, Material scan, and a deferred/manual Android milestone before release.
