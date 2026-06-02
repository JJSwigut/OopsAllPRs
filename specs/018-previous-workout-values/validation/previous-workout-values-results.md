# Validation Results: Previous Workout Values

## Automated Gates

| Gate | Result | Evidence |
|------|--------|----------|
| Previous-value common tests | Pass | `./gradlew :shared:testDebugUnitTest --tests "*PreviousWorkout*"` BUILD SUCCESSFUL |
| Adjacent active/routine regressions | Pass | `./gradlew :shared:testDebugUnitTest --tests "com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutAddExerciseTest" --tests "com.jjswigut.oopsallprs.ui.workout.WorkoutHomeTemplateLaunchTest" --tests "com.jjswigut.oopsallprs.domain.usecase.TemplateLaunchSeparationTest"` BUILD SUCCESSFUL |
| Full shared unit tests | Pass | `./gradlew :shared:testDebugUnitTest` BUILD SUCCESSFUL |
| Android debug build | Pass | `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` BUILD SUCCESSFUL |
| iOS simulator compile | Pass | `./gradlew :shared:compileKotlinIosSimulatorArm64` BUILD SUCCESSFUL with existing expect/actual beta warnings |
| Material/style guard | Pass | `rg` returned no forbidden Material 3/shared styling matches |
| Whitespace validation | Pass | `git diff --check -- .` returned no errors |

## Android Emulator Smoke

Ran on a clean Pixel 9 Pro emulator (`emulator-5554`) with the debug build installed from
`androidApp/build/outputs/apk/debug/androidApp-debug.apk`.

| Flow | Expected Result | Outcome | Evidence |
|------|-----------------|---------|----------|
| Add exercise from prior completed workout | Draft uses latest completed values | Pass | Seeded Progress demo, started a fresh workout, added `Barbell Bench Press - Medium Grip`; logging draft showed `5` reps and `87.5` kg. Screenshot: `validation/screenshots/pixel-9-pro-weighted-previous-values.png` |
| Bodyweight previous reps | Draft is reps-only | Pass | Added `Wide-Grip Rear Pull-Up`; logging draft showed `10` reps, `Reps only`, and no weight field. Screenshot: `validation/screenshots/pixel-9-pro-bodyweight-previous-values.png` |
| Recently used exercise ordering | Exercises with completed history appear above full catalog | Pass | Add-exercise sheet showed `Recently used` with bench, pull-up, squat, and deadlift above `All exercises`. |
| Routine target precedence | Explicit routine targets are preserved | Automated | Covered by `PreviousWorkoutRoutineLaunchTest`; not manually re-smoked in the emulator pass. |
| Recovery | Previous-value draft recovers after restart | Automated | Covered by `ActiveWorkoutPreviousValuesTest`; not manually re-smoked in the emulator pass. |

## Notes

- Automated tests remain the completion gate for routine precedence and recovery edge cases.
- Android emulator smoke covered the fresh-workout add-exercise paths for weighted and bodyweight movements.
- Previous values are derived from completed workout history and do not add a database migration.
- Add-exercise defaults are persisted through existing active UX draft persistence.
- Routine launch fills only missing target fields; saved routine templates remain unchanged.
