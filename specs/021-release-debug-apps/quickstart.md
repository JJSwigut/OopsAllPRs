# Quickstart: Release and Debug Apps

## Android Validation

Build debug and release artifacts:

```bash
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease
```

Install/run debug on a connected device:

```bash
./gradlew :androidApp:installDebug
/Users/swig/Library/Android/sdk/platform-tools/adb shell am start -n com.jjswigut.oopsallprs.android.debug/com.jjswigut.oopsallprs.MainActivity
```

Expected debug result:

- Installed app identity is distinguishable from release.
- App display name includes a debug suffix.
- Profile shows the Developer section.

Release checks:

```bash
./gradlew :androidApp:assembleRelease
```

Expected release result:

- Release application id is `com.jjswigut.oopsallprs.android`.
- Release display name is `Oops All PRs`.
- Profile has no Developer section or developer seed controls.

## iOS Simulator Validation

Build Debug:

```bash
xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/ios-derived build
```

Expected debug result:

- Bundle identifier uses the debug suffix.
- Display name includes a debug suffix.
- Profile shows the Developer section.

Build Release:

```bash
xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Release -destination 'platform=iOS Simulator,name=iPhone 17' -derivedDataPath build/ios-derived-release build
```

Expected release result:

- Bundle identifier is `com.jjswigut.oopsallprs.ios`.
- Display name is `Oops All PRs`.
- Profile has no Developer section or developer seed controls.

## Shared Test Validation

```bash
./gradlew :shared:testDebugUnitTest
```

Expected result:

- Profile developer tools visibility tests pass for enabled and disabled states.
