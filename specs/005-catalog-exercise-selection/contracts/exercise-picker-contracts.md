# Contracts: Catalog Exercise Selection

## Picker Open Contract

**Given** an active workout id exists,
**when** Add Exercise is invoked,
**then** the picker opens with default local catalog results and no active
workout mutation.

## Local Search Contract

**Given** the picker is open,
**when** the query changes,
**then** results are refreshed from local catalog data only.

**Given** the query has no matches,
**when** results load,
**then** the picker shows a no-results state and keeps custom creation
available.

## Selection Contract

**Given** a result row is selected,
**when** add succeeds,
**then** the exercise is appended to the active workout, focus moves to the
new block, and the picker closes.

**Given** selection fails,
**when** the error returns,
**then** the picker remains open, the error is visible, and the active workout
is unchanged.

## Bodyweight Contract

**Given** the selected catalog result is bodyweight,
**when** the exercise block is created,
**then** the next draft uses bodyweight rules and can log reps with no weight.

## Custom Creation Contract

**Given** the user enters a valid custom exercise name and classification,
**when** creation succeeds,
**then** the exercise is saved locally, appended to the active workout, and
appears in later searches.

**Given** the custom exercise name is blank,
**when** the user attempts creation,
**then** no local exercise is saved, no active workout block is appended, and
an inline validation error is shown.

## Cancel Contract

**Given** the picker is open with any query or custom draft,
**when** the user cancels,
**then** the picker closes and active workout exercise blocks, logged rows, and
drafts remain unchanged.
