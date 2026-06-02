# Implementation Plan: Developer Demo Data Seeding

**Branch**: `codex/017-developer-demo-seed` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/017-developer-demo-seed/spec.md`

## Summary

Add developer-only seed scenarios that populate realistic local data for manual QA. Implement seed orchestration in shared code through existing exercise, routine, workout, set logging, and PR derivation use cases; expose it only when the Android debug app passes an explicit developer-tools flag into shared app state.

## Technical Context

**Language/Version**: Kotlin Multiplatform on the repo's configured Kotlin/Gradle toolchain

**Primary Dependencies**: Existing shared Compose UI, Fit design system, SQLDelight-backed repositories, existing workout/routine/progress use cases

**Storage**: Existing SQLDelight local database; no schema changes planned

**Local Data Model**: Use reserved demo routine names and deterministic demo timestamps to detect already-loaded scenarios. Completed workout data is created through normal active-workout start, exercise add, set confirm, and finish paths so PR and progress rows derive from ordinary ledger data.

**Testing**: Shared common tests for demo scenario idempotency, PR/chart derivation, bodyweight reps-only data, routine rest data, and active recovery behavior; Android debug build; iOS simulator shared compile; release visibility guard; Material guard; whitespace check

**Target Platform**: Android debug app validated first; seed orchestration remains shared and platform-neutral

**Platform Scope**: Android receives the first developer entry point using the app debug flag. iOS remains first-class through shared seed use case and nullable developer-tool state boundary; no iOS UI is required in this slice.

**Project Type**: Kotlin Multiplatform mobile app

**Performance Goals**: Loading all demo scenarios should complete within 30 seconds on a fresh local install and should not affect normal app hydrate time when developer tools are disabled.

**Constraints**: Local-only/offline-only; no release-visible seed controls; no direct SQL writes for demo workouts; no destructive reset of user data in this slice; FitTheme components only for visible debug UI

**Scale/Scope**: Developer QA dataset covering several weeks of completed workouts, at least two routines, and one active recovery session. Cloud sync, full fixture import/export, and production sample accounts are out of scope.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Normal active logging remains unchanged; demo controls live in developer-only Profile tooling.
- **Ledger Integrity**: PASS. Demo workouts use existing use cases so sets are persisted before completion and PRs derive from the ledger.
- **Session Recovery**: PASS. Active recovery seed scenario explicitly creates recoverable active session state.
- **Progress Promise**: PASS. Dataset intentionally covers PRs, bodyweight reps-only, charts, and evidence.
- **Local-First Ownership**: PASS. Demo data is local-only and does not require network or sync.
- **Shared-First KMP**: PASS. Seed orchestration is shared; Android only provides the debug flag and visible entry point.
- **Design System and Accessibility**: PASS. Debug card uses existing Profile/Fit components and is absent from release builds.
- **Public Release Gates**: PASS. Plan includes common tests, Android build, iOS compile, release guard, Material scan, diff check, and validation notes.

## Project Structure

### Documentation (this feature)

```text
specs/017-developer-demo-seed/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── developer-demo-seed-contracts.md
├── validation/
│   └── developer-demo-seed-results.md
└── tasks.md
```

### Source Code (repository root)

```text
androidApp/
└── src/main/kotlin/com/jjswigut/oopsallprs/

shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/
│   ├── dev/
│   ├── ui/navigation/
│   └── ui/profile/
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/dev/
```

**Structure Decision**: Put seed orchestration and developer state in `shared/src/commonMain` so Android and future iOS debug tooling can reuse it. Gate the UI by a nullable `developerSeeds` boundary in `AppState`, populated only when Android passes `BuildConfig.DEBUG`.

## Complexity Tracking

No constitution violations or extra architectural complexity.

## Phase 0: Research

Research decisions are recorded in [research.md](research.md).

## Phase 1: Design & Contracts

- Data model: [data-model.md](data-model.md)
- UI/use-case contracts: [contracts/developer-demo-seed-contracts.md](contracts/developer-demo-seed-contracts.md)
- Quickstart and validation plan: [quickstart.md](quickstart.md)
- Agent context: [AGENTS.md](../../AGENTS.md) points to this plan.

## Post-Design Constitution Check

- **Fast-Loop Logging**: PASS. No normal logging controls are changed.
- **Ledger Integrity**: PASS. Seed paths avoid direct SQL workout writes and use normal finish/PR rebuild behavior.
- **Session Recovery**: PASS. Active recovery scenario is covered by tests and quickstart.
- **Progress Promise**: PASS. Progress demo creates weighted and bodyweight chart/PR evidence.
- **Local-First Ownership**: PASS. No network, account, or cloud dependency is introduced.
- **Shared-First KMP**: PASS. Shared seed orchestration compiles for iOS; Android-specific work is only a debug flag handoff.
- **Design System and Accessibility**: PASS. Visible controls use Profile/Fit components and remain debug-only.
- **Public Release Gates**: PASS. Validation gates are captured in tasks and results documentation.
