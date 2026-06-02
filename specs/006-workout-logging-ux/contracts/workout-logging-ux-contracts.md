# Contracts: Workout Logging UX V1

## Train Entry Contract

**Given** no active workout exists,
**when** Train is shown,
**then** Start workout is the first primary action and no title or descriptive
copy competes with it.

**Given** an active workout exists,
**when** Train is shown,
**then** Resume is visible with enough context to identify the active session.

## Focused Active Mode Contract

**Given** the user starts or resumes a workout,
**when** active logging opens,
**then** the screen enters focused workout mode with compact Close and no
visible top-level navigation competing with logging.

**Given** active logging is open,
**when** the user closes it,
**then** the active session remains resumable from Train unless it was
explicitly finished or discarded.

## Next Set Contract

**Given** an exercise has no logged rows,
**when** its block appears,
**then** the next-set draft uses sensible defaults for that exercise type.

**Given** an exercise has logged rows,
**when** a new draft appears,
**then** reps and weight default from the latest relevant logged set.

**Given** the user edits a draft,
**when** the app backgrounds or restarts,
**then** the edited draft values recover where the exercise still exists.

## Ledger Confirmation Contract

**Given** the user taps Log set,
**when** confirmation begins,
**then** the draft becomes pending and duplicate confirms are ignored.

**Given** persistence succeeds,
**when** the saved set returns,
**then** the set appears as a logged row with `loggedAt`, inline PR feedback is
derived if applicable, and the next draft is prepared.

**Given** persistence fails,
**when** the error returns,
**then** no logged row appears, the draft values remain editable, and a retry
path is visible.

## Bodyweight Contract

**Given** an exercise is bodyweight,
**when** the next-set input is shown,
**then** reps are the only required field.

**Given** a bodyweight draft has positive reps and no weight,
**when** the user confirms it,
**then** the set logs successfully as bodyweight.

**Given** added load is supported later,
**when** the user stays in the default bodyweight flow,
**then** added load controls remain hidden.

## Add Exercise Contract

**Given** active logging is open,
**when** Add Exercise is selected,
**then** a search-first picker opens with local results and no unrelated app
destinations.

**Given** a result is selected,
**when** append succeeds,
**then** the picker closes, the new exercise block appears, and focus moves to
its next set.

**Given** selection or custom creation fails,
**when** the error returns,
**then** the picker remains open and active workout state is unchanged.

**Given** the picker is canceled,
**when** active logging resumes,
**then** exercise blocks, logged rows, drafts, and focus match the pre-picker
state.

## Inline PR Contract

**Given** a confirmed set improves a record,
**when** it appears in logged rows,
**then** an inline PR marker identifies the improvement without blocking the
next action.

**Given** a confirmed set does not improve a record,
**when** it appears in logged rows,
**then** no distracting PR decoration appears.

**Given** the app restarts during an active workout,
**when** active logging hydrates,
**then** PR feedback can be recomputed for logged rows using durable workout
and progress data.

## Visual Manual Review Contract

Manual milestone review must cover:
- Pixel-class phone portrait.
- Train with and without active workout.
- Active workout empty state.
- Weighted exercise with multiple logged sets and next-set defaults.
- Bodyweight exercise with reps-only next set.
- Add Exercise with keyboard visible.
- Long exercise names.
- Failed set retry state.
- Restart during active workout with logged rows and edited draft.

Passing review means no primary controls overlap the status bar, navigation
bar, keyboard, or each other, and the tester can complete the core flow without
leaving active workout mode.
