# Feature Specification: Mistake Recovery and Editing

**Feature Branch**: `codex/012-mistake-recovery-editing`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Add mistake recovery and editing for Oops All PRs. Users can edit or delete logged sets during an active workout, undo the most recently logged set, discard an active workout with confirmation, delete completed workouts from History, and delete saved templates from Train/Profile as appropriate. Preserve local-first SQLDelight integrity, PR recalculation, completed workout ledger consistency, bodyweight reps-only behavior, active session recovery, and Android-first validation with iOS kept first-class through shared domain/state boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Correct an Active Set (Priority: P1)

As a lifter logging quickly, I want to edit, delete, or undo a set I just logged so a tap mistake does not poison my workout history or PRs.

**Why this priority**: Logging trust depends on fast correction. The user should not have to abandon a workout because a rep or weight was entered wrong.

**Independent Test**: Start a workout, add weighted and bodyweight exercises, log sets, edit one logged set, delete another, undo the most recent logged set, restart the app, and verify the active workout ledger, draft focus, and PR feedback reflect only the remaining logged data.

**Acceptance Scenarios**:

1. **Given** a weighted logged set in an active workout, **When** the user edits reps or weight and confirms, **Then** the set row updates in place, keeps its logged identity and timestamp evidence, and PR feedback is recalculated.
2. **Given** a bodyweight logged set, **When** the user edits reps, **Then** the edit remains reps-only and does not require or invent a weight.
3. **Given** one or more logged sets in an active workout, **When** the user deletes a logged set, **Then** the set is removed from visible logged rows, the next draft remains usable, and the deletion survives app restart.
4. **Given** the user has just logged a set, **When** they choose undo, **Then** the most recently logged set in that active workout is removed without changing older logged sets.

---

### User Story 2 - Safely Abandon Bad Local Data (Priority: P2)

As a user who started the wrong workout or created the wrong template, I want clear destructive actions with confirmation so I can remove mistakes without accidental data loss.

**Why this priority**: Destructive actions are necessary but risky. They must be obvious, confirmed, and limited to the exact thing the user chose.

**Independent Test**: Start an active workout and discard it through confirmation, create a saved template and delete it, then verify Train, History, Progress, and app restart no longer surface the removed local data.

**Acceptance Scenarios**:

1. **Given** an active workout is in progress, **When** the user requests discard, **Then** a confirmation appears that names the consequence before any data is removed.
2. **Given** the user confirms discard, **When** the app returns to Train, **Then** there is no active workout resume state after restart.
3. **Given** a saved template exists, **When** the user deletes it and confirms, **Then** it disappears from Train and cannot be launched, while completed workout history remains intact.
4. **Given** a destructive confirmation is shown, **When** the user cancels, **Then** no workout, set, history item, template, or PR data changes.

---

### User Story 3 - Remove Completed Workout Mistakes (Priority: P3)

As a user reviewing history, I want to delete a completed workout that should not count so my History, Progress, PRs, and exports stay truthful.

**Why this priority**: Completed workout deletion affects durable history and PR evidence, so it comes after active logging corrections but must be supported before serious use.

**Independent Test**: Complete a workout that produces a PR, delete it from History through confirmation, and verify History, Progress, PR evidence, templates created from other workouts, and export output no longer include that completed workout.

**Acceptance Scenarios**:

1. **Given** a completed workout exists, **When** the user requests deletion from its summary, **Then** a confirmation appears before the completed workout is removed.
2. **Given** the user confirms completed workout deletion, **When** History and Progress refresh, **Then** that workout no longer appears and PRs are rebuilt from the remaining completed workouts.
3. **Given** a routine was created from a completed workout, **When** the completed workout is deleted, **Then** the routine remains reusable with its own snapshot data unless the user separately deletes the routine.
4. **Given** exports are requested after deletion, **When** export files are generated, **Then** deleted workouts and their deleted set rows are absent.

### Edge Cases

- Editing a logged set to invalid values must fail with a visible recoverable error and leave the previous logged set unchanged.
- Deleting the only logged set in an active workout must keep the active workout open with a usable next draft.
- Undo with no logged sets must be disabled or report that there is nothing to undo without changing state.
- Deleting or editing a set that has already been removed must return a recoverable not-found result.
- Discarding an active workout must clear active session state, active set drafts, rest timers, and resume banners.
- Completed workout deletion must rebuild PRs even if the deleted workout was the only source of a PR.
- Bodyweight reps-only sets must remain reps-only after edit, undo, delete, completed deletion, PR rebuild, and export.
- All recovery actions must work offline and survive app restart/process death after persistence succeeds.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to edit reps and, where applicable, weight for logged sets in an active workout.
- **FR-002**: Editing a logged set MUST validate the same weighted/bodyweight rules as initial logging and MUST not mutate the prior value if validation or persistence fails.
- **FR-003**: Edited logged sets MUST preserve their identity, logged-set status, and original logged timestamp evidence unless a future explicit re-log workflow is introduced.
- **FR-004**: Users MUST be able to delete an individual logged set from an active workout.
- **FR-005**: Users MUST be able to undo the most recently logged set in the active workout when at least one logged set exists.
- **FR-006**: Active set edits, deletes, and undo operations MUST update visible set rows, next draft position, focus, and inline PR feedback.
- **FR-007**: Active set edits, deletes, and undo operations MUST persist locally and restore correctly after app restart.
- **FR-008**: Users MUST be able to discard an active workout only after an explicit confirmation.
- **FR-009**: Discarding an active workout MUST remove active workout rows, active session state, active drafts, rest state, and active resume UI without touching completed history or saved templates.
- **FR-010**: Users MUST be able to delete saved templates/routines only after explicit confirmation.
- **FR-011**: Deleting a template MUST remove it from Train/template lists and prevent future launch without mutating completed workout history.
- **FR-012**: Users MUST be able to delete completed workouts from History only after explicit confirmation.
- **FR-013**: Deleting a completed workout MUST remove it from History, Progress evidence, PR derivation, and future exports.
- **FR-014**: Completed workout deletion MUST preserve independently saved templates unless the user separately deletes those templates.
- **FR-015**: PR history MUST be recalculated from remaining completed workouts after any active logged-set edit/delete that changes displayed active PR feedback and after completed workout deletion.
- **FR-016**: All destructive confirmations MUST have a cancel path that leaves data unchanged.
- **FR-017**: All recovery and deletion operations MUST be local-only, offline-capable, and explicit-success/failure based.
- **FR-018**: UI for recovery actions MUST use the shared design system, remain thumb-reachable in active workout flows, and expose readable labels for assistive technologies.
- **FR-019**: Android validation MUST be completed first; shared behavior MUST remain compatible with iOS through shared domain/state boundaries.

### Key Entities *(include if feature involves data)*

- **Logged Set Edit**: A user correction to reps, weight, or both for an already logged active set.
- **Logged Set Deletion**: Removal of a specific logged set from an active workout ledger before the workout is completed.
- **Undo Last Set**: A shortcut that resolves to the most recently logged set in the active workout and deletes it.
- **Discard Confirmation**: A reversible UI state that asks the user to confirm active workout removal.
- **Template Deletion**: Removal of a reusable routine/template without changing completed workout history.
- **Completed Workout Deletion**: Removal of a completed workout and its PR/export evidence from local history.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can correct an active logged weighted set in under 10 seconds from the active workout screen on a Pixel-class target.
- **SC-002**: Bodyweight set correction never requires a weight value in tests or manual validation.
- **SC-003**: Undo last set removes exactly one most-recent logged set and leaves older logged sets intact in automated tests.
- **SC-004**: Discarding an active workout removes resume state after app restart in automated tests and manual validation.
- **SC-005**: Deleting a completed workout rebuilds PR history from remaining completed workouts in automated tests.
- **SC-006**: Deleting a saved template removes it from Train without mutating the completed workout it came from in automated tests.
- **SC-007**: Android unit tests, Android debug build, iOS simulator compile, Material scan, whitespace validation, and a Pixel-class manual smoke pass complete for the slice.

## Assumptions

- Editing logged sets in this slice means correcting the existing set row, not creating an audit-history UI.
- Original `loggedAt` timestamps remain the evidence of when the set was logged; `updatedAt` reflects corrections.
- Template deletion applies to reusable routines/templates shown on Train; there is no separate routine editor yet.
- Completed workout deletion is hard local deletion for this offline app; future sync may need tombstones in a later feature.
- Rest timer cancellation on active discard uses existing lifecycle discard behavior.
- Full accessibility screen-reader review can build on the existing deferred accessibility pass, but labels and confirmation states must be present for new controls.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Corrections live near active logging and add fast undo/edit/delete paths without slowing normal set confirmation.
- **Ledger Integrity**: Logged set mutation happens only through explicit edit/delete paths with validation, persistence results, and PR/export recalculation.
- **Recovery Behavior**: Active correction, undo, delete, and discard results must recover after restart through local persistence.
- **Progress Promise**: PR feedback and Progress records rebuild when source logged sets or completed workouts change, including bodyweight reps-only records.
- **Local-First Ownership**: All recovery operations are offline, local, user-controlled, and future sync-aware through explicit mutation boundaries.
- **Platform Scope**: Android is validated first; domain, persistence, state, and shared UI live in shared code with no Android-only shortcut in business logic.
- **Design System & Accessibility**: UI uses Neo-Glass Fit components/tokens, thumb-reachable destructive confirmations, readable labels, cancel paths, and reduced-motion/haptic alternatives from existing settings.
- **Release Evidence**: Requires state/repository tests for edit/delete/discard/history/template deletion, Android build/tests, iOS compile, Material scan, whitespace validation, and manual Pixel-class smoke evidence.
