# Quickstart: Exercise Progress Charting

## Automated Validation

1. Run `./gradlew :shared:testDebugUnitTest`.
2. Run `./gradlew :androidApp:assembleDebug`.
3. Run `./gradlew :shared:compileKotlinIosSimulatorArm64`.
4. Run the shared Material guard:

   ```sh
   rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
   ```

5. Run `git diff --check -- .`.

## Manual Android Smoke

1. Launch the debug app on a Pixel-class emulator or device.
2. Ensure the app has completed workouts with PR/progress points.
3. Open Progress.
4. Select an exercise with trend points.
5. Confirm the exercise detail shows a chart and metric selector.
6. Switch metrics and confirm chart/trend rows update.
7. Select a trend row or point with source evidence and confirm evidence opens.
8. Confirm an active workout resume banner, if present, does not overlap chart controls.

## Evidence To Record

- Command results.
- Pixel/emulator model.
- Progress chart screenshot or deferred reason.
- Any known visual or interaction limitations.
