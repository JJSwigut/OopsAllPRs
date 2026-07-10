# Tasks: User-Owned Cloud Backup Sync

**Input**: Design documents from `/specs/024-cloud-backup-sync/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: This feature changes persistence, restore, Profile state, platform file access, and data ownership flows, so each user story includes automated validation tasks before implementation tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish backup package and sync-state scaffolding without changing user flows.

- [X] T001 Add kotlinx serialization dependency if absent in shared/build.gradle.kts
- [X] T002 [P] Create backup package model scaffolding in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupPackageModels.kt
- [X] T003 [P] Create sync state domain models in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/BackupSyncModels.kt
- [X] T004 [P] Add backup/sync repository interfaces in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/repository/FoundationRepositories.kt
- [X] T005 Add sync_state schema and queries in shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/Database.sq
- [X] T006 Add sync_state migration in shared/src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/migrations/6.sqm

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core snapshot, revision, storage, and adapter boundaries required by every story.

- [X] T007 [P] Add BackupDocumentAdapter boundary in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/platform/PlatformAdapters.kt
- [X] T008 [P] Add local revision calculator in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/LocalRevisionCalculator.kt
- [X] T009 [P] Add backup snapshot reader in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSnapshotReader.kt
- [X] T010 [P] Add backup package serializer/validator in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupPackageCodec.kt
- [X] T011 Add SQL sync state repository in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupSyncRepository.kt
- [X] T012 Wire backup/sync repositories and platform adapter through shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/di/AppModule.kt
- [X] T013 [P] Add Android no-op compile placeholder or real adapter type in shared/src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/AndroidPlatformAdapters.kt
- [X] T014 [P] Add iOS compile-safe document adapter type in shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/IosPlatformAdapters.kt

**Checkpoint**: Foundation ready - user story implementation can begin.

---

## Phase 3: User Story 1 - Link and Create a Portable Backup (Priority: P1) MVP

**Goal**: Users can link a visible backup file, write a complete backup, and see status restored after restart.

**Independent Test**: Link a backup file, run Backup now, restart, and verify linked metadata plus backup status reload.

### Tests for User Story 1

- [X] T015 [P] [US1] Add backup serialization round-trip tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/BackupPackageCodecTest.kt
- [X] T016 [P] [US1] Add snapshot coverage test for preferences/exercises/routines/active session/history/sets/progress in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSnapshotReaderTest.kt
- [X] T017 [P] [US1] Add sync state persistence test in shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupSyncRepositoryTest.kt
- [X] T018 [P] [US1] Add Profile link and Backup now state tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileBackupSyncStateHolderTest.kt

### Implementation for User Story 1

- [X] T019 [US1] Implement full backup snapshot extraction in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSnapshotReader.kt
- [X] T020 [US1] Implement backup package JSON encoding and plain-file metadata in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupPackageCodec.kt
- [X] T021 [US1] Implement link metadata and last outcome persistence in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupSyncRepository.kt
- [X] T022 [US1] Add Profile backup state/actions in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt
- [X] T023 [US1] Add Profile link, Backup now, linked location, last backup, and plain-file privacy UI in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt
- [X] T024 [US1] Implement Android SAF create/open/read/write adapter in shared/src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/AndroidPlatformAdapters.kt
- [X] T025 [US1] Wire Android document picker launch/persisted URI permission from androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt

**Checkpoint**: User Story 1 is functional and independently testable.

---

## Phase 4: User Story 2 - Restore from a Backup Safely (Priority: P2)

**Goal**: Users can restore a valid backup with validation, active-workout warning, safety backup, and transactional replacement.

**Independent Test**: Restore into empty and populated local databases, including a simulated failure that leaves prior data intact.

### Tests for User Story 2

- [X] T026 [P] [US2] Add restore-into-empty repository test in shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupRestoreRepositoryTest.kt
- [X] T027 [P] [US2] Add restore-over-existing safety backup and active-workout warning tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileBackupRestoreStateTest.kt
- [X] T028 [P] [US2] Add simulated restore failure transaction test in shared/src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupRestoreTransactionTest.kt

### Implementation for User Story 2

- [X] T029 [US2] Add backup restore validator and restore plan builder in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupRestorePlanner.kt
- [X] T030 [US2] Implement FK-safe transactional restore in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/repository/SqlBackupRepository.kt
- [X] T031 [US2] Implement safety backup creation before destructive restore in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSafetyExporter.kt
- [X] T032 [US2] Add Restore from file state flow and confirmation models in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt
- [X] T033 [US2] Add Restore from file UI, active-workout warning, and safety-copy status in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt

**Checkpoint**: User Story 2 is functional and independently testable.

---

## Phase 5: User Story 3 - Sync and Resolve Conflicts Explicitly (Priority: P3)

**Goal**: Users can run manual or launch/resume sync checks and make explicit conflict decisions.

**Independent Test**: Exercise local-only, backup-only, both-changed cancel, keep-local, and restore-backup states.

### Tests for User Story 3

- [X] T034 [P] [US3] Add conflict policy tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSyncConflictPolicyTest.kt
- [X] T035 [P] [US3] Add Profile Sync now and conflict sheet tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileBackupConflictStateTest.kt
- [X] T036 [P] [US3] Add launch/resume non-blocking sync check test in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShellBackupSyncTest.kt

### Implementation for User Story 3

- [X] T037 [US3] Implement local/backup revision comparison in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSyncCoordinator.kt
- [X] T038 [US3] Implement local-only write and backup-only restore prompt handling in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSyncCoordinator.kt
- [X] T039 [US3] Implement both-changed conflict decisions in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSyncCoordinator.kt
- [X] T040 [US3] Add Sync now, conflict summary, and decision handling to shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt
- [X] T041 [US3] Add conflict sheet and status rows to shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt
- [X] T042 [US3] Trigger lightweight launch/resume sync checks from shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt

**Checkpoint**: User Story 3 is functional and independently testable.

---

## Phase 6: Platform Hardening & Polish

**Purpose**: Complete platform behavior, validation evidence, and release-quality checks.

- [X] T043 [P] Complete iOS document picker/bookmark adapter in shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/IosPlatformAdapters.kt
- [X] T044 [P] Add iOS host presentation wiring in iosApp/iosApp/ContentView.swift
- [X] T045 [P] Add Android document-provider smoke notes to specs/024-cloud-backup-sync/quickstart.md
- [X] T046 Verify existing CSV exports still pass via shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/export/ExportSnapshotTest.kt
- [X] T047 Run Profile accessibility/token review for shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt
- [X] T048 Run ./gradlew :shared:allTests
- [X] T049 Run ./gradlew :androidApp:assembleDebug
- [X] T050 Run iOS compile gate documented in specs/024-cloud-backup-sync/quickstart.md
- [X] T051 Record final validation evidence in specs/024-cloud-backup-sync/validation/cloud-backup-sync-results.md

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on Setup and blocks all user stories.
- **User Story 1 (P1)**: Depends on Foundational and is the MVP.
- **User Story 2 (P2)**: Depends on Foundational and can be developed after or alongside US1, but safety backup may reuse US1 package writing.
- **User Story 3 (P3)**: Depends on Foundational and benefits from US1/US2 services for write/restore decisions.
- **Platform Hardening & Polish**: Depends on desired story completion.

### Parallel Opportunities

- T002-T004 can run in parallel.
- T007-T010 and T013-T014 can run in parallel after setup.
- Test tasks within each user story can run in parallel.
- iOS adapter hardening can run in parallel with Android smoke documentation after shared contracts stabilize.

### Parallel Example: User Story 1

```bash
Task: "Add backup serialization round-trip tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/BackupPackageCodecTest.kt"
Task: "Add snapshot coverage test in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/data/backup/BackupSnapshotReaderTest.kt"
Task: "Add Profile link and Backup now state tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileBackupSyncStateHolderTest.kt"
```

## Implementation Strategy

### MVP First

1. Complete Setup and Foundational phases.
2. Deliver User Story 1: link plus Backup now.
3. Validate backup package completeness and persisted linked status.
4. Stop and demo Profile-owned portable backup before adding restore/sync.

### Incremental Delivery

1. Add safe restore as User Story 2.
2. Add light sync and explicit conflict handling as User Story 3.
3. Complete platform hardening, iOS adapter wiring, and final evidence.

## Notes

- Keep backup package and CSV export separate.
- Keep sync checks non-blocking for active workout logging.
- Do not introduce app accounts, OAuth, provider SDKs, hidden provider storage, encryption, or background sync in V1.
- Verify restore tests fail before implementation and cover partial-failure rollback.
