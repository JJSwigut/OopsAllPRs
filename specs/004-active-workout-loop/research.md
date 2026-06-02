# Research: Active Workout Logging Loop

## Decision: Model one editable draft row per exercise block

**Rationale**: The fastest logging loop needs the next set ready without modal
setup. A single `SetRowDraft` per block can default from the last logged set or
routine target, keep edits local until confirmed, and avoid mutating logged
rows.

**Alternatives considered**:
- Persist every unlogged planned set as an `ExerciseSet`: rejected for this
  slice because it blurs ledger semantics.
- Require a modal before logging: rejected by the fast-loop principle.

## Decision: Derive visible exercise blocks from active workout plus drafts

**Rationale**: `ActiveWorkout` already owns active exercises and logged set
history. UI-specific state should decorate that data with draft values, pending
status, inline errors, and focus rather than changing domain ledger rows.

**Alternatives considered**:
- Separate UI-only workout tree with independent ids: rejected because it risks
  drift from repository state.

## Decision: Use `FitRoller`-based app-level `RollerField`

**Rationale**: The design system already provides the tactile roller primitive.
The active workout needs app-level behavior around tap-step, value formatting,
validation, and direct entry, so `RollerField` belongs in shared app UI rather
than the design-system module.

**Alternatives considered**:
- Add active-workout-specific behavior to `:design-system`: rejected because it
  would make the design system domain-aware.
- Use only text fields: rejected because it slows the normal gym-side edit path.

## Decision: Block duplicate confirms while a draft is pending

**Rationale**: Ledger integrity requires that duplicate taps cannot create
duplicate persisted rows. The reducer should mark a draft pending before
calling persistence and ignore additional confirm requests until completion.

**Alternatives considered**:
- De-dupe after persistence: weaker because user-visible state can flicker and
  repository behavior becomes harder to reason about.

## Decision: Recover focus and drafts through shared state

**Rationale**: Feature 003 already persists route recovery. This feature adds
workout-specific focus and draft state so an interruption does not erase the
next set the user was editing. Confirmed ledger rows remain the source of truth
for logged sets.

**Alternatives considered**:
- Recover only confirmed sets: rejected because it loses in-progress user work.
- Persist drafts as logged rows with missing timestamps: rejected because it
  weakens ledger clarity.

## Decision: Validate with common reducer/use-case tests plus platform compile

**Rationale**: The highest-risk behavior is state and ledger correctness, which
can be covered in common tests. Android assembly and iOS compile verify the
shared UI remains portable.

**Alternatives considered**:
- Instrumented UI tests now: valuable later, but not required to prove reducer
  and persistence contracts for this slice.
