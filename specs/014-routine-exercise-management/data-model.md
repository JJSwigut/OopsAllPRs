# Data Model: Routine & Exercise Management

## Routine Draft

Represents a user-editable new or existing routine before explicit save.

Fields:

- `routineId`: optional stable id for editing an existing routine.
- `name`: editable display name.
- `sourceCompletedWorkoutId`: optional id preserved for routines created from history.
- `exercises`: ordered list of routine exercise drafts.
- `isSaving`: save-in-progress flag.
- `errorMessage`: validation or persistence error.

Validation:

- Name is trimmed and must not be blank.
- A routine may be saved only when it has at least one exercise.
- Exercise positions are contiguous at save time.

## Routine Exercise Draft

Represents one planned exercise inside a routine draft.

Fields:

- `draftId`: stable UI draft id.
- `routineExerciseId`: optional persisted id for existing routine exercises.
- `exerciseCatalogId`: catalog entry id.
- `displayNameSnapshot`: display name copied into the routine.
- `isBodyweight`: future set-kind default.
- `position`: order in the routine.
- `rest`: `RestConfiguration`.
- `plannedSets`: ordered list of set drafts.

Validation:

- Must reference a non-archived exercise when newly added.
- Rest duration cannot be negative.
- At least one planned set is recommended by UI; saving can auto-create one default set when the exercise has none.

## Routine Set Draft

Represents a planned target set.

Fields:

- `draftId`: stable UI draft id.
- `routineSetId`: optional persisted id for existing routine set templates.
- `position`: set order.
- `setKind`: weighted or bodyweight.
- `targetReps`: optional positive reps.
- `targetWeight`: optional canonical kilograms.

Validation:

- Reps, when present, must be positive.
- Weighted sets reject negative weights.
- Bodyweight sets allow null weight and reject negative added load.

## Managed Exercise

View model for exercise management.

Fields:

- `exerciseCatalogId`: catalog id.
- `displayName`: editable only when user-created.
- `subtitle`: muscle/equipment/bodyweight summary.
- `isBodyweight`: editable only when user-created and not blocked by usage rules.
- `isUserCreated`: source flag.
- `isArchived`: archived state.
- `canEdit`: true only for user-created exercises.
- `canArchive`: true only for user-created exercises.

Validation:

- Seeded exercises are read-only.
- User-created names trim whitespace and cannot be blank.
- Canonical names must remain unique among active exercises.
- Archived exercises are hidden from default picker and routine-builder search.

## Exercise Edit Draft

Fields:

- `exerciseCatalogId`: optional id for edit.
- `name`: editable name.
- `isBodyweight`: editable classification.
- `isSaving`: save-in-progress flag.
- `errorMessage`: validation or persistence error.

State transitions:

- `New -> Saving -> Saved`
- `Existing -> Editing -> Saving -> Saved`
- `Existing -> ConfirmArchive -> Archived`
- Any state can return to closed/canceled without persistence unless save/archive succeeds.
