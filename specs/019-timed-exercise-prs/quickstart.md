# Quickstart: Timed Exercise PRs

## Automated Validation

Run:

```bash
./gradlew :shared:testDebugUnitTest --tests "*Timed*"
./gradlew :shared:testDebugUnitTest --tests "*PersonalRecordDerivationTest" --tests "*Progress*"
./gradlew :shared:testDebugUnitTest --tests "*Routine*" --tests "*PreviousWorkout*"
./gradlew :shared:testDebugUnitTest --tests "*Export*" --tests "*Seed*"
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
git diff --check -- .
```

## Manual Pixel Smoke

1. Install the debug build on the connected Pixel.
2. Start a fresh workout and add a timed exercise such as Plank or Wall Sit.
3. Start or enter a duration, log the set, and verify no reps or weight are required.
4. Log a longer timed set for the same exercise and finish the workout.
5. Open History and verify timed sets show readable duration values and PR evidence includes achieved date.
6. Open Progress for the timed exercise and verify the latest PR and trend show duration values.
7. Create or edit a routine with the timed exercise, leave target duration blank, launch it, and verify previous duration prefills without changing the saved routine.
8. Add an explicit timed routine target, launch it, and verify the explicit target wins over previous defaults.
9. Restart the app while a timed draft or running timed-set counter is active and verify active workout recovery.
10. Export data and verify timed rows include duration fields and labels.

## Evidence To Record

- Common timed test result.
- SQLDelight migration/repository test result.
- Android debug build result.
- iOS simulator compile result.
- Material/style scan result.
- Whitespace result.
- Export sanity result.
- Manual Pixel validation status with screenshots if layout changes are visible.
