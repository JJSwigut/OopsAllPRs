# Contracts: Routine & Exercise Management

## Routine Builder State Contract

The shared routine state holder exposes:

- Open create draft.
- Open edit draft for an existing routine.
- Update routine name.
- Add exercise by catalog row/reference.
- Remove exercise.
- Add planned set to exercise.
- Remove planned set.
- Update planned set reps, weight, and kind.
- Adjust or toggle exercise rest.
- Save draft as a `ReusableRoutine`.
- Cancel draft without persistence.

Invariants:

- Save returns explicit success/failure.
- Failed save leaves draft open with an error.
- Successful save refreshes routine/template rows.
- Cancel does not mutate repository state.
- Save never mutates completed workouts or logged sets.

## Exercise Management State Contract

The shared exercise management state holder exposes:

- Load user-created exercises and optionally seed rows for read-only display.
- Search active exercises.
- Open create draft.
- Open edit draft for a user-created exercise.
- Update draft name and bodyweight classification.
- Save create or edit.
- Request archive and confirm/cancel archive.

Invariants:

- Seeded exercises are read-only.
- Archive hides user-created exercises from normal picker/search.
- Historical workout snapshots remain unchanged after edit/archive.
- All mutations return explicit success/failure.

## UI Contract

Routine management:

- Entry point appears on Train near routines/templates.
- Create/edit uses bottom-weighted controls suitable for one-handed compact Android use.
- Exercise selection reuses catalog search behavior and custom exercise creation where possible.
- Planned set rows expose stepper/direct-entry style controls consistent with active workout inputs.
- Rest controls match the routine rest timer feature.

Exercise management:

- Entry point appears under Profile.
- User-created exercises are clearly distinguished from seeded read-only exercises.
- Archive is a confirmable action.
- Create/edit form uses Fit components and token spacing/typography.

## Validation Contract

Automated validation must cover:

- Routine create -> save -> launch.
- Routine edit -> save -> launch.
- Routine edits do not mutate completed history.
- Custom exercise create -> edit -> search -> archive.
- Archived exercises hidden from selection.
- Active session/draft/rest non-regression after management flows.
- SQL persistence for routine edits and exercise update/archive.
