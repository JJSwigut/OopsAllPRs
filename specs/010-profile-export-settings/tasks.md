# Tasks: Profile Settings and Local Export

**Input**: Design documents from `/specs/010-profile-export-settings/`

**Prerequisites**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `contracts/profile-export-settings-contracts.md`, `quickstart.md`

**Tests**: Required. This slice changes durable settings, local export orchestration, platform export handoff, shared UI, and app-shell preferences.

**Organization**: Tasks are grouped by user story so each story can be implemented and verified independently.

## Phase 1: Setup

**Purpose**: Confirm the feature package and existing persistence/export surfaces are ready.

- [X] T001 Verify Spec Kit artifacts for feature 010 have no unresolved placeholders in `specs/010-profile-export-settings/spec.md`, `specs/010-profile-export-settings/plan.md`, `specs/010-profile-export-settings/research.md`, `specs/010-profile-export-settings/data-model.md`, `specs/010-profile-export-settings/contracts/profile-export-settings-contracts.md`, and `specs/010-profile-export-settings/quickstart.md`
- [X] T002 Confirm `AGENTS.md` points to `specs/010-profile-export-settings/plan.md`
- [X] T003 [P] Inspect Profile, app-state, platform handoff, and design-system component signatures in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/platform/PlatformAdapters.kt`, and `design-system/src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/`

---

## Phase 2: Foundational

**Purpose**: Shared Profile dependencies and test coverage that all stories use.

**CRITICAL**: No user story can complete until Profile state can receive repositories and expose deterministic state.

- [X] T004 Add shared Profile state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolderTest.kt`
- [X] T005 Inject `PreferencesRepository`, `ExportRepository`, and optional `FileExportHandoff` into `ProfileStateHolder` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T006 Wire Profile dependencies through app construction in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt`, and `androidApp/src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt`

**Checkpoint**: Profile state can compile with repository-backed settings/export dependencies.

---

## Phase 3: User Story 1 - Choose Durable Display Units (Priority: P1)

**Goal**: Users can select pounds or kilograms from Profile and the selection survives restart.

**Independent Test**: Persist a unit through Profile state, recreate the state holder against the same store, hydrate, and verify the unit reloads without mutating source weights.

### Tests for User Story 1

- [X] T007 [P] [US1] Add preference hydration and durable unit selection tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolderTest.kt`

### Implementation for User Story 1

- [X] T008 [US1] Implement `hydrate` and `setWeightUnit` behavior in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T009 [US1] Add unit-selection Profile UI in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T010 [US1] Refresh Profile and Progress state after unit changes in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: Durable unit settings work independently.

---

## Phase 4: User Story 2 - Export Local Training Data (Priority: P2)

**Goal**: Users can export workouts, routines, exercises, and PR history from Profile.

**Independent Test**: Request each export type through Profile state and verify generated metadata, row counts, current unit, and handoff calls.

### Tests for User Story 2

- [X] T011 [P] [US2] Add export success and failure state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolderTest.kt`

### Implementation for User Story 2

- [X] T012 [US2] Implement Profile export orchestration in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T013 [US2] Add Profile export controls and status feedback in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T014 [US2] Implement Android export share handoff in `shared/src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/AndroidPlatformAdapters.kt`

**Checkpoint**: Profile can export all local data categories independently.

---

## Phase 5: User Story 3 - Review Local-First Status and Interaction Preferences (Priority: P3)

**Goal**: Profile shows local-first/backup status and controls runtime interaction preferences.

**Independent Test**: Open Profile offline, adjust runtime preferences, and verify the app shell state changes immediately while local-first status remains visible.

### Tests for User Story 3

- [X] T015 [P] [US3] Add interaction preference and local status state tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolderTest.kt`

### Implementation for User Story 3

- [X] T016 [US3] Implement haptics, reduced-motion, palette, and local-status state behavior in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T017 [US3] Add local-first status and interaction controls in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T018 [US3] Apply Profile interaction preferences through navigation state in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`

**Checkpoint**: Profile owns local status display and app-shell interaction controls.

---

## Phase 6: Polish and Release Validation

**Purpose**: Cross-cutting validation for shared KMP, design-system usage, export sanity, and release readiness.

- [X] T019 [P] Verify Profile UI uses `FitTheme` and design-system components only in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T020 Run `./gradlew :shared:testDebugUnitTest`
- [X] T021 Run `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`
- [X] T022 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T023 Run Material scan: `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts`
- [X] T024 Run whitespace validation: `git diff --check -- .`
- [X] T025 Attempt Android manual gate discovery with `~/Library/Android/sdk/platform-tools/adb devices -l` and `~/Library/Android/sdk/emulator/emulator -list-avds`; if no target exists, document the deferred manual gate
- [X] T026 Stage and commit feature 010 changes with message `Implement profile export settings`

---

## Dependencies and Execution Order

- Phase 1 must complete before implementation.
- Phase 2 blocks all user stories because Profile dependency injection and tests are shared.
- US1 is the MVP and should complete before export because exports use the selected unit.
- US2 depends on US1 unit state and shared Profile export dependencies.
- US3 can follow US1/US2 and uses the same Profile UI/state surface.
- Phase 6 runs after all selected stories are implemented.

## Parallel Opportunities

- T003 can run independently after setup.
- T007, T011, and T015 share one test file and should be coordinated, but each validates a separate behavior.
- T009, T013, and T017 share one UI file and should be implemented sequentially.
- T020, T021, T022, T023, and T024 are independent validation gates but should be reported separately.

## Implementation Strategy

1. Create Profile state tests and inject repository dependencies.
2. Implement durable weight unit hydration/selection first.
3. Implement export orchestration and Android platform handoff.
4. Complete Profile UI for status, interaction preferences, and export actions.
5. Run Android-first and iOS compile gates before commit.
