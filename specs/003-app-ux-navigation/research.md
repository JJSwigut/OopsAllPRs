# Research: App UX Navigation Shell

## Decision: Use a small shared navigation state model instead of a navigation library

**Rationale**: The first slice needs four stable top-level destinations,
active-workout overlay presentation, resume-banner visibility, and route
recovery. A simple shared state holder with explicit intents is easier to test
in common code and keeps Android/iOS behavior aligned. The active workout is a
mode layered over the tabs, not a deep-link stack, so a full navigation library
would add unnecessary behavior surface now.

**Alternatives considered**:
- AndroidX Navigation Compose: Android-centric and not appropriate for shared
  KMP UI.
- Decompose/Voyager: viable later if stack complexity grows, but unnecessary
  for this shell and would expand the dependency surface before the app has
  real nested flows.
- Ad hoc mutable route strings in composables: rejected because route parsing,
  recovery, and tests would be weak.

## Decision: Represent adaptive navigation with a shared layout classifier

**Rationale**: The design-system module does not include a nav rail, and the
constitution forbids pulling in Material 3 for layout semantics. A shared
`NavigationLayoutClass` derived from available width lets compact screens use
`FitTabBar` and medium/expanded screens use an app-level `AppNavRail` composed
from design-system tokens and primitives.

**Alternatives considered**:
- AndroidX WindowSizeClass: Android-oriented and commonly paired with Material
  APIs; it would not help iOS shared UI.
- Always bottom tabs: simpler, but fails the large-screen acceptance scenario.

## Decision: Retire the Material 3 bridge in shared during this slice

**Rationale**: Feature 002 produced the official `:design-system` module, and
constitution v3.0.0 requires user-facing UI to use `FitTheme` tokens and
design-system components. The bootstrap `MaterialTheme`, `Surface`, `Button`,
and `Text` stubs should be replaced with `FitTheme`, `FitButton`, `FitCard`,
`FitStartButton`, `FitTabBar`, and `BasicText` styled from `FitTheme.type` and
`FitTheme.colors`.

**Alternatives considered**:
- Keep Material 3 as a temporary compatibility bridge: rejected because it
  creates a second styling system and masks missing design-system usage.
- Remove Material 3 only after all screens are complete: rejected because this
  shell is the right boundary to establish the app-level theme contract.

## Decision: Recover route and overlay state from `active_session_state.last_opened_route`

**Rationale**: The foundation schema already includes `last_opened_route`, and
`ActiveSessionState` already exposes it. Reusing that state avoids a new schema
migration and keeps active workout recovery, elapsed time, rest anchors, and
navigation recovery in one session snapshot.

**Alternatives considered**:
- Add a new `app_navigation_state` table: unnecessary for this slice because a
  singleton session row already exists.
- Do not persist route state: rejected by the constitution's session recovery
  rule.

## Decision: Keep active workout as an overlay above top-level destinations

**Rationale**: The spec defines active workout as a full-screen flow layered
above tabs, with a resume banner visible on top-level screens. This preserves a
single mental model: top-level tabs remain stable, and active logging can be
entered or left without losing session state.

**Alternatives considered**:
- Make Active Workout a fifth tab: rejected because active workout is
  conditional and should not compete with core destinations when no workout is
  active.
- Navigate away from the tab host into a separate stack: viable later, but the
  resume banner contract is simpler with explicit overlay state.

## Decision: Validate this slice with shared state tests plus Android/iOS compile gates

**Rationale**: The highest-risk behavior in this slice is state transition and
recovery, not pixel-perfect UI. Common tests can verify route parsing, layout
classification, overlay visibility, and persisted route updates. Android debug
assembly and iOS compile verify that shared UI remains multiplatform. Manual
device checks cover navigation ergonomics, haptics, and resume behavior.

**Alternatives considered**:
- Add Compose instrumented UI tests immediately: valuable, but the design-system
  plan explicitly deferred `runComposeUiTest` setup. This slice should not block
  on that infrastructure.
- Rely only on compile checks: rejected because route/recovery transitions are
  behavior, not type checking.

## Decision: Defer progress charts and Material-flavored chart dependencies

**Rationale**: The first Progress screen can render PR summaries from existing
state with design-system components. The current `vico-multiplatform-m3`
dependency is not needed for this slice and conflicts with the Material-free UI
direction unless wrapped or replaced later.

**Alternatives considered**:
- Keep Vico M3 in shared for future charts: rejected for this slice because it
  is unused and keeps a Material-flavored dependency in shared.
- Build charts from scratch now: rejected because charts are outside the shell
  slice and should be planned with the full Progress feature.
