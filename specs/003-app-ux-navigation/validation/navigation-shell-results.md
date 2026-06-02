# Validation: Navigation Shell Results

**Date**: 2026-05-30

## Unit Tests

`./gradlew :shared:testDebugUnitTest` — PASS on 2026-05-30.

Coverage includes route parsing/fallbacks, compact/medium/expanded layout
classification, app shell hydrate/select/resume/dismiss transitions, and
last-opened-route preservation through active session updates.

## Android Build

`./gradlew :design-system:testDebugUnitTest :shared:compileKotlinMetadata :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` — PASS on 2026-05-30.

## iOS Compile

`./gradlew :shared:compileKotlinIosSimulatorArm64` — PASS on 2026-05-30.

## Material Scan

`rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts` — PASS on 2026-05-30 with no matches.

Note: the initially generated broad scan pattern included bare `Surface`,
which also matches valid design-system token names such as `onSurface`. The
recorded gate uses a word-boundary `Surface` pattern to target Material usage
without rejecting `FitTheme.colors.onSurface`.

## Accessibility Review

Static review completed on 2026-05-30 for:

- `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt`
- `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/AppNavRail.kt`

Result: PASS for this slice. Compact navigation uses `FitTabBar` with tab
roles and design-system haptics; rail items use `FitListRow` with tab role and
selected state; resume and shell actions use design-system buttons with
`FitTheme.size.touchMin`-backed targets. Haptics are gated through `FitTheme`,
and visual feedback remains present when haptics are disabled.

## Manual Android Milestone

Deferred with user approval on 2026-05-30. `/Users/swig/Library/Android/sdk/platform-tools/adb devices`
returned no attached devices or emulators, so the manual Android milestone will
be run later before release/device validation rather than blocking this
implementation slice.
