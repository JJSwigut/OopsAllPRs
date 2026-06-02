# Research: Active Workout Resume

## Decision: Use the persistent resume card as the only active-workout action surface on Train

**Rationale**: The screenshot shows three active-workout affordances: a top Resume button, a Start workout button that cannot succeed, and the lower active-workout card. Keeping the lower card as the only active-session surface matches the app shell recovery pattern and removes conflicting actions.

**Alternatives considered**:

- Keep the top Resume button and remove only Start workout. Rejected because the user specifically asked for only the lower card.
- Keep Start workout disabled. Rejected because a disabled action still occupies primary decision space while the user is mid-workout.

## Decision: Add direct Discard to the resume card

**Rationale**: The user asked to discard directly from the lower card. Existing active-workout discard logic already clears active state and notifications, so the Train card can route to that lifecycle path and refresh app state.

**Alternatives considered**:

- Open the active workout overlay and use its existing discard confirmation. Rejected because it adds navigation friction and does not satisfy direct discard from the card.
- Add a confirmation dialog on Train. Rejected for this slice because the requested behavior emphasized direct action and removing extra UI.

## Decision: Suppress active-session template launch conflicts on Train

**Rationale**: Template rows can remain visible for edit/delete, but they should not attempt to start another workout while one is active. If a start path is unavailable, the UI should avoid surfacing a conflict error that the user cannot resolve from that action.

**Alternatives considered**:

- Hide templates during an active workout. Rejected because template management remains useful and the user did not ask to remove the card.
- Keep rows startable and show an error. Rejected because the error is part of the problem.

## Decision: Refresh missing packaged seed exercises for existing installs

**Rationale**: Current catalog hydration only seeds when the catalog is empty. Existing installs therefore do not receive newly added seed exercises. The safer update path is to parse the packaged seed list, compare canonical names, and ingest only when one or more packaged seeds are missing. User-created exercises remain authoritative on canonical-name conflicts.

**Alternatives considered**:

- Require users to clear app data or reinstall. Rejected because existing users/testers should receive updated seed content.
- Re-ingest on every app hydration. Rejected because it creates unnecessary seed import churn and risks extra writes.
- Remove seed rows that are no longer packaged. Deferred because it requires a separate archive policy for existing references and is not needed to make new exercises visible.
