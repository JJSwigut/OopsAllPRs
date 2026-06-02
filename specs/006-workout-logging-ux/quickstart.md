# Quickstart: Workout Logging UX V1

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

## Android Manual Milestone

1. Install and launch the debug app on a Pixel-class phone or emulator.
2. Confirm Train has no app title and Start workout is the primary action.
3. Start an empty workout and confirm active logging opens full-screen with no
   bottom navigation.
4. Open Add Exercise, search for a seeded weighted exercise, and add it.
5. Log three weighted sets using defaults and steppers; confirm the flow can be
   completed quickly without opening a separate editor.
6. Add a bodyweight exercise and log three reps-only sets without entering a
   required weight.
7. Trigger or seed a record-setting set and confirm inline PR feedback appears
   on the logged row without a modal.
8. Open Add Exercise with the keyboard visible and confirm search/results/add
   controls do not overlap.
9. Edit a draft, restart the app, resume the workout, and confirm logged rows,
   focus, and edited draft values recover.
10. Simulate a persistence failure if available and confirm no logged row is
    shown, the draft remains intact, and Retry is visible.

## Completion Evidence

- Shared unit test results for draft defaults, persistence recovery,
  bodyweight reps-only logging, failed confirmation, picker cancel/selection,
  and PR feedback.
- Android debug build result.
- iOS simulator shared compile result.
- Material scan result.
- Manual screenshots or notes for Train, Active Workout, Add Exercise with
  keyboard, weighted logging, bodyweight logging, PR feedback, and recovery.
