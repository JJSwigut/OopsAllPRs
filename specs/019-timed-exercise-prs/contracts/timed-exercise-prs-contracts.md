# Contracts: Timed Exercise PRs

## Timed Exercise Selection Contract

**given** an exercise catalog item is classified with timed logging mode,
**when** the user adds it to an active workout or routine,
**then** the created exercise defaults to `SetKind.TIMED` and presents duration-first logging.

**and** existing weighted and bodyweight exercises keep their current default set kind and validation behavior.

## Timed Set Logging Contract

**given** an active workout contains a timed exercise,
**when** the user enters or times a positive duration and taps log,
**then** one logged set is persisted with `SetKind.TIMED`, positive `durationMs`, `loggedAt`, and no required reps or weight.

**given** a timed exercise draft has missing, zero, or negative duration,
**when** the user taps log,
**then** no ledger set is created and the draft remains available for correction.

## Timed Draft Recovery Contract

**given** a timed draft has a duration value or a running timed-set counter,
**when** the app restarts, is backgrounded, or the process is recreated,
**then** the active workout restores the same focused exercise and draft with duration state intact.

**and** if the timed-set counter was running,
**then** elapsed time is recomputed from the persisted wall-clock start instant.

## Timed Routine Contract

**given** a routine contains a timed exercise with explicit `targetDurationMs`,
**when** the routine is launched,
**then** the active timed draft uses that explicit target.

**given** a routine contains a timed exercise with blank target duration and previous completed timed values exist,
**when** the routine is launched,
**then** the active timed draft is prefilled from the previous completed timed value for the matching set index.

**and** the saved routine is not overwritten by launch-time previous defaults.

## Time PR Contract

**given** a completed timed set exceeds the user's prior best duration for the same exercise,
**when** PR derivation runs,
**then** a `TIME` personal record is produced with source workout, source set, duration, and achieved date evidence.

**given** a completed timed set ties the current best duration,
**when** PR derivation runs,
**then** no duplicate `TIME` personal record is created for the tie.

## History And Evidence Contract

**given** a completed workout contains timed sets,
**when** the user opens History,
**then** each timed set displays a readable duration label and no placeholder reps or weight requirement.

**given** a time PR has source data available,
**when** the user opens PR evidence from History or Progress,
**then** the source workout, exercise name, set duration, and achieved date are visible.

**given** source evidence was deleted,
**when** the user opens PR evidence,
**then** the app shows a source-unavailable state instead of crashing or inventing evidence.

## Progress Contract

**given** a timed exercise has completed duration points,
**when** the user opens Progress for that exercise,
**then** the latest record and trend use duration labels and chronological time points.

**and** timed points are not mixed into weight, volume, estimated one-rep max, or bodyweight reps charts.

## Export Contract

**given** workouts, routines, or PRs include timed data,
**when** the user exports data,
**then** timed rows include explicit duration fields and labels while existing reps and weight columns keep their current meaning.
