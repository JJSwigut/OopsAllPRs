# Implementation Plan: Release and Debug Apps

**Branch**: `021-release-debug-apps` | **Date**: 2026-06-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/021-release-debug-apps/spec.md`

## Summary

Create explicit debug and release app paths for Android and iOS, with developer tools enabled only in debug builds. Android already passes `BuildConfig.DEBUG` into shared app state, so implementation focuses on debug artifact identity and validation. iOS must stop hardcoding developer tools on, add a build configuration bridge into the shared Compose host, and expose distinct debug/release bundle metadata.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; Swift 5; Xcode 26.5 project for iOS host.

**Primary Dependencies**: Existing `:shared` KMP module, `:androidApp`, manually maintained `iosApp/OopsAllPRs.xcodeproj`, SQLDelight native/Android drivers, Compose UI.

**Storage**: Existing SQLDelight local database. No schema migration, persistence change, or seed-data change is required.

**Local Data Model**: Reuse the existing `developerToolsEnabled` capability and `AppState.developerSeeds: DeveloperSeedStateHolder?`. Add no persistent entities.

**Testing**: Shared unit tests for Profile developer card visibility contract; Android debug/release build tasks; iOS simulator debug/release Xcode builds and plist checks.

**Target Platform**: Android and iOS app artifacts.

**Platform Scope**: Shared Profile UI remains the single developer-tools visibility surface. Android provides `BuildConfig.DEBUG`. iOS provides an equivalent build configuration constant from Swift/Xcode into the shared `IosAppViewControllerFactory`.

**Project Type**: Kotlin Multiplatform mobile app with Android app bootstrap and iOS Swift host.

**Performance Goals**: No runtime overhead beyond reading a build-time boolean; Profile render time remains equivalent.

**Constraints**: Developer tools must not be controlled by persisted or user-editable state. Release builds must not expose developer seed UI. Debug builds must remain available for local validation. No user data mutation.

**Scale/Scope**: One shared UI visibility contract, Android build metadata updates, iOS build metadata/configuration updates, and release/debug validation documentation.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Active logging flows are untouched.
- **Ledger Integrity**: PASS. No workout/session/set/PR/export mutation path changes.
- **Session Recovery**: PASS. Build channel is static for an installed artifact and not restored from session state.
- **Progress Promise**: PASS. PR, history, last-set, bodyweight, unit, and progression behavior are unchanged.
- **Local-First Ownership**: PASS. Offline local data model and export ownership are unchanged.
- **Shared-First KMP**: PASS. Shared UI consumes a platform-provided capability; platform code remains adapter/bootstrap only.
- **Design System and Accessibility**: PASS. Profile remains built from existing Fit components; release removes developer-only controls rather than adding alternate UI.
- **Public Release Gates**: PASS. Plan includes Android and iOS debug/release builds plus release Profile visibility checks.

## Project Structure

### Documentation (this feature)

```text
specs/021-release-debug-apps/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── profile-developer-tools-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── App.kt
│   ├── AppState.kt
│   └── ui/profile/ProfileFlow.kt
├── src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/
│   └── ProfileDeveloperToolsVisibilityTest.kt
└── src/iosMain/kotlin/com/jjswigut/oopsallprs/
    └── MainViewController.kt

androidApp/
├── build.gradle.kts
└── src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

iosApp/
├── OopsAllPRs.xcodeproj/project.pbxproj
└── iosApp/
    ├── ContentView.swift
    ├── Info.plist
    └── OopsAllPRsApp.swift
```

**Structure Decision**: Keep capability gating in shared state/UI, with Android and iOS supplying build-time booleans through existing app bootstrap boundaries. No repository or domain layer changes are needed.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/profile-developer-tools-contract.md](./contracts/profile-developer-tools-contract.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Check

PASS. The design remains shared-first, keeps platform-specific build facts in platform bootstrap code, avoids data model changes, and adds explicit release evidence for both app targets.

## Complexity Tracking

No constitution violations are planned.
