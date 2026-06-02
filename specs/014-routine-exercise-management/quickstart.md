# Quickstart: Routine & Exercise Management

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

1. Open the app on a Pixel-class device or emulator.
2. From Train, create a routine named "Push".
3. Add Bench Press and Pull-Up.
4. Configure Bench Press weighted sets and Pull-Up reps-only sets.
5. Change rest settings for each exercise.
6. Save the routine and verify it appears in Train.
7. Launch the routine and verify active workout planned sets/rest values.
8. Edit the routine name and one set target, save, and launch again.
9. From Profile, create a custom exercise, rename it, and archive it.
10. Verify archived custom exercises no longer appear in picker default/search results.
11. Start an active workout, create/open management surfaces, cancel, and verify resume/focus/rest recovery remains intact.

## Evidence To Record

- Unit/build command results.
- Created routine in Train.
- Edited routine launch behavior.
- Custom exercise create/edit/archive behavior.
- Active workout non-regression after management flow.
- Android compact layout review notes.
