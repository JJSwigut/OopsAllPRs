# Implementation Plan: Profile Settings and Local Export

**Branch**: `codex/010-profile-export-settings` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/010-profile-export-settings/spec.md`

## Summary

Turn the Profile destination from a placeholder into the local ownership and
settings surface. The slice wires Profile state to existing local repositories
for durable weight-unit preference and export generation, adds shared UI for
unit selection, interaction preferences, local-first status, and export
actions, and sends generated export content through a platform handoff. Android
gets the first runtime handoff; iOS remains first-class through the existing
shared expect/actual adapter boundary and compile validation.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight-backed `SqlFoundationStore`, repository interfaces, `ExportService`, Compose runtime/state, Kotlin coroutines, Android intent share handoff, existing iOS no-op handoff boundary.

**Storage**: SQLDelight local SQLite database through the already-implemented preference and export repository paths. Weight unit is persisted in `user_preferences`; export snapshots are recorded by the existing export repository.

**Local Data Model**: Profile state exposes durable weight unit, runtime palette/haptics/reduced-motion preferences, local ownership status, export in-progress state, latest export metadata, and recoverable export errors. Export rows are produced from completed workouts, routines, exercises, and personal records already stored locally.

**Testing**: Common unit tests for `ProfileStateHolder` preference hydration, durable unit changes, export result state, and interaction preference state. Android unit tests already cover SQL export output and preference durability; this slice reuses those paths and runs the full Android/iOS gates.

**Target Platform**: Android runtime first. iOS remains first-class through shared Profile state/UI and platform export handoff compile validation.

**Platform Scope**: Shared code owns Profile state, UI, export orchestration, and repository/use-case interaction. Android platform code owns the concrete share-sheet intent. iOS keeps an adapter boundary and compile-safe no-op implementation until the native app shell provides a real presenter.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI.

**Performance Goals**: Profile hydration should complete from local storage within normal top-level navigation latency. Export generation should remain responsive for ordinary personal workout history volumes and report progress with an in-flight state.

**Constraints**: Offline-only; no account/cloud/network dependency; export reads must not mutate workout ledger rows; all persisted weights stay canonical kilograms; Profile UI must use `FitTheme` tokens and design-system components; no Material 3 reintroduction.

**Scale/Scope**: Single-user local workout history, seeded/user-created exercise catalog, routines/templates, PR history, local preference row, export snapshots, Android share handoff. Import/restore, cloud sync, per-user Android backup policy toggles, and fully native iOS share presentation remain out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Profile is outside the active set logging path and does not add steps to confirming sets.
- **Ledger Integrity**: PASS. Export reads completed ledger/PR/source rows and records snapshots without altering source workout data.
- **Session Recovery**: PASS. Unit preference is recovered from durable local storage; active workout recovery and timers are not changed.
- **Progress Promise**: PASS. PR export includes source ids and bodyweight reps-only behavior; display unit changes support progress interpretation.
- **Local-First Ownership**: PASS. Feature works offline and exposes local export/backup status without sync or accounts.
- **Shared-First KMP**: PASS. Profile state and UI live in shared code; Android and iOS differences are behind platform adapters.
- **Design System and Accessibility**: PASS. Profile UI will use Fit design-system components/tokens, large touch targets, readable labels, haptic alternatives, and reduced-motion support.
- **Public Release Gates**: PASS. Plan includes Profile state tests, export/preference behavior validation, Android tests/build, iOS compile, Material scan, whitespace validation, and manual device discovery.

## Project Structure

### Documentation (this feature)

```text
specs/010-profile-export-settings/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- profile-export-settings-contracts.md
|-- checklists/
|   `-- requirements.md
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- App.kt
|   |-- AppState.kt
|   |-- data/export/ExportService.kt
|   |-- domain/repository/FoundationRepositories.kt
|   |-- platform/PlatformAdapters.kt
|   |-- ui/navigation/AppShell.kt
|   `-- ui/profile/
|       |-- ProfileFlow.kt
|       `-- ProfileStateHolder.kt
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
|   `-- AndroidPlatformAdapters.kt
|-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/
|   `-- IosPlatformAdapters.kt
`-- src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/profile/
    `-- ProfileStateHolderTest.kt

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt
```

**Structure Decision**: Keep Profile as shared UI/state. Inject existing
`PreferencesRepository` and `ExportRepository` into `ProfileStateHolder`, and
optionally inject `FileExportHandoff` from platform app construction. Avoid new
database schema for this slice because the SQL-backed preference/export paths
already exist.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Use the existing SQL-backed `PreferencesRepository` for durable weight unit.
- Use the existing `ExportService`/`ExportRepository` for CSV generation and snapshot recording.
- Add Profile orchestration in shared state rather than composables directly calling repositories.
- Implement Android share handoff with a platform adapter and keep iOS compile-safe through the same boundary.
- Keep interaction preferences runtime-only for this slice except weight unit, because durable settings beyond units need a separate preference contract.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/profile-export-settings-contracts.md](./contracts/profile-export-settings-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. The design remains local-first, shared-first, design-system-only, and
export-focused without changing active logging or ledger mutation behavior. No
unresolved clarification or complexity violation remains.
