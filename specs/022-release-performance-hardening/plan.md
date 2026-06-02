# Implementation Plan: Release Performance Hardening

**Branch**: `022-release-performance-hardening` | **Date**: 2026-06-02 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/022-release-performance-hardening/spec.md`

## Summary

Add a release-hardening pass for both app targets. Android release builds should enable R8/minification and resource shrinking with conservative keep rules and release smoke validation. iOS release builds should validate release shared framework linkage, release metadata, symbol/dSYM evidence, and a release Profile/core-flow smoke path. Implementation must also document environmental prerequisites, especially disk-space cleanup before large release builds.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31; Swift 5; Xcode 26.5 project.

**Primary Dependencies**: Existing `:androidApp`, `:shared`, `:design-system`, Android Gradle Plugin release optimizer/R8, Kotlin/Native framework linking, Xcode Release configuration, SQLDelight, Compose resources.

**Storage**: Existing SQLDelight local database. No schema migration or storage behavior change.

**Local Data Model**: No persistent entities. Release evidence is documentation/output, not app data.

**Testing**: Gradle unit tests, Android debug/release assemble, Android release metadata inspection, Android release device smoke test, iOS Debug/Release simulator builds, iOS release metadata inspection, and archive/signing checks where environment supports them.

**Target Platform**: Android and iOS release artifacts.

**Platform Scope**: Android build configuration and R8/resource shrink rules; iOS Xcode Release configuration, shared framework linkage, Info.plist metadata, dSYM/symbol evidence. Shared code should change only if release optimizer validation exposes missing keep/resource boundaries.

**Project Type**: Kotlin Multiplatform mobile app with Android app bootstrap and Swift iOS host.

**Performance Goals**: Android release artifact uses code and resource shrinking; iOS Release uses optimized Swift/Kotlin release outputs. Startup/Profile/core workout smoke flows remain functional.

**Constraints**: Debug remains unminified and developer-tool enabled. Release keeps developer tools disabled. No user data model, export format, or workout behavior changes. Low disk space must be resolved before large release builds.

**Scale/Scope**: Build configuration, keep rules, release validation docs, and smoke checks for existing app flows. Store submission automation is out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Release optimization must preserve startup and active logging responsiveness.
- **Ledger Integrity**: PASS. Plan includes smoke checks for workout lifecycle and avoids storage changes.
- **Session Recovery**: PASS. Release smoke checks include active session/rest notification risk areas.
- **Progress Promise**: PASS. PR/history/progress behavior unchanged; no derived data model changes.
- **Local-First Ownership**: PASS. No network or sync changes; export availability included in validation.
- **Shared-First KMP**: PASS. Optimizer settings stay platform-side; shared code only changes if release validation reveals required retention boundaries.
- **Design System and Accessibility**: PASS. No new UI styling; release checks keep Profile/core navigation visible and developer UI absent.
- **Public Release Gates**: PASS. This feature expands release gates for optimized artifacts, metadata, symbols, and manual smoke checks.

## Project Structure

### Documentation (this feature)

```text
specs/022-release-performance-hardening/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── release-artifact-contract.md
└── tasks.md
```

### Source Code (repository root)

```text
androidApp/
├── build.gradle.kts
├── proguard-rules.pro
└── src/main/AndroidManifest.xml

shared/
├── build.gradle.kts
└── src/commonMain/composeResources/

iosApp/
├── OopsAllPRs.xcodeproj/project.pbxproj
└── iosApp/Info.plist

specs/022-release-performance-hardening/
└── quickstart.md
```

**Structure Decision**: Treat this as build/release infrastructure, not product behavior. Keep platform release configuration in platform build files and keep release validation instructions with the feature spec.

## Phase 0 Research

See [research.md](./research.md).

## Phase 1 Design

See [data-model.md](./data-model.md), [contracts/release-artifact-contract.md](./contracts/release-artifact-contract.md), and [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Check

PASS. The plan remains release-infrastructure-only, preserves local-first data behavior, validates user-data flows after shrinking, and adds explicit public release evidence.

## Complexity Tracking

No constitution violations are planned.
