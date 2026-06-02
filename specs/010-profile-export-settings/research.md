# Research: Profile Settings and Local Export

## Decision: Reuse the Existing Preference Repository for Weight Unit

**Rationale**: The SQL-backed foundation already persists `weight_unit` in the
singleton user preference row and `ProgressStateHolder` already reads the same
repository for display formatting. Wiring Profile to this repository gives
durable settings without introducing another storage mechanism.

**Alternatives considered**:

- Store units in platform settings: rejected because it would split user data
  between SQL and platform stores and complicate future sync/export.
- Keep units in memory only: rejected because restart durability is a core
  acceptance criterion.

## Decision: Keep Profile Export Orchestration in Shared State

**Rationale**: Shared state can coordinate export in-progress state, errors,
last export metadata, selected unit, and repository failures without embedding
data operations inside composables. This matches the existing state-holder
pattern for Workout, History, Routines, and Progress.

**Alternatives considered**:

- Trigger repository calls from `ProfileFlow`: rejected because composables
  should stay declarative and easier to preview/test.
- Add a new export screen: rejected for this slice because Profile can expose
  export actions directly with a smaller, independently testable scope.

## Decision: Use Existing CSV Export and Snapshot Behavior

**Rationale**: The existing `ExportRepository` returns `ExportFile` and records
SQL export snapshots for durable local ownership evidence. Profile only needs
to request the right type with the selected unit and surface the result.

**Alternatives considered**:

- Add a new export format: rejected because CSV already satisfies inspectable
  local backup needs for the first implementation.
- Add import/restore now: rejected as larger scope with more ledger and
  migration risk.

## Decision: Android Share Intent First, iOS Adapter Boundary Kept

**Rationale**: Android can hand generated text/CSV to the OS share sheet through
the existing `FileExportHandoff` expect/actual boundary. iOS remains
first-class at the shared contract level and can later replace the no-op actual
with a native presenter without shared-state changes.

**Alternatives considered**:

- Write files directly into public downloads: rejected because scoped storage
  and permissions add release risk for this slice.
- Leave Android handoff as no-op: rejected because the feature needs a visible
  export path on the first runtime target.

## Decision: Runtime Interaction Preferences Stay Runtime-Only

**Rationale**: Palette, haptics, and reduced motion already drive `FitTheme`
through navigation state. Persisting all of them would require expanding the
preference repository contract beyond the current durable unit setting. This
slice makes them adjustable in Profile and applies them immediately, while
weight unit is durable.

**Alternatives considered**:

- Expand the preference schema now: rejected because the current feature is
  primarily data ownership/export and the existing SQL preference contract only
  needs units for acceptance.
- Hide interaction settings from Profile: rejected because Profile is currently
  the only user settings surface and should control app shell behavior.
