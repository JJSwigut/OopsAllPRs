# Workout Logging UX V1 Validation

## Common Tests

- PASS: `./gradlew :shared:testDebugUnitTest`
- Result: 75 tests, 0 failures, 0 errors.
- Covered typed active workout UX persistence, compact set input math,
  Train resume/start state, focused active mode state, weighted draft logging,
  dense picker add/cancel behavior, bodyweight reps-only logging, persisted
  focus/draft recovery, and inline weighted/bodyweight PR feedback.

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
  completed the active logging milestone flow.
- Evidence is recorded in
  `specs/011-milestone-ux-hardening/validation/milestone-results.md`.
- Screenshots include `continuity-07-active-empty-final.png`,
  `continuity-09-picker-scrolled-final.png`,
  `continuity-11-bodyweight-logged-final.png`,
  `continuity-12-finish-history-final.png`, and
  `continuity-13-train-after-finish-final.png`.
- Launch command: `$HOME/Library/Android/sdk/platform-tools/adb shell monkey -p com.jjswigut.oopsallprs.android -c android.intent.category.LAUNCHER 1`
- Foreground check confirmed `com.jjswigut.oopsallprs.android/com.jjswigut.oopsallprs.MainActivity`
  as `mCurrentFocus`.
- UX feedback iteration installed on Pixel 9 Pro after moving primary actions
  toward the bottom, replacing capped picker defaults with a full scrollable
  catalog plus recent exercises, rounding glass sheen clipping, and using
  numeric/select-all set inputs.
- Latest screenshot showed the phone on the lock screen/notification shade, so
  app foreground visibility still requires unlocking the physical device.
- Follow-up thumb-zone iteration completed: active set input is now docked in a
  bottom logging panel, exercise search moved to the picker bottom controls,
  secondary/icon/text-field glow was reduced, and exercise cards no longer embed
  the editable logging controls.
- Final thumb-zone iteration added a reachable Finish action in the active
  logging bottom controls and removed the large highlighted glass shell around
  the bottom navigation bar.
- 011 follow-up found and fixed three milestone blockers: picker row selection
  was interfering with scroll gestures, bodyweight logging could lose focus to
  the first exercise, and route-only session rows could show a false resume
  state after finish.
- Keyboard/direct-entry check passed: selecting a numeric field and typing
  replaced the existing value rather than appending, with the logging controls
  still visible.

## Screenshots

- See `specs/011-milestone-ux-hardening/validation/screenshots/`.

## Deferred Gates

- COMPLETE: Pixel-class manual review for the active logging milestone.
- COMPLETE: keyboard/status/navigation overlap review for the active logging
  flow covered by the 011 screenshots.
- DEFERRED: TalkBack labels/state descriptions, reduced-motion behavior, and
  non-haptic alternatives still require a dedicated accessibility pass.

## Story Notes

- US1: Automated state tests cover start/resume, focused active mode,
  defaulted weighted drafts, pending duplicate suppression, successful logging,
  and retry-preserving validation failures.
- US2: Automated picker tests cover dense local search selection, focus handoff,
  cancel preserving active drafts/focus, and bodyweight seeded selection.
- US3: Automated tests cover bodyweight drafts defaulting to null weight,
  reps-only confirmation, and bodyweight picker-to-logging flow.
- US4: Automated tests cover persisted focus/draft hydration and inline PR
  feedback for weighted and bodyweight logged rows.

## Static Reviews

- PASS: `git diff --check -- .`
- PASS: Active logging UI files use FitTheme/design-system styling paths; no
  raw dp/sp/Color styling was found in the active logging scan.
- PASS: keyboard/status/navigation overlap scenarios were manually reviewed on
  the Pixel-class AVD during feature 011.
- DEFERRED: TalkBack labels/state descriptions, reduced-motion behavior, and
  non-haptic alternatives require dedicated manual device review.
