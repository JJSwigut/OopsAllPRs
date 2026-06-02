# Quickstart: Bootstrap Oops All PRs App Foundation

## Prerequisites

- JDK 17+
- Android SDK with API 35 available
- Xcode available for iOS adapter compilation when validating iOS locally
- Network access only for dependency resolution; app behavior must work offline

## Planned Build Commands

Run from the repository root after implementation tasks create the Gradle
project:

```bash
./gradlew :shared:build
./gradlew :androidApp:assembleDebug
./gradlew testDebugUnitTest
./gradlew check
```

If iOS build tasks are configured in the fresh project, also run the generated
shared iOS framework task selected by Gradle for the local host.

## Foundation Validation Flow

### 1. Exercise Seed Validation

1. Install or run the app with a fresh local database.
2. Confirm the baseline exercise seed list is imported.
3. Search for representative exercises:
   - Barbell movement
   - Dumbbell movement
   - Bodyweight movement
   - Cable or machine movement
4. Create a custom user exercise.
5. Re-run seed ingestion.
6. Confirm the custom exercise remains unchanged and no duplicate canonical
   seed names are introduced.

Expected result: valid seed rows are available, malformed rows are reported,
and user-created exercises are preserved.

### 2. Start and Recover Active Workout

1. Start an empty workout.
2. Add one weighted exercise.
3. Confirm one weighted set.
4. Start a rest timer if available in the foundation slice.
5. Restart the app or recreate the active session state.
6. Confirm the same active workout, logged set, elapsed time, and rest remaining
   are restored from local state.

Expected result: active workout identity and logged set data survive process
recreation, and timers are based on wall-clock anchors.

### 3. Bodyweight Logging

1. Add a bodyweight exercise.
2. Confirm a reps-only set without required weight.
3. Restart/recreate app state.
4. Confirm the bodyweight set remains logged.
5. Verify the set appears in history/export-ready data and PR derivation.

Expected result: bodyweight reps-only sets are first-class workout evidence.

### 4. Routine Separation

1. Finish a workout with logged sets.
2. Save the completed workout as a reusable routine.
3. Start a new active workout from that routine.
4. Modify the active workout by adding or editing planned sets.
5. Confirm the reusable routine remains unchanged unless explicitly edited.

Expected result: routine plans and active workout ledger state remain distinct.

### 5. Finish Integrity

1. Start an active workout with multiple planned sets.
2. Log some but not all planned sets.
3. Finish the workout.
4. Inspect completed history.

Expected result: all logged set tuples are preserved exactly, and unlogged
planned sets are not part of completed history.

### 6. Unit Conversion and Locale Input

1. Enter a fractional value in pounds.
2. Switch display unit to kilograms.
3. Confirm stored evidence round-trips through canonical kilograms.
4. Enter a decimal value with comma separator where supported.

Expected result: display/input conversions never mutate canonical evidence, and
locale decimal input validates predictably.

### 7. PR and Progress Evidence

1. Complete at least one weighted workout with a best set.
2. Complete at least one bodyweight workout with a rep best.
3. Recalculate or inspect PR/progress foundation data.
4. Trace each PR back to source workout and set.

Expected result: weighted and bodyweight PRs are derived from completed logged
sets and remain inspectable.

### 8. Export and Ownership

1. Generate workout export-ready data.
2. Generate PR export-ready data.
3. Verify CSV quoting for names containing commas or quotes.
4. Verify bodyweight reps-only sets are included.
5. Review Android backup and permissions behavior for release notes.

Expected result: export works offline, uses display units, and includes all
logged workout evidence.

## Milestone Manual Gate

Before moving beyond the foundation milestone:

- Fresh install to first logged set completes in <=30 seconds on Android.
- Active workout restore completes in <=2 seconds with logged set data intact.
- No shared foundation code imports Android-only or iOS-only APIs directly.
- Backup behavior, export behavior, release artifact asset checks, and
  accessibility basics are documented.

