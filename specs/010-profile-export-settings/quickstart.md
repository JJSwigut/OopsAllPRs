# Quickstart: Profile Settings and Local Export

## Automated Validation

Run from `/Users/swig/Development/oops-all-prs`:

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts
git diff --check -- .
```

Expected:

- Shared tests pass.
- Android debug compile/build passes.
- iOS simulator shared compile passes.
- Material scan returns no matches.
- Whitespace check returns no output.

## Manual Android Smoke

1. Launch the app on a connected Android device or emulator.
2. Open Profile.
3. Switch units between pounds and kilograms.
4. Leave Profile, open Progress, and confirm progress labels use the selected unit when PR data exists.
5. Return to Profile and export Workouts, Routines, Exercises, and PRs.
6. Confirm Android opens the share/export surface for each generated CSV.
7. Toggle haptics, reduced motion, and palette mode; confirm the app shell responds immediately.
8. Kill and relaunch the app; confirm the selected weight unit is restored.

## Deferred Manual Gate Handling

If no Android device or emulator is available, run:

```bash
~/Library/Android/sdk/platform-tools/adb devices -l
~/Library/Android/sdk/emulator/emulator -list-avds
```

Record the device/emulator discovery result in the final implementation notes
and defer the manual Profile/export gate to milestone verification.
