# Cloud Backup Sync Validation Results

**Date**: 2026-06-05
**Branch**: `024-cloud-backup-sync`

## Automated Results

- `./gradlew :shared:compileKotlinIosSimulatorArm64` - PASS
- `ANDROID_HOME=/Users/swig/Library/Android/sdk ./gradlew :shared:testDebugUnitTest` - PASS
- `ANDROID_HOME=/Users/swig/Library/Android/sdk ./gradlew :shared:allTests` - PASS
- `ANDROID_HOME=/Users/swig/Library/Android/sdk ./gradlew :androidApp:assembleDebug` - PASS
- `xcodebuild -project iosApp/OopsAllPRs.xcodeproj -scheme OopsAllPRs -configuration Debug -destination 'generic/platform=iOS Simulator' ARCHS=arm64 ONLY_ACTIVE_ARCH=YES build` - PASS

## Notes

- Gradle consistently reports a corrupted user cache journal at `/Users/swig/.gradle/caches/journal-1/file-access.bin`, but all validation commands above completed successfully.
- Android SDK was available at `/Users/swig/Library/Android/sdk`; commands were run with `ANDROID_HOME` set rather than committing a machine-specific `local.properties`.
- iOS backup document picker/bookmark behavior now compiles with `UIDocumentPickerViewController`, security-scoped bookmarks, and host presentation wiring through `ContentView.swift`.
- Android SAF adapter compiles and is wired through `MainActivity`.
- Android document-provider smoke steps are documented in `quickstart.md`; physical provider smoke still requires a device/emulator with the desired providers installed.
- Profile backup accessibility/token review is recorded in `validation/profile-backup-accessibility-token-review.md`.
- A generic iOS simulator build without `ARCHS=arm64` attempted to link x86_64 against the local arm64 simulator shared framework and failed; the arm64 simulator build passed.

## Completed Coverage

- Sync state persistence test.
- Restore into empty SQL database test.
- Restore safety-copy and active-workout warning state test.
- Simulated restore failure rollback test.
- Conflict policy and Profile conflict-state tests.
- Launch/resume linked backup check test.
- Existing CSV export tests through `:shared:allTests`.
