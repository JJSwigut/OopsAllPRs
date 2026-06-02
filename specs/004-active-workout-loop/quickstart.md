# Quickstart: Active Workout Logging Loop

## Build and Test

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

## Material Scan

```bash
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts
```

Expected result: no matches.

## Manual Android Milestone

1. Launch the Android debug app.
2. Start an empty workout from Train.
3. Add a weighted exercise and log one set with one tap.
4. Add a bodyweight exercise and log reps only.
5. Edit reps/weight inline before logging and confirm the logged tuple uses the edited values.
6. Try an invalid set and confirm the inline error preserves entered values.
7. Restart the app during an active workout and confirm focus/draft recovery.

Manual device validation may be deferred if no emulator/device is attached, but
must be completed before release.
