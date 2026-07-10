# Implementation Plan: User-Owned Cloud Backup Sync

**Branch**: `024-cloud-backup-sync` | **Date**: 2026-06-04 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/024-cloud-backup-sync/spec.md`

## Summary

Add Profile-based user-owned backup and light sync while keeping Oops All PRs local-first and account-free. Shared code will introduce backup package models, snapshot/restore orchestration, sync state, conflict policy, and Profile state/UI. Platform code will expose user-selected document read/write adapters: Android through Storage Access Framework and iOS through document picker/bookmark coordination. Existing CSV export remains separate for human-readable exports; the new backup package is restore-capable plain JSON.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31; Swift 5; Xcode 26.5 project.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight-backed `SqlFoundationStore`, repository interfaces, existing `ExportService`, kotlinx serialization for JSON package encoding if already available or added to shared, Compose state/UI, Kotlin coroutines, Android SAF intents, iOS `UIDocumentPickerViewController` and security-scoped bookmarks.

**Storage**: SQLDelight local SQLite remains the source of truth. Add a lightweight `sync_state` table plus migrations for linked backup metadata, local/backup revision tracking, last timestamps, and last outcome/error. Backup files live in user-selected platform document providers and are not stored in app-private hidden provider storage for V1.

**Local Data Model**: Backup package contains format/app schema metadata, device id, local revision, preferences, exercises, routines, active workout/session/drafts, completed workouts, sets, progress/PR rows, and export metadata when useful. Local revision derives from existing created/updated timestamps initially. Restore replaces dependent rows inside one transaction in FK-safe order.

**Testing**: Common unit tests for backup serialization, snapshot coverage, restore validation, restore transaction failure, sync conflict policy, and Profile state. Android unit/instrumented or smoke validation for document link/read/write with persisted URI permission. iOS compile/adapter tests for bookmark boundary and shared failure handling. Existing Gradle unit tests remain the main automated gate.

**Target Platform**: Android runtime first; iOS remains first-class through shared state/UI and native document adapter boundaries.

**Platform Scope**: Shared code owns backup package format, snapshot/restore repository, sync repository, conflict decision logic, Profile state models, and shared Profile UI. Android owns SAF open/create document launchers, persisted URI permission, and stream read/write. iOS owns document picker presentation, security-scoped bookmark persistence handoff, coordinated reads/writes, and adapter error mapping.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI and native Android/iOS platform adapters.

**Performance Goals**: Launch/resume sync check should only inspect linked backup metadata and not block workout navigation. Manual backup/restore can show in-progress state. Ordinary personal workout history backup should complete within a few seconds on modern devices, with recoverable errors for provider latency or unavailable files.

**Constraints**: No Firebase, app accounts, hosted backend, OAuth, provider-specific SDK, hidden appDataFolder-style storage, encryption/passphrases, background sync, or field-level multi-device merge in V1. Plain backups must be explicitly labeled as readable by anyone with file/storage access. Existing CSV export remains intact.

**Scale/Scope**: Single-user local workout history, seeded and custom exercises, routines/templates, active session state, completed ledger, PR/progress evidence, preferences, export metadata, one linked backup file, and whole-snapshot conflict choices.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Sync checks are lightweight and non-blocking; conflict and restore choices are Profile actions outside the active logging path.
- **Ledger Integrity**: PASS. Backup and restore cover full ledger rows, timestamps, active session state, and PR evidence; restore requires safety backup and transactional replacement.
- **Session Recovery**: PASS. Active session data is included in backups, and linked/sync state recovers after app restart.
- **Progress Promise**: PASS. PR/progress rows, source evidence, bodyweight, fractional weights, and units are covered by backup round-trip tests.
- **Local-First Ownership**: PASS. Feature is account-free, provider-agnostic, visible, offline-capable, and user-controlled.
- **Shared-First KMP**: PASS. Business rules and Profile state live in shared code; platform differences stay behind adapters.
- **Design System and Accessibility**: PASS. Profile additions use existing Fit design-system components/tokens and must provide readable status, accessible actions, and explicit conflict warnings.
- **Public Release Gates**: PASS. Plan includes serialization, restore, transaction, conflict, platform adapter, Profile state, Android smoke, iOS compile, permissions, backup, and export sanity checks.

## Project Structure

### Documentation (this feature)

```text
specs/024-cloud-backup-sync/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- backup-sync-contracts.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- data/backup/
|   |-- data/repository/
|   |-- domain/model/
|   |-- domain/repository/FoundationRepositories.kt
|   |-- platform/PlatformAdapters.kt
|   `-- ui/profile/
|-- src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
|   |-- Database.sq
|   `-- migrations/6.sqm
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/data/repository/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
|-- src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/backup/
`-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

iosApp/
`-- iosApp/
```

**Structure Decision**: Keep backup/sync orchestration in shared domain/data/state and introduce only narrow platform document adapters. Extend existing Profile state/UI instead of adding a new top-level destination. Keep CSV export service separate from restore-capable backup package generation.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Use a visible user-selected plain JSON backup package for V1.
- Use whole-snapshot backup/restore and conflict decisions rather than field-level merge.
- Store only linked document access metadata and sync state locally.
- Derive V1 local revision from persisted timestamps and defer mutation logs.
- Use platform document providers instead of account/provider SDK integrations.
- Generate a safety backup before destructive restore.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/backup-sync-contracts.md](./contracts/backup-sync-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. The design remains local-first, user-controlled, shared-first, and non-blocking for workout logging. Restore and sync decisions protect ledger integrity with validation, safety backup, transactionality, and explicit user confirmation.
