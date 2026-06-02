# Implementation Plan: Mistake Recovery and Editing

**Branch**: `codex/012-mistake-recovery-editing` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/012-mistake-recovery-editing/spec.md`

## Summary

Add ledger-safe mistake recovery across active logging and saved local data.
The slice adds explicit shared use-case/repository mutation paths for editing,
deleting, and undoing active logged sets; confirmation-backed active workout
discard; template deletion; completed workout deletion; and PR recalculation
after source data changes. Android remains the first runtime validation target
while iOS stays first-class through shared domain/state/UI code and compile
validation.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight-backed `SqlFoundationStore`, repository/use-case layers, shared Compose UI/state holders, Kotlin coroutines, Android emulator/ADB tooling.

**Storage**: SQLDelight local SQLite database. This feature uses existing active workout, set, completed workout, routine, PR, progress point, active session, active UX, and export snapshot tables. No migration is planned unless implementation discovers that a required deletion contract cannot be represented safely.

**Local Data Model**: Existing active workouts and exercise sets gain explicit mutation flows for active logged-set edits/deletes/undo. Existing routines/templates gain a delete path. Completed workouts gain a delete path that removes source completed data and triggers PR/progress rebuild. UI state gains confirmation and edit draft states.

**Testing**: Common unit tests for active set edit/delete/undo, active workout discard state, template deletion, completed workout deletion, PR rebuild, and UI state holder behavior; existing Android SQL integration tests where persistence behavior changes; `./gradlew :shared:testDebugUnitTest`; `./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug`; `./gradlew :shared:compileKotlinIosSimulatorArm64`; Material scan; `git diff --check -- .`; Pixel-class manual smoke validation.

**Target Platform**: Android first on emulator or connected Pixel-class device. iOS remains first-class through shared code boundaries and iOS simulator compile.

**Platform Scope**: Shared code owns domain rules, persistence contracts, state holders, and Compose UI. Android work is limited to runtime validation. iOS code should not require platform-specific changes unless a shared boundary compile failure appears.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI, Android bootstrap app, and iOS bootstrap app.

**Performance Goals**: Active correction actions should keep the gym-side flow responsive; edit/delete/undo should update local state within normal set logging latency. PR rebuild after completed workout deletion should be acceptable for local single-user history volumes.

**Constraints**: Offline-only; no accounts, sync, import/restore, or remote tombstones. All destructive actions require explicit confirmation except undo-last-set, which is itself the explicit recovery action. UI must use `FitTheme` and design-system components/tokens with no Material 3 reintroduction.

**Scale/Scope**: Single-user local workout history, active workouts, saved routines/templates, PR/progress evidence, and exports. Full audit-history UI, soft-delete sync tombstones, batch editing, routine editing, and advanced accessibility pass are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Normal logging remains one primary action; correction tools are available when needed and avoid blocking set confirmation.
- **Ledger Integrity**: PASS. All logged-set and durable history changes are explicit mutation paths with validation, persistence results, and PR/export implications.
- **Session Recovery**: PASS. Active corrections and discard behavior define restart recovery expectations and use existing session clearing.
- **Progress Promise**: PASS. PR evidence and Progress views rebuild when source logged sets or completed workouts change, including bodyweight reps-only records.
- **Local-First Ownership**: PASS. All changes operate offline against local data and keep future sync implications documented.
- **Shared-First KMP**: PASS. Domain/data/state/UI changes belong in shared code; Android-first validation does not introduce Android-only business logic.
- **Design System and Accessibility**: PASS. New controls use Fit design-system components, confirmation labels, cancel paths, touch targets, and existing reduced-motion/haptic settings.
- **Public Release Gates**: PASS. Plan includes unit/integration tests, Android build/manual smoke, iOS compile, Material scan, whitespace validation, and validation notes.

## Project Structure

### Documentation (this feature)

```text
specs/012-mistake-recovery-editing/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- mistake-recovery-contracts.md
|-- checklists/
|   `-- requirements.md
|-- validation/
|   `-- mistake-recovery-results.md
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- data/repository/
|   |-- domain/repository/FoundationRepositories.kt
|   |-- domain/usecase/
|   |-- ui/history/
|   |-- ui/navigation/
|   |-- ui/routine/
|   `-- ui/workout/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|   |-- data/
|   |-- domain/usecase/
|   |-- ui/history/
|   |-- ui/routine/
|   `-- ui/workout/
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
`-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/

androidApp/
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt
```

**Structure Decision**: Keep all product behavior in `:shared`. Extend the
existing repository/use-case/state-holder layers rather than introducing a new
editing subsystem. Put reusable confirmation/edit state in the closest owning
flow: active workout state for active logged sets and discard, routine state
for template deletion, and History state for completed workout deletion.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Edit active logged sets in place while preserving set id and original `loggedAt`.
- Implement delete/undo for active sets by removing logged set rows before completion.
- Use hard local delete for templates and completed workouts in this offline-only slice.
- Rebuild PR/progress projections after completed workout deletion and after active logged-set edits that affect inline PR feedback.
- Use simple confirmation state in shared UI rather than platform dialogs.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/mistake-recovery-contracts.md](./contracts/mistake-recovery-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. The design remains local-first, ledger-explicit, shared-first, and
design-system-only. It adds no cloud/sync/tombstone scope and keeps audit
history beyond current correction metadata out of scope for this feature.
