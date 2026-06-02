# Validation Results: Routine & Exercise Management

Date: 2026-05-31

## Automated Gates

- PASS: `./gradlew :shared:testDebugUnitTest :shared:compileDebugKotlinAndroid :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64`
  - Result: `BUILD SUCCESSFUL`
  - Notes: Kotlin expect/actual beta warnings remain in existing platform adapter files.
- PASS: Material guard against shared/design-system sources:
  - Command: `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts`
  - Result: no matches.
- PASS: `git diff --check -- .`

## Manual Android Smoke

- PASS: Pixel 9 Pro emulator (`OopsAllPRs_Pixel_9_Pro`, Android 15) debug install and launch.
- PASS: Train -> create routine -> search "bench" -> add Bench Dips bodyweight exercise -> save.
- PASS: Train -> edit saved routine -> routine editor opens with saved bodyweight set and rest controls.
- PASS: Train -> launch saved routine -> active workout opens with bodyweight reps-only logging card.
- PASS: Log bodyweight set -> rest timer starts -> completed set shows PR -> finish workout shows completed summary.
- PASS: Profile -> manage exercises -> create custom bodyweight exercise -> archive with confirmation -> custom exercise list returns to empty.
- PASS: Start fresh workout -> search archived custom exercise -> archived exercise is hidden from picker; no local matches are shown.
- PASS: Active workout can be discarded after picker search cleanup.

Notes:

- Local ADB did not detect a physical Pixel device during this pass, so validation used the configured Pixel 9 Pro emulator.
- ADB text entry with spaces writes literal escape sequences, so the smoke routine name became `Test%20Routinexbench`; this is an automation artifact, not a user-keyboard finding.
- Product polish follow-ups observed during smoke: the clean Train screen has excessive empty vertical space, and bodyweight exercise metadata renders as `Bodyweight • Bodyweight` in routine/exercise picker contexts.
