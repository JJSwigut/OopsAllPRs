# Contracts: App UX Navigation Shell

These contracts define observable behavior for the first feature 003
implementation slice.

## Shell Composition Contract

**Given** the app starts,
**when** `App()` composes,
**then** content is wrapped in `FitTheme` using a `FitPalettes` palette and all
visible shell UI reads design-system tokens/components.

**Required components**:
- Compact layout: `FitTabBar` with Train, History, Progress, Profile.
- Medium/expanded layout: app-level `AppNavRail` composed from `FitTheme`
  tokens and design-system primitives.
- Top-level content: Train, History, Progress, Profile surfaces.
- Conditional content: `ResumeBanner` above bottom navigation or beside rail.
- Active content: active-workout full-screen overlay.

**Forbidden in shared UI**:
- `androidx.compose.material3.*` imports.
- `org.jetbrains.compose.material3.*` dependencies.
- `MaterialTheme`, `Surface`, `Button`, or Material `Text` in shared screen
  code.
- Raw style literals when a `FitTheme` token exists.

## Route Persistence Contract

Persisted route values are stable strings:

```text
train
history
progress
profile
active-workout
```

**Given** `last_opened_route` is missing or unknown,
**when** the shell hydrates,
**then** it selects Train.

**Given** `last_opened_route` is a top-level route,
**when** the shell hydrates,
**then** it selects that route.

**Given** `last_opened_route = active-workout` and an active workout exists,
**when** the shell hydrates,
**then** the active-workout overlay is presented.

**Given** `last_opened_route = active-workout` and no active workout exists,
**when** the shell hydrates,
**then** it selects Train and does not present the overlay.

## Top-Level Navigation Contract

**Given** four top-level destinations,
**when** the user selects a tab or rail item,
**then** the shell updates `selectedDestination`, hides the active-workout
overlay, persists the selected route, and fires the design-system Selection
haptic where the selected component supports haptics.

**Given** an active workout exists,
**when** the user is on any top-level destination,
**then** `ResumeBanner` is visible and includes elapsed workout time plus a
Resume action.

**Given** no active workout exists,
**when** the user is on any top-level destination,
**then** `ResumeBanner` is not visible.

## Active Workout Overlay Contract

**Given** an active workout exists,
**when** the user taps Resume,
**then** the active-workout overlay is presented and `last_opened_route` is
persisted as `active-workout`.

**Given** the active-workout overlay is shown,
**when** the user dismisses it without finishing or discarding,
**then** the app returns to the previous top-level destination and the active
workout remains recoverable through `ResumeBanner`.

**Given** no active workout exists,
**when** `PresentActiveWorkout` is requested,
**then** the state holder ignores the request or surfaces a non-destructive
error; it must not create a fake active session.

## Adaptive Layout Contract

**Given** compact width,
**when** the shell renders,
**then** it uses bottom `FitTabBar` and places `ResumeBanner` above the bar.

**Given** medium or expanded width,
**when** the shell renders,
**then** it uses a navigation rail and keeps the active workout available from
the resume affordance.

**Given** expanded width,
**when** later active-workout content supports two panes,
**then** `NavigationLayoutClass.Expanded` is the trigger for two-pane
composition. This first slice only exposes the classifier.

## Accessibility Contract

Every interactive shell element must:
- Meet or exceed `FitTheme.size.touchMin`.
- Expose a stable content description or selectable role.
- Preserve visible feedback when haptics are disabled.
- Collapse non-essential motion when reduce motion is enabled.
- Keep text styled through `FitTheme.type` and color through `FitTheme.colors`.

## Validation Contract

Automated checks must cover:
- Route string parsing and fallback behavior.
- Hydration from `ActiveSessionState.lastOpenedRoute`.
- Resume-banner visibility.
- Overlay present/dismiss transitions.
- Compact vs medium/expanded layout classification.
- Shared source/dependency scan proving Material 3 removal.
- Android debug assembly and iOS simulator-arm64 shared compile.

Manual milestone checks must cover:
- Launch on Android and switch all four destinations.
- Start or simulate an active workout, leave the overlay, confirm resume banner,
  tap Resume, and confirm state returns.
- Kill/restart the app during an active workout and confirm route/resume
  recovery.
- Verify compact phone and larger screen layout behavior.
