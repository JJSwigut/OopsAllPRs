# Contracts: Previous Workout Values

## Add Exercise Contract

**given** an active workout exists and the selected exercise has valid completed history,
**when** the exercise is added to the active workout,
**then** the visible next-set draft uses the first valid set from the most recent completed workout for that exercise.

**and** if no valid completed history exists,
**then** the visible next-set draft uses the existing safe default for the exercise type.

## Bodyweight Contract

**given** the selected exercise is bodyweight and prior completed history has reps,
**when** a previous-value draft is created,
**then** reps are prefilled and weight remains absent so the draft can be logged reps-only.

## Routine Launch Contract

**given** a saved routine has explicit planned targets,
**when** the routine is launched after newer completed history exists,
**then** explicit target fields remain unchanged in the active workout.

**given** a saved routine has missing target fields,
**when** the routine is launched and previous completed values exist,
**then** missing fields are filled from the matching prior set index.

## Set Order Contract

**given** the prior completed workout has ordered valid sets for an exercise,
**when** active drafts are prepared for that exercise,
**then** the active set at index `n` uses the previous set at index `n` when available.

**and** when `n` exceeds the previous set count,
**then** the final valid previous set may be reused.

## Ledger Integrity Contract

**given** previous values are applied to active drafts,
**when** the user reviews History, routines, Progress, PR evidence, or exports without logging those drafts,
**then** no completed workout, routine template, PR, progress point, or export source data has changed.

## Recovery Contract

**given** a previous-value draft is visible in an active workout,
**when** active UX draft persistence runs and the app restarts,
**then** the recovered draft retains the same reps, set kind, and canonical weight values.
