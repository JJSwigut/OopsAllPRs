# Feature Specification: Profile Settings and Local Export

**Feature Branch**: `codex/010-profile-export-settings`

**Created**: 2026-05-30

**Status**: Draft

**Input**: User description: "Implement Profile settings and local data export for Oops All PRs. Use the existing local persistence foundation and design-system components to let users choose canonical display units, review local-first/backup status, export workouts, routines, exercises, and PR history, and keep settings durable after restart. Keep Android validated first and iOS first-class through shared state and platform export handoff boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Choose Durable Display Units (Priority: P1)

As a lifter, I want to choose whether weights are shown in pounds or kilograms from Profile so all progress and export views use the unit I expect after restart.

**Why this priority**: Unit trust affects logging confidence, PR interpretation, and exported data ownership.

**Independent Test**: Open Profile, switch units, restart the app against the same local data, and verify Profile and Progress show the selected unit while stored workout data remains unchanged.

**Acceptance Scenarios**:

1. **Given** the user has the default unit, **When** they choose kilograms in Profile, **Then** the selected unit is shown immediately and remains selected after app restart.
2. **Given** existing weighted workouts and PRs, **When** the user switches display units, **Then** values are displayed/exported in the selected unit without changing the underlying workout ledger.
3. **Given** bodyweight reps-only history, **When** the display unit changes, **Then** bodyweight PRs and bodyweight exports remain reps-based and do not invent a weight.

---

### User Story 2 - Export Local Training Data (Priority: P2)

As a local-first user, I want to export workouts, routines, exercises, and PR history from Profile so I can inspect, back up, or move my own data without cloud sync.

**Why this priority**: Export is the explicit user-owned data path while the app remains local-only.

**Independent Test**: Seed exercises, complete at least one workout with PR history, create a routine, request each export type, and verify each generated file has the expected name, rows, unit context, and platform handoff.

**Acceptance Scenarios**:

1. **Given** completed workouts exist, **When** the user exports workouts, **Then** a workout export file is generated from local ledger rows and handed to the platform sharing/export path.
2. **Given** routines and exercises exist, **When** the user exports routines or exercises, **Then** each export reflects current local data and avoids duplicate seed rows.
3. **Given** PR history exists, **When** the user exports PRs, **Then** the export includes source workout and source set evidence ids.
4. **Given** an export succeeds, **When** Profile returns to idle, **Then** it shows the most recent export name, row count, and unit used.

---

### User Story 3 - Review Local-First Status and Interaction Preferences (Priority: P3)

As a user preparing to trust the app with training history, I want Profile to clearly show that data is local-first, backup-eligible, and controlled by accessible interaction preferences.

**Why this priority**: Profile is the right place to make privacy/ownership and accessibility behavior visible before broader account or sync features exist.

**Independent Test**: Open Profile offline, toggle haptics/reduced motion/palette mode, leave and return to Profile, and verify the local-first status remains visible and interaction settings drive the app shell without blocking logging flows.

**Acceptance Scenarios**:

1. **Given** the device is offline, **When** Profile opens, **Then** local-first and backup eligibility status is visible without any network dependency.
2. **Given** haptics are disabled or reduced motion is enabled, **When** the user returns to workout flows, **Then** the app shell uses those interaction settings.
3. **Given** Profile is opened on a small phone viewport, **When** the user changes units or triggers an export, **Then** primary controls remain reachable, readable, and built from the shared design system.

### Edge Cases

- An export is requested when there are no rows for that export type; the file should still include headers and report zero rows.
- An export fails because local data cannot be read or the platform handoff is unavailable; Profile must show a recoverable error and preserve the user's data.
- The app is restarted after unit selection but before any export; the selected unit must reload before Profile displays.
- The app is restarted after export; prior export snapshots may remain in local storage, but Profile only needs to show current-session export status.
- Bodyweight reps-only sets and PRs must remain exportable without a weight value.
- Fractional weights must be exported in the selected unit while persisted source values remain canonical.
- Profile must remain usable offline and must not introduce account, sync, or network requirements.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to select pounds or kilograms as their display/export weight unit from Profile.
- **FR-002**: The selected weight unit MUST persist locally and reload after app restart.
- **FR-003**: Display-unit changes MUST NOT mutate stored workout, set, template, or PR source values.
- **FR-004**: Users MUST be able to export workouts, routines, exercise catalog data, and personal records from Profile.
- **FR-005**: Each export MUST include a deterministic file name, content, row count, export type, created timestamp, and selected weight unit where applicable.
- **FR-006**: Workout exports MUST include completed workout ledger rows and preserve bodyweight reps-only rows without fabricated weight values.
- **FR-007**: Personal record exports MUST include source workout and source set evidence ids.
- **FR-008**: Export requests MUST use only local data and MUST work without network access.
- **FR-009**: Successful export requests MUST surface the generated file through a platform export/share handoff when a handoff is available.
- **FR-010**: Profile MUST show the latest export result or a recoverable export error without blocking navigation.
- **FR-011**: Profile MUST show local-first and backup eligibility status in user-facing terms.
- **FR-012**: Users MUST be able to adjust haptics, reduced motion, and palette mode from Profile.
- **FR-013**: Interaction preferences changed in Profile MUST apply to the app shell immediately.
- **FR-014**: Profile UI controls MUST use the shared design system and maintain accessible touch targets and readable labels on phone-sized layouts.
- **FR-015**: Android validation MUST be completed first; shared behavior MUST remain compatible with iOS through platform handoff boundaries.

### Key Entities *(include if feature involves data)*

- **Profile Settings**: User-visible preferences for weight unit, palette, haptics, and reduced motion.
- **Export Request**: A user action to generate one category of local data for external handoff.
- **Export Result**: Generated file metadata, row count, selected unit, and success/error state shown in Profile.
- **Local Data Ownership Status**: Readable status explaining offline/local behavior, backup eligibility, and future sync boundary.
- **Platform Export Handoff**: Platform-owned operation that receives export file metadata/content from shared state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A unit preference selected in Profile is restored after app state recreation in automated tests.
- **SC-002**: Export actions for workouts, routines, exercises, and personal records each produce file metadata and content from local repositories in automated tests.
- **SC-003**: Bodyweight reps-only data exports with blank weight values, while weighted data exports in the selected display unit.
- **SC-004**: Profile applies haptics, reduced motion, and palette changes to the app shell immediately in runtime state.
- **SC-005**: Android unit tests, Android debug build, iOS simulator compile, Material scan, and whitespace validation pass for the slice.
- **SC-006**: Android device/emulator discovery is attempted; if no target is available, the manual Profile/export gate is explicitly deferred.

## Assumptions

- The app remains local-only and account-free for this feature.
- Existing local persistence already stores the weight unit and export snapshots; this feature exposes those behaviors through Profile.
- Platform export means handing generated content to the operating system share/export surface, not implementing cloud sync or import/restore.
- Android Auto Backup is enabled by the app manifest and documented as backup-eligible status; per-user backup policy controls can be planned separately if needed.
- Palette, haptics, and reduced motion settings are runtime app-shell preferences for this slice; durable persistence beyond weight unit can be added in a later settings persistence feature.

## Constitution Alignment *(mandatory)*

- **Fast-Loop Impact**: Profile changes do not add friction to active set logging and keep settings outside the main workout confirmation path.
- **Ledger Integrity**: Exports read durable completed workout, exercise, routine, and PR evidence data without mutating ledger rows.
- **Recovery Behavior**: Unit preference must recover after restart; active session/timer behavior is not changed.
- **Progress Promise**: PR exports include source evidence ids, selected-unit formatting, and bodyweight reps-only behavior.
- **Local-First Ownership**: Feature is offline-only, improves user-controlled export, and keeps future sync out of scope.
- **Platform Scope**: Android is validated first; shared state owns export/settings behavior and platform code owns export handoff.
- **Design System & Accessibility**: Profile uses Neo-Glass design-system components and keeps controls touch-friendly, readable, and compatible with reduced motion and haptic alternatives.
- **Release Evidence**: Requires profile state tests, export behavior tests, Android build/tests, iOS compile, design-system/Material scan, whitespace check, and manual device/emulator discovery.
