# Feature Specification: Release Performance Hardening

**Feature Branch**: `022-release-performance-hardening`

**Created**: 2026-06-02

**Status**: Draft

**Input**: User description: "Use Spec Kit to start release performance hardening for both platforms, including Android R8/minification/resource shrinking and iOS release archive/framework validation, while keeping debug behavior separate."

## Clarifications

### Session 2026-06-02

- Q: Should release optimization prioritize maximum shrinking or release safety? -> A: Prioritize release safety; enable standard optimizations with explicit validation and keep rules before pursuing aggressive shrinking.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Android Release Is Optimized Safely (Priority: P1)

A developer preparing an Android release can build an optimized release artifact that uses shrinking/minification safely without breaking startup, Profile, Compose resources, SQLDelight/database access, exports, rest notifications, or workout logging.

**Why this priority**: Android release artifacts need the largest immediate performance and size hardening gap closed, and R8/resource shrinking can break runtime behavior if not validated.

**Independent Test**: Build the Android release artifact, inspect that minification and resource shrinking are active, install/run it, and complete a release smoke flow.

**Acceptance Scenarios**:

1. **Given** Android release hardening is enabled, **When** the release artifact is built, **Then** R8/minification and resource shrinking are active for the release variant.
2. **Given** the optimized Android release app is installed, **When** the app starts and Profile opens, **Then** the app loads normally and no Developer section is visible.
3. **Given** the optimized Android release app is installed, **When** a workout smoke flow is performed, **Then** the user can start/log/finish or discard without runtime failures.

---

### User Story 2 - iOS Release Is Archive-Ready (Priority: P2)

A developer preparing an iOS release can build or archive the iOS release app with release framework linkage, correct bundle metadata, symbol/debug evidence, and no developer tools.

**Why this priority**: iOS must remain a first-class release target; release framework/archive validation prevents the iOS host from staying debug-only.

**Independent Test**: Build the iOS Release simulator artifact and, when signing resources are available, produce an archive or archive-equivalent validation with release metadata and no developer tools.

**Acceptance Scenarios**:

1. **Given** iOS release hardening is configured, **When** the iOS Release simulator build runs, **Then** it links the release shared framework and produces release bundle metadata.
2. **Given** the iOS release app is launched, **When** Profile opens, **Then** no Developer section is visible.
3. **Given** an archive or archive-equivalent validation is run, **When** release evidence is inspected, **Then** symbols/dSYM and bundle metadata are available for release review.

---

### User Story 3 - Release Evidence Is Repeatable (Priority: P3)

A developer can run documented release-hardening checks and collect evidence for Android and iOS without relying on memory or one-off commands.

**Why this priority**: Release hardening is only useful if the team can repeat it before public release and compare artifact behavior over time.

**Independent Test**: Follow the quickstart commands and produce a validation summary that reports build success, artifact identity, optimization state, and smoke-test status.

**Acceptance Scenarios**:

1. **Given** the release-hardening quickstart, **When** a developer follows the Android section, **Then** they can build, inspect, and smoke-test the optimized release artifact.
2. **Given** the release-hardening quickstart, **When** a developer follows the iOS section, **Then** they can build or archive and inspect release metadata.
3. **Given** validation cannot complete because of environment constraints, **When** the run ends, **Then** the blocker is recorded with a concrete prerequisite such as freeing disk space or installing signing assets.

---

### Edge Cases

- R8/minification must not remove Kotlin serialization/resource accessors, Compose resources, SQLDelight database classes, notification receiver entry points, or file export paths required at runtime.
- Resource shrinking must not remove packaged exercise seed CSV files or Compose resources needed by shared UI.
- Release builds must keep developer tools disabled even after optimization.
- Debug builds must remain unminified and developer-tool enabled for local testing.
- iOS Release validation must account for simulator and device/archive differences.
- If the machine lacks disk space or signing assets, validation must stop with a documented blocker rather than silently passing.
- Existing workout history, routines, PRs, exports, user-created exercises, and settings must not be mutated by release hardening.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Android release builds MUST enable code shrinking/minification through the platform release optimizer.
- **FR-002**: Android release builds MUST enable resource shrinking when compatible with the current app resources.
- **FR-003**: Android release builds MUST include explicit keep rules for app entry points, platform receivers, shared resources, and KMP/Compose/SQLDelight runtime needs that cannot be safely inferred.
- **FR-004**: Android debug builds MUST remain unminified and distinguishable from release builds.
- **FR-005**: iOS Release builds MUST link release shared framework outputs for simulator/device targets.
- **FR-006**: iOS Release validation MUST verify release bundle metadata, display name, bundle identifier, and developer-tool disabled state.
- **FR-007**: Release validation MUST include startup, Profile, export availability, and core workout smoke checks on Android and iOS where the environment supports launch.
- **FR-008**: Release evidence MUST include artifact paths, artifact identity, optimization state, and any environment blockers.
- **FR-009**: Release hardening MUST NOT change user data models, persistence schema, export formats, or workout/progress behavior.
- **FR-010**: Implementation MUST document cleanup/prerequisite steps for low-disk validation environments before running large iOS/Android release builds.

### Key Entities *(include if feature involves data)*

- **Release Optimization Profile**: Build-time release settings and keep rules that control shrinking/minification/resource retention.
- **Release Artifact Evidence**: Paths, metadata, optimization state, symbol/debug evidence, and smoke-test results collected during validation.
- **Validation Environment**: Local machine/device/simulator state needed for release checks, including disk space and signing availability.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Android release build reports minification enabled and resource shrinking enabled, and produces an APK/AAB artifact.
- **SC-002**: Android optimized release smoke test starts successfully and opens Profile with zero developer-tool UI.
- **SC-003**: iOS Release simulator build succeeds with release bundle metadata and release shared framework linkage.
- **SC-004**: Release validation docs identify every command needed to reproduce Android and iOS checks.
- **SC-005**: Any incomplete release validation has a documented blocker and a specific next action.

## Assumptions

- Android release optimization will start with standard R8/minification and resource shrinking, not aggressive custom optimization beyond validated defaults.
- iOS public distribution signing may not be available locally; simulator Release and archive-equivalent validation are acceptable starting points until signing assets are configured.
- Current disk pressure must be resolved before running large release builds again.
- This feature starts release performance hardening and evidence collection; marketplace submission automation is out of scope.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Release optimization must improve or preserve startup and gym-side responsiveness without adding taps or modal friction.
- **Ledger Integrity**: Shrinking/minification must not break persistence, set logging, finish/discard behavior, exports, or PR derivation.
- **Recovery Behavior**: Release artifacts must preserve active workout/session recovery and rest notification behavior.
- **Progress Promise**: PR, history, last-set, bodyweight, unit, and progression behavior must remain unchanged.
- **Local-First Ownership**: Release hardening keeps data local and does not introduce network, sync, backup, or export-format changes.
- **Platform Scope**: Android and iOS both require release validation; shared KMP code remains shared and platform-specific release settings stay in platform build configuration.
- **Design System & Accessibility**: No new UI styling is introduced; release smoke checks include Profile visibility and core navigation readability.
- **Release Evidence**: This feature directly expands release gates with optimized artifacts, symbol/debug evidence, artifact metadata, and manual smoke validation.
