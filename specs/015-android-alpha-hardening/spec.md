# Feature Specification: Android Alpha Hardening

**Feature Branch**: `codex/015-android-alpha-hardening`

**Created**: 2026-05-31

**Status**: Draft

**Input**: User description: "Prepare the app for Android alpha dogfooding: physical Pixel/manual smoke checklist, top/bottom spacing and keyboard issues that show up, force-close recovery checks, notification/rest timer permission behavior, empty states for Train/History/Progress/Profile, export/local data clarity, and a release checklist for good-enough-to-dogfood."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Dogfood Smoke Confidence (Priority: P1)

As the product owner testing on Android, I want a clear alpha smoke checklist and visible app readiness cues so I can quickly decide whether the current build is good enough for daily workout dogfooding.

**Why this priority**: Without repeatable validation, the app can compile while still failing the gym-side experience that matters most.

**Independent Test**: Install a debug build on a Pixel-class device or emulator, follow the alpha smoke checklist, and confirm each core flow has a pass/fail note and screenshot expectation.

**Acceptance Scenarios**:

1. **Given** a debug Android build, **When** the tester opens the alpha validation checklist, **Then** the checklist covers start/resume, exercise selection, bodyweight and weighted set logging, rest timer, finish/discard, history, progress, profile, export, and recovery.
2. **Given** the Profile screen, **When** the tester reviews local data and readiness information, **Then** the app clearly communicates local storage, cloud sync off, backup eligibility, notification status, and export availability without implying cloud functionality.

---

### User Story 2 - Recovery Trust (Priority: P2)

As a lifter interrupted mid-workout, I want active workout and rest state to recover after force close, backgrounding, and expired rest timers so I do not lose confidence in the app during a real workout.

**Why this priority**: Session recovery is constitutionally core and directly affects whether the app can be trusted in the gym.

**Independent Test**: Start a workout with a rest timer, simulate cold-start hydration after the rest ends, and verify the workout remains resumable, expired rest is cleared, notifications are canceled, and navigation still points to the right recovery affordance.

**Acceptance Scenarios**:

1. **Given** an active workout with an unexpired rest timer, **When** the app hydrates after process restart, **Then** the resume state includes the active workout and remaining rest duration.
2. **Given** an active workout whose rest timer ended while the app was stopped, **When** the app hydrates after the rest end time, **Then** the active workout remains resumable, the rest timer is cleared, and pending rest notification state is canceled.

---

### User Story 3 - Empty-State Polish (Priority: P3)

As a new user on a fresh install, I want each top-level screen to explain its empty state and next value plainly, without large awkward gaps or misleading labels.

**Why this priority**: Fresh-install polish affects first impression and reduces confusion during alpha testing.

**Independent Test**: Clear app data, launch the app, visit Train, History, Progress, and Profile, and confirm each screen has useful state, no overlapping controls, and thumb-reachable primary actions.

**Acceptance Scenarios**:

1. **Given** a fresh install with no completed workouts, **When** the user opens History, **Then** the screen explains that completed workouts appear after finishing a workout and does not present dead-end or misleading controls.
2. **Given** a fresh install with no PRs, **When** the user opens Progress, **Then** the screen explains that PRs appear after logged workouts and keeps progress sections compact and scannable.
3. **Given** a fresh install, **When** the user opens Train, **Then** primary start/create actions remain one-handed and the screen does not feel visually abandoned.

### Edge Cases

- The physical Pixel is not visible to ADB; emulator validation must still record the attempted device state and emulator fallback.
- Notification permission is denied or unavailable; rest timer recovery must remain correct even if the completion notification cannot display.
- Rest timer expires while the app is killed; hydration must clear rest state without discarding the active workout.
- App data is fresh or empty; top-level empty states must avoid implying cloud sync, historical data, or PRs already exist.
- Existing completed workouts, routines, PRs, exports, and user-created exercises must remain unchanged by this hardening slice.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide an Android alpha smoke checklist covering core dogfood flows, expected evidence, and pass/fail recording.
- **FR-002**: System MUST expose local-readiness status in Profile covering local storage, sync-off behavior, backup eligibility, rest notification expectations, and export availability.
- **FR-003**: System MUST preserve active workout recovery after cold-start hydration, including resume affordance and active workout identity.
- **FR-004**: System MUST clear expired rest timer state on hydration while preserving the active workout and canceling pending rest notification state.
- **FR-005**: System MUST keep rest timer behavior correct when notification display permission is denied or unavailable.
- **FR-006**: System MUST improve fresh-install empty states for Train, History, and Progress so each screen states what will appear there and what action creates it.
- **FR-007**: System MUST keep all hardening UI built from existing Fit design-system components and tokens.
- **FR-008**: System MUST keep Android as the manual validation target and preserve iOS readiness through shared-code compile validation.
- **FR-009**: System MUST record alpha validation outcomes, known limitations, and screenshots/paths in the feature validation artifact.

### Key Entities *(include if feature involves data)*

- **AlphaSmokeChecklist**: The repeatable manual validation checklist for Android dogfooding, including flow, expected result, evidence, and outcome fields.
- **LocalReadinessStatus**: Profile-facing status that summarizes local storage, sync, backup, rest notifications, export, and manual validation readiness.
- **RecoveryValidationEvidence**: Test and manual evidence proving active session, rest timer, and resume state survive cold-start scenarios.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A tester can complete the alpha smoke checklist in 15 minutes or less on a Pixel-class device or emulator.
- **SC-002**: Fresh-install Train, History, Progress, and Profile screens each show at least one meaningful status or guidance line with no overlapping primary controls on a Pixel 9 Pro viewport.
- **SC-003**: Automated validation proves expired rest hydration clears rest state and cancels notification state while keeping the active workout resumable.
- **SC-004**: Android debug build, shared unit tests, iOS simulator shared compile, Material guard, and whitespace validation all pass before the feature is complete.

## Assumptions

- Physical Pixel validation is preferred, but Pixel 9 Pro emulator validation is acceptable when ADB cannot see the physical device.
- This feature does not introduce runtime analytics, cloud sync, login, import/restore, or new release channels.
- Notification permission prompting UI is out of scope unless the current implementation cannot safely handle denied permission.
- Empty-state changes should be copy/layout polish, not new navigation architecture.
- Public app-store submission remains out of scope; this is an internal alpha dogfood gate.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Hardening preserves bottom-reachable workout actions and validates the gym-side logging loop.
- **Ledger Integrity**: No confirmed workout, set, PR, export, routine, or exercise data is mutated except through existing flows; validation checks protect against loss.
- **Recovery Behavior**: Cold-start, expired rest, and resume behavior are explicit acceptance criteria.
- **Progress Promise**: Empty progress states must accurately explain when PRs appear and not overstate progress before evidence exists.
- **Local-First Ownership**: Profile readiness status reinforces local-only, cloud-sync-off, export, and backup behavior.
- **Platform Scope**: Android is manually validated first; shared code and iOS simulator compile remain required.
- **Design System & Accessibility**: UI polish uses existing FitTheme/Fit components, large touch targets, readable text, and no parallel styling.
- **Release Evidence**: Automated gates, manual alpha checklist, and validation notes are required before completion.
