# Research: Timed Exercise PRs

## Decision: Store timed set duration as canonical milliseconds

**Rationale**: Milliseconds match the app's existing timer vocabulary, preserve
sub-second accuracy if a live hold timer is used later, and avoid locale or
display-format drift. UI can render minutes/seconds while persistence and PR
comparison remain numeric and unambiguous.

**Alternatives considered**:

- Store only whole seconds. Rejected because it loses precision from a timer
  interaction and makes future stopwatch behavior harder to represent.
- Store duration as formatted text. Rejected because it is not reliable for
  validation, sorting, PR derivation, export, or localization.

## Decision: Add `SetKind.TIMED` instead of overloading bodyweight

**Rationale**: Planks and wall sits are often bodyweight movements, but their
primary work value is duration, not reps. A dedicated timed set kind keeps
validation simple: timed sets require positive duration and do not require reps
or weight. It also preserves current bodyweight reps-only behavior.

**Alternatives considered**:

- Reuse `SetKind.BODYWEIGHT` with nullable reps and duration. Rejected because
  it weakens existing validation and makes bodyweight display/PR rules branch on
  optional fields.
- Add a separate exercise-only timed flag while leaving set kind unchanged.
  Rejected because set validation, export, PR derivation, and history display
  need to know what kind of logged work each set represents.

## Decision: Classify exercises by default logging mode

**Rationale**: The current catalog already carries bodyweight classification,
but timed exercises need a first-class way to choose the right logging card.
Adding a catalog/reference logging mode allows seed data, user-created
exercises, routine templates, and active workouts to agree on the expected set
kind while preserving stable exercise identities.

**Alternatives considered**:

- Infer timed behavior only from exercise names such as "plank". Rejected
  because name heuristics are brittle, hidden from users, and hard to migrate.
- Let users switch set kind only inside an active workout. Rejected because it
  creates routine/default/export ambiguity and adds friction to repeated timed
  exercises.

## Decision: Use duration-first logging with optional live timer support

**Rationale**: The fastest reliable path is to show a duration control in the
active logging card and allow one-tap logging once a duration exists. A live
start/stop timer can feed that same duration value, while direct correction
remains available for users who timed the set elsewhere or need to fix a value.
Any running timer uses wall-clock anchors for recovery.

**Alternatives considered**:

- Manual duration entry only. Rejected because "timing" hold exercises commonly
  implies the app can time the hold, and adding the model now avoids a second
  migration for timer recovery fields.
- Stopwatch only with no correction. Rejected because gym logging needs an
  escape hatch when the user starts late, forgets to stop, or records from a
  gym clock.

## Decision: Time PRs are longest-duration records

**Rationale**: Longest hold duration is the clearest first scoring rule for
planks, wall sits, dead hangs, and similar movements. The rule is easy to test,
format, export, and explain in active PR feedback and Progress.

**Alternatives considered**:

- Shortest time wins. Rejected for this feature because pace, intervals, and
  time-to-completion exercises are explicitly out of scope.
- Multiple time PR buckets by duration range or exercise subtype. Rejected as
  feature bloat for the first timed slice.

## Decision: Do not create duplicate PRs on ties

**Rationale**: Matching a previous best is useful feedback but not a new record.
Avoiding duplicate tie PRs keeps Progress and History evidence clean and aligns
with the spec's longest-duration acceptance criteria.

**Alternatives considered**:

- Create a PR event for every tie. Rejected because it inflates records and
  makes the latest PR less meaningful.
- Show no feedback for ties. Accepted for persistence; a non-persistent "ties
  best" message can be considered later without changing ledger data.

## Decision: Timed routine targets are optional and previous defaults fill blanks

**Rationale**: This matches the routine direction already chosen for weighted
and bodyweight work: routines can be loose plans, explicit targets override
defaults, and blank targets inherit the user's latest completed values at
launch without mutating the saved routine.

**Alternatives considered**:

- Require every timed routine set to include a target duration. Rejected because
  it makes routine creation heavier for users who just want exercise order.
- Rewrite routines with the last completed timed duration. Rejected because it
  silently changes saved plans and violates ledger/planning separation.

## Decision: Reuse existing PR/progress storage shape with timed enum values

**Rationale**: Existing `personal_records.value` and `progress_points.value`
already provide a numeric comparator and chart value. Adding time enum values
and treating `value` as duration milliseconds avoids unnecessary progress-table
migration while source sets retain canonical `durationMs` evidence.

**Alternatives considered**:

- Add dedicated duration columns to PR/progress tables. Rejected because the
  source set is the canonical evidence and `value` is sufficient for records and
  trends.
- Create a separate timed PR table. Rejected because it fragments Progress and
  export behavior.

## Decision: Seed known hold-style exercises as timed while preserving existing seed data

**Rationale**: The current app seed list remains the baseline. The migration and
seed ingestion path should classify known hold-style exercises such as planks,
wall sits, and dead hangs as timed using explicit rules that can be inspected
and extended, while leaving existing exercises stable.

**Alternatives considered**:

- Replace the exercise seed list with a new external catalog. Rejected because
  licensing and classification quality are not currently better than the known
  local seed baseline.
- Require users to create all timed exercises manually. Rejected because it
  makes the feature feel incomplete at first launch.

## Decision: Export timed values with explicit duration columns and labels

**Rationale**: Exported history must remain understandable outside the app.
Including machine-friendly duration milliseconds and human-friendly duration
labels avoids ambiguity in spreadsheets while keeping weight/reps columns stable
for non-timed sets.

**Alternatives considered**:

- Put duration into the reps column. Rejected because it corrupts meaning and
  breaks external analysis.
- Export only formatted duration labels. Rejected because labels are harder to
  sort, chart, or import later.
