# Quickstart: Developer Demo Data Seeding

## Automated Validation

Run from `/Users/swig/Development/oops-all-prs`:

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "androidx\\.compose\\.material3|MaterialTheme|androidx\\.compose\\.material\\." shared design-system androidApp --glob '*.kt'
git diff --check -- .
```

Expected:
- Shared tests pass.
- Android debug app builds.
- iOS simulator shared compile passes.
- Material guard returns no matches.
- Whitespace check passes.

## Manual Android Debug Review

1. Install and launch the debug app on a Pixel-class device or emulator.
2. Open Profile.
3. Confirm a developer seed card is visible.
4. Tap `Progress demo`.
5. Open History and verify completed workouts appear.
6. Open Progress and verify PR rows, exercise groups, and an exercise chart with multiple points.
7. Return to Profile and tap `Routines demo`.
8. Open Train and verify demo routines appear with rest-aware planned sets.
9. Tap `Active recovery demo` only when no active workout exists.
10. Confirm the resume banner appears, kill/restart the app, and verify the same active workout restores.
11. Tap the same demo actions again and verify feedback reports skipped/existing data rather than duplicates.

## Release Visibility Check

Build or inspect release configuration before public release:

```bash
./gradlew :androidApp:assembleRelease
```

Expected:
- Release build does not show developer seed controls in Profile.
- No seed scenario is invoked automatically.
