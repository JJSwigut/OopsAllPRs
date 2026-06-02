# Feature Specification: Release and Debug Apps

**Feature Branch**: `021-release-debug-apps`

**Created**: 2026-06-02

**Status**: Draft

**Input**: User description: "I want to make sure we have release versions and debug versions of both apps. So developer options and things like that should not be in the profile."

## Clarifications

### Session 2026-06-02

- Q: Should debug artifacts be separately identifiable from release artifacts? → A: Yes; use distinct debug install identity and display naming where platform build systems support it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Release Profile Has No Developer Options (Priority: P1)

A person using a public release build can open Profile without seeing developer-only tools, seed/demo controls, debug labels, or any entry point that implies internal build functionality.

**Why this priority**: Public builds must not expose internal controls that can confuse users, mutate demo data, or weaken release polish.

**Independent Test**: Build and run a release app variant, open Profile, and verify no developer options or debug-only controls are present.

**Acceptance Scenarios**:

1. **Given** a release Android build is installed, **When** the user opens Profile, **Then** no developer options section, developer controls, debug labels, or demo tooling entry point appears.
2. **Given** a release iOS build is installed, **When** the user opens Profile, **Then** no developer options section, developer controls, debug labels, or demo tooling entry point appears.

---

### User Story 2 - Debug Builds Keep Developer Tools Available (Priority: P2)

A developer running a debug build can still access developer tools needed for local testing, demo data, and diagnostics.

**Why this priority**: Removing developer tools globally would slow testing and make manual validation harder; the separation must be build-aware.

**Independent Test**: Build and run a debug app variant, open Profile, and verify the developer tools entry point remains available.

**Acceptance Scenarios**:

1. **Given** a debug Android build is installed, **When** the developer opens Profile, **Then** developer options are available.
2. **Given** a debug iOS build is installed, **When** the developer opens Profile, **Then** developer options are available.

---

### User Story 3 - Debug and Release Apps Are Distinguishable (Priority: P3)

A developer can distinguish debug and release builds during validation so they do not mistake a debug app for the public release artifact.

**Why this priority**: Release validation needs confidence that the checked artifact is the right build type on each platform.

**Independent Test**: Produce Android and iOS debug/release artifacts and verify debug builds are identifiable without exposing debug UI inside release builds.

**Acceptance Scenarios**:

1. **Given** Android debug and release apps are built, **When** their install identity or display naming is inspected, **Then** the debug app is distinguishable from the release app.
2. **Given** iOS debug and release apps are built, **When** their bundle identity or display naming is inspected, **Then** the debug app is distinguishable from the release app.

---

### Edge Cases

- Release gating must remain correct after app restart, process death, and ordinary navigation back to Profile.
- Debug-only visibility must not depend on local persisted profile data that can leak into release.
- Existing workout history, templates, PRs, exports, and user-created exercises must remain unchanged by build-type gating.
- If developer tooling is added later, the release build must remain protected by the same build-type source of truth.
- Android and iOS may use different platform build mechanisms, but the user-facing behavior must be equivalent.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST define a release build path and a debug build path for Android.
- **FR-002**: The system MUST define a release build path and a debug build path for iOS.
- **FR-003**: Release builds MUST disable developer tools at the shared app entry point so developer-only UI is not reachable through Profile.
- **FR-004**: Debug builds MUST enable developer tools at the shared app entry point so local testing tools remain available.
- **FR-005**: The Profile screen MUST omit developer-only sections and controls whenever developer tools are disabled.
- **FR-006**: Build-type gating MUST be derived from platform build configuration, not from user-editable or persisted app state.
- **FR-007**: Debug artifacts MUST be distinguishable from release artifacts by install identity and display naming where platform build systems support it.
- **FR-008**: Release build validation MUST include explicit checks that Profile has no developer options on both Android and iOS.
- **FR-009**: Existing user data, exports, workout history, routines, PRs, and exercise catalog content MUST NOT be modified by switching developer-tool availability.

### Key Entities *(include if feature involves data)*

- **Build Channel**: The build-time classification of an app artifact as debug or release. It controls whether developer tools are enabled.
- **Developer Tools Availability**: A shared app capability flag consumed by Profile and any developer-only UI entry points.
- **App Artifact Identity**: Platform-specific package or bundle identity and display metadata used to distinguish debug artifacts from release artifacts during validation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of release Profile checks on Android and iOS show zero developer options or developer-only controls.
- **SC-002**: 100% of debug Profile checks on Android and iOS show the developer tools entry point.
- **SC-003**: Android and iOS release artifacts can be built from documented commands without requiring source edits.
- **SC-004**: Android and iOS debug artifacts can be built from documented commands without requiring source edits.
- **SC-005**: Automated or scripted validation can identify the effective developer-tool availability for debug and release builds on both platforms.

## Assumptions

- Debug builds may expose developer options in Profile; release builds must not.
- Debug and release artifacts should be distinguishable to developers, ideally co-installable where the platform supports it, but release artifact naming should remain user-facing and polished.
- Existing developer tools are intended for local testing only and are not public product features.
- This feature does not change the user account model, workout data model, or export format.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: No active workout logging controls change; release Profile cleanup reduces non-workout clutter without adding gym-side taps.
- **Ledger Integrity**: Developer-tool gating must not mutate workout/session/set/PR/export data and must not alter persistence semantics.
- **Recovery Behavior**: Build-type gating is static for the installed artifact and must remain stable after restart, process death, and navigation recovery.
- **Progress Promise**: PR, history, last-set, bodyweight, unit, and progression behavior are unchanged.
- **Local-First Ownership**: The feature works offline and does not introduce network, sync, backup, or export changes.
- **Platform Scope**: Android and iOS both require debug and release build paths; shared UI consumes a platform-provided build capability.
- **Design System & Accessibility**: No new styling system is introduced; Profile remains built from Fit components and release removal avoids inaccessible hidden developer controls.
- **Release Evidence**: Validation must include debug and release build checks for Android and iOS, plus Profile inspection proving release artifacts have no developer options.
