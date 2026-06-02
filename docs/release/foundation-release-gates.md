# Foundation Release Gates

## Android Backup and Permissions

- Android Auto Backup is enabled for database and shared preference domains through `androidApp/src/main/res/xml/backup_rules.xml`.
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
