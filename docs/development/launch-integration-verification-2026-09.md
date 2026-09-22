# Local Launch Integration Verification

Date: 2026-09-21 EDT

## 2026-09-22 Empty Workout Completion Guard

An active workout without a logged set can no longer be completed. The active
workout surface omits `Finish workout` until at least one set is logged; the
state holder also rejects a stale finish request with recovery guidance. Both
the in-memory and SQL completion paths enforce the same validation before
writing history, a completion receipt, or a free-workout allowance change.

Focused Android/JVM coverage passed with 35 tests across finish-state recovery,
completion accounting, SQL atomicity, and entitlement gating:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
./gradlew --no-daemon :shared:testDebugUnitTest \
  --tests 'com.jjswigut.oopsallprs.data.workout.FinishWorkoutLedgerTest' \
  --tests 'com.jjswigut.oopsallprs.data.workout.InMemoryCompletionAccountingTest' \
  --tests 'com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutMistakeRecoveryTest' \
  --tests 'com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCasesTest' \
  --tests 'com.jjswigut.oopsallprs.data.repository.SqlLocalCompletionAtomicityTest'
```

An isolated API 36 `EventideSmoke` run rebuilt, installed, and launched the
debug app, producing `build/smoke/empty-workout-completion-guard-api36/launch.png`
and `foreground-activity.txt`; its QEMU process was shut down afterward. This
is launch proof, while the focused tests provide the completion invariant.

The same shared code was then built and launched on a disposable iPhone 18 Pro
running iOS 27.0. The smoke passed its non-blank settled-frame check and
captured `build/smoke/ios/launch.png` and `launch.txt`; the temporary simulator
was removed afterward.

## 2026-09-21 Recent Training Review

Implemented a local-only Recent Training review on Progress. Its device-local,
injected-time trailing seven-calendar-day window includes only completed
workouts whose `finishedAt` is inside the half-open window. It reports
completed workouts, logged sets, persisted duration, and source-backed granular
records; existing Evidence Ladder readings remain independently computed. No
schema, backup, entitlement, analytics, or store behavior changed.

Focused coverage includes exact lower/upper bounds, source-backed record
filtering, daylight-saving local-midnight boundaries, and sub-minute duration
copy. The full command passed with 687 shared tests and zero failures, errors,
or skips:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
./gradlew --no-daemon :shared:testDebugUnitTest :androidApp:assembleDebug
```

API 35 `EventideSmoke` proof used the debug APK and a temporary completed
workout. The Progress overview showed `Last 7 days`, `1 workout`, `1 set`, and
`<1m`; `View records` opened every source-backed PR in the window, each row
opened evidence detail, and Back returned detail-to-list-to-overview. `Train`
returned to the template/empty-workout surface. The empty seven-day state was
also checked. A first runtime screenshot revealed a content-width card and
`0m` for an 18-second session; the card was made full-width and the duration
now says `<1m`. A second review found that a count action opened only one
record; it was replaced with a real recent-records route and the decorative
facts card was removed from that route. Normal and 320dp/130% text screenshots
plus UI dumps are in `build/smoke/recent-training-review/`. The original smoke
database was restored byte-for-byte, display overrides were reset, and the
emulator was shut down.

## Latest Follow-Up

Trial accounting and completion recovery now pass 656 shared/JVM tests, 16 native
Android tests, migration verification, and debug/release builds. Actual emulator
upgrade preserved history and allowance; a temporary boundary fixture verified
one completed workout consumes one remaining slot and opens History before the
next workout is gated. Details and remaining boundaries are in
`trial-accounting-verification-2026-09.md`. All changes remain local/uncommitted.

## 2026-09-22 Android Smoke Runner Recovery

The Android release gate completed successfully on the integrated worktree with
the iOS package intentionally skipped:

```sh
tools/release_gate.sh --skip-ios
```

It produced the debug APK, unsigned release APK, release bundle, mapping files,
and `build/release-gate/proof.env`. A host-update emulator launch then failed
before ADB registration. This exposed an unbounded `adb wait-for-device` in
`tools/android_emulator_smoke.sh`, which could hang a local or CI smoke run
indefinitely.

The runner now polls ADB with configurable bounded attempts and delay, and
when `ANDROID_SHUTDOWN_STARTED_EMULATOR=1` is set it stops the specific AVD's
forked QEMU child even on failure. The intentional one-attempt failure probe
returned exit code `3`, reported no online emulator, and left no QEMU process
or connected device:

```sh
ANDROID_AVD_NAME=EventideSmoke \
ANDROID_EMULATOR_BOOT_ATTEMPTS=1 \
ANDROID_EMULATOR_BOOT_DELAY_SECONDS=1 \
ANDROID_SHUTDOWN_STARTED_EMULATOR=1 \
tools/android_emulator_smoke.sh
```

This proves the harness recovery behavior, not a new live-app smoke after the
macOS update. The earlier isolated-emulator visual proofs remain valid; the
previous API 35 AVD remains unsuitable for fresh host validation.

The host-compatible retry used the available API 36 AVD with the software
renderer. It built the debug APK, installed
`com.jjswigut.oopsallprs.android.debug`, and reached an awake foreground
`MainActivity` (PID `5900`) on `emulator-5554`:

```sh
ANDROID_AVD_NAME=EventideApi36 \
ANDROID_EMULATOR_ARGS='-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect' \
ANDROID_EMULATOR_BOOT_ATTEMPTS=45 \
ANDROID_EMULATOR_BOOT_DELAY_SECONDS=2 \
ANDROID_SHUTDOWN_STARTED_EMULATOR=1 \
SMOKE_ARTIFACT_DIR=build/smoke/android-api36 \
tools/android_emulator_smoke.sh
```

The settled Train chooser screenshot and foreground state are at
`build/smoke/android-api36/launch.png` and `foreground-activity.txt`. The
emulator shut down after the smoke and no physical device was selected.

The full combined Android release proof then passed with the same isolated
API 36 configuration:

```sh
ANDROID_AVD_NAME=EventideApi36 \
ANDROID_EMULATOR_ARGS='-no-window -no-audio -no-boot-anim -gpu swiftshader_indirect' \
ANDROID_EMULATOR_BOOT_ATTEMPTS=45 \
ANDROID_EMULATOR_BOOT_DELAY_SECONDS=2 \
ANDROID_SHUTDOWN_STARTED_EMULATOR=1 \
tools/release_gate.sh --skip-ios --android-smoke
```

The gate completed at `2026-09-22T15:57:48Z` with Android unit tests, SQLDelight
migration verification, debug/release lint, debug/release APK packaging,
release bundle packaging, and an awake foreground launch of
`com.jjswigut.oopsallprs.android.debug`. Its authoritative proof is
`build/release-gate/proof.env`, which records `android_smoke=passed` and
`ios_release_package=skipped`. The smoke emulator shut down afterward.

### Production First-Run UX (2026-09-22)

The production package was aligned and locally signed only with the Android
debug key for an isolated API 36 emulator; this is not a distributable release
artifact. After clearing only `com.jjswigut.oopsallprs.android`, the Train
destination showed a focused first-run surface: `Start your first workout`, a
dominant `Start workout` action, and a secondary `Create routine` action. No
debug seed, purchase state, history, template, or physical device was used.

Visual and accessibility evidence is in
`build/smoke/production-first-run-api36/train.png` and `train.xml`. The
emulator was shut down after capture.

### Production First Workout State (2026-09-22)

From the same clean production package, selecting `Start workout` opened the
unconfigured active-workout state. It displayed `Starts with first set`,
`Your workout is ready`, and `Add your first exercise when you're ready.` The
primary action is `Add exercise`; `Finish workout` and `Discard workout` remain
available but visually secondary. This confirms a user is not shown a running
timer before logging and has an explicit next action when no exercises exist.

Evidence: `build/smoke/production-first-workout-api36/active.png` and
`active.xml`. The isolated emulator was shut down afterward.

### Production Exercise Picker Orientation (2026-09-22)

The clean production picker initially opened into an alphabetical list with
repeated `Add` actions but no visible page context. `ExercisePickerFlow` now
renders an accessible `Add exercise` heading above the list without changing
the existing recent-result, search, custom-exercise, or add behavior.

The focused `*ExercisePicker*` unit suite passed with selection, cancellation,
custom creation, recovery, and bodyweight cases green. Debug and Release APK
assemblies also passed. The locally debug-signed production runtime proof shows
the heading, seven immediately scannable exercise choices, pinned search, and
custom action in
`build/smoke/production-exercise-picker-heading-api36/picker.png`; the
corresponding XML contains the semantic heading. The isolated emulator was
shut down afterward.

### Production First Set Entry (2026-09-22)

The clean production first-run flow was driven through `Start workout`,
`Add exercise`, and the `Add` action for a seeded exercise. The resulting
active screen showed the selected exercise, `0 sets`, reps and weight steppers,
the primary `Log set` action, and secondary discard/add/finish actions. It
continued to show `Starts with first set` and did not surface a rest timer
before any logged set. Evidence:
`build/smoke/production-first-set-entry-api36/set-entry.png` and
`set-entry.xml`. The isolated emulator was shut down afterward.

## 2026-09-21 Android Candidate

The current local integration was checked without any store, credential, or
branch mutation:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
./gradlew --no-daemon :shared:testDebugUnitTest \
  :androidApp:assembleDebug :androidApp:assembleRelease :androidApp:bundleRelease
```

- Shared Android suite: 684 tests, zero failures, errors, or skips.
- Fresh release outputs: `androidApp-release-unsigned.apk` and
  `androidApp-release.aab`.
- A direct repeat of `:androidApp:assembleRelease :androidApp:bundleRelease`
  passed, including release lint and R8 packaging.

The broader `./gradlew check` was initially blocked by a host CoreSimulator
version mismatch. That environment gate is resolved; the iOS package and
runtime proof below was completed after the macOS/Xcode update.

## 2026-09-22 iOS Release Package and Runtime Smoke

The host now reports macOS 27.0, Xcode 27.0 (27A266a), and CoreSimulator
1171.7. `xcodebuild -license check` and `xcodebuild -checkFirstLaunchStatus`
both passed.

```sh
IOS_RELEASE_TIMEOUT_SECONDS=600 tools/ios_release_package.sh
```

The command completed and produced valid simulator release archives:

- `build/release-artifacts/OopsAllPRs-release-simulator-app.zip`
  SHA-256 `c9cf1769d102739d73704ab827ae6578245955f0bc0feda3b244ec0b8634c83d`
- `build/release-artifacts/OopsAllPRs-release-simulator-dSYM.zip`
  SHA-256 `e7ee6efbaeb57cfb3050797088448b6b83b22f6e9dd95b5baac27bca2345ffb2`

`unzip -t` passed for the app archive. It contains the app executable,
Info.plist, the Rest Timer Live Activity extension, and the Compose exercise
catalog resource.

A disposable iPhone 17 Pro simulator running iOS 26.5 was created, booted,
and given the produced Release `.app`. `OopsAllPRs` launched successfully;
`build/smoke/ios-release-launch.png` and
`build/smoke/ios-release-launch.log` were captured after launch. The app log
contained no `Fatal`, `Exception`, `crash`, or `Terminated` markers. The
simulator was then shut down and deleted. This is launch proof only: it does
not establish a complete iOS interaction flow, StoreKit sandbox purchase, or
App Store/TestFlight delivery.

### Local StoreKit Contract Fixture (2026-09-22)

`iosApp/iosApp/FullAccess.storekit` now defines the local Xcode StoreKit
fixture for the single shared `lifetime_unlock` offer. The fixture declares a
non-consumable purchase at $14.99, matching
`FullAccessBillingConfig.LIFETIME_UNLOCK_PRODUCT_ID`.

```sh
tools/verify_ios_storekit_fixture.sh
IOS_RELEASE_TIMEOUT_SECONDS=600 tools/ios_release_package.sh
```

The fixture contract verifier passed, JSON parsing passed, `xcodebuild -list`
recognized the project, and the Release simulator package rebuilt successfully
with the fixture referenced by the project. This catches local product-ID and
offer-type drift and validates that the project reference does not break
packaging. A focused app-hosted `OopsAllPRsStoreKitTests` target now invokes
`SKTestSession` against the fixture, and the shared Debug scheme references it
for Xcode-driven testing. On this Xcode 27/iOS 26.5 host, `xcodebuild test`
does not activate the scheme-level StoreKit configuration; the resulting
`StoreKitError.notEntitled` is recorded as an environment limitation, not
transaction proof. Purchase, cancellation, restore, entitlement persistence,
and revocation still require a successful local Xcode session plus Apple
sandbox/TestFlight acceptance evidence.

After adding the fixture, test target, and shared scheme reference,
`IOS_RELEASE_TIMEOUT_SECONDS=600 tools/ios_release_package.sh` passed again.
The resulting simulator app archive SHA-256 is
`141e5a761a2ddf729778b5957229979780564887a32bef4628ef502dbfd39b92` and the
dSYM archive SHA-256 is
`575997eb4f7d144bd899c3f6ac6ae043ba48066240634ed49e37a8fa76340e47`.
The test target is not part of the released application product.

### iOS 27 Release Launch and Local StoreKit Lifecycle (2026-09-22)

After installing the iOS 27 simulator runtime, the focused local StoreKit
lifecycle target passed on an iPhone 18 Pro running iOS 27.0. It covers the
`lifetime_unlock` non-consumable purchase, entitlement visibility in a fresh
`SKTestSession`, and entitlement removal after a local refund/revocation. The
result bundle is `build/ios-storekit/test-20260922124253.xcresult`.

The full local gate then passed at `2026-09-22T16:43:37Z` with Android tests,
lint, release APK/AAB packaging, iOS Release simulator packaging, and the
local StoreKit lifecycle smoke. Its authoritative proof is
`build/release-gate/proof.env`, which records both
`ios_release_package=passed` and `ios_storekit_smoke=passed`.

A fresh install of the produced Release simulator app was also launched on the
iOS 27 iPhone 18 Pro simulator:

```sh
xcrun simctl install CFBECCDE-7117-4B83-90DD-28F7D9A7AC58 \
  build/ios-derived-release/Build/Products/Release-iphonesimulator/OopsAllPRs.app
xcrun simctl launch CFBECCDE-7117-4B83-90DD-28F7D9A7AC58 \
  com.jjswigut.oopsallprs.ios
```

The process launched as PID `64141`; the clean first-run Train screen is in
`build/smoke/ios27-release-launch/launch.png`. The embedded Rest Timer Live
Activity extension also launched, confirming the release bundle's extension
is present and executable. The simulator was shut down afterward. This is
local StoreKit and launch proof only; real App Store sandbox/TestFlight
purchase, restore, and Live Activity presentation still require an Apple
account test on a physical iPhone.

### iOS Device Archive Architecture (2026-09-22)

The first unsigned physical-device archive after the host update revealed that
Xcode searched both the simulator and device shared-framework directories, and
selected the simulator binary during an `iphoneos` link. The build failed with
an architecture mismatch before signing. The target build settings now select
exactly one framework directory per SDK:

- `iphoneos` resolves `shared/build/bin/iosArm64/releaseFramework`;
- `iphonesimulator` resolves
  `shared/build/bin/iosSimulatorArm64/releaseFramework`.

The package script checks both settings before producing the simulator archive,
so a future project change cannot silently reintroduce the mixed-architecture
search path. The following unsigned device archive then passed on Xcode 27:

```sh
xcodebuild archive \
  -project iosApp/OopsAllPRs.xcodeproj \
  -scheme OopsAllPRs \
  -configuration Release \
  -sdk iphoneos \
  -archivePath build/ios-archive-nosign-fixed/OopsAllPRs.xcarchive \
  CODE_SIGNING_ALLOWED=NO CODE_SIGNING_REQUIRED=NO
```

The resulting archive identified `com.jjswigut.oopsallprs.ios`, contained
`OopsAllPRs.app`, and embedded
`RestTimerLiveActivityExtension.appex`. This validates device architecture and
extension packaging only. It is intentionally unsigned; a profile for the
Live Activity extension is still required before a real TestFlight upload.

## Profile Purchase-Recovery UX (2026-09-22)

An API 35 `EventideSmoke` review of the exhausted-free-workout Profile surface
found that the disabled `Unlock forever` purchase button still appeared to be
the main available action when the local store was unavailable. The primary
action now follows the actionable state:

- while the store price is loading, it is visibly unavailable;
- once a valid offer is available, it reads `Unlock for <store price>`;
- when the store is unavailable, it is removed and `Retry store connection`
  becomes the only primary action; and
- `Restore purchase` remains available as a secondary recovery action.

Focused `FullAccessPurchaseOptionsTest` coverage passed, and a rebuilt debug
APK was installed on `emulator-5554`. The settled unavailable-store UI XML
contains `Retry store connection` and `Restore purchase`, with no `Unlock
forever` control. The verified screenshot is
`build/smoke/current-history-audit/profile-recovery.png`. This confirms the
local unavailable-offer recovery state only; it does not establish a live
Google Play or App Store purchase.

### Updated Cross-Platform Release Gate

`tools/release_gate.sh` now runs explicit Android shared unit tests, SQLDelight
migration verification, Android lint, debug/release assembly, and release AAB
packaging before creating the iOS Release simulator package. This replaces the
generic Gradle `check` task, which attempts an unsupported
`iosSimulatorArm64Test` task under the current Xcode runtime. The iOS Release
package remains the iOS build gate and is not skipped.

The updated gate passed on 2026-09-22 at 14:27:15Z:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
IOS_RELEASE_TIMEOUT_SECONDS=600 tools/release_gate.sh
```

Its proof file reports `ios_release_package=passed`; fresh Android release APK,
AAB, mapping/resources files, and iOS simulator app/dSYM archives were present.
`android_smoke=not-run`, so this is not a replacement for device interaction
proof.

### Android Smoke Integrity (2026-09-22)

The former Android smoke accepted a live process while the connected owner
Pixel was locked, producing a lock-screen screenshot. The script now ignores
physical ADB devices unless `ANDROID_DEVICE_SERIAL` is explicitly supplied,
uses or starts an emulator by default, wakes/dismisses an emulator keyguard,
requires the app package in `mCurrentFocus` with `isSleeping=false`, and waits
for the starting window to settle before capture.

The revised smoke was run with a connected Pixel present but no explicit device
serial. It started `EventideSmoke`, installed the debug APK, confirmed the
rendered `History` screen as the awake focused activity, captured
`build/smoke/android-emulator-settled/launch.png`, and shut down the emulator.
The Pixel was not selected. The captured activity state records both
`ResumedActivity` and `mCurrentFocus` for
`com.jjswigut.oopsallprs.android.debug/.MainActivity` with `isSleeping=false`.

### Full Cross-Platform Gate After macOS 27 Update (2026-09-22)

The complete local release gate passed on the macOS 27 / Xcode 27 host at
`2026-09-22T19:55:42Z`:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
ANDROID_AVD_NAME=EventideSmoke \
ANDROID_SHUTDOWN_STARTED_EMULATOR=1 \
IOS_SMOKE_RUNTIME='iOS 27.0' \
IOS_SMOKE_DEVICE_TYPE='iPhone 18 Pro' \
tools/release_gate.sh --android-smoke --ios-smoke
```

The authoritative `build/release-gate/proof.env` records all four required
results as passed: iOS Release package, local StoreKit lifecycle, Android
emulator smoke, and iOS simulator smoke. The Android run used only the
disposable `EventideSmoke` AVD; no physical device was selected. The temporary
Android emulator and iOS simulator were removed after completion.

This gate includes the empty-workout completion guard. Earlier first-workout
screenshots that visibly offered `Finish workout` before any logged set are
historical evidence only and must not be used to describe the current UI.

### Rest Timer Live Activity Package Invariant (2026-09-22)

`tools/verify_ios_app_bundle.sh` now verifies more than the Compose exercise
catalog. For an `.app` or uploaded `.ipa`, it requires the app to declare
`NSSupportsLiveActivities=true`, embed the Rest Timer Live Activity extension,
preserve its app-derived bundle identifier, retain the WidgetKit extension
point, and include the extension executable. This prevents an otherwise
successful package from silently losing the opt-in rest surface.

The verifier passed against the current Release simulator app and an
IPA-shaped package. The exact normal packaging route also passed after the
change:

```sh
IOS_RELEASE_TIMEOUT_SECONDS=900 tools/ios_release_package.sh
```

That produced fresh simulator app and dSYM archives in `build/release-artifacts/`.
This checks delivery structure; actual Live Activity presentation remains a
physical-iPhone acceptance item because simulator packaging cannot prove
lock-screen or Dynamic Island behavior.

### Completed Workout Receipt Visual Audit (2026-09-22)

The debug app was rebuilt and inspected on the disposable API 36
`EventideSmoke` emulator at its configured 130% Android font scale. The
initial review exposed an oversized `Completed workout` heading that wrapped
on a normal phone and displaced the receipt. The detail heading now uses the
compact, semibold body style while retaining its accessibility heading
semantics.

The rebuilt receipt keeps the title on one line, then shows date, duration,
exercise and set counts, the specific achievement summary, reuse/correction
actions, and source-linked PR evidence without overlap. The before/after
captures and UI dumps are in `build/smoke/post-workout-visual-audit/` as
`completed-workout.png` and `completed-workout-compact-header.png`.

Focused completion, achievement, and Train-handoff tests passed:

```sh
./gradlew --no-daemon :shared:testDebugUnitTest \
  --tests 'com.jjswigut.oopsallprs.ui.history.CompletedWorkoutFinishWarningTest' \
  --tests 'com.jjswigut.oopsallprs.ui.history.HistorySummaryPresentationTest' \
  --tests 'com.jjswigut.oopsallprs.ui.navigation.AppShellFinishSummaryTest'
```

The emulator was shut down afterward; no physical device was used.

## Scope and Provenance

Local branch `swiggy/launch-integration`, based on development commit
`4d7567c68d916850d2dfd3b972e42667f934974c`. Combines the preserved purchase-access,
backup-conflict-safety, and Evidence Ladder worktrees. No commit, push, branch
promotion, store submission, pricing, entitlement policy, or telemetry change
is authorized by this verification work.

The mechanical merge recorded 75 paths and their original source hashes in
`build/integration/source-manifest.json`. All source hashes were rechecked and
matched after copying. Original worktrees remain intact. Manual reconciliation
was needed in Profile state; AppState and AppShell changes were also inspected.

## Integration Corrections

- Profile hydration retains persisted backup warnings while preserving purchase
  results that arrive during its suspended reads.
- A locked automatic backup check retains the persisted warning without reading
  or writing the linked document.
- Successful purchase, purchase restoration, or asynchronous entitlement refresh
  must restore a durable backup warning hidden by a temporary access-gate message.
  Denied backup operations are never replayed automatically.
- System Back resolves detail state only for the visible destination. Opening a
  source workout in History must not cause Back to clear a hidden Progress reading.

## Automated Evidence

Commands use Android Studio JBR and the local Android SDK:

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
./gradlew --no-daemon :shared:testDebugUnitTest \
  :shared:verifySqlDelightMigration :androidApp:assembleDebug :androidApp:assembleRelease
```

- First warning regression: 10 focused tests, 1 behavioral failure before fix.
- Initial combined build: 564 tests, zero failures/errors/skips; SQL migration
  verification and Android debug/release assembly passed (207 tasks, 1m32s).
- Destination Back policy: seven focused tests and debug assembly passed.
- Unlock warning regression: five tests, three behavioral failures before fix;
  hydration/purchase race and unrelated-error guards already passed.
- RED XML reports retained under `build/integration/red/`.
- Final full verification after the second warning correction: 577 tests across
  166 suites, zero failures/errors/skips; migration verification and debug/release
  builds passed (207 tasks, 1m16s). `git diff --check` passed.

## Android Runtime Evidence

API 35 `EventideSmoke`, `emulator-5554`. Installed with `adb install -r`; did not
clear app data or reseed the existing demo history.

### Return-to-Training CTA (2026-09-22)

- The Progress overview was opened against the existing debug demo history,
  whose latest completed workout fell outside the trailing seven-day review.
  The screen showed `No completed workouts in the last 7 days.` alongside the
  visible primary `Plan next workout` action in
  `build/smoke/progress-recovery-cta/progress.png`.
- Tapping that action opened the Train chooser, displaying saved templates and
  `Start empty workout`. The captured UI had no active-session controls,
  rest timer, or `Log set` action. The route is therefore an explicit planning
  path, not an accidental workout start. Evidence:
  `build/smoke/progress-recovery-cta/train.xml` and `.png`.
- `ProgressStateHolderTest` completed with 8 tests and zero failures, and the
  Android debug APK assembled successfully before this emulator proof.
- The overview also now renders a semantic `Progress` page heading above the
  dashboard, matching the primary-destination hierarchy used by History and
  Train. Visual evidence: `build/smoke/progress-recovery-cta/progress-heading.png`
  and the corresponding UI XML.

### Empty History Recovery (2026-09-22)

- An unsigned production `release` APK was aligned and signed only with the
  local Android debug key for installation on an isolated `EventideSmoke`
  emulator. This preserved the production package and disabled debug-only demo
  seeding; no physical device was selected or modified.
- After clearing only `com.jjswigut.oopsallprs.android` on that emulator, the
  History destination displayed `0 workouts`, `No completed workouts yet`, and
  one visible primary `Plan your first workout` action. The action gives a new
  user a clear recovery path instead of a dead-end empty state. Evidence:
  `build/smoke/first-use-release/history.png` and `history.xml`.
- The emulator was shut down at the end of the check. The signed emulator copy
  is not a distributable release artifact.

### First-Set Timer Clarity (2026-09-21)

- Headless cold boot was used because windowed emulator startup was unreliable
  in the host session. The AVD reached ADB and was shut down after the proof.
- The current debug APK opened through `MainActivity`; a temporary debug-only
  empty database loaded its existing demo seed and local developer access.
- From History, the accessibility-labelled `Train` destination opened a fresh
  empty workout. Before a set was logged, the active UI XML contained `Starts
  with first set`, `Your workout is ready`, `Add exercise`, `Finish workout`,
  and `Discard workout`.
- The initial baseline had exhausted its free-workout allowance, so it was not
  used to create a new active workout. No billing state was changed. After the
  proof, `build/smoke/workout-summary/baseline.db` was restored byte-for-byte;
  the captured UI is `build/smoke/first-set-timer-clarity.xml`.

- Initial combined build launched and retained demo progress/history.
- Profile offered no fabricated price when Play was unavailable; purchase was
  disabled, with retry and restore controls. This is unavailable-store behavior,
  not proof of a successful store purchase.
- Before navigation correction, Back from a source workout incorrectly left the
  completed-workout detail visible. After correction, Back showed History list;
  returning to Progress retained the original Capability reading and source rows.
- Actual UI XML and inspected screenshots are in
  `build/smoke/launch-integration/`. Navigation proof: `source-fixed.xml`,
  `history-root-fixed.xml`, and `reading-retained.xml` / `.png`.
- No AndroidRuntime error output during the inspected flows.
- Final APK reinstalled without clearing data and the source-workout/Back/return
  flow repeated successfully (`final-reading.xml`, `final-source.xml`,
  `final-back.xml`, `final-retained.xml`, `final.png`). App PID 3699 remained live;
  AndroidRuntime error output was empty. No emulator preferences were changed.
- Final debug APK SHA256:
  `c5015f6fa72efb1d9d7de137ff9c1aff36efca360cda5e371d50096260fb6a0c`.

## Remaining Acceptance Gaps

### Local StoreKit Simulator Runtime (2026-09-22)

- macOS 27.0 and Xcode 27.0 successfully built and launched the debug iOS app
  on a newly created iOS 26.5 simulator. The app bundle was installed at
  `com.jjswigut.oopsallprs.ios.debug`.
- The app-hosted `FullAccessStoreKitTests` target ran from the Xcode IDE, but
  its purchase test failed. `SKTestSession` reported `SKInternalErrorDomain`
  code 3 while saving configuration, clearing overrides, and clearing
  transactions; `buyProduct("lifetime_unlock")` then returned `notEntitled`.
  This is a runtime-level local StoreKit failure, not purchase evidence.
- The project has the active local configuration, a non-consumable
  `lifetime_unlock` product, and the test fixture is included in the test
  bundle. Apple documents an iOS 26.6 StoreKit Simulator fix for
  `SKTestSession` failing to connect to the test environment. This was the
  historical iOS 26.5 limitation; the current iOS 27 verification follows.

### Local StoreKit Entitlement Lifecycle (2026-09-22)

- Xcode 27.0 downloaded and installed the iOS 27.0 Simulator runtime. The
  `OopsAllPRsStoreKitTests` target passed three tests on an iPhone 18 Pro
  simulator (iOS 27.0, build 24A434): purchase creates the current lifetime
  entitlement, the entitlement remains available to a fresh StoreKit session,
  and refund revocation removes the entitlement.
- The authoritative result bundle is
  `~/Library/Developer/Xcode/DerivedData/OopsAllPRs-djxfsxjaqglnsfgszfpnjgceiofe/Logs/Test/Test-OopsAllPRs-2026.09.22_12-28-51--0400.xcresult`.
  `xcresulttool` reported 3 passed tests and 0 failures. The simulator was
  shut down after the run.
- This proves local StoreKit configuration and entitlement transitions. A real
  App Store sandbox purchase and the app's restore-button flow remain required
  before public iOS release promotion.

### Rest Notification Opt-In (2026-09-22)

- The rest countdown remains active in the workout UI and completion scheduling
  remains intact, but the outside-app timer is now off by default. Android does
  not request notification permission after the first logged set unless the
  person has explicitly enabled `Show rest timer outside app` in Profile.
- New installs, missing preference rows, legacy V1 backups without this
  preference, and existing database rows are all normalized to opt-in. The
  schema-14-to-15 migration removes the prior implicit enabled value.
- Focused profile, backup, rest-lifecycle, effective-start, and schema 13-15
  tests passed, along with SQLDelight migration verification and a release APK
  build. The isolated API 36 emulator became unavailable during the post-host-
  upgrade visual retry, so the final visual proof of this narrow behavior must
  be rerun before release promotion.

### Android Post-Upgrade Smoke (2026-09-22)

- The isolated `EventideApi36` emulator built, installed, foregrounded, and
  retained the debug app after the macOS 27 host upgrade. No physical device
  was selected. Evidence is `build/smoke/android-post-macos27/launch.png` and
  `foreground-activity.txt`.
- The rendered Train entry screen shows saved templates and a clear `Start
  empty workout` primary action; the emulator was shut down by the smoke
  harness after capture.

### iOS Rendered Launch Smoke (2026-09-22)

- `tools/ios_simulator_smoke.sh` now builds the debug app for a fresh iPhone
  18 Pro iOS 27.0 simulator, installs and launches it, captures a screenshot,
  and deletes only the simulator it created. It does not select a physical
  device.
- The first three-second capture showed an almost-uniform startup surface even
  though the process had launched. Investigation on a retained disposable
  simulator showed the Train screen after the initial Compose frame completed.
  The smoke now waits 15 seconds and samples rendered colors to reject an
  almost-uniform capture. The guard rejected the original blank capture with
  two sampled colors and accepted the rendered Train screen with 41.
- The verified screenshot is
  `build/smoke/ios27-rendered-20260922/launch.png`. It shows Train, saved
  templates, and `Start empty workout`; this is first-launch visual proof, not
  interaction, purchase, or restore proof.
- The former empty `UILaunchScreen` used the system light background before
  Compose rendered. `LaunchBackground` now uses the product background
  (`#06080F`) through the iOS asset catalog. A one-second cold-launch capture
  at `build/smoke/ios27-launch-background-20260922/launch.png` is dark and
  transitions to the rendered Train screen at `rendered.png`. The normal
  guarded smoke then passed from the exact configuration, producing
  `build/smoke/ios27-launch-background-verified/launch.png`.
- `tools/ios_release_package.sh` now checks both the `UILaunchScreen` asset
  name and the `#06080F` component values before its normal Release package
  build. The full package passed after that preflight and regenerated the
  simulator app and dSYM archives under `build/release-artifacts/`.

Subsequent local backup-safety verification passed 613 tests, Android
debug/release builds, migrations, and nine native Android database tests. See
[backup atomicity verification](backup-atomicity-verification-2026-09.md) for the
newer APK identity and proof. The 577-test checkpoint above is historical.

- The macOS/Xcode/CoreSimulator mismatch is resolved, and a Release simulator
  archive plus first-launch smoke now pass. Native iOS interaction coverage and
  real Apple/Google sandbox purchase, restore, delivery, and revocation still
  need verification.
- On 2026-09-22, the complete local release gate passed from
  `4d7567c68d916850d2dfd3b972e42667f934974c`, including Android unit tests,
  debug/release APK and AAB packaging, release lint, iOS Release simulator
  packaging, the three-test StoreKit lifecycle suite, an Android API 36
  emulator launch, and an iOS 27 iPhone 18 Pro simulator launch. The proof is
  `build/release-gate/proof.env`; it records `ios_release_package=passed`,
  `ios_storekit_smoke=passed`, `android_smoke=passed`, and `ios_smoke=passed`.
  Both runtime checks used disposable emulators/simulators, never a physical
  owner device. The final rendered Train captures are
  `build/smoke/android/launch.png` and `build/smoke/ios/launch.png`.
- Local snapshot consistency and the guarded check-to-restore transaction are
  now verified. Remote-provider write races and some reconstructed metadata
  remain follow-up risks; local transactions do not provide remote CAS.
- Google listing/production status has been inspected read-only; Apple still
  needs login. See [store checkpoint](store-readiness-checkpoint-2026-09.md).
  Neither local builds nor saved listing assets establish review acceptance.
- Free export is intentionally allowed. Import, restore, backup, and sync
  remain paid-only until a durable entitlement/identity model is introduced.
- No user retention, willingness-to-pay, or profitability result is established
  by these tests. Real tester acceptance and commercial evidence remain needed.

### Latest Integrated Release Gate (2026-09-22)

`IOS_RELEASE_TIMEOUT_SECONDS=900 tools/release_gate.sh` passed from the current
dirty local integration worktree, whose checked-out commit was
`4d7567c68d916850d2dfd3b972e42667f934974c`. The authoritative proof is
`build/release-gate/proof.env`, completed at `2026-09-22T18:51:50Z`. It records
successful Android shared tests, migration verification, release lint, debug
and release APK/AAB packaging, iOS Release simulator packaging, and the local
StoreKit purchase, persistence, and revocation suite.

This specific gate intentionally did not run Android or iOS interactive smoke
commands (`android_smoke=not-run`, `ios_smoke=not-run`); those have separate
disposable-device evidence above. It made no GitHub, Play, App Store Connect,
or physical-device change.

### Latest Current-Worktree Release Gate (2026-09-22)

After the compact set-entry accessibility layout update, the same full local
gate passed again at `2026-09-22T19:23:23Z` from the dirty
`swiggy/launch-integration` worktree at
`4d7567c68d916850d2dfd3b972e42667f934974c`. The authoritative record is
`build/release-gate/proof.env`. It covers shared tests, SQLDelight migration
verification, Android lint and debug/release APK/AAB packaging, iOS Release
simulator packaging, and three passing local StoreKit entitlement lifecycle
tests.

As with the preceding gate, `android_smoke=not-run` and `ios_smoke=not-run`
refer only to the optional interactive smoke steps of that command; separate
disposable-device launch evidence remains documented above. This run did not
perform any GitHub, store, account, or physical-device mutation.
