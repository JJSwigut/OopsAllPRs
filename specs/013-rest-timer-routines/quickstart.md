# Quickstart: Routine-Aware Rest Timers

## Automated Validation

1. Run `./gradlew :shared:testDebugUnitTest`.
2. Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`.
3. Run `./gradlew :shared:compileKotlinIosSimulatorArm64`.
4. Run the shared Material guard:

   ```sh
   rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
   ```

5. Run `git diff --check -- .`.

## Manual Android Smoke

1. Install and open the app on a Pixel-class device or emulator.
2. Start an empty workout.
3. Add an exercise and verify the default rest setting is visible in the active workout.
4. Log a set and verify the RestBar starts automatically.
5. Tap +15, -15, and Skip.
6. Launch a saved routine with different exercise rest durations and verify each exercise starts the correct timer.
7. Background or lock the app during rest and confirm alert behavior where permissions allow.
8. Restart during rest and confirm remaining time is wall-clock accurate.
9. Finish or discard and confirm rest/resume state clears.

## Evidence To Record

- Unit/build command results.
- RestBar after auto-start.
- RestBar after adjustment.
- Routine-launched per-exercise rest.
- Restart recovery state.
- Android alert behavior or permission limitation note.
