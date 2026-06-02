# Research: Previous Workout Values

## Decision: Derive previous values on demand from completed workouts

**Rationale**: The current repository already exposes completed workouts as local read models. On-demand derivation avoids a migration, avoids stale caches after history deletion/edit flows, and keeps future sync semantics simpler because previous values remain projections rather than materialized state.

**Alternatives considered**:

- Persist a `last_values` table per exercise. Rejected for this slice because it creates new invalidation rules after completed workout edits/deletes and adds migration risk without clear performance need.
- Store previous values on routines. Rejected because it would silently mutate planned routines and conflict with explicit target precedence.

## Decision: Most recent completed workout wins

**Rationale**: This matches user expectation when they ask for previous workout values: use what they did last time they performed that exercise. Completed workout `finishedAt` is the correct ledger timestamp because it reflects workout completion order and is already available.

**Alternatives considered**:

- Use highest PR values. Rejected because users usually want last performed training load, not maximum ever.
- Use any matching set across all history by latest set timestamp. Rejected because completed workout order is clearer and avoids cross-workout mixing.

## Decision: Explicit routine targets override previous values

**Rationale**: A routine is a saved plan. Replacing routine targets with newer completed values would surprise users and mutate the meaning of a plan at launch. Previous values are useful only for missing targets or additional next-set drafts.

**Alternatives considered**:

- Always overwrite routine targets with previous values. Rejected due to ledger/planning surprise and because the user asked to preserve explicit targets.
- Show a prompt to choose target source. Rejected as fast-loop friction for this first slice.

## Decision: Fill missing target fields independently

**Rationale**: If a planned routine set provides reps but no weight, preserving reps while filling weight from the prior set gives the most useful target without discarding explicit user intent.

**Alternatives considered**:

- Treat any partially filled routine set as fully explicit. Rejected because it leaves obvious missing values empty.
- Treat partial routine sets as fully previous-derived. Rejected because it could replace deliberate reps.

## Decision: Reuse prior set order, repeat the last valid prior set when needed

**Rationale**: Ordered set defaults match how routines and completed workouts are already modeled. Repeating the final prior set gives a safe default for extra drafts while still making logging one-tap ready.

**Alternatives considered**:

- Average prior sets. Rejected because it obscures real ledger values and can produce awkward fractional reps/loads.
- Leave extra drafts empty. Rejected because it weakens the fast-loop benefit.

## Decision: Keep bodyweight defaults reps-only

**Rationale**: Bodyweight movements in the current product are reps-first and should not require weight. Previous-value lookup should preserve that behavior by ignoring load for bodyweight defaults unless future added-load support is explicitly introduced.

**Alternatives considered**:

- Carry optional bodyweight load forward. Rejected for this slice because the current UX and user expectation are reps-only bodyweight logging.

## Decision: Manual real-device validation deferred, automated gates required

**Rationale**: The user cannot perform validation yet. The feature can still ship as a committed development slice with common tests, Android build, iOS compile, style scan, whitespace check, and documented manual deferral.

**Alternatives considered**:

- Block implementation on manual validation. Rejected by user direction.
