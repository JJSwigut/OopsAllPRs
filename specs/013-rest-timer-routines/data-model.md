# Data Model: Routine-Aware Rest Timers

## RestConfiguration

- `durationSeconds: Int`
- `autoStart: Boolean`

Validation:

- `durationSeconds` must be greater than or equal to `0`.
- A configuration is active only when `autoStart == true` and
  `durationSeconds > 0`.
- Default duration is 120 seconds.

## ActiveExercise

Existing active exercise plus:

- `rest: RestConfiguration`

Rules:

- Empty-workout added exercises use the current default rest preference.
- Routine-launched exercises copy rest configuration from the routine exercise.
- Rest configuration does not change logged set tuples.

## RoutineExercise

Existing routine exercise plus:

- `rest: RestConfiguration`

Rules:

- Saving a completed workout as a routine assigns the default rest preference.
- Older routines without stored rest use the default preference at launch.

## ActiveSessionState

Existing active session rest fields remain the single active timer source:

- `restStartedAt: Instant?`
- `restEndsAt: Instant?`
- `restOriginSetId: FoundationId?`

Rules:

- Rest starts after logged-set persistence succeeds.
- Rest end is calculated from wall-clock time, not countdown deltas.
- Skip/finish/discard clears rest fields and cancels pending alerts.
- Deleting or undoing the origin set clears rest.

## Rest Preferences

Extends local user preferences:

- `defaultRestSeconds: Int`
- `restSoundEnabled: Boolean`

Rules:

- Defaults apply to newly added active/routine exercises.
- Existing routines are not mutated when defaults change.
- Sound preference controls audible alerts but not visual RestBar behavior.

## State Transitions

```text
No rest
  -> log set with enabled rest
Running rest
  -> add/subtract time
Running rest
  -> rest end reached
Completed visual state
  -> user logs another set or skips/clears
Running rest
  -> skip, finish, discard, delete origin, or undo origin
No rest
```
