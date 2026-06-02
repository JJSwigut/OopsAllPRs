# Contracts: Profile Settings and Local Export

## Profile State Holder Contract

`ProfileStateHolder` is the shared orchestration boundary for Profile.

### hydrate

**Input**: none.

**Behavior**:

- Loads durable weight unit from local preferences.
- Publishes local-first ownership status.
- Does not trigger network access.

**Success**:

- Profile state has `isHydrated = true`.
- `weightUnit` equals the durable preference or the default when no preference exists.

### setWeightUnit

**Input**: supported weight unit.

**Behavior**:

- Persists the selected unit locally.
- Updates Profile state after persistence succeeds.
- Clears stale export errors.

**Failure**:

- Leaves the previous unit in state.
- Publishes a recoverable error message.

### export

**Input**: export type.

**Behavior**:

- Captures the current Profile weight unit.
- Requests an export file from the export repository.
- Sends the generated file to the platform handoff when supplied.
- Publishes latest export metadata.

**Failure**:

- Publishes a recoverable error message.
- Does not mutate source workout, routine, exercise, or PR rows.

## Profile UI Contract

`ProfileFlow` receives immutable Profile state and callbacks.

**Inputs**:

- `ProfileState`
- unit selection callback
- palette selection callback
- haptics callback
- reduced-motion callback
- export callback

**Behavior**:

- Renders unit selection with design-system controls.
- Renders interaction preferences with design-system controls.
- Renders local-first/backup status.
- Renders export actions for workouts, routines, exercises, and PR history.
- Shows latest export metadata or a recoverable error.

**Accessibility requirements**:

- Tappable controls use design-system touch sizing.
- Labels are readable on phone viewports.
- Toggle state is represented visually and semantically by the component.

## Platform Export Handoff Contract

`FileExportHandoff.share(fileName, content)` is platform-owned.

**Shared expectations**:

- Called only after export generation succeeds.
- Receives generated file name and full CSV content.
- Must not alter the source ledger.

**Android actual**:

- Opens the OS share/export surface for generated CSV text.

**iOS actual**:

- Remains compile-safe and replaceable by the native presenter when the iOS app shell is wired.
