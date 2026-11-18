# Foundation Release Gates

## Android Backup and Permissions

- Android Auto Backup is enabled for database and shared preference domains. Android 12 and later use `androidApp/src/main/res/xml/data_extraction_rules.xml` for cloud backup and device transfer. The legacy `fullBackupContent` file remains aligned if support is later broadened to older Android versions.
- The persisted `android_auto_backup_allowed` preference does not change Android OS backup behavior. The manifest and extraction rules control that behavior, so changing the backup policy requires updating both rule files and the privacy disclosures.
- Notification permission is declared for future rest-complete notification scheduling.
- Rest notification scheduling remains behind shared platform adapters.

## Export Behavior

- Export-ready CSV data is available for workouts, exercises, routines, and personal records through the shared export service.
- CSV fields containing commas, quotes, or newlines are escaped.
- Bodyweight reps-only sets are included in workout exports.

## Release Artifact Checks

- Android debug artifact builds with `./gradlew :androidApp:assembleDebug`.
- Shared Android unit tests pass with `./gradlew :shared:testDebugUnitTest`.
- iOS adapter compile is validated with `./gradlew :shared:compileKotlinIosSimulatorArm64`.

## Remaining Manual Gate

Real-device milestone validation is still required for:
- Fresh install to first logged set in 30 seconds or less.
- Process-death recovery in 2 seconds or less.
- Backup/export behavior review on a supported Android device.
- Accessibility review under gym-side use conditions.
