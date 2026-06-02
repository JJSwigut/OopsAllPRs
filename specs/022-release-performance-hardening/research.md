# Research: Release Performance Hardening

## Decision: Enable Android R8 and Resource Shrinking Conservatively

**Rationale**: Android release builds currently define a release variant but do not enable minification or resource shrinking. Standard R8 plus resource shrinking is the expected baseline for release artifacts, but this app uses Compose resources, SQLDelight, notification receivers, and KMP-generated code, so keep rules and smoke tests are mandatory.

**Alternatives considered**:

- Maximum/aggressive custom optimization immediately: rejected because it increases runtime breakage risk before baseline release evidence exists.
- Leave Android unminified until store submission: rejected because release-size/performance issues should surface before submission.

## Decision: Add App-Owned R8 Rules

**Rationale**: Default AGP rules are not enough to document app-specific runtime boundaries. The release hardening pass should add an app `proguard-rules.pro` for platform entry points, notification receiver safety, Compose resources/KMP resource access, SQLDelight/database classes if needed, and file export paths discovered by validation.

**Alternatives considered**:

- Rely only on generated library consumer rules: rejected because app receivers/resources and release smoke assumptions need explicit ownership.

## Decision: Validate iOS Release Framework and Metadata Before Archive Automation

**Rationale**: The iOS project now links release framework paths for Release, but the previous release build could not complete because the machine ran out of disk. The first hardening step is repeatable simulator Release validation and release metadata/symbol checks; signed archive/distribution checks can follow when disk and signing assets are available.

**Alternatives considered**:

- Skip simulator Release and go straight to archive: rejected because local disk/signing constraints would block progress.
- Treat iOS as debug-only until Android is finished: rejected because the constitution requires iOS as a first-class product target.

## Decision: Make Disk Space a Release Validation Prerequisite

**Rationale**: The last iOS Release attempt hit 100% disk usage, leaving only hundreds of MB free. Release builds and archives need several GB free. Quickstart and tasks must require cleanup before running large build validations.

**Alternatives considered**:

- Keep retrying builds in the current environment: rejected because it risks partial artifacts and unusable workspace state.
