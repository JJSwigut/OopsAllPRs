# Implementation Plan: Milestone Validation and UX Hardening

**Branch**: `codex/011-milestone-ux-hardening` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-milestone-ux-hardening/spec.md`

## Summary

Run a structured milestone validation pass against the existing Oops All PRs
app and use the findings to harden the current UX without adding new product
scope. The implementation centers on Pixel-class Android emulator/device
review of Train, active logging, exercise picker, keyboard entry, finish,
History/templates, Progress, and Profile/export; targeted shared Compose and
design-system fixes; and updated evidence for deferred manual gates.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight-backed local store, shared Compose UI, Android debug APK, Android emulator/ADB tooling.

**Storage**: Existing SQLDelight local SQLite database. This feature should not add schema changes unless a defect requires a narrowly scoped fix; validation reads and normal user actions may create workouts, templates, PRs, preferences, and export snapshots.

**Local Data Model**: No new product data model is planned. The feature adds validation evidence and may update UI/state behavior around active workouts, exercise picker state, completed workouts/templates, PR views, Profile preferences, and export handoff.

**Testing**: `./gradlew :shared:testDebugUnitTest`, targeted common tests for any changed state models, `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`, `./gradlew :shared:compileKotlinIosSimulatorArm64`, design-system/shared Material scan, `git diff --check -- .`, and ADB/emulator screenshot evidence.

**Target Platform**: Android first on a Pixel-class emulator or connected Pixel 9 Pro. iOS remains first-class through shared code boundaries and iOS simulator compile validation.

**Platform Scope**: Shared code owns UI/state hardening. Android platform work is limited to runtime validation, app install/launch, and platform-specific observations. iOS code should change only if a shared boundary requires compile-safe adjustment.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI, Android bootstrap app, and iOS bootstrap app.

**Performance Goals**: Validation should preserve the current fast-loop targets: start-to-log remains quick, exercise add remains under the prior 10-second manual target, and top-level navigation remains responsive with ordinary local data.

**Constraints**: Offline-only; no accounts, network, analytics, or sync. All UI fixes must use `FitTheme` and design-system components/tokens. Hardening must not introduce Material 3 into shared/design-system code. Manual evidence must distinguish platform emulator issues from app defects.

**Scale/Scope**: One milestone polish slice across existing features 002, 006, 007, 008, 009, and 010. Scope is defect discovery, targeted fixes, validation records, and deferred-gate cleanup; new workouts, new analytics, native iOS share UI, cloud sync, import/restore, and new exercise data sources are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The slice is explicitly about preserving next-action visibility, thumb reach, picker speed, numeric correction, and finish discoverability.
- **Ledger Integrity**: PASS. The plan uses normal app flows for any generated data and forbids silent mutation of completed sets, PR sources, templates, and exports.
- **Session Recovery**: PASS. Manual validation includes restart/resume where relevant and does not alter timer source-of-truth behavior unless a defect is found and covered.
- **Progress Promise**: PASS. Progress/PR evidence, bodyweight reps-only records, and display-unit behavior are included in the validation pass.
- **Local-First Ownership**: PASS. The slice works entirely offline and validates local Profile/export behavior without adding network dependencies.
- **Shared-First KMP**: PASS. Hardening belongs in shared UI/state or design-system tokens/components, with platform-specific behavior behind existing adapters.
- **Design System and Accessibility**: PASS. The feature validates and fixes FitTheme/design-system usage, touch target shape, glow, reduced motion, haptics alternatives, labels, and state descriptions.
- **Public Release Gates**: PASS. Plan includes automated gates, manual Android evidence, export sanity, previous deferred gate updates, and static scans.

## Project Structure

### Documentation (this feature)

```text
specs/011-milestone-ux-hardening/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- milestone-validation-contracts.md
|-- checklists/
|   `-- requirements.md
|-- validation/
|   |-- milestone-results.md
|   `-- screenshots/
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- ui/exercise/
|   |-- ui/history/
|   |-- ui/navigation/
|   |-- ui/profile/
|   |-- ui/progress/
|   `-- ui/workout/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|   |-- ui/exercise/
|   |-- ui/history/
|   |-- ui/navigation/
|   |-- ui/profile/
|   |-- ui/progress/
|   `-- ui/workout/
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
`-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/

design-system/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/foundation/
`-- src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/theme/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt
```

**Structure Decision**: Keep validation artifacts under the feature spec and
fix defects in the narrowest owning module: reusable interaction/style defects
in `:design-system`, flow-specific behavior in `:shared`, and Android-only
handoff/runtime notes in validation docs.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Use emulator/device screenshots and written scenario notes as the manual gate evidence.
- Treat platform System UI warnings as environment findings unless app focus, input, or navigation is blocked.
- Keep fixes targeted to existing Fit design-system and shared UI surfaces.
- Close older deferred manual gates by linking to the new milestone evidence rather than duplicating screenshots across every feature.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/milestone-validation-contracts.md](./contracts/milestone-validation-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. The design remains local-first, shared-first, design-system-only, and
release-evidence focused. It adds no new product scope and keeps any runtime
defect fix tied to a reproducible validation finding.
