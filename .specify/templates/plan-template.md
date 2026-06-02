# Implementation Plan: [FEATURE]

**Branch**: `[###-feature-name]` | **Date**: [DATE] | **Spec**: [link]

**Input**: Feature specification from `/specs/[###-feature-name]/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

[Extract from feature spec: primary requirement + technical approach from research]

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: [e.g., Python 3.11, Swift 5.9, Rust 1.75 or NEEDS CLARIFICATION]

**Primary Dependencies**: [e.g., FastAPI, UIKit, LLVM or NEEDS CLARIFICATION]

**Storage**: [if applicable, e.g., PostgreSQL, CoreData, files or N/A]

**Local Data Model**: [entities, persistence strategy, migrations, seed data, export/backup impact]

**Testing**: [e.g., pytest, XCTest, cargo test or NEEDS CLARIFICATION]

**Target Platform**: [e.g., Linux server, iOS 15+, WASM or NEEDS CLARIFICATION]

**Platform Scope**: [Android-first implementation, iOS impact/adapter needs, shared code boundary]

**Project Type**: [e.g., library/cli/web-service/mobile-app/compiler/desktop-app or NEEDS CLARIFICATION]

**Performance Goals**: [domain-specific, e.g., 1000 req/s, 10k lines/sec, 60 fps or NEEDS CLARIFICATION]

**Constraints**: [domain-specific, e.g., <200ms p95, <100MB memory, offline-capable or NEEDS CLARIFICATION]

**Scale/Scope**: [domain-specific, e.g., 10k users, 1M LOC, 50 screens or NEEDS CLARIFICATION]

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Fast-Loop Logging**: Active workout changes preserve or reduce taps,
  avoid modal interruption in normal set logging, and keep the next logging
  action obvious. Any added friction has documented user value.
- **Ledger Integrity**: Workout, exercise, set, PR, export, and persistence
  changes prove confirmed work cannot be lost, duplicated, silently mutated, or
  shown logged before persistence succeeds.
- **Session Recovery**: Active session, elapsed timer, rest timer, and
  navigation changes define process-death/cold-start behavior and wall-clock
  anchors where time is involved.
- **Progress Promise**: PR, history, last-set, bodyweight, fractional-weight,
  unit conversion, and locale input behavior are covered when relevant.
- **Local-First Ownership**: Feature works offline, stores data locally by
  default, documents export/backup impact, and avoids blocking future sync.
- **Shared-First KMP**: Domain, data, validation, state, and shared UI remain in
  shared code unless an explicit platform adapter boundary is documented.
  Android-first sequencing includes iOS implications.
- **Design System and Accessibility**: UI plans use the Neo-Glass token-driven
  design system (FitTheme tokens/components) with no parallel styling, plus
  adaptive layouts, touch target, TalkBack, dynamic type, reduced-motion, and
  haptic-alternative expectations.
- **Public Release Gates**: Plan identifies automated tests, release artifact
  checks, asset validation, permissions/backup review, export sanity checks, and
  milestone/release manual device verification.

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)
<!--
  ACTION REQUIRED: Replace the placeholder tree below with the concrete layout
  for this feature. Delete unused options and expand the chosen structure with
  real paths (e.g., apps/admin, packages/something). The delivered plan must
  not include Option labels.
-->

```text
# [REMOVE IF UNUSED] Option 1: Single project (DEFAULT)
src/
├── models/
├── services/
├── cli/
└── lib/

tests/
├── contract/
├── integration/
└── unit/

# [REMOVE IF UNUSED] Option 2: Web application (when "frontend" + "backend" detected)
backend/
├── src/
│   ├── models/
│   ├── services/
│   └── api/
└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Kotlin Multiplatform mobile app
shared/
├── src/commonMain/      # domain, data, validation, state, shared Compose UI
├── src/commonTest/
├── src/androidMain/     # Android adapters only
├── src/androidUnitTest/
└── src/iosMain/         # iOS adapters only

androidApp/
└── src/main/            # Android app bootstrap and manifest

iosApp/
└── [iOS app bootstrap, if included in scope]
```

**Structure Decision**: [Document the selected structure and reference the real
directories captured above]

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
