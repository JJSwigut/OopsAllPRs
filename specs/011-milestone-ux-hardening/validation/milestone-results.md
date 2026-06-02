# Milestone UX Hardening Validation

## Target

- Branch: `codex/011-milestone-ux-hardening`
- Date: 2026-05-30
- App build: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Package: `com.jjswigut.oopsallprs.android`
- Runtime target: Pixel-class AVD `OopsAllPRs_Pixel_9_Pro`, `1280x2856`, density `480`

## Environment Notes

- Fresh emulator boots may show an Android System UI warning. This was treated as `ENV-001` because the app stayed focusable and responsive after dismissing or waiting.
- ADB activity resolution uses `com.jjswigut.oopsallprs.android/com.jjswigut.oopsallprs.MainActivity`.

## Existing Deferred Gates

- `specs/002-neo-glass-design-system/tasks.md`: T024 on-device visual tuning pass for roller centering and faux-glass glow.
- `specs/006-workout-logging-ux/tasks.md`: T054 manual Pixel flow, T056 keyboard/status/navigation overlap, T057 active logging accessibility and alternatives.
- `specs/007-history-templates/tasks.md`: T038 manual Pixel flow, T040 History/template accessibility and alternatives.

## Automated Gates

- Android debug build: PASS, `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- Targeted regression tests: PASS, `./gradlew :shared:testDebugUnitTest --tests "com.jjswigut.oopsallprs.ui.workout.WorkoutHomeStateHolderTest" --tests "com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolderTest" --tests "com.jjswigut.oopsallprs.ui.exercise.ExercisePickerUxStateTest"`
- Full shared unit tests: PASS, `./gradlew :shared:testDebugUnitTest`
- iOS simulator compile: PASS, `./gradlew :shared:compileKotlinIosSimulatorArm64`
- Material/design-system scan: PASS, `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src design-system/src shared/build.gradle.kts design-system/build.gradle.kts` returned no matches.
- Whitespace validation: PASS, `git diff --check -- .`
- Accessibility follow-up build: PASS on 2026-05-31, `./gradlew :design-system:testDebugUnitTest --tests com.jjswigut.oopsallprs.ds.ComponentMathTest :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- Accessibility follow-up iOS compile: PASS on 2026-05-31, `./gradlew :shared:compileKotlinIosSimulatorArm64`
- Accessibility follow-up whitespace validation: PASS on 2026-05-31, `git diff --check -- .`

## Scenario Results

### CORE-START-LOG-FINISH

- Result: PASS after fixes.
- Evidence: `core-20-final-picker-scroll.png`, `core-21-final-bodyweight-ready.png`, `core-22-final-bodyweight-focus-kept.png`, `core-23-finish-history.png`, `continuity-07-active-empty-final.png` through `continuity-13-train-after-finish-final.png`.
- Notes: Start workout, bottom Add Exercise, full seeded picker browsing, bodyweight reps-only logging, direct numeric replacement, inline PR feedback, and Finish to completed summary were validated on the AVD.

### CONTINUITY-HISTORY-PROGRESS-PROFILE

- Result: PASS.
- Evidence: `continuity-14-history-summary-final.png` through `continuity-26-restart-session-recovered.png`.
- Notes: Completed workout summary, save-as-template, Train template visibility, template launch, Progress PR evidence, Profile units/export, Android share-sheet handoff, and restart recovery with active workout resume were validated.

### POLISH-ACCESSIBILITY-INTERACTION

- Result: PASS for emulator hierarchy/state polish; live screen-reader/device-setting verification remains a pre-release manual gate.
- Evidence: `continuity-06-train-clean.png`, `continuity-09-picker-scrolled-final.png`, `continuity-11-bodyweight-logged-final.png`, `continuity-20-progress-final.png`, `continuity-21-profile-final.png`, `polish-01-profile-accessibility.png`, and UIAutomator dump `/tmp/oops18-profile-top-final.xml`.
- Notes: Thumb-zone reachability, shape-consistent buttons, reduced visible glow intensity, status/nav overlap, keyboard replacement behavior, selected tab/segment labels, and Profile switch labels were reviewed on the Pixel-class AVD. UIAutomator cannot prove spoken TalkBack output, OS reduced-motion behavior, or haptic-disabled device behavior, so those remain separate pre-release checks.

## UX Findings

- `UX-001` P1: Exercise picker row selection conflicted with browse gestures. Long swipes over row content could select a row instead of scrolling past the first visible A-list items.
- `UX-002` P1: Logging a set for a non-first/bodyweight exercise could move focus back to the first exercise because focus was restored by the old draft id after a new draft was generated.
- `UX-003` P1: After finishing a workout and saving the last opened route, Train could show a false `Resume workout` state because route-only session rows were treated as active sessions.
- `UX-004` P2: Segmented controls and bottom tabs exposed label and selected state as split accessibility nodes, making selected controls ambiguous in hierarchy inspection.
- `UX-005` P2: Profile switch labels were not passed into the reusable switch component, so the control depended on nearby visible text rather than owning its accessible label.
- `ENV-001`: The local AVD intermittently showed an Android System UI warning during startup; app flows were still testable.

## Fixes

- `UX-001`: `ExercisePickerFlow.kt` now leaves row bodies as stable scroll surface and makes exercise selection explicit through each row's `Add` button.
- `UX-002`: `ActiveWorkoutModels.kt` now preserves focus by exercise instance id and refreshes the focused draft id to the next draft for the same exercise. Regression coverage added in `ActiveWorkoutStateHolderTest.kt`.
- `UX-003`: `WorkoutHomeStateHolder.kt` now treats only sessions with a non-null `activeWorkoutId` as resumable. Regression coverage added in `WorkoutHomeStateHolderTest.kt`.
- `UX-004`: `FitTabBar.kt` and `FitSegmentedControl.kt` now clear descendant semantics after click wiring and expose role, label, selected state, and state description on the same control node. Stable state-description coverage was added in `ComponentMathTest.kt`.
- `UX-005`: `FitToggle.kt` now accepts a label, exposes switch role/state description, avoids initial-composition haptics, and performs haptics only when the value changes. `ProfileFlow.kt` passes the row label into the switch.

## Screenshots

- Baseline: `baseline-01-launch.png`, `baseline-02-train.png`, `baseline-05-black-after-wait.png`.
- Core findings/fixes: `core-03-picker-open.png`, `core-05-picker-scrolled-long.png`, `core-09-picker-fixed-scrolled.png`, `core-12-weight-replaced.png`, `core-18-bodyweight-set-logged.png`, `core-22-final-bodyweight-focus-kept.png`.
- Final continuity: `continuity-12-finish-history-final.png`, `continuity-13-train-after-finish-final.png`, `continuity-18-train-template-final.png`, `continuity-19-template-launched-final.png`, `continuity-20-progress-final.png`, `continuity-22-export-workouts-final.png`, `continuity-26-restart-session-recovered.png`.
- Accessibility follow-up: `polish-01-profile-accessibility.png`.

## Deferred-Gate Updates

- `006-workout-logging-ux`: manual Pixel-class flow, keyboard/status/nav overlap, and emulator hierarchy label/state checks are now covered by 011/018 evidence. Live TalkBack, OS reduced-motion, and OS haptic-disabled verification remain pre-release manual checks.
- `007-history-templates`: manual Pixel-class flow and emulator hierarchy label/state checks are now covered by 011/018 evidence. Live TalkBack, OS reduced-motion, and OS haptic-disabled verification remain pre-release manual checks.
- `002-neo-glass-design-system`: on-device visual tuning is covered for current app surfaces, including button shape clipping and glow intensity. Instrumented UI semantics and font-bundling tasks remain separate deferred work.
