# Quickstart: Mistake Recovery and Editing

## Automated Validation

Run from `/Users/swig/Development/oops-all-prs`:

```sh
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts
git diff --check -- .
```

The Material scan passes when it returns no matches.

## Manual Android Smoke

Use a Pixel-class emulator or connected Pixel device.

1. Install and launch `androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
2. Start a workout from Train.
3. Add a weighted exercise, log a set, edit the logged set, and verify the row updates without duplicate rows.
4. Delete a logged set and verify the active workout remains usable.
5. Log multiple sets and use undo last set; verify exactly one most-recent set is removed.
6. Add a bodyweight exercise, log and edit reps, and verify no weight is required.
7. Restart the app and verify the corrected active workout state is restored.
8. Discard the active workout through confirmation and verify Train has no resume banner after restart.
9. Complete a workout that creates a PR, delete it from History through confirmation, and verify Progress no longer shows PR evidence from that workout.
10. Save a completed workout as a template, delete the template through confirmation, and verify Train no longer lists it.

## Expected Evidence

Record results in `specs/012-mistake-recovery-editing/validation/mistake-recovery-results.md`.

Suggested screenshots:

- `active-edit.png`
- `active-delete.png`
- `active-undo.png`
- `discard-confirm.png`
- `history-delete.png`
- `template-delete.png`
- `restart-recovery.png`

## Out of Scope

- Full set edit history or revision browsing.
- Sync tombstones or cloud conflict resolution.
- Batch deletion.
- Native iOS runtime smoke beyond shared compile validation.
