# Quickstart: Active Workout Resume

## Automated Validation

Run:

```bash
./gradlew :shared:testDebugUnitTest
```

Expected:

- Workout home tests pass.
- Template launch conflict suppression is covered.
- Seed refresh tests show missing packaged exercises are added without duplicate canonical names.

## Manual Android Validation

1. Install the debug app on the connected Pixel 9 Pro:

   ```bash
   ./gradlew :androidApp:installDebug
   ```

2. Launch the app:

   ```bash
   /Users/swig/Library/Android/sdk/platform-tools/adb -s 56121FDAP002FT shell monkey -p com.jjswigut.oopsallprs.android -c android.intent.category.LAUNCHER 1
   ```

3. With no active workout, verify Train still offers Start workout.
4. Start a workout and return to Train.
5. Verify Train no longer shows a top Resume workout button or Start workout button.
6. Verify the lower active-workout card shows Resume and Discard.
7. Tap Resume and verify the active workout opens.
8. Return to Train, tap Discard on the lower card, and verify the card disappears.
9. Search exercises for `Dead Hang` and verify it appears after hydration on an existing install.
