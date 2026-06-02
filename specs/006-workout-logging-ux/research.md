# Research: Workout Logging UX V1

## Decision: Treat active workout as focused full-screen mode

**Rationale**: The manual review showed that bottom navigation, large top
chrome, and overlay-style composition compete with the set logging task. A
focused active mode makes the next action clear and gives the logging surface
the full viewport, while still allowing a compact Close action.

**Alternatives considered**:
- Keep active workout as a translucent overlay above the shell: rejected
  because background bleed and duplicated navigation made the screen feel
  unstable.
- Keep bottom navigation visible during logging: rejected because logging is a
  mode, not a top-level browsing destination.

## Decision: Use dense exercise blocks instead of nested card stacks

**Rationale**: Repeated set logging needs compact scanning. Completed sets
should read as rows, and only the current next-set input needs visual weight.
Nested glass cards made the layout bulky, caused edge glow artifacts, and
reduced the amount of workout information visible at once.

**Alternatives considered**:
- Keep each set in a card: rejected because it slows scanning and creates
  visual clutter.
- Make the whole screen a flat table: rejected because exercise grouping and
  the current next action still need clear hierarchy.

## Decision: Persist focus and drafts as typed active-workout UX state

**Rationale**: The existing active session state recovers route, rest, and
session timing but does not persist editable drafts. The spec requires draft
and focus recovery after restart/process death. A typed local persistence
contract keeps unlogged work recoverable without weakening the set ledger.

**Alternatives considered**:
- Recover only confirmed sets: rejected because edited next-set values would be
  lost during normal phone interruptions.
- Persist unlogged drafts as `ExerciseSet` rows with no `loggedAt`: rejected
  because it blurs planned, draft, and confirmed ledger semantics.
- Store drafts in a serialized blob: rejected because typed SQLDelight rows are
  easier to validate, migrate, and eventually sync.

## Decision: Keep next-set defaults derived from the latest relevant set

**Rationale**: Most gym-side logging repeats or slightly adjusts the prior set.
Defaulting reps and weight from the latest logged row reduces input work while
keeping direct correction available.

**Alternatives considered**:
- Always reset to fixed defaults: rejected because it forces repeated edits.
- Require templates for all default values: rejected because empty workouts and
  on-the-fly exercises are core flows.

## Decision: Keep bodyweight reps-only in the default path

**Rationale**: Bodyweight exercises should not ask for weight unless the user
explicitly wants added load. This matches the product requirement and reduces
mistakes during common reps-only movements.

**Alternatives considered**:
- Always show optional load beside reps: rejected because it implies weight is
  part of the normal bodyweight path.
- Model bodyweight sets as zero-weight weighted sets: rejected because it
  weakens PR semantics and export clarity.

## Decision: Derive inline PR feedback after successful logging

**Rationale**: The set must be accepted before the app celebrates it. Inline
feedback can compare the newly logged set to existing personal records and
active-workout logged rows, attach the result to the logged row, and remain
non-blocking.

**Alternatives considered**:
- Show a modal PR celebration: rejected because it interrupts continued set
  logging.
- Wait until the workout is finished before showing any PR feedback: rejected
  because the product promise is immediate progress recognition.
- Persist PR feedback as separate UI state: rejected for this slice because it
  can be recomputed from durable sets and existing progress data.

## Decision: Keep Add Exercise search-first and scoped to active logging

**Rationale**: Adding an exercise during a session should feel like a command
inside the active workout, not navigation to a catalog management area. Dense
rows with inline Add actions support fast selection and keyboard use.

**Alternatives considered**:
- Large result cards with full-width buttons: rejected because they reduce
  visible results and create excessive scrolling.
- Separate catalog browsing destination: rejected because it interrupts the
  active workout loop.

## Decision: Make manual Pixel review a planning gate for this UX slice

**Rationale**: The problem was discovered visually on a Pixel-class layout, and
automated state tests cannot prove absence of overlap, keyboard crowding, or
poor screen density. A milestone manual pass with screenshots is required
before this slice is considered done.

**Alternatives considered**:
- Rely only on shared unit tests and compile: rejected because the failure mode
  is visual and ergonomic.
