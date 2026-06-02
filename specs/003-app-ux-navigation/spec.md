# Feature Specification: App UX, Layout & Navigation

**Feature Branch**: `003-app-ux-navigation`

**Created**: 2026-05-29

**Status**: Draft

**Input**: User description: "Define the app-level UX, screen layouts, and navigation for Oops All PRs, and how the Neo-Glass design-system components are composed across every screen — intuitive, fast, accessible, with surprise-and-delight motion/haptics."

> Migrated from the superpowers UX/layout design spec (now removed). Forward-looking: this
> feature defines the UX contract; it decomposes into per-story implementation features (see
> "Build order / decomposition"). Depends on feature 002 (design system) and the domain/data
> foundation (feature 001 / future data feature).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Navigate the App and Resume Mid-Workout (Priority: P1)

A lifter moves between Train, History, Progress, and Profile via a bottom `FitTabBar`. While a
workout is active, a persistent "active workout" bar sits above the tab bar on every screen, so
they can leave and return without losing the session. Active workout is a full-screen flow layered
above the tabs.

**Why this priority**: Navigation and resume are the frame everything else hangs on, and resume
is a core constitution promise (III).

**Independent Test**: With an active workout, leave the active flow to another tab, confirm the
resume bar is present, tap it, and confirm the full logging flow returns with state intact.

**Acceptance Scenarios**:

1. **Given** four top-level destinations, **When** the user taps a tab, **Then** the glass tab bar slides its active indicator and a `Selection` haptic fires.
2. **Given** an active workout, **When** the user is on any top-level screen, **Then** a persistent resume bar (elapsed time + Resume) is shown above the tab bar.
3. **Given** a large screen (tablet/foldable), **When** the layout adapts, **Then** the tab bar becomes a nav rail and the active workout uses a two-pane layout.

### User Story 2 - Log Sets Fast With One Tap (Priority: P1)

A lifter logs a set with a single tap on the glowing Log button (pre-filled from target/last set).
When a value needs changing, the inline **RollerField** handles it: tap above/below center = ±1
step; drag = fast roll with detents and snap. No modal interrupts the loop.

**Why this priority**: Fast-loop logging is the product's reason to exist (Principle I).

**Independent Test**: On the active screen, tap Log and confirm a set is logged with no dialog;
tap a value to ±1 step; drag to jump; long-press for direct numeric entry.

**Acceptance Scenarios**:

1. **Given** an active set pre-filled from history, **When** Log is tapped, **Then** the set logs immediately with a Heavy press + Success-on-persist haptic and an `AnimatedCount` roll.
2. **Given** a `RollerField`, **When** the user taps its top/bottom half, **Then** the value changes ±1 step with a `Selection` haptic; **When** dragged, **Then** it rolls with detents and snaps.
3. **Given** a logged set, **When** persistence has not confirmed, **Then** the row does not show "logged"; on failure it shows a non-destructive inline error (ledger integrity, Principle II).

### User Story 3 - Manage Rest Without Leaving the Loop (Priority: P2)

After logging, a non-blocking RestBar docks at the bottom with remaining time and ±30s/skip.
Tapping it expands a `FitRestTimer` ring. Rest is wall-clock anchored and survives restart; a
notification is scheduled on start and canceled on skip/finish/discard.

**Independent Test**: Log a set, confirm rest auto-starts in the bar, restart the app mid-rest,
and confirm remaining time is correct and a notification is scheduled.

**Acceptance Scenarios**:

1. **Given** a logged set, **When** rest begins, **Then** the RestBar appears without blocking the next set and a notification is scheduled.
2. **Given** rest is running, **When** it reaches the final 3 seconds and then zero, **Then** a `Tick` ramp then `Success` haptic fire and the ring completes.

### User Story 4 - Celebrate a PR (Priority: P2)

When a logged set is a personal record, the app delivers a layered celebration: a `FitProgressRing`
glow burst, an `AnimatedCount` roll, a PRBadge with a `bouncy` spring, and a `Success`→`Heavy`
haptic — only on genuine PRs, and explained/inspectable.

**Independent Test**: Log a set that beats history and confirm the celebration fires and the PR is
explained ("+5 over <date>"); log a non-PR set and confirm no celebration.

**Acceptance Scenarios**:

1. **Given** a logged set beating the record, **When** persisted, **Then** the celebration fires and a PRBadge announces "New PR: <lift>, <value>, up <delta> from <date>".
2. **Given** reduce-motion or haptics-off, **When** a PR fires, **Then** it degrades to a quick fade + badge / visual-only with no lost information.

### User Story 5 - Review History, Progress, and Manage Data (Priority: P3)

A lifter reviews past workouts (History), PRs/progression (Progress), and manages units, theme,
haptics, reduce-motion, and export (Profile) — all offline.

**Independent Test**: Open each top-level screen and confirm it renders from design-system
components with the expected content and that export produces a user-owned file.

**Acceptance Scenarios**:

1. **Given** workout history, **When** History is opened, **Then** workouts list reverse-chronologically with stat tiles and per-set `loggedAt` detail.
2. **Given** Profile, **When** units/theme/haptics/reduce-motion are changed, **Then** displays reformat and the theme animates without mutating canonical stored data.

### Edge Cases

- Process death at any point restores active workout, logged/unlogged set states, focused set, scroll, elapsed clock, and running rest.
- Failed persistence on log: row stays unlogged, surfaces a `danger`-token inline error.
- Finish keeps logged sets and discards unlogged planned sets without mutating logged tuples; Discard requires explicit confirmation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Provide a single shared navigation host owning four top-level destinations (Train, History, Progress, Profile) via `FitTabBar`, with the active workout as a full-screen overlay and a persistent resume bar.
- **FR-002**: Adapt navigation by `WindowSizeClass`: bottom bar (compact) → nav rail + two-pane active workout (medium/expanded).
- **FR-003**: Active-workout `SetRow` MUST default to one-tap logging (pre-filled from target/last) and use the inline `RollerField` (tap = ±1 step, drag = roll+snap) for edits; long-press opens `FitTextField` numeric entry. No modal in the logging path.
- **FR-004**: A set MUST show "logged" only after persistence confirms; failures surface a non-destructive inline error.
- **FR-005**: Rest MUST be wall-clock anchored, non-blocking (RestBar), notification-scheduled on start and canceled on skip/finish/discard, and recover after restart.
- **FR-006**: PR detection (weighted, bodyweight, fractional, unit-converted) drives a celebration moment with reduce-motion/no-haptic fallbacks; PRs are explained and inspectable.
- **FR-007**: Finish keeps logged sets and drops unlogged planned sets without mutating logged tuples; Discard requires confirmation and never deletes prior history.
- **FR-008**: Profile exposes units (`FitSegmentedControl`), theme/palette + light/dark (animated swap), haptics (`LocalHapticsEnabled`), reduce-motion (`LocalReduceMotion`), and user-controlled export.
- **FR-009**: All screens MUST be built only from design-system components/tokens (no parallel styling), meet ≥`touchMin` targets, set semantics, and honor reduce-motion + haptic alternatives.
- **FR-010**: Motion/haptics follow a budget (press→Light, log→Success, roller detent→Selection, rest final-3s→Tick ramp, PR→Success+Heavy, finish→Success, discard→Warning, tab/segment→Selection), all reduce-motion/haptics-aware.

### Key Entities *(include if feature involves data)*

- **Screens**: Train, Active Workout, Rest, Exercise Picker, Finish/Discard, History, Progress, Profile.
- **Composite components to build (over design-system primitives, no new styling)**: `RollerField` (unified tap-step / drag-roll over `FitRoller`), `SetRow` (embeds two `RollerField`s), `ExerciseBlock`, `RestBar`, `ResumeBanner`, `RoutineCard`, `WorkoutHistoryCard`, `PRBadge`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Logging a repeat set takes a single tap with no modal.
- **SC-002**: Active workout + rest fully recover after process death (wall-clock timing).
- **SC-003**: A genuine PR triggers the celebration; a non-PR does not.
- **SC-004**: Every screen renders solely from design-system components/tokens (no ad hoc styling).
- **SC-005**: Core flows pass accessibility checks (targets, semantics, reduce-motion, haptic alternatives) on Android and iOS.

## Assumptions

- Depends on feature 002 (design system) and a domain/data foundation (entities, SQLDelight, repositories, PR/last-set rules) — the screens bind to that state.
- `RollerField` and other composites are app-level compositions, not new design-system primitives.
- The roller is the *editor*, never the *gate*: one-tap logging never requires a drag.

## Constitution Alignment *(mandatory)*

- **I (Fast-loop logging first)**: one-tap default, stepper-via-RollerField, no modal interruption.
- **II (Ledger)**: logged-only-after-persist, explicit edit/finish/discard semantics.
- **III (Session recovery)**: wall-clock timers, resume bar, full state restore.
- **IV (Progress is the promise)**: PR celebration + progression screens.
- **VI (Shared-first)**: shared Compose UI; notifications/export/dynamic-type/reduce-motion sources are platform adapters.
- **VII (Single token-driven system, accessible)**: built entirely from the Neo-Glass design system; full a11y.

## Build order / decomposition

This feature is the UX contract; implement as sequential sub-features (each its own plan/tasks):

1. Domain/data foundation (entities, SQLDelight, repos, PR rules) — prerequisite.
2. Navigation shell + theme wiring (`FitTheme` inputs, `FitTabBar`/rail, overlay, ResumeBanner, adaptive). **Also retires the bootstrap's placeholder Material 3 scaffolding:** add the `shared → :design-system` dependency, replace `DesignSystemBridge` (`MaterialTheme`/`Surface`/`Button`) and the `material3.Text` flow stubs with design-system equivalents (`FitTheme`, `BasicText`/DS components), then drop the `compose.material3` dependency from `shared/build.gradle.kts` (last Material 3 usage in the app).
3. Active Workout loop (`RollerField`, `SetRow`, `ExerciseBlock`, one-tap log→ledger, RestBar + notifications, recovery) — highest risk.
4. Finish/Discard + PR detection + celebration.
5. History, Progress, Profile/Export.
