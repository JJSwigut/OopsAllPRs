# Quickstart: Android Alpha Hardening

## Automated Validation

1. Run `./gradlew :shared:testDebugUnitTest`.
2. Run `./gradlew :androidApp:assembleDebug`.
3. Run `./gradlew :shared:compileKotlinIosSimulatorArm64`.
4. Run the shared Material guard:

   ```sh
   rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
   ```

5. Run `git diff --check -- .`.

## Manual Android Alpha Smoke

1. Install `androidApp/build/outputs/apk/debug/androidApp-debug.apk` on a Pixel-class device or emulator.
2. Launch fresh app data and confirm Train has immediate start/create actions, no overlap, and thumb-reachable primary controls.
3. Start an empty workout.
4. Open exercise picker, search for a common exercise, and add it.
5. Log one weighted set.
6. Add or choose a bodyweight exercise and log a reps-only set.
7. Start a rest timer, skip it, start it again, and let it complete.
8. Deny or disable notification display if available, then confirm rest completion does not break active workout state.
9. Force close and reopen with an active workout; confirm resume affordance and active workout identity.
10. Finish the workout and confirm it appears in History.
11. Confirm Progress empty/PR state is truthful for the available data.
12. Open Profile and confirm local storage, sync off, backup, rest alert, export, and alpha readiness messages.
13. Use export/share path if available and confirm it produces user-owned local data output.

## Evidence To Record

- Command results from automated validation.
- Device or emulator model used.
- Pass/fail/deferred status for each manual smoke step.
- Screenshots or notes for any spacing, overlap, keyboard, or top/bottom reach issue.
- Known limitations that should become future backlog items.
