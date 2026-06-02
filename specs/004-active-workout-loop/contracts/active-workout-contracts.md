# Contracts: Active Workout Logging Loop

## One-Tap Logging Contract

**Given** an active exercise block has an editable draft,
**when** the user taps Log,
**then** the draft enters pending state and no second confirm can run for that
draft until the first call completes.

**Given** persistence succeeds,
**when** the saved set is returned,
**then** the set appears in logged history with `loggedAt`, the draft clears its
pending state, and a new next-set draft is prepared.

**Given** persistence fails,
**when** the error returns,
**then** the draft remains unlogged with its values intact and an inline error.

## Bodyweight Logging Contract

**Given** an exercise block is bodyweight,
**when** the user logs reps with no weight,
**then** the set is valid and persists as bodyweight.

**Given** a bodyweight draft has negative added load,
**when** the user logs it,
**then** the draft remains unlogged with an inline validation error.

## Roller Field Contract

**Given** a roller field has a min, max, and step,
**when** tap-step or drag updates the value,
**then** the result clamps to range and snaps to a valid increment.

**Given** direct entry is used,
**when** parsing fails,
**then** the draft value is not changed and an inline error is shown.

## Add Exercise Contract

**Given** an active workout is open,
**when** the user selects a catalog exercise,
**then** the exercise is appended to the active workout and focus moves to the
new block.

**Given** adding fails,
**when** the error returns,
**then** the active workout remains unchanged and the error is shown inline.

## Recovery Contract

**Given** a focused exercise and draft values exist,
**when** active workout state is recreated from saved state,
**then** the same focused exercise and draft values are restored where the
exercise still exists.

**Given** the focused exercise no longer exists,
**when** state recovers,
**then** focus falls back to the first available exercise block.

## Validation Contract

Automated validation must cover:
- Bodyweight reps-only logging.
- Weighted validation errors.
- Duplicate pending confirm suppression.
- Inline error preservation after failure.
- Add-exercise focus handoff.
- Focus/draft recovery.
- Android build and iOS shared compile.
