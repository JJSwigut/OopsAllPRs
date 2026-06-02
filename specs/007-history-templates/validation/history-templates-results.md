# Completed Workout History and Templates Validation

## Common Tests

- PASS: `./gradlew :shared:testDebugUnitTest`
- Result: 90 tests, 0 failures, 0 errors.
- Covered completed workout summary projection, bodyweight completed rows, PR
  markers, History ordering/detail state, template save validation, completed
  history immutability, template launch separation, active workout conflict
  handling, bodyweight template launch, and export-ready routine data.

## Android Build

- PASS: `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`

## iOS Compile

- PASS: `./gradlew :shared:compileKotlinIosSimulatorArm64`

## Material Scan

- PASS: `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts`
- Result: no matches.

## Manual Pixel Review

- COMPLETE via feature 011: installed and launched `androidApp-debug.apk` on
  Pixel-class AVD `OopsAllPRs_Pixel_9_Pro` (`1280x2856`, density `480`) and
  completed the History/template milestone flow.
- Evidence is recorded in
  `specs/011-milestone-ux-hardening/validation/milestone-results.md`.
- Screenshots include `continuity-12-finish-history-final.png`,
  `continuity-14-history-summary-final.png`,
  `continuity-18-train-template-final.png`,
  `continuity-19-template-launched-final.png`, and
  `continuity-26-restart-session-recovered.png`.
- The manual pass also found and fixed the route-only session issue that could
  show a false `Resume workout` action on Train after finishing a workout.

## Screenshots

- See `specs/011-milestone-ux-hardening/validation/screenshots/`.

## Deferred Gates

- COMPLETE: Pixel-class manual review for completed summary, save-as-template,
  Train template visibility, template launch, Progress PR evidence, Profile
  export, and restart recovery.
- DEFERRED: TalkBack/state description, reduced-motion, and non-haptic
  alternative review still require a dedicated accessibility pass.

## Story Notes

- US1: Automated tests cover finish-to-summary state and app-level history
  summary routing after a workout is completed.
- US2: Automated tests cover History empty state, newest-first ordering,
  selection, and clearing detail selection.
- US3: Automated tests cover template name validation, save-from-completed, and
  completed workout immutability after template creation.
- US4: Automated tests cover Train template hydration, launch into active
  workout, conflict behavior, bodyweight planned sets, and no copied logged
  timestamps.

## Static Reviews

- PASS: `git diff --check -- .`
- PASS: History and Train template UI static scan found no raw `dp`, `sp`, or
  `Color(...)` styling literals in the changed shared UI files.
