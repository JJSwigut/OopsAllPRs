# Data Model: Timed Exercise PRs

## SetKind.TIMED

New set kind for duration-first work.

Rules:

- Timed sets require positive `durationMs`.
- Timed sets do not require `reps` or `weight`.
- Non-timed set validation remains unchanged.
- Existing weighted/bodyweight rows remain valid with `durationMs = null`.

## ExerciseLoggingMode

Catalog/reference classification used to choose the default logging path.

Values:

- `WEIGHTED`: Default weighted logging.
- `BODYWEIGHT`: Reps-only bodyweight logging.
- `TIMED`: Duration-first timed logging.

Fields on catalog/reference models:

- `loggingMode`: Canonical mode used when adding an exercise to an active workout or routine.
- `isBodyweight`: Preserved for existing bodyweight behavior and filtering.

Validation:

- Seeded and user-created timed exercises must map to `SetKind.TIMED` by default.
- Existing bodyweight exercises that are not timed keep reps-only bodyweight behavior.
- Archived exercises retain their historical mode for existing routines/workouts.

## ExerciseSet

Existing logged-set ledger entity extended for timed work.

Added field:

- `durationMs`: Optional positive duration in canonical milliseconds.

Validation:

- `SetKind.TIMED` requires `durationMs > 0`.
- `SetKind.TIMED` ignores missing `reps` and `weight`.
- `SetKind.WEIGHTED` keeps requiring positive reps and non-negative weight.
- `SetKind.BODYWEIGHT` keeps requiring positive reps and no negative added load.
- Edits to duration update `updatedAt` and `editedAt` through the explicit edit path.

State transitions:

- Draft timed value becomes a logged ledger set only after confirmation succeeds.
- Logged timed sets can be edited or deleted through the same explicit set paths as other set kinds.
- Finished workouts retain logged timed sets and discard unlogged timed drafts.

## TimedSetDraft

Active workout UX draft for a timed exercise.

Fields:

- `draftId`: Stable draft identity.
- `activeWorkoutId`: Owning active workout.
- `exerciseInstanceId`: Owning active exercise.
- `position`: Planned set position.
- `setKind`: `TIMED`.
- `durationMs`: Optional accumulated duration.
- `timerStartedAt`: Optional wall-clock instant for a running timed-set counter.
- `updatedAt`: Last persisted draft change.

Validation:

- Drafts may have no duration while the user is preparing to time or enter a hold.
- Confirmation is blocked until accumulated duration plus any running timer elapsed time is positive.
- Running timers use `timerStartedAt`, not countdown deltas, for cold-start recovery.

Recovery:

- Persisted drafts restore selected exercise, duration value, and running timer anchor after restart.
- If the app restarts while the timed counter is running, elapsed duration is recomputed from the wall clock.

## TimedRoutineTarget

Routine set template extension for timed exercises.

Added field:

- `targetDurationMs`: Optional positive planned duration.

Rules:

- Explicit `targetDurationMs` overrides previous workout duration when launching a routine.
- Blank `targetDurationMs` allows previous completed duration to prefill the active draft.
- Launch-time prefills must not rewrite the saved routine.
- Non-timed routine set targets continue using existing reps/weight fields.

## PreviousTimedValue

Derived previous-value default for a timed exercise.

Fields:

- `setIndex`: Zero-based set position from the previous completed exercise.
- `durationMs`: Positive source duration.
- `sourceCompletedWorkoutId`: Completed workout that supplied the value.
- `sourceSetId`: Completed set that supplied the value.

Rules:

- Values are derived from the most recent completed workout containing that exercise.
- Invalid or missing timed durations are ignored.
- Extra active drafts may repeat the last valid prior timed value.
- The lookup is a projection and is not persisted as standalone source data.

## TimePersonalRecord

Personal record for the longest completed timed set for an exercise.

Fields:

- `recordKind`: `TIME`.
- `exerciseCatalogId`: Timed exercise identity.
- `value`: Duration in canonical milliseconds for comparison and display.
- `sourceWorkoutId`: Completed workout evidence.
- `sourceSetId`: Logged timed set evidence.
- `achievedAt`: Logged set time when available, otherwise completed workout time.
- `createdAt`: Record derivation time.

Validation:

- Only logged timed sets with positive duration are eligible.
- A new record is created only when duration is greater than the previous best.
- Ties do not create duplicate time PR records.
- If source workout/set evidence is deleted later, display shows source unavailable without fabricating evidence.

## TimedProgressPoint

Progress trend point for timed exercises.

Fields:

- `metric`: `TIME`.
- `value`: Duration in canonical milliseconds.
- `sourceWorkoutId`: Completed workout source.
- `sourceSetId`: Logged timed set source.
- `recordedAt`: Logged set time or completed workout fallback.

Rules:

- Points sort chronologically by `recordedAt`.
- Display formatting converts milliseconds into readable duration labels.
- Time points do not mix with weight, volume, or bodyweight reps chart metrics.

## Export Rows

Workout and PR exports include timed data.

Fields:

- `set_kind`: Includes `TIMED`.
- `duration_ms`: Machine-friendly duration for timed sets; blank for non-timed sets.
- `duration_label`: Human-friendly duration such as `1:15` or `1:02:30`.
- Existing weight and reps fields remain unchanged.

Validation:

- Exported timed rows remain understandable without app context.
- Existing export columns for weighted/bodyweight data keep their current meaning.
