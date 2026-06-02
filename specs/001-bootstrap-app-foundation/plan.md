# Implementation Plan: Bootstrap Oops All PRs App Foundation

**Branch**: `001-bootstrap-app-foundation` | **Date**: 2026-05-29 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-bootstrap-app-foundation/spec.md`

## Summary

Bootstrap a fresh Oops All PRs foundation as a local-first Kotlin Multiplatform
mobile app. Android is implemented and validated first, while shared domain,
data, validation, and adapter boundaries keep iOS first-class. The foundation
establishes the workout ledger, active-session recovery, routines, exercise
seed ingestion, bodyweight reps-only logging, canonical weight units, PR/progress
derivation, export-ready local data, and validation gates for public-release
quality.

## Technical Context

**Language/Version**: Kotlin Multiplatform. Baseline versions should track the
existing app unless implementation research identifies a safer patch update:
Kotlin 2.1.x, Compose Multiplatform 1.8.x, AGP 8.9.x, SQLDelight 2.0.x,
coroutines 1.9.x, kotlinx-datetime 0.6.x.

**Primary Dependencies**: The existing OopsAllPRs **Neo-Glass design-system module**
(`:design-system`, `com.jjswigut.oopsallprs.ds`) for all UI components and theme tokens —
Material 3 is not used (constitution v3.0.0, Principle VII) — plus SQLDelight,
kotlinx-coroutines, kotlinx-datetime, Koin, multiplatform UUID support, and Vico or an
equivalent chart-ready data model for future progression UI. Theming/color comes from the
design-system token system (`FitTheme`), not a Material dynamic-color library.

**Storage**: Local SQLite via SQLDelight, Android driver first and native iOS
driver kept in adapter scope. No remote persistence, accounts, or cloud sync in
this feature.

**Local Data Model**: Separate domain concepts for reusable routines, active
workouts, completed workouts, exercises, exercise sets, active session pointers,
rest timer anchors, personal records, progress points, exercise seed rows, and
user preferences. SQLDelight schema uses explicit migrations, stable IDs,
timestamps, canonical kilogram storage, source references for PR evidence,
seed-version metadata, and user-created exercise preservation.

**Testing**: Shared unit tests for domain rules and use cases; SQLDelight JVM
tests with SQLite driver for persistence, migrations, seed ingestion, ledger
integrity, PR derivation, and export data; Android unit/device validation for
active session restart, backup/permissions review, and first-run manual flow.

**Target Platform**: Android first for executable validation and manual device
checks. iOS remains first-class through shared code, iOS database/settings/
notification adapter contracts, and no Android-only dependency in shared
foundation behavior.

**Platform Scope**: Shared code owns domain models, validation, repositories,
use cases, active-session state, seed parsing, unit conversion, PR derivation,
and shared contracts. Android and iOS provide database drivers, settings
storage, notification scheduling, file sharing/export, haptics, permissions,
and app bootstrap.

**Project Type**: Kotlin Multiplatform mobile app with local persistence and
shared domain/data foundation.

**Performance Goals**: Fresh app start restores an active workout in <=2s on a
supported Android test device; first empty workout plus first logged set can be
completed in <=30s during manual validation; common workout lookup and active
workout hydration avoid N+1 query patterns for expected workout sizes; seed
ingestion completes before exercise search is exposed and must not prevent the
fresh-install first-set flow from meeting the <=30s manual validation target.

**Constraints**: Fully offline operation; no account requirement; Android Auto
Backup allowed and documented before release; bodyweight reps-only sets are
valid; canonical weights stored in kilograms; locale decimal input accepted;
future sync compatibility protected by stable IDs, timestamps, explicit
mutation paths, and conflict-aware boundaries.

**Scale/Scope**: Baseline exercise seed list from current OopsAllPRs app
(591 exercise rows plus header at discovery time), single-user local workout
history, routines, active workout recovery, PR/progress source data, and export-
ready local records. Cloud sync, accounts, social sharing, complete active
workout UI polish, and app-store submission are outside this feature.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Foundation supports start empty, start from
  routine, add exercises on the fly, and reps-only bodyweight logging without
  requiring planning-heavy flows. Full active UI polish is out of scope but
  later UI must preserve stepper-first/one-tap-first logging.
- **Ledger Integrity**: PASS. Plan centers on durable confirmed sets,
  logged timestamps, explicit logged-set edits, finish behavior that strips
  unlogged planned sets, and tests for tuple preservation.
- **Session Recovery**: PASS. Active workout identity, started-at, rest
  ends-at, and navigation recovery are planned as first-class local state with
  wall-clock anchors.
- **Progress Promise**: PASS. PR and progress source models are part of the
  foundation, including weighted, bodyweight, fractional, and unit-converted
  evidence.
- **Local-First Ownership**: PASS. Feature is offline-only/local-only, includes
  export-ready data and backup documentation, and protects future sync through
  identifiers and mutation boundaries.
- **Shared-First KMP**: PASS. Shared domain/data/state owns foundation behavior;
  Android is first executable target and iOS adapter contracts remain required.
- **Design System and Accessibility**: PASS. This foundation includes minimal
  shared app shell expectations that must integrate the existing Neo-Glass
  design-system module (`FitTheme` tokens/components, no Material 3) and satisfy
  accessibility gates. No new bespoke UI component system is
  introduced in this plan.
- **Public Release Gates**: PASS. Plan includes automated validation, release
  artifact checks, seed/asset validation, permissions/backup review, export
  sanity checks, and milestone/release manual device verification.

## Project Structure

### Documentation (this feature)

```text
specs/001-bootstrap-app-foundation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── foundation-contracts.md
└── tasks.md             # Created by /speckit-tasks
```

### Source Code (repository root)

```text
settings.gradle.kts
build.gradle.kts
gradle/
└── libs.versions.toml

shared/
├── build.gradle.kts
├── src/commonMain/
│   ├── kotlin/com/jjswigut/oopsallprs/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   ├── validation/
│   │   │   └── usecase/
│   │   ├── data/
│   │   │   ├── exercise/
│   │   │   ├── export/
│   │   │   ├── repository/
│   │   │   └── session/
│   │   ├── db/
│   │   ├── platform/
│   │   ├── settings/
│   │   └── ui/          # Minimal shared flow integration around design system
│   ├── resources/
│   │   └── exercises.csv
│   └── sqldelight/com/jjswigut/oopsallprs/db/
│       ├── Database.sq
│       └── migrations/
├── src/commonTest/
├── src/androidMain/
├── src/androidUnitTest/
└── src/iosMain/

androidApp/
└── src/main/

iosApp/
└── iosApp/
```

**Structure Decision**: Use the Kotlin Multiplatform mobile structure above.
The fresh repo will not preserve the current app's large catch-all
`DatabaseHelper`; it will split typed SQLDelight access, mappers, repositories,
seed ingestion, and derived-data services into focused shared modules. Platform
source sets are adapter-only, and UI work in this feature wires shared flows
through the existing design-system module rather than planning new components.

## Complexity Tracking

No constitution violations are planned.
