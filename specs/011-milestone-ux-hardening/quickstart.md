# Quickstart: Milestone Validation and UX Hardening

## 1. Build and Install Android Debug App

```bash
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
~/Library/Android/sdk/platform-tools/adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
~/Library/Android/sdk/platform-tools/adb shell am start -n com.jjswigut.oopsallprs.android/com.jjswigut.oopsallprs.MainActivity
```

If no device is connected, use the configured Pixel-class AVD:

```bash
~/Library/Android/sdk/emulator/emulator -avd OopsAllPRs_Pixel_9_Pro -no-snapshot -no-audio -gpu swiftshader_indirect
```

## 2. Capture Evidence

Store screenshots under:

```text
specs/011-milestone-ux-hardening/validation/screenshots/
```

Use stable names such as:

```bash
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > specs/011-milestone-ux-hardening/validation/screenshots/core-01-train.png
```

Record notes in:

```text
specs/011-milestone-ux-hardening/validation/milestone-results.md
```

## 3. Manual Scenario Checklist

### Core Logging Loop

- Start from Train with no active workout.
- Start a fresh workout.
- Open the exercise picker.
- Browse beyond the first visible catalog section without search.
- Search for and select one weighted exercise.
- Log at least two weighted sets.
- Tap a numeric field, type a replacement value, and log the corrected set.
- Add one bodyweight exercise.
- Confirm bodyweight logging requires reps only.
- Finish the workout.
- Confirm the completed workout is reachable from History.

### Cross-Destination Continuity

- Review the completed workout in History.
- Save or launch a template where available.
- Open Progress and inspect PR evidence or the empty state.
- Open Profile, change units if needed, and trigger export.
- Restart the app and verify the relevant local state is still discoverable.

### Accessibility and Interaction Polish

- Review primary touch targets on Train, active workout, picker, History,
  Progress, and Profile.
- Check that selected/pressed/checked feedback follows rounded control shapes.
- Check that glow does not overlap neighboring rows or controls.
- Verify keyboard does not hide the next logging action.
- Verify reduced-motion and haptic-disabled preferences preserve visible state.
- Review TalkBack labels/state descriptions where possible on the target.

## 4. Automated Gates

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
git diff --check -- .
```

The Material scan passes when `rg` returns no matches.

## 5. Deferred Gate Cleanup

Update these files after the milestone pass:

- `specs/006-workout-logging-ux/validation/workout-logging-ux-results.md`
- `specs/006-workout-logging-ux/tasks.md`
- `specs/007-history-templates/validation/history-templates-results.md`
- `specs/007-history-templates/tasks.md`
- `specs/002-neo-glass-design-system/validation/build-and-test-results.md`
- `specs/002-neo-glass-design-system/tasks.md`
