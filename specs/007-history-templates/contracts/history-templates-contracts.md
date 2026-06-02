# Contracts: Completed Workout History and Templates

## Finish Summary Contract

**Given** an active workout has logged sets,
**when** the user finishes it,
**then** a completed workout summary is available with duration, exercises,
logged set rows, and set counts.

**Given** the completed workout includes bodyweight reps-only sets,
**when** the summary is shown,
**then** those rows display reps without a required or invented weight.

**Given** the completed workout includes record-setting sets,
**when** the summary is shown,
**then** PR markers appear on the relevant set rows without blocking review.

## History List Contract

**Given** completed workouts exist,
**when** History is opened,
**then** rows appear in reverse chronological order.

**Given** a History row is shown,
**when** the user scans it,
**then** it communicates date, duration, exercise count, set count, and PR
presence when relevant.

**Given** no completed workouts exist,
**when** History is opened,
**then** the empty state explains that finished workouts will appear there.

## History Detail Contract

**Given** the user selects a completed workout from History,
**when** detail opens,
**then** it shows the same exercises and logged set rows as the finish summary.

**Given** the app restarts,
**when** History detail is opened for an existing workout,
**then** the detail is restored from local completed workout data.

## Save Template Contract

**Given** a completed workout detail is open,
**when** the user saves it as a template with a non-blank name,
**then** a reusable local template is created with planned targets copied from
logged set values.

**Given** the user enters a blank template name,
**when** save is attempted,
**then** no template is created and the entered state remains retryable.

**Given** a template is created from a completed workout,
**when** the completed workout is viewed again,
**then** the completed workout is unchanged.

## Train Template Contract

**Given** templates exist,
**when** Train is shown,
**then** template launch is visible or directly reachable while Start workout
remains the primary empty-session action.

**Given** a template row is shown,
**when** the user scans it,
**then** it communicates template name, exercise count, and planned set count.

## Template Launch Contract

**Given** no active workout exists,
**when** the user launches a template,
**then** active workout mode opens with template exercises and planned set
targets ready to log.

**Given** a launched template was created from completed history,
**when** the active workout hydrates,
**then** no logged timestamps, completed workout ids, completed exercise ids, or
PR markers are copied into the active workout.

**Given** an active workout already exists,
**when** the user attempts to launch a template,
**then** the app offers a conflict path instead of silently creating a second
active workout.

## Manual Review Contract

Manual milestone review must cover:
- Finishing a workout and seeing the summary.
- History empty state, list state, and detail view.
- Mixed weighted and bodyweight completed workout details.
- PR markers in completed workout context.
- Saving a completed workout as a template.
- Blank template name validation.
- Train with empty-start and template launch options.
- Launching a template and confirming planned sets are unlogged.
- Restarting and confirming History/templates remain available.

Passing review means primary controls do not overlap system bars, keyboard, or
each other; the user can complete finish-review-save-launch without
instructions; and active logging remains fast after template launch.
