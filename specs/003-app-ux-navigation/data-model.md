# Data Model: App UX Navigation Shell

This slice adds shared UI state models and uses existing session persistence.
It does not add workout-ledger tables or mutate confirmed workout data.

## TopLevelDestination

Represents the four stable top-level app destinations.

**Fields**:
- `route: String` - Stable persisted route value. Allowed values: `train`,
  `history`, `progress`, `profile`.
- `label: String` - Display label for the tab/rail item.
- `order: Int` - Stable order in the bottom bar and nav rail.

**Validation rules**:
- Unknown persisted routes fall back to `train`.
- Duplicate route values are invalid.
- Order is fixed: Train, History, Progress, Profile.

## AppRoute

Represents shell presentation targets beyond top-level destinations.

**Fields**:
- `topLevel: TopLevelDestination?` - Present for top-level routes.
- `isActiveWorkout: Boolean` - True for the active-workout overlay route.
- `persistedValue: String` - Stable route string. Allowed values:
  `train`, `history`, `progress`, `profile`, `active-workout`.

**Validation rules**:
- `active-workout` is valid only when an active session has an
  `activeWorkoutId`.
- Invalid values recover to `train`.

## NavigationLayoutClass

Represents the adaptive shell layout.

**Fields**:
- `kind: Compact | Medium | Expanded`
- `usesBottomBar: Boolean`
- `usesNavigationRail: Boolean`
- `usesTwoPaneActiveWorkout: Boolean`

**Validation rules**:
- Compact uses bottom `FitTabBar`.
- Medium and Expanded use app-level `AppNavRail`.
- Expanded may enable two-pane active workout layout in later active-workout
  slices; this shell only exposes the layout class.

## ActiveWorkoutResume

Display-ready summary derived from existing `ActiveSessionState`.

**Fields**:
- `activeWorkoutId: FoundationId`
- `startedAt: Instant`
- `elapsedMillis: Long`
- `restRemainingMillis: Long`
- `displayTitle: String`
- `resumeRoute: AppRoute`

**Validation rules**:
- Exists only when `ActiveSessionState.activeWorkoutId` is not null.
- `elapsedMillis` and `restRemainingMillis` are derived from wall-clock
  instants, never stored countdown deltas.
- Resume action presents the active-workout overlay and persists
  `lastOpenedRoute = active-workout`.

## AppShellState

Single source of truth for the shared app shell.

**Fields**:
- `selectedDestination: TopLevelDestination`
- `layoutClass: NavigationLayoutClass`
- `activeWorkoutResume: ActiveWorkoutResume?`
- `isActiveWorkoutPresented: Boolean`
- `previousTopLevelDestination: TopLevelDestination`
- `reduceMotion: Boolean`
- `hapticsEnabled: Boolean`
- `paletteMode: PaletteMode`
- `errorMessage: String?`

**Validation rules**:
- If no active workout exists, `isActiveWorkoutPresented` must be false.
- Selecting a top-level destination hides the active-workout overlay but keeps
  the resume banner visible when an active workout exists.
- Presenting the active-workout overlay is allowed only when
  `activeWorkoutResume` exists.
- Dismissing the overlay returns to `previousTopLevelDestination`.

## NavigationIntent

Inputs handled by the shared navigation state holder.

**Values**:
- `Hydrate(session: ActiveSessionState?, now: Instant)`
- `SelectDestination(destination: TopLevelDestination)`
- `PresentActiveWorkout`
- `DismissActiveWorkout`
- `SetLayoutClass(layoutClass: NavigationLayoutClass)`
- `SetPreferences(reduceMotion: Boolean, hapticsEnabled: Boolean, paletteMode: PaletteMode)`
- `ActiveSessionChanged(session: ActiveSessionState?, now: Instant)`

**State transitions**:
- `Hydrate` with `lastOpenedRoute = active-workout` and active workout present
  presents the overlay; otherwise it selects the recovered top-level route.
- `SelectDestination` persists the destination route and hides the overlay.
- `PresentActiveWorkout` persists `active-workout`.
- `DismissActiveWorkout` persists the previous top-level route.
- `ActiveSessionChanged(null)` hides the overlay and clears the resume summary.

## PaletteMode

Represents user-facing theme mode for the shell.

**Fields**:
- `value: System | Dark | Light`
- `resolvedPalette: FitPalette`

**Validation rules**:
- Defaults to the dark Neo-Glass palette until the Profile settings slice
  exposes full preference editing.
- Switching palettes must go through `FitTheme` so color transitions animate.

## Persistence Mapping

The shell reuses the existing `active_session_state` row.

**Existing columns used**:
- `active_workout_id` - Drives resume banner and active overlay availability.
- `started_at` - Drives elapsed workout time.
- `rest_ends_at`, `rest_started_at`, `rest_origin_set_id` - Drive rest summary
  when later rest UI is wired.
- `last_opened_route` - Persists `train`, `history`, `progress`, `profile`, or
  `active-workout`.
- `updated_at` - Supports explicit session snapshot updates.

No new SQLDelight migration is required for this slice.
