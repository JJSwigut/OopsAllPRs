# Validation Results: Routine-Aware Rest Timers

Recorded on 2026-05-31.

## Automated Gates

- PASS: `./gradlew :shared:testDebugUnitTest`
- PASS: `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- PASS: `./gradlew :shared:compileKotlinIosSimulatorArm64`
- PASS: Material guard found no `material3`, `MaterialTheme`, or `Surface` references in shared/design-system sources.
- PASS: `git diff --check -- .`

## Coverage Notes

- Rest auto-start is covered by common active-workout state-holder tests.
- SQLDelight persistence covers active exercise rest settings, active session rest recovery, default rest preference, and rest sound preference.
- Routine tests cover completed-workout-to-routine rest preservation and default-rest fallback when completed exercise rest was disabled.
- Rest controls cover +15, -30, skip, focus preservation, and deleted-origin cleanup.
- Scheduler tests cover platform-boundary schedule and cancel behavior using a common fake scheduler.

## Android Alert Permission Behavior

Android rest alerts are scheduled through `AlarmManager.setAndAllowWhileIdle` and delivered by `RestTimerReceiver`. The receiver posts a notification on a sound or silent channel based on the user's rest sound preference. If Android notification permission is denied or unavailable, notification posting is caught as a `SecurityException`; the active session still recovers visually on return through the wall-clock anchored rest state.

## Manual Follow-Up

Pixel/emulator manual validation was not run in this pass. The next manual check should verify audible notification behavior on a real device with notification permission allowed and denied.
