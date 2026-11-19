# Foundation Release Gates

## Android Backup and Permissions

- System-managed backups are the intended release policy on both platforms: Android Auto Backup and Apple device/iCloud backups. Do not exclude the workout database or preferences from either platform's system backup.
- Android Auto Backup is enabled for database and shared preference domains. Android 12 and later use `androidApp/src/main/res/xml/data_extraction_rules.xml` for cloud backup and device transfer. The legacy `fullBackupContent` file remains aligned if support is later broadened to older Android versions.
- The persisted `android_auto_backup_allowed` preference does not change Android OS backup behavior. The manifest and extraction rules control that behavior, so changing the backup policy requires updating both rule files and the privacy disclosures.
- iOS keeps the SQLDelight database in the default `Application Support` container and sets no backup exclusion, so system device and iCloud backups include it without extra configuration.
- `ruby fastlane/test/system_backup_policy_test.rb` guards both platform policies; it runs as part of `tools/release_gate.sh`.
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
