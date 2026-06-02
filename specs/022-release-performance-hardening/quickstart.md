# Quickstart: Release Performance Hardening

## Prerequisite: Disk Space

Check free disk before large release builds:

```bash
df -h .
```

Do not start iOS Release/archive validation until several GB are free.

Current implementation baseline:

- Release validation should start with at least 8 GiB free for simulator builds.
- Archive validation may need substantially more space depending on dSYM and derived-data output.
- Last checked during implementation: 58 GiB free on 2026-06-02.

## Android Release Hardening Checks

Run shared tests:

```bash
./gradlew :shared:testDebugUnitTest
```

Build Android debug and release:

```bash
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease
```

Inspect release metadata:

```bash
cat androidApp/build/outputs/apk/release/output-metadata.json
/Users/swig/Library/Android/sdk/build-tools/35.0.0/aapt dump badging androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk | sed -n '1,12p'
find androidApp/build/outputs -path '*mapping*' -o -name '*mapping*.txt'
test -f androidApp/proguard-rules.pro && echo "App R8 rules present"
```

Expected release results:

- Package id is `com.jjswigut.oopsallprs.android`.
- Display name is `Oops All PRs`.
- R8/minification is enabled.
- Resource shrinking is enabled.
- Mapping evidence exists for release.
- `androidApp/proguard-rules.pro` is included in the release build.
- Profile has no Developer section after launch.

## Android Smoke Flow

Install release on a device when signing permits:

```bash
./gradlew :androidApp:installRelease
```

Smoke checklist:

- App starts.
- Profile opens and has no Developer section.
- Export controls are present.
- Start a workout.
- Log one set.
- Finish or discard the workout.
- Restart app and confirm no crash.

## iOS Release Build Checks

Build iOS Release simulator app:

```bash
xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Release -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/ios-derived-release build
```

Inspect release metadata:

```bash
plutil -p build/ios-derived-release/Build/Products/Release-iphonesimulator/OopsAllPRs.app/Info.plist | rg 'CFBundleDisplayName|CFBundleIdentifier|CADisable'
```

Expected release results:

- Bundle id is `com.jjswigut.oopsallprs.ios`.
- Display name is `Oops All PRs`.
- Release shared framework path is used.
- Profile has no Developer section after launch.

## iOS Archive Checks

When signing assets and disk space are available:

```bash
xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Release -destination 'generic/platform=iOS' -archivePath build/OopsAllPRs.xcarchive archive
```

Expected archive results:

- Archive succeeds.
- dSYM/symbol evidence is present.
- Release target has `DEBUG_INFORMATION_FORMAT=dwarf-with-dsym`.
- Bundle id and display name match release metadata.

## Release Evidence Template

Use this shape when recording a release validation run:

```text
Date:
Disk free before build:
Android debug build:
Android release build:
Android release package id:
Android release display name:
Android mapping path:
Android smoke result:
Android blocker:
iOS Release build:
iOS bundle id:
iOS display name:
iOS dSYM/symbol evidence:
iOS smoke result:
iOS archive result:
iOS blocker:
Next action:
```

## Evidence Notes

If any command cannot finish, record:

- Command
- Exit status
- Last artifact path
- Blocker
- Next action

## Latest Validation: 2026-06-02

Disk free before final validation:

- `df -h .`: 58 GiB free.

Android results:

- `./gradlew :shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease`: passed.
- Release APK: `androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk` (3.6 MB).
- Debug APK: `androidApp/build/outputs/apk/debug/androidApp-debug.apk` (27 MB).
- Release package id: `com.jjswigut.oopsallprs.android`.
- Release display name: `Oops All PRs`.
- Release mapping evidence: `androidApp/build/outputs/mapping/release/mapping.txt`.
- Release resource shrink evidence: `androidApp/build/outputs/mapping/release/resources.txt`.
- Android smoke blocker: release artifact is unsigned locally; install/smoke must wait for release signing or a signed release variant.

iOS results:

- `xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Release -destination 'id=20A1F2D0-45DB-47A6-989F-3E4BA8CA8308' -derivedDataPath build/ios-derived-release build`: passed.
- Release app: `build/ios-derived-release/Build/Products/Release-iphonesimulator/OopsAllPRs.app`.
- Release dSYM: `build/ios-derived-release/Build/Products/Release-iphonesimulator/OopsAllPRs.app.dSYM`.
- Release bundle id: `com.jjswigut.oopsallprs.ios`.
- Release display name: `Oops All PRs`.
- Release framework evidence: `shared/build/bin/iosSimulatorArm64/releaseFramework/shared.framework/shared`.
- Release simulator launch: `xcrun simctl install booted build/ios-derived-release/Build/Products/Release-iphonesimulator/OopsAllPRs.app && xcrun simctl launch booted com.jjswigut.oopsallprs.ios` passed; launch returned pid 1411.
- Release simulator visual smoke: screenshot showed the Profile screen and no Developer section.
- Release `CopySwiftLibs` no longer scans `debugFramework` after removing the stale Xcode framework file reference.
- iOS device framework blocker: `./gradlew :shared:linkReleaseFrameworkIosArm64` became idle after `:shared:linkReleaseFrameworkIosArm64`; wrapper was terminated with exit 143. Last artifact state was `shared/build/bin/iosArm64/releaseFramework` without a framework binary.
- iOS archive blocker: archive was not run because signing assets/provisioning were not validated in this local run.
