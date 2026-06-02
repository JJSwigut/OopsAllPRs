# Research: Routine-Aware Rest Timers

## Decision: Use Per-Exercise Rest Configuration

**Rationale**: Hevy and FitNotes both make per-exercise rest a natural routine
setting. The current Oops All PRs model has routine exercises and active
exercises, but does not yet have set types like warm-up/drop/failure. Per-set or
set-type rest would create UI and data complexity before set types exist.

**Alternatives considered**:

- Per-set rest: rejected for this slice because planned sets are lightweight
  and rest would create more setup friction than value.
- Global-only rest: rejected because routines need different rest for heavy
  compounds versus accessory movements.

## Decision: Keep One Active Rest Timer

**Rationale**: The active workout flow has one focused logging loop and one
existing active session rest state. A single latest-set timer matches Strong's
auto-start behavior and keeps the UI simple.

**Alternatives considered**:

- Multiple simultaneous timers: rejected until supersets/circuits are added.
- Timer rows per exercise: rejected because it increases visual load and makes
  the next logging action less obvious.

## Decision: Auto-Start Only After Set Persistence Succeeds

**Rationale**: Constitution ledger rules say a set must not appear logged until
persistence succeeds. Rest should therefore not start until the logged set
exists and has a stable id.

**Alternatives considered**:

- Optimistic timer start: rejected because a failed set write could leave a
  timer pointing to work that never persisted.

## Decision: Reuse Active Session Rest Anchors

**Rationale**: `ActiveSessionState` already stores `restStartedAt`,
`restEndsAt`, and `restOriginSetId`, and restore behavior already clears
expired rests by wall-clock time. Extending that path avoids a second source of
truth.

**Alternatives considered**:

- New rest timer table: rejected because there is only one active rest timer and
  the active session already owns recoverable workout timing.

## Decision: Android Alert Adapter, iOS No-Op For Now

**Rationale**: Android is the validation target and already has an expect/actual
`RestNotificationScheduler`. Native iOS alert implementation can remain a
future platform slice without blocking shared timer correctness.

**Alternatives considered**:

- Shared sound playback abstraction: rejected because notification/sound
  permission behavior is platform-specific.

## Decision: Save Competitor Parity As Backlog

**Rationale**: Previous workout values, set tags, RPE/RIR, supersets, plate
calculator, warm-up calculator, and body measurements are valuable but would
make this feature too broad.

**Alternatives considered**:

- Bundle all competitor gaps now: rejected as feature bloat and high risk for
  the core logging loop.
