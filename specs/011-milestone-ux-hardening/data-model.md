# Data Model: Milestone Validation and UX Hardening

This feature does not add product database entities. The model below describes
the validation artifacts and review state used to close milestone gates.

## Milestone Validation Run

Represents one dated manual validation pass.

Fields:

- `date`: Calendar date of the run.
- `branch`: Feature branch under validation.
- `appBuild`: Human-readable build command or APK path.
- `target`: Device/emulator name and Android version when available.
- `scenarios`: Ordered list of scenario results.
- `environmentNotes`: Platform warnings or emulator/device limitations.
- `screenshots`: Relative paths to captured evidence.
- `automatedGateResults`: Commands and pass/fail results for common tests,
  Android build, iOS compile, Material scan, and whitespace validation.

Validation rules:

- Must identify the target and build.
- Must distinguish app defects from platform/environment limitations.
- Must link each failed scenario to at least one UX finding or remaining reason.

## Scenario Result

Represents the result of one user-facing validation scenario.

Fields:

- `scenarioId`: Stable id such as `CORE-START-LOG-FINISH`.
- `userStory`: `US1`, `US2`, or `US3`.
- `steps`: Short reproduction or validation steps.
- `result`: `PASS`, `FAIL`, `PARTIAL`, or `BLOCKED`.
- `evidence`: Screenshot paths and notes.
- `findings`: Related UX finding ids.

Validation rules:

- A `PASS` result must have either screenshot evidence or written notes.
- A `FAIL` result must describe expected and actual behavior.
- A `BLOCKED` result must state whether the blocker is app, device, or tooling.

## UX Finding

Represents an observed issue from manual or static validation.

Fields:

- `id`: Stable id such as `UX-001`.
- `surface`: Train, active workout, picker, History, Progress, Profile, or
  design-system component.
- `severity`: `P0`, `P1`, `P2`, or `P3`.
- `actual`: Observed behavior.
- `expected`: Desired behavior from the spec.
- `reproduction`: Steps or screenshot reference.
- `status`: `OPEN`, `FIXED`, `DEFERRED`, or `NOT_APP_DEFECT`.
- `resolution`: Commit/file reference or deferral reason.

Validation rules:

- `FIXED` findings must link to a code change or validation note.
- `DEFERRED` findings must include a reason and suggested future owner.
- `NOT_APP_DEFECT` findings must identify the platform/tooling source.

## Hardening Fix

Represents a scoped implementation change that resolves a finding.

Fields:

- `findingId`: UX finding being resolved.
- `filesChanged`: Source or documentation files changed.
- `behaviorChanged`: User-visible behavior change.
- `tests`: Automated or manual checks run.
- `risk`: Ledger/session/progress/platform risk assessment.

Validation rules:

- Must not add new product scope.
- Must preserve ledger integrity and local-first behavior.
- Must include automated gates scaled to the touched code.

## Validation Evidence

Represents screenshot or text evidence stored with the spec.

Fields:

- `path`: Relative path under `specs/011-milestone-ux-hardening/validation/`.
- `description`: What the evidence proves.
- `scenarioId`: Scenario the evidence belongs to.
- `timestamp`: Capture time when available.

Validation rules:

- Evidence paths must be stable repository paths, not temporary `/tmp` paths.
- Screenshots should be named by scenario and sequence.
