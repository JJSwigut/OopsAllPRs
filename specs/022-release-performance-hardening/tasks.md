# Tasks: Release Performance Hardening

**Input**: Design documents from `/specs/022-release-performance-hardening/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/release-artifact-contract.md, quickstart.md

**Tests**: Release hardening requires automated build checks plus manual/smoke validation on optimized artifacts. Large build validation must wait until disk space is available.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup

**Purpose**: Establish release-hardening baseline and make environment blockers visible.

- [X] T001 Record current disk space and release-build blocker status in `specs/022-release-performance-hardening/quickstart.md`
- [X] T002 Review Android release build settings in `androidApp/build.gradle.kts`
- [X] T003 Review iOS Release build settings and framework paths in `iosApp/OopsAllPRs.xcodeproj/project.pbxproj`
- [X] T004 [P] Review existing Android manifest/receiver/resource entry points in `androidApp/src/main/AndroidManifest.xml`
- [X] T005 [P] Review shared Compose resource and seed CSV paths under `shared/src/commonMain/composeResources/`

---

## Phase 2: Foundational

**Purpose**: Add release evidence and keep-rule scaffolding before enabling aggressive behavior.

- [X] T006 Create Android app keep-rule file `androidApp/proguard-rules.pro`
- [X] T007 Add release evidence checklist section to `specs/022-release-performance-hardening/quickstart.md`
- [X] T008 Define release smoke checklist for startup/Profile/export/workout in `specs/022-release-performance-hardening/quickstart.md`

**Checkpoint**: Release validation surfaces are documented before optimizer changes.

---

## Phase 3: User Story 1 - Android Release Is Optimized Safely (Priority: P1) 🎯 MVP

**Goal**: Android release uses R8/minification and resource shrinking while preserving core app flows.

**Independent Test**: Build Android release, inspect optimization evidence, install/run smoke flow when device/signing permits.

### Tests for User Story 1

- [X] T009 [P] [US1] Add Android release metadata inspection commands to `specs/022-release-performance-hardening/quickstart.md`
- [X] T010 [P] [US1] Add expected mapping/resource-shrink evidence to `contracts/release-artifact-contract.md`

### Implementation for User Story 1

- [X] T011 [US1] Enable Android release minification in `androidApp/build.gradle.kts`
- [X] T012 [US1] Enable Android release resource shrinking in `androidApp/build.gradle.kts`
- [X] T013 [US1] Wire `androidApp/proguard-rules.pro` into the Android release build in `androidApp/build.gradle.kts`
- [X] T014 [US1] Add conservative keep rules for `com.jjswigut.oopsallprs.platform.RestTimerReceiver` and runtime entry points in `androidApp/proguard-rules.pro`
- [X] T015 [US1] Add keep/resource rules needed for Compose/KMP resources, SQLDelight, and packaged seed resources after validation in `androidApp/proguard-rules.pro`

**Checkpoint**: Android release optimization is configured and ready for build validation.

---

## Phase 4: User Story 2 - iOS Release Is Archive-Ready (Priority: P2)

**Goal**: iOS release validation proves release framework linkage, clean release metadata, and symbol evidence.

**Independent Test**: Build iOS Release simulator app and archive when disk/signing prerequisites are available.

### Tests for User Story 2

- [X] T016 [P] [US2] Add iOS Release metadata inspection commands to `specs/022-release-performance-hardening/quickstart.md`
- [X] T017 [P] [US2] Add iOS archive/symbol evidence expectations to `contracts/release-artifact-contract.md`

### Implementation for User Story 2

- [X] T018 [US2] Verify Release framework path selection in `iosApp/OopsAllPRs.xcodeproj/project.pbxproj`
- [X] T019 [US2] Add or document dSYM/symbol release evidence settings in `iosApp/OopsAllPRs.xcodeproj/project.pbxproj`
- [X] T020 [US2] Document signing-asset and disk-space prerequisites for iOS archive validation in `specs/022-release-performance-hardening/quickstart.md`

**Checkpoint**: iOS Release is ready for simulator build and archive validation once environment prerequisites are satisfied.

---

## Phase 5: User Story 3 - Release Evidence Is Repeatable (Priority: P3)

**Goal**: Release hardening can be rerun and reviewed without one-off commands.

**Independent Test**: Follow quickstart and produce an evidence summary for Android and iOS.

### Implementation for User Story 3

- [X] T021 [US3] Add a release evidence output template to `specs/022-release-performance-hardening/quickstart.md`
- [X] T022 [US3] Add blocker-reporting instructions for low disk, missing signing, or failed smoke checks in `specs/022-release-performance-hardening/quickstart.md`
- [X] T023 [US3] Update `specs/022-release-performance-hardening/tasks.md` with validation outcomes after implementation

**Checkpoint**: Release evidence is repeatable and incomplete validation cannot be mistaken for success.

---

## Phase 6: Polish & Cross-Cutting Validation

**Purpose**: Run final checks after implementation and adequate disk cleanup.

- [X] T024 Run `./gradlew :shared:testDebugUnitTest`
- [X] T025 Run `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease`
- [X] T026 Inspect Android release APK/AAB metadata, mapping output, and shrink evidence
- [ ] T027 Install and smoke-test Android release when signing/device conditions allow
- [X] T028 Run iOS Release simulator build after freeing disk space
- [X] T029 Inspect iOS Release app metadata and symbol evidence
- [ ] T030 Run iOS archive validation when signing assets are available
- [X] T031 Record final release evidence and blockers in `specs/022-release-performance-hardening/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on setup review.
- **User Story 1 (Phase 3)**: Depends on foundational keep-rule scaffold.
- **User Story 2 (Phase 4)**: Depends on setup review.
- **User Story 3 (Phase 5)**: Can proceed after foundational docs, then refined after US1/US2.
- **Polish (Phase 6)**: Depends on implementation and sufficient disk space.

### Parallel Opportunities

- T004 and T005 can run in parallel.
- T009 and T010 can run in parallel.
- T016 and T017 can run in parallel.
- Android optimization tasks and iOS archive-readiness tasks can be implemented independently after Phase 2.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete setup and keep-rule scaffold.
2. Enable Android release minification/resource shrinking.
3. Validate Android release artifact after disk cleanup.

### Incremental Delivery

1. Android release optimization.
2. iOS Release/archive readiness.
3. Repeatable evidence collection.
4. Full cross-platform validation.

## Notes

- Current environment had 58 GiB free during final validation.
- Android release install/smoke remains pending until release signing is available.
- iOS archive validation remains pending until signing assets/provisioning are available.
- Separate iOS device Release KMP framework validation became idle in Gradle/Kotlin Native and was terminated with exit 143; simulator Release framework validation passed through Xcode.
- Optional Spec Kit git commit hooks were not executed during task generation.
