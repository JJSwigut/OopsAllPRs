# Tasks: Release and Debug Apps

**Input**: Design documents from `/specs/021-release-debug-apps/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/profile-developer-tools-contract.md, quickstart.md

**Tests**: Behavior changes require shared unit tests plus Android and iOS build validation.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Phase 1: Setup

**Purpose**: Confirm current build-channel wiring and validation targets.

- [X] T001 Review existing Android and iOS app entry points in `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt` and `shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/MainViewController.kt`
- [X] T002 Review Profile developer card gating in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`

---

## Phase 2: Foundational

**Purpose**: Add a shared testable contract for developer-tool availability.

- [X] T003 [P] Add developer-tools availability tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/AppStateDeveloperToolsTest.kt`
- [X] T004 Add any required test helpers in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/testing/AppStateFixtures.kt`

**Checkpoint**: Shared build-channel capability is covered before platform edits.

---

## Phase 3: User Story 1 - Release Profile Has No Developer Options (Priority: P1) 🎯 MVP

**Goal**: Release builds disable developer tools and Profile receives no developer state.

**Independent Test**: Build release variants and verify release Profile capability is disabled.

### Tests for User Story 1

- [X] T005 [P] [US1] Add release-disabled coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/AppStateDeveloperToolsTest.kt`

### Implementation for User Story 1

- [X] T006 [US1] Update `shared/src/iosMain/kotlin/com/jjswigut/oopsallprs/MainViewController.kt` so iOS receives `developerToolsEnabled` from platform bootstrap instead of hardcoding `true`
- [X] T007 [US1] Update `iosApp/iosApp/ContentView.swift` to pass `false` for Release and `true` for Debug using Swift build configuration
- [X] T008 [US1] Verify Android release still passes `BuildConfig.DEBUG` from `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`

**Checkpoint**: Release app paths cannot render developer options.

---

## Phase 4: User Story 2 - Debug Builds Keep Developer Tools Available (Priority: P2)

**Goal**: Debug builds still expose developer seed tools for local validation.

**Independent Test**: Build debug variants and verify developer capability is enabled.

### Tests for User Story 2

- [X] T009 [P] [US2] Add debug-enabled coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/AppStateDeveloperToolsTest.kt`

### Implementation for User Story 2

- [X] T010 [US2] Ensure iOS Debug passes `developerToolsEnabled = true` in `iosApp/iosApp/ContentView.swift`
- [X] T011 [US2] Verify Android debug still passes `BuildConfig.DEBUG=true` through `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`

**Checkpoint**: Debug app paths retain developer tools.

---

## Phase 5: User Story 3 - Debug and Release Apps Are Distinguishable (Priority: P3)

**Goal**: Debug artifacts are distinguishable from release artifacts on both platforms.

**Independent Test**: Inspect Android package metadata and iOS plist metadata for Debug and Release.

### Implementation for User Story 3

- [X] T012 [US3] Add Android debug `applicationIdSuffix` and debug display-name placeholder in `androidApp/build.gradle.kts`
- [X] T013 [US3] Update Android manifest display label in `androidApp/src/main/AndroidManifest.xml`
- [X] T014 [US3] Add iOS Debug-specific bundle identifier and display-name build settings in `iosApp/OopsAllPRs.xcodeproj/project.pbxproj`
- [X] T015 [US3] Update `iosApp/iosApp/Info.plist` to consume the display-name build setting

**Checkpoint**: Debug and release artifacts are not ambiguous during validation.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate the full feature and update Spec Kit evidence.

- [X] T016 Run `./gradlew :shared:testDebugUnitTest`
- [X] T017 Run `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease`
- [X] T018 Run iOS Debug simulator build from `specs/021-release-debug-apps/quickstart.md`
- [ ] T019 Run iOS Release simulator build from `specs/021-release-debug-apps/quickstart.md`
- [X] T020 Inspect generated Android and iOS metadata to confirm debug/release identity and developer-tool expectations

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies.
- **Foundational (Phase 2)**: Depends on setup review.
- **User Story 1 (Phase 3)**: Depends on foundational shared contract.
- **User Story 2 (Phase 4)**: Depends on foundational shared contract.
- **User Story 3 (Phase 5)**: Can proceed after setup, but final validation depends on US1 and US2.
- **Polish (Phase 6)**: Depends on all implementation phases.

### Parallel Opportunities

- T003/T005/T009 affect one test file and should be implemented together.
- Android metadata tasks T012-T013 can be done independently from iOS metadata tasks T014-T015.
- Validation builds can run independently when disk space allows.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete setup and shared contract tests.
2. Fix iOS release gating.
3. Validate release Profile capability is disabled.

### Incremental Delivery

1. Release hiding first.
2. Debug availability second.
3. Artifact identity third.
4. Full build validation last.

## Notes

- Optional Spec Kit git commit hooks were not executed during task generation.
- T019 was attempted, but the iOS Release simulator build was interrupted after the machine reached 100% disk usage. The generated Release `Info.plist` metadata was still inspected and matched the release identity contract.
