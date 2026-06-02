# Implementation Plan: Android Alpha Hardening

**Branch**: `codex/015-android-alpha-hardening` | **Date**: 2026-05-31 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/015-android-alpha-hardening/spec.md`

## Summary

Prepare the current Android build for internal alpha dogfooding by adding an explicit smoke checklist, improving local/readiness and fresh-install messaging, and hardening active session/rest recovery validation. Implementation stays in shared KMP state/UI where possible, keeps Android as the manual validation target, and preserves iOS readiness through shared compile gates.

## Technical Context

**Language/Version**: Kotlin Multiplatform on the repo's configured Kotlin/Gradle toolchain

**Primary Dependencies**: Compose Multiplatform shared UI, SQLDelight persistence, app-local Fit design system (`FitTheme` and Fit components)

**Storage**: Existing SQLDelight local database and in-memory test store; no new persisted tables for this hardening slice

**Local Data Model**: Active workouts, rest timers, routes, exercises, routines, completed workouts, PRs, exports, and profile status remain in existing models; this feature adds/readjusts UI readiness models and validation evidence only

**Testing**: Shared common unit tests, Android debug unit tests/build, Android app assemble, iOS simulator shared compile, Material guard, whitespace check, manual Android smoke checklist

**Target Platform**: Android debug build validated first on Pixel-class device or emulator; shared code remains iOS-ready

**Platform Scope**: Android manual dogfood gate; iOS has no new adapter work but must continue compiling via shared code boundaries

**Project Type**: Kotlin Multiplatform mobile app

**Performance Goals**: No added latency in active workout logging or app hydration; checklist should be executable in 15 minutes or less

**Constraints**: Offline-only/local-first behavior; no cloud sync, analytics, login, or release channel changes; hardening UI must use Fit design-system components/tokens

**Scale/Scope**: Alpha dogfood readiness for core Train, History, Progress, Profile, rest timer, export, and recovery flows

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: PASS. The feature validates and preserves thumb-friendly active logging; no extra modal work is added to the set confirmation path.
- **Ledger Integrity**: PASS. No ledger model changes are planned; tests verify active workout identity and rest state recovery without mutating completed history.
- **Session Recovery**: PASS. Expired rest hydration, notification cancellation, resume affordance, and route recovery are explicit acceptance and test targets.
- **Progress Promise**: PASS. Empty Progress copy must explain when PRs appear without fabricating progress.
- **Local-First Ownership**: PASS. Profile readiness clarifies local database, sync off, backup eligibility, export availability, and notification expectations.
- **Shared-First KMP**: PASS. Shared UI/state/test changes are preferred; Android-specific behavior stays behind existing platform adapters.
- **Design System and Accessibility**: PASS. UI changes use Fit components/tokens only and avoid a parallel Material/styling surface.
- **Public Release Gates**: PASS. Plan requires Android build/tests, iOS shared compile, guard checks, alpha checklist, and validation evidence.

## Project Structure

### Documentation (this feature)

```text
specs/015-android-alpha-hardening/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
├── contracts/
│   └── android-alpha-hardening-contracts.md
├── validation/
│   └── android-alpha-hardening-results.md
└── tasks.md
```

### Source Code (repository root)

```text
shared/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/profile/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/progress/
├── src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/workout/
└── src/commonTest/kotlin/com/jjswigut/oopsallprs/

androidApp/
└── src/main/

design-system/
└── src/commonMain/
```

**Structure Decision**: Keep alpha-readiness UI and recovery validation in shared code. Android platform notification behavior remains in the existing adapter; this slice documents and validates denied/unavailable notification behavior without adding new platform UI.

## Complexity Tracking

No constitution violations or added architectural complexity.

## Phase 0: Research

Research resolves alpha-hardening decisions in [research.md](research.md).

## Phase 1: Design & Contracts

- Data model: [data-model.md](data-model.md)
- UI/manual contracts: [contracts/android-alpha-hardening-contracts.md](contracts/android-alpha-hardening-contracts.md)
- Quickstart and validation plan: [quickstart.md](quickstart.md)
- Agent context: [AGENTS.md](../../AGENTS.md) points to this plan.

## Post-Design Constitution Check

- **Fast-Loop Logging**: PASS. Empty/readiness copy is informational and does not interrupt logging.
- **Ledger Integrity**: PASS. Tests target recovery side effects only; no completed workout data is rewritten.
- **Session Recovery**: PASS. Expired and unexpired rest states are covered by tasks and evidence.
- **Progress Promise**: PASS. Progress empty state says PRs appear after completed logged work.
- **Local-First Ownership**: PASS. Readiness status names local storage, sync off, backup, export, and notification expectations.
- **Shared-First KMP**: PASS. New behavior is shared or existing-adapter bounded; iOS compile is a completion gate.
- **Design System and Accessibility**: PASS. Fit components/tokens are required in tasks and guard checks.
- **Public Release Gates**: PASS. Automated and manual gates are recorded in validation artifacts.
