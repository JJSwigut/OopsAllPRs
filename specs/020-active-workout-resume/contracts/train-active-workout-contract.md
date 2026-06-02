# UI Contract: Train Active Workout State

## No Active Workout

- Train may show Start workout.
- Train may show Templates.
- Tapping a template row starts a workout from that template.
- The lower active-workout resume card is absent.

## Active Workout

- Train does not show Start workout in the main content.
- Train does not show a top Resume workout button in the main content.
- Train may show Templates for review, edit, and delete.
- Tapping a template row must not start another workout and must not show a visible active-workout conflict error.
- The lower active-workout card is present.
- The lower active-workout card shows:
  - Active workout label/status
  - Resume action
  - Discard action

## Resume Action

- Opens the active workout overlay.
- Leaves the active session intact.

## Discard Action

- Discards the active workout directly.
- Clears active-session state.
- Removes the lower active-workout card after refresh.
- Leaves completed workout history and user-created exercises untouched.

## Seed Refresh Contract

- App hydration checks whether packaged seed exercise canonical names are missing locally.
- Missing packaged seeds are added to existing catalogs.
- User-created exercises are not overwritten.
- Duplicate canonical exercise names are not created.
