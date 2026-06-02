# Data Model: Developer Demo Data Seeding

## DeveloperSeedScenario

Represents a named developer-only scenario.

Fields:
- `id`: Stable scenario key.
- `label`: Developer-facing name.
- `description`: Short explanation of what the scenario creates.
- `status`: Idle, loading, loaded, skipped, or failed.

Validation:
- Scenario actions are only visible when developer tooling is enabled.
- Scenario load operations must return explicit success, skip, or failure feedback.

## DemoDatasetMarker

Logical marker derived from existing data rather than a new table.

Fields:
- `routineName`: Reserved `Demo:` routine name for routine-backed demo data.
- `startedAt`: Deterministic timestamp for completed demo workouts.
- `routineName`: Stable visible name for developer review.

Validation:
- Re-running a scenario must detect existing markers and skip duplicate creation.
- Markers must not match ordinary user-created routines or workouts unless they use the reserved demo name pattern.

## DemoCompletedWorkout

Completed workout created for progress, history, export, chart, and PR review.

Fields:
- Uses existing completed workout fields.
- Includes routine id references to demo routines where applicable.
- Contains weighted and bodyweight logged sets.

Validation:
- Must be created by starting an active workout, adding exercises or launching a routine, confirming sets, and finishing the workout.
- Must produce PR records and progress points through normal derivation.

## DemoRoutine

Reusable routine created for routine launch and rest timer validation.

Fields:
- Stable id.
- Name prefixed with `Demo:`.
- Exercises with planned sets.
- Exercise rest durations.

Validation:
- Must include at least one weighted exercise and one bodyweight exercise.
- Planned bodyweight sets must not require weight.

## DemoActiveWorkout

Active workout created for resume and recovery validation.

Fields:
- Existing active workout id.
- Started-at wall-clock timestamp.
- Routine snapshot if launched from a demo routine.
- Logged sets plus remaining unlogged planned sets.

Validation:
- Must not be created if any active workout already exists.
- Must restore through the existing hydrate/session recovery path.
