# Tasks: Android Alpha Hardening

**Input**: Design documents from `/specs/015-android-alpha-hardening/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Required for recovery/session behavior and shared readiness model changes. Manual Android alpha smoke evidence is required for the release-gate artifact, but may be deferred with a recorded reason when no physical device is available.

## Phase 1: Setup

**Purpose**: Confirm feature artifacts and context are ready.

- [X] T001 Confirm spec, plan, research, data model, contracts, quickstart, checklist, and validation artifacts exist in `specs/015-android-alpha-hardening/`
- [X] T002 Confirm AGENTS.md and `.specify/feature.json` point to `specs/015-android-alpha-hardening/`

---

## Phase 2: Foundational

**Purpose**: Shared UI/readiness model and recovery test scaffolding that block user stories.

- [X] T003 Inspect existing Profile, History, Progress, Train, navigation, lifecycle, notification adapter, and recovery tests for alpha-hardening gaps in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/` and `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/`
- [X] T004 [P] Add Profile readiness status test coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileReadinessStatusTest.kt`
- [X] T005 [P] Add active session expired-rest hydration test coverage in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActiveSessionHydrationHardeningTest.kt`

---

## Phase 3: User Story 1 - Dogfood Smoke Confidence (Priority: P1) MVP

**Goal**: Users/testers can see local-readiness truth in Profile and follow a repeatable alpha smoke checklist.

**Independent Test**: Open Profile and confirm readiness rows accurately communicate local storage, sync off, backup, rest alerts, export, and manual alpha gate; follow the validation checklist.

### Tests for User Story 1

- [X] T006 [US1] Run Profile readiness tests added in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileReadinessStatusTest.kt`

### Implementation for User Story 1

- [X] T007 [US1] Extend `LocalReadinessStatus` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T008 [US1] Render additional readiness rows in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T009 [US1] Update alpha smoke checklist evidence in `specs/015-android-alpha-hardening/validation/android-alpha-hardening-results.md`

---

## Phase 4: User Story 2 - Recovery Trust (Priority: P2)

**Goal**: Active workout and rest state recover correctly across cold-start hydration, including expired rest timer cleanup.

**Independent Test**: Run recovery hardening tests and verify expired rest clears, notification state cancels, and active workout remains resumable.

### Tests for User Story 2

- [X] T010 [US2] Run expired-rest hydration tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/ActiveSessionHydrationHardeningTest.kt`

### Implementation for User Story 2

- [X] T011 [US2] Adjust shared lifecycle/rest recovery behavior only if tests expose a gap in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt` or related state holders
- [X] T012 [US2] Record recovery evidence in `specs/015-android-alpha-hardening/validation/android-alpha-hardening-results.md`

---

## Phase 5: User Story 3 - Empty-State Polish (Priority: P3)

**Goal**: Fresh-install Train, History, Progress, and Profile states are truthful, compact, and not visually abandoned.

**Independent Test**: Clear data, visit each top-level destination, and confirm each shows meaningful guidance/status with no overlapping primary controls.

### Tests for User Story 3

- [X] T013 [P] [US3] Update existing progress/history state tests if empty copy changes require assertions in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/`

### Implementation for User Story 3

- [X] T014 [US3] Improve History empty-state copy in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`
- [X] T015 [US3] Improve Progress empty-state copy in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressStateHolder.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt`
- [X] T016 [US3] Verify Train/Profile fresh-install status remains compact using existing Fit components in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt` and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`

---

## Phase 6: Polish & Validation

**Purpose**: Run gates, record evidence, and close tasks.

- [X] T017 Run `./gradlew :shared:testDebugUnitTest`
- [X] T018 Run `./gradlew :androidApp:assembleDebug`
- [X] T019 Run `./gradlew :shared:compileKotlinIosSimulatorArm64`
- [X] T020 Run Material guard against shared/design-system sources
- [X] T021 Run `git diff --check -- .`
- [X] T022 Record manual Android alpha smoke outcome or deferred physical-device note in `specs/015-android-alpha-hardening/validation/android-alpha-hardening-results.md`

## Dependencies & Execution Order

- Phase 1 has no dependencies.
- Phase 2 blocks user story work.
- US1 and US3 can proceed independently after Phase 2.
- US2 depends on recovery test scaffolding from Phase 2.
- Polish depends on all user stories selected for this slice.

## Parallel Opportunities

- T004 and T005 can be written in parallel.
- US1 Profile readiness copy and US3 empty-state copy can be implemented in parallel after test scaffolding.
- Automated validation gates should run after all code changes are complete.

## Implementation Strategy

1. Complete feature docs and confirm Spec Kit pointers.
2. Add targeted tests for readiness and recovery.
3. Implement Profile readiness and empty-state polish.
4. Run automated validation gates and update validation evidence.
5. Defer or record manual Pixel validation based on connected device availability.
