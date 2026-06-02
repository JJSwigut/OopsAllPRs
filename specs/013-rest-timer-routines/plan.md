# Implementation Plan: Routine-Aware Rest Timers

**Branch**: `codex/013-rest-timer-routines` | **Date**: 2026-05-30 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/013-rest-timer-routines/spec.md`

## Summary

Add routine-aware rest timers to the active workout loop. The feature extends
active and routine exercises with local per-exercise rest configuration,
auto-starts wall-clock rest after successful set logging, surfaces a
thumb-reachable RestBar with +15/-15/skip controls, persists/restores active
rest state through the existing active session table, and wires Android rest
completion alerts behind the existing platform adapter boundary while iOS
remains compile-safe.

## Technical Context

**Language/Version**: Kotlin Multiplatform 2.1.0; Compose Multiplatform 1.8.2; AGP 8.9.0; compileSdk 35; minSdk 31.

**Primary Dependencies**: Existing `:shared` KMP module, `:design-system`, SQLDelight-backed `SqlFoundationStore`, repository/use-case layers, shared Compose UI/state holders, Kotlin coroutines, Android notification/alarm APIs behind platform adapters.

**Storage**: SQLDelight local SQLite database. This feature extends active exercise and routine exercise rows with rest seconds and auto-start flags, uses existing active session rest columns, and extends user preferences for rest default and sound preference.

**Local Data Model**: Add a shared rest configuration value object. Attach it to `ActiveExercise` and `RoutineExercise`. Persist defaults in preferences. Active rest continues to use `ActiveSessionState.restStartedAt`, `restEndsAt`, and `restOriginSetId`. Rest origin cleanup is required when sets are deleted, undone, finished, discarded, or skipped.

**Testing**: Common unit tests for rest configuration, active auto-start, adjust/skip, restart recovery, deleted-origin cleanup, routine carryover, default preference, sound preference, and profile state. Android SQL unit tests for schema persistence and rest session recovery. Android compile/assemble, iOS simulator compile, Material guard, and diff check remain required.

**Target Platform**: Android first on emulator or connected Pixel-class device. iOS remains first-class through shared code and no-op/compile-safe platform alert boundary.

**Platform Scope**: Shared code owns rest domain rules, persistence contracts, state holders, and Compose UI. Android code owns notification channel/alarm/alert delivery. iOS adapter remains a no-op until native iOS alert work is prioritized.

**Project Type**: Kotlin Multiplatform mobile app with shared domain/data/state/Compose UI, Android bootstrap app, and iOS bootstrap app.

**Performance Goals**: Logging a set still produces visible confirmation and rest start within 3 seconds. Rest ticking should update roughly once per second while active without blocking set input. Session restore remains within the existing 2-second budget.

**Constraints**: Offline-only; no cloud sync; no wearable support; no set-type-specific rest in this slice. UI must use `FitTheme` and design-system components/tokens with no Material 3 reintroduction. Rest cannot be modal during normal set logging.

**Scale/Scope**: Single-user local active workout, routines/templates, preferences, and rest alerts. Competitor parity backlog is preserved separately and not implemented here.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Rest auto-starts after successful logging and controls sit in the active loop without adding a blocking modal.
- **Ledger Integrity**: PASS. Rest starts only after persistence succeeds and references logged set ids; set deletion/undo clears invalid rest origin references.
- **Session Recovery**: PASS. Rest uses wall-clock start/end instants and active session recovery.
- **Progress Promise**: PASS. PR and completed history derivation remain unchanged; rest metadata does not mutate logged set values.
- **Local-First Ownership**: PASS. Rest configuration, preferences, and active rest state stay local/offline-first and future sync-ready through stable ids/timestamps.
- **Shared-First KMP**: PASS. Domain/data/state/UI changes are shared; Android alert delivery is isolated behind `RestNotificationScheduler`.
- **Design System and Accessibility**: PASS. RestBar uses existing Fit components/tokens, large touch targets, accessible labels, reduced-motion-safe updates, and one-handed controls.
- **Public Release Gates**: PASS. Plan includes common and Android persistence tests, Android build, iOS compile, Material scan, diff check, and device/manual alert note.

## Project Structure

### Documentation (this feature)

```text
specs/013-rest-timer-routines/
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   `-- rest-timer-contracts.md
|-- checklists/
|   `-- requirements.md
|-- validation/
|   `-- rest-timer-results.md
|-- competitor-gap-backlog.md
`-- tasks.md
```

### Source Code (repository root)

```text
shared/
|-- src/commonMain/kotlin/com/jjswigut/oopsallprs/
|   |-- AppState.kt
|   |-- data/repository/
|   |-- domain/model/
|   |-- domain/repository/FoundationRepositories.kt
|   |-- domain/usecase/
|   |-- platform/PlatformAdapters.kt
|   |-- ui/navigation/
|   |-- ui/profile/
|   `-- ui/workout/
|-- src/commonMain/sqldelight/com/jjswigut/oopsallprs/db/
|-- src/commonTest/kotlin/com/jjswigut/oopsallprs/
|-- src/androidMain/kotlin/com/jjswigut/oopsallprs/platform/
|-- src/androidUnitTest/kotlin/com/jjswigut/oopsallprs/
`-- src/iosMain/kotlin/com/jjswigut/oopsallprs/platform/

androidApp/
|-- src/main/AndroidManifest.xml
`-- src/main/kotlin/com/jjswigut/oopsallprs/MainActivity.kt
```

**Structure Decision**: Keep rest behavior in existing shared layers:
repository/use-case/state-holder/shared Compose UI. Add Android-only alert
delivery behind the existing adapter. Avoid a separate timer subsystem until
wearables or multiple concurrent timers exist.

## Complexity Tracking

No constitution violations are planned.

## Phase 0 Research Summary

Research is captured in [research.md](./research.md). Key decisions:

- Per-exercise rest configuration is the implementation slice.
- Active rest remains a single active timer anchored in `ActiveSessionState`.
- Auto-start happens only after set persistence succeeds.
- Android alerts use the existing platform adapter; iOS is compile-safe no-op.
- Broader competitor parity is preserved in `competitor-gap-backlog.md`.

## Phase 1 Design Summary

Design artifacts are captured in:

- [data-model.md](./data-model.md)
- [contracts/rest-timer-contracts.md](./contracts/rest-timer-contracts.md)
- [quickstart.md](./quickstart.md)

## Post-Design Constitution Re-Check

PASS. The design remains local-first, shared-first, recovery-safe, and
design-system-only. Feature breadth is intentionally limited to routine-aware
rest timers; broader competitor parity remains backlog-only.
