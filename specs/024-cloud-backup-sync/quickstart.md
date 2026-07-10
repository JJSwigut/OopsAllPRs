# Quickstart: User-Owned Cloud Backup Sync

## Automated Validation

Run common tests:

```bash
./gradlew :shared:allTests
```

Run Android unit tests:

```bash
./gradlew :shared:testDebugUnitTest
```

Run SQLDelight/schema-related checks through the normal shared build:

```bash
./gradlew :shared:build
```

Compile Android debug:

```bash
./gradlew :androidApp:assembleDebug
```

Compile iOS shared framework where the local Xcode/Kotlin environment supports it:

```bash
./gradlew :shared:iosSimulatorArm64Test
```

## Manual Android Smoke

1. Install debug app on an Android device or emulator.
2. Open Profile.
3. Choose Link backup file and create a visible `.json` backup in a document provider.
4. Run Backup now and confirm Profile shows success.
5. Force-stop and restart the app.
6. Confirm the linked file display name remains visible.
7. Run Sync now and confirm no conflict when nothing changed.
8. Make a local training change, return to Profile, run Sync now, and confirm the backup is updated.
9. Modify or replace the backup with a different valid package, run Sync now, and confirm the restore prompt or conflict sheet appears as expected.

### Android Document-Provider Notes

- Run the smoke flow once with the system file picker local Documents provider and once with an installed cloud-backed provider such as Google Drive, Dropbox, or a synced folder app.
- The app should request only the document picker flow; no broad storage permission prompt should appear.
- After force-stop/restart, Backup now and Sync now should still read/write the linked URI through the persisted document permission.
- If the provider removes the file or revokes access, Profile should keep local logging available and show a recoverable linked-file error.
- A Restore from file flow should ask for a safety backup destination before local rows are replaced.

## Restore Safety Scenario

1. Start a local workout and log at least one set.
2. Choose Restore from file.
3. Select a valid backup with different active session state.
4. Confirm the restore UI warns about replacing the active workout.
5. Verify a safety backup is created or offered before destructive replacement.
6. Complete restore and verify Profile, Train, History, Routines, Exercises, and Progress hydrate from the backup.

## Failure Scenario

1. Link a backup file, then remove provider access or move the file outside the app.
2. Relaunch the app and open Profile.
3. Confirm the linked status reports a recoverable access error.
4. Verify local workout logging still works.
5. Relink a valid backup file and confirm the error clears after successful read/write.

## Evidence to Record

- Backup serialization round-trip test results.
- Restore into empty database test results.
- Simulated restore failure transaction test results.
- Conflict policy test results.
- Android document provider smoke result.
- iOS adapter compile result.
- Profile state test result.
- Notes that CSV exports still work and plain backup privacy copy is visible.
