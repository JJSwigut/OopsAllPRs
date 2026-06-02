# Validation: Catalog Exercise Selection Results

**Date**: 2026-05-30

## Unit Tests

Passed.

- `./gradlew :shared:testDebugUnitTest`
- Covered local default results, seeded search, seeded selection/focus handoff,
  bodyweight reps-only selection, custom exercise creation, cancel/no-op
  behavior, and add failure preservation.

## Android Build

Passed.

- `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`

## iOS Compile

Passed.

- `./gradlew :shared:compileKotlinIosSimulatorArm64`

## Material Scan

Passed.

- `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts`
- Result: no matches.

## Manual Android Milestone

Deferred until an Android device/emulator and `adb` are available.

- `adb devices` could not run in this shell: `adb: command not found`.
- Manual milestone remains a release gate: open Add Exercise from an active
  workout, search/select seeded weighted and bodyweight exercises, create a
  missing local exercise, verify later local search, cancel the picker, and
  confirm existing workout drafts/logged rows are preserved.
