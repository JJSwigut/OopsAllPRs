# Implementation Plan: Exercise Progress Charting

**Branch**: `codex/016-exercise-progress-charting` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/016-exercise-progress-charting/spec.md`

## Summary

Add a visual per-exercise progress chart inside the existing Progress exercise detail. Use existing `ProgressPoint` data, add shared chart-ready view models and metric selection state, render with a lightweight Fit design-system Canvas component, and keep source evidence reachable through the existing evidence detail flow.

## Technical Context

**Language/Version**: Kotlin Multiplatform on the repo's configured Kotlin/Gradle toolchain

**Primary Dependencies**: Compose Multiplatform shared UI, existing Fit design system, existing progress repositories/use cases

**Storage**: Existing local SQLDelight/in-memory progress points only; no schema changes

**Local Data Model**: Derive `ProgressChartState` and `ProgressChartPoint` from `ProgressPoint` values grouped by selected exercise and metric

**Testing**: Shared common unit tests for chart state/model generation and state-holder metric switching; Android debug build; iOS simulator shared compile; Material guard; whitespace check; optional Pixel 9 Pro emulator screenshot

**Target Platform**: Android validated first; shared implementation remains iOS-ready

**Platform Scope**: Shared state/model/UI component work only; no Android-only adapter changes

**Project Type**: Kotlin Multiplatform mobile app

**Performance Goals**: Chart generation remains in-memory over the selected exercise's local points and does not affect active workout logging latency

**Constraints**: Offline/local-only; no heavy chart dependency; FitTheme/Fit design-system tokens only; chart must fit compact screens with resume banner present

**Scale/Scope**: Per-exercise charting for existing progress metrics; range filters and predictive analytics are deferred

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. Progress charting is review-only and does not alter active workout logging.
- **Ledger Integrity**: PASS. Chart points are derived from existing persisted progress points and evidence links remain read-only.
- **Session Recovery**: PASS. No active session state is changed; smoke validation includes active resume banner coexistence.
- **Progress Promise**: PASS. The feature directly improves visible progress while keeping source evidence inspectable.
- **Local-First Ownership**: PASS. Charts work entirely from local progress data and require no network.
- **Shared-First KMP**: PASS. State, model, and UI are shared Compose; no platform shortcut is planned.
- **Design System and Accessibility**: PASS. Chart uses FitTheme tokens, labels, and non-color-only value context.
- **Public Release Gates**: PASS. Automated chart tests, Android build, iOS compile, guard checks, and visual smoke evidence are planned.

## Project Structure

### Documentation (this feature)

```text
specs/016-exercise-progress-charting/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── exercise-progress-charting-contracts.md
├── validation/
│   └── exercise-progress-charting-results.md
└── tasks.md
```

### Source Code (repository root)

```text
design-system/
└── src/commonMain/kotlin/com/jjswigut/oopsallprs/ds/component/

shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/ui/progress/
```

**Structure Decision**: Put the reusable chart primitive in `design-system` and keep Progress-specific chart state/model assembly in `shared` UI progress code. No persistence or platform adapter directories are touched.

## Complexity Tracking

No constitution violations or added architectural complexity.

## Phase 0: Research

Research decisions are recorded in [research.md](research.md).

## Phase 1: Design & Contracts

- Data model: [data-model.md](data-model.md)
- UI contracts: [contracts/exercise-progress-charting-contracts.md](contracts/exercise-progress-charting-contracts.md)
- Quickstart and validation plan: [quickstart.md](quickstart.md)
- Agent context: [AGENTS.md](../../AGENTS.md) points to this plan.

## Post-Design Constitution Check

- **Fast-Loop Logging**: PASS. The active logging loop is untouched.
- **Ledger Integrity**: PASS. Chart evidence uses existing source ids and no ledger mutations.
- **Session Recovery**: PASS. Resume banner coexistence is part of validation.
- **Progress Promise**: PASS. The selected exercise now has visible trend context.
- **Local-First Ownership**: PASS. No sync/network dependencies.
- **Shared-First KMP**: PASS. Shared Compose and shared tests cover the slice.
- **Design System and Accessibility**: PASS. `FitLineChart` is token-driven and exposes textual value context.
- **Public Release Gates**: PASS. Required test/build/guard/manual gates are captured in tasks and validation.
