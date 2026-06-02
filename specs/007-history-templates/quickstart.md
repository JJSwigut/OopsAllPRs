# Quickstart: Completed Workout History and Templates

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
2. Start an empty workout, add one weighted exercise, and log at least two sets.
3. Add one bodyweight exercise and log at least one reps-only set.
4. Finish the workout and confirm the completed summary shows duration,
   exercises, logged sets, bodyweight rows, and PR markers when present.
5. Open History and confirm the completed workout appears in the list.
6. Open the History detail and confirm it matches the finish summary.
7. Save the completed workout as a template with a non-blank name.
8. Attempt a blank template name and confirm validation is visible and
   non-destructive.
9. Return to Train and confirm the saved template is visible or directly
   reachable without hiding Start workout.
10. Launch the template and confirm active logging opens with planned set
    targets but no logged rows copied from the completed workout.
11. Restart the app and confirm History and templates remain available.

## Completion Evidence

- Shared unit test results for summary mapping, History ordering/detail,
  bodyweight completed rows, template save validation, template/history
  separation, and template launch.
- Android debug build result.
- iOS simulator shared compile result.
- Material scan result.
- Manual screenshots or notes for finish summary, History list/detail,
  template save, Train template launch, and launched active workout state.
