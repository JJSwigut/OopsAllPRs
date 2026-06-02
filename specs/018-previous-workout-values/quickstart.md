# Quickstart: Previous Workout Values

## Automated Validation

Run:

```bash
./gradlew :shared:testDebugUnitTest --tests "*PreviousWorkout*"
./gradlew :shared:testDebugUnitTest --tests "com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutAddExerciseTest" --tests "com.jjswigut.oopsallprs.ui.workout.WorkoutHomeTemplateLaunchTest"
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
git diff --check -- .
```

## Manual Smoke When Validation Is Available

1. Load developer progress demo seed or create a completed workout with Bench Press and Pull Up.
2. Start a new empty workout.
3. Add Bench Press and verify the first draft uses the last completed Bench Press weight and reps.
4. Add Pull Up and verify the draft is reps-only and does not require weight.
5. Launch a saved routine with explicit planned targets and verify those targets remain unchanged.
6. Launch or create an under-specified routine and verify missing target fields are filled from prior completed values.
7. Restart the app while a previous-value draft is visible and verify the active workout recovers with the same draft.
8. Check History, Progress, PR evidence, routines, and export behavior for no silent mutation before the draft is logged.

## Evidence To Record

- Common test result.
- Android debug build result.
- iOS simulator compile result.
- Material/style scan result.
- Whitespace result.
- Manual real-device validation status. For this turn, record as deferred per user request.
