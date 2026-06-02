# Data Model: Catalog Exercise Selection

## ExercisePickerState

Represents transient picker state while adding an exercise to an active
workout.

Fields:
- `activeWorkoutId`: workout receiving the selected exercise.
- `query`: current local search query.
- `results`: visible local catalog results.
- `isOpen`: whether the picker overlay is active.
- `isCreatingCustom`: whether the custom exercise form is active.
- `customDraft`: draft name/classification for a new local exercise.
- `isSaving`: whether selection or creation is pending.
- `errorMessage`: user-visible validation or persistence error.

Validation:
- Canceling must not mutate active workout state.
- Saving state must suppress duplicate select/create actions.

## ExercisePickerResultRow

Represents a selectable local catalog result.

Fields:
- `exerciseCatalogId`: stable catalog id.
- `displayName`: visible exercise name.
- `subtitle`: distinguishing metadata such as muscle group and equipment.
- `isBodyweight`: classification used by active workout drafts.
- `isUserCreated`: whether the result came from local user creation.

Validation:
- Result rows must preserve bodyweight classification from catalog data.

## CustomExerciseDraft

Represents a minimal exercise creation form.

Fields:
- `name`: user-entered display name.
- `isBodyweight`: true for reps-only default behavior, false for weighted.

Validation:
- Name must be non-blank after trimming.
- Name is canonicalized before persistence.
- Creation failure must preserve the draft and show an error.

## ExerciseCatalogItem

Existing domain entity representing a persisted local exercise.

Used fields:
- `id`
- `canonicalName`
- `displayName`
- `muscleGroup`
- `equipment`
- `movementPattern`
- `exerciseType`
- `experienceLevel`
- `bodyRegion`
- `isBodyweight`
- `isUserCreated`
- `createdAt`
- `updatedAt`

State transitions:
- Seeded item remains available unless archived.
- User-created item is saved locally and becomes searchable immediately.

## ExerciseReference

Existing active workout input derived from a catalog result.

Fields:
- `exerciseCatalogId`
- `displayNameSnapshot`
- `isBodyweight`

Relationship:
- Passed to active workout add logic to append an `ActiveExercise`.
