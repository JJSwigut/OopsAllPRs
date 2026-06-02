# Quickstart: Catalog Exercise Selection

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
2. Start or resume an active workout.
3. Tap Add Exercise.
4. Search for a seeded weighted exercise and select it.
5. Confirm the exercise block appears focused and can log a weighted set.
6. Search for a seeded bodyweight exercise and select it.
7. Confirm the draft can log reps without weight.
8. Search for a missing exercise, create it locally, select it, and confirm it
   appears in later search results.
9. Cancel the picker and verify existing active workout drafts/logged rows are
   unchanged.

Manual device validation may be deferred if no emulator/device is attached,
but must be completed before release.
