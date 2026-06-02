---
description: "Task list for the App UX navigation shell"
---

# Tasks: App UX Navigation Shell

**Input**: Design documents from `/specs/003-app-ux-navigation/`

**Prerequisites**: [plan.md](./plan.md), [spec.md](./spec.md),
[research.md](./research.md), [data-model.md](./data-model.md),
[contracts/app-navigation-contracts.md](./contracts/app-navigation-contracts.md),
[quickstart.md](./quickstart.md)

**Scope**: This task list implements the first feature 003 slice from the plan:
navigation shell, `FitTheme` wiring, Train/History/Progress/Profile
destinations, active-workout overlay/resume banner, route recovery, and shared
Material 3 retirement. Active set logging, rest UI, PR celebration, detailed
history/progress/profile, and export UI are intentionally deferred to later
feature 003 slices.

**Tests**: Required for route parsing, adaptive layout classification, shell
state transitions, active-session recovery, Material retirement, Android build,
and iOS shared compile.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel after dependencies in earlier phases are done
- **[Story]**: User story label from `spec.md`; this slice implements US1
- All task descriptions include exact file paths

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Prepare shared build dependencies and validation evidence files.

- [X] T001 Update `shared/build.gradle.kts` to add the `:design-system` dependency and remove shared `libs.compose.material3`, `libs.compose.material.icons.extended`, and `libs.vico.multiplatform.m3` dependencies
- [X] T002 Update `androidApp/build.gradle.kts` to remove the unused `libs.androidx.compose.material3` dependency if no Android source imports Material 3
- [X] T003 Update `gradle/libs.versions.toml` to remove now-unused Material 3, material icon, and Vico aliases only after T001 and T002 remove all references
- [X] T004 [P] Create `specs/003-app-ux-navigation/validation/navigation-shell-results.md` with empty sections for unit tests, Android build, iOS compile, Material scan, and manual validation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared primitives and route persistence required before the user
story shell can compile.

**CRITICAL**: No US1 shell composition work should begin until these tasks are complete.

- [X] T005 Replace the Material 3 implementation in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/designsystem/DesignSystemBridge.kt` with `FitTheme`, `FitButton`, and `BasicText` helpers styled from design-system tokens
- [X] T006 Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/accessibility/FoundationAccessibility.kt` so shared touch-target helpers meet `FitTheme.size.touchMin` expectations and no longer encode the old 48dp minimum
- [X] T007 [P] Implement `TopLevelDestination` and `AppRoute` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppDestination.kt`
- [X] T008 [P] Implement `NavigationLayoutClass` and width classification rules in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/NavigationLayout.kt`
- [X] T009 [P] Implement `AppShellState`, `ActiveWorkoutResume`, `NavigationIntent`, and `PaletteMode` in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppNavigationState.kt`
- [X] T010 Add route persistence and last-opened-route preservation methods to `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleUseCases.kt`
- [X] T011 Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt` to create and expose the app navigation state holder alongside existing workout state
- [X] T012 [P] Replace Material 3 imports/usages in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/exercise/ExercisePickerFlow.kt` with design-system bridge text/actions
- [X] T013 [P] Replace Material 3 imports/usages in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineFlow.kt` with design-system bridge text/actions

**Checkpoint**: Shared dependencies no longer provide Material 3 to shared UI,
and navigation domain primitives are ready for US1.

---

## Phase 3: User Story 1 - Navigate the App and Resume Mid-Workout (Priority: P1) MVP

**Goal**: A lifter can move between Train, History, Progress, and Profile,
leave an active workout, see a persistent resume banner, and return to the
active-workout overlay with route/session state intact.

**Independent Test**: With an active session, select another top-level
destination, confirm the resume banner remains visible, tap Resume, and confirm
the active-workout overlay is presented from recovered state.

### Tests for User Story 1

- [X] T014 [P] [US1] Add route parsing and fallback tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppDestinationTest.kt`
- [X] T015 [P] [US1] Add compact/medium/expanded layout classification tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/NavigationLayoutTest.kt`
- [X] T016 [P] [US1] Add shell hydrate/select/resume/dismiss transition tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppNavigationStateHolderTest.kt`
- [X] T017 [P] [US1] Add active-session last-opened-route preservation tests in `shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleRouteTest.kt`

### Implementation for User Story 1

- [X] T018 [US1] Implement `AppNavigationStateHolder` reducer, hydration, route persistence calls, and active-session updates in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppNavigationStateHolder.kt`
- [X] T019 [P] [US1] Implement `ResumeBanner` using design-system components/tokens in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt`
- [X] T020 [P] [US1] Implement the medium/expanded `AppNavRail` using design-system components/tokens in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/AppNavRail.kt`
- [X] T021 [P] [US1] Implement `HistoryStateHolder` backed by completed workout data in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolder.kt`
- [X] T022 [P] [US1] Implement `HistoryFlow` placeholder content using design-system components in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt`
- [X] T023 [P] [US1] Implement `ProfileStateHolder` for shell-level unit/theme/haptic placeholders in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileStateHolder.kt`
- [X] T024 [P] [US1] Implement `ProfileFlow` placeholder content using design-system components in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt`
- [X] T025 [P] [US1] Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/WorkoutHomeFlow.kt` to use design-system text/actions and callbacks for start/resume
- [X] T026 [P] [US1] Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/ActiveWorkoutFlow.kt` to use design-system text/actions and expose an overlay dismiss action
- [X] T027 [P] [US1] Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/ProgressFlow.kt` to use design-system text/cards for PR summary placeholder content
- [X] T028 [US1] Implement compact bottom bar, rail layout, destination content, resume banner placement, and active-workout overlay in `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`
- [X] T029 [US1] Update `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/App.kt` to wrap the app in `FitTheme` and render `AppShell`
- [X] T030 [US1] Verify US1 behavior with `./gradlew :shared:testDebugUnitTest` and record results in `specs/003-app-ux-navigation/validation/navigation-shell-results.md`

**Checkpoint**: US1 is functional and independently testable: four
destinations render from the design system, active workout can be resumed, and
route recovery is covered by tests.

---

## Final Phase: Polish & Cross-Cutting Concerns

**Purpose**: Validate platform health, Material retirement, accessibility, and
milestone evidence for the slice.

- [X] T031 Run `rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts` and record the no-match result in `specs/003-app-ux-navigation/validation/navigation-shell-results.md`
- [X] T032 Run `./gradlew :design-system:testDebugUnitTest :shared:compileKotlinMetadata :shared:compileDebugKotlinAndroid :androidApp:assembleDebug` and record results in `specs/003-app-ux-navigation/validation/navigation-shell-results.md`
- [X] T033 Run `./gradlew :shared:compileKotlinIosSimulatorArm64` and record iOS compile results in `specs/003-app-ux-navigation/validation/navigation-shell-results.md`
- [X] T034 [P] Review `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt`, `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt`, and `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/AppNavRail.kt` for touch target, role/description, reduce-motion, and haptic-alternative coverage
- [X] T035 Defer the manual Android milestone checklist from `specs/003-app-ux-navigation/quickstart.md` because no device/emulator is attached; record the deferral in `specs/003-app-ux-navigation/validation/navigation-shell-results.md`
- [X] T036 Update this task list in `specs/003-app-ux-navigation/tasks.md` so completed tasks are checked and any intentionally deferred 003 UX stories remain documented as out of scope

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; start here.
- **Foundational (Phase 2)**: Depends on Setup; blocks all US1 shell composition.
- **US1 (Phase 3)**: Depends on Foundational; delivers the MVP.
- **Polish (Final Phase)**: Depends on US1 implementation and tests.

### User Story Dependencies

- **US1 (P1)**: Depends only on Setup and Foundational phases.
- **US2, US3, US4, US5 from the broader spec**: Deferred to later feature 003
  slices; they must receive their own plan/tasks before implementation.

### Within US1

- Tests T014-T017 should be written before implementation tasks T018-T029.
- Navigation models T007-T009 must exist before T014-T016 and T018.
- Route persistence T010 must exist before T017 and T018.
- Design-system bridge T005 must exist before flow conversion tasks T012-T013
  and T025-T027.
- `AppShell.kt` T028 depends on navigation state, resume banner, nav rail,
  history/profile flows, and converted destination flows.
- `App.kt` T029 depends on `AppShell.kt` T028.

### Parallel Opportunities

- T004 can run independently of dependency cleanup.
- T007, T008, and T009 can run in parallel.
- T012 and T013 can run in parallel after T005.
- T014, T015, T016, and T017 can run in parallel after their target models
  exist.
- T019, T020, T021, T022, T023, T024, T025, T026, and T027 can run in parallel
  after Foundational tasks are complete because they touch different files.
- T034 can run in parallel with validation command execution once US1 compiles.

---

## Parallel Example: User Story 1

```text
Task: "Add route parsing and fallback tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppDestinationTest.kt"
Task: "Add compact/medium/expanded layout classification tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/NavigationLayoutTest.kt"
Task: "Add shell hydrate/select/resume/dismiss transition tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppNavigationStateHolderTest.kt"
Task: "Add active-session last-opened-route preservation tests in shared/src/commonTest/kotlin/com/jjswigut/oopsallprs/domain/usecase/WorkoutLifecycleRouteTest.kt"
```

```text
Task: "Implement ResumeBanner using design-system components/tokens in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/ResumeBanner.kt"
Task: "Implement the medium/expanded AppNavRail using design-system components/tokens in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/components/AppNavRail.kt"
Task: "Implement HistoryFlow placeholder content using design-system components in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt"
Task: "Implement ProfileFlow placeholder content using design-system components in shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/ProfileFlow.kt"
```

---

## Implementation Strategy

### MVP First (US1 Only)

1. Complete Phase 1 setup.
2. Complete Phase 2 foundational navigation/design-system cleanup.
3. Write US1 tests T014-T017 and confirm they fail before implementation.
4. Implement US1 tasks T018-T029.
5. Run T030 and stop to validate navigation/resume independently.

### Incremental Delivery

1. Deliver shell models and Material retirement.
2. Deliver compact navigation and destination placeholders.
3. Add active-workout overlay and resume banner.
4. Add adaptive rail behavior.
5. Run platform and manual validation.

### Later Feature 003 Slices

After this MVP shell is stable, create separate task lists for:

- Active Workout loop: `RollerField`, `SetRow`, `ExerciseBlock`, one-tap log,
  rest bar, and recovery.
- Finish/Discard plus PR detection and celebration.
- History, Progress, Profile settings, and export.

## Notes

- Keep all visible shared UI on `FitTheme` tokens and `:design-system`
  components.
- Do not reintroduce Material 3 into `shared`.
- Preserve local-only behavior and avoid adding navigation dependencies unless
  a later plan justifies them.
- Record validation evidence before marking the final phase complete.
