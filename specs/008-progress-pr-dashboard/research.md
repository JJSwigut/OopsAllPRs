# Research: Progress and PR Dashboard V1

## Decision: Make Recent PRs the Primary Progress Surface

**Rationale**: The app's value proposition is PR recognition. A recent PR list
gives immediate motivation and makes the existing progress data useful without
requiring chart interpretation.

**Alternatives considered**:
- Lead with aggregate stats. Rejected because it weakens the product promise.
- Lead with charts. Rejected because V1 can be more useful with source-backed
  PR rows and no new charting dependency.

## Decision: Derive Display Rows From Progress + Completed Workout Data

**Rationale**: `PersonalRecord` already stores source workout/set ids and
record values. Completed workout data contains exercise display snapshots and
set evidence. Combining them creates readable rows without duplicating storage.

**Alternatives considered**:
- Store separate dashboard rows. Rejected because it creates stale derived
  state.
- Use progress records alone. Rejected because exercise names and source set
  details require completed workout context.

## Decision: Extend PR Derivation for e1RM and Volume

**Rationale**: The domain model already has record kinds for estimated one-rep
max and volume, but current derivation only materializes weight-for-reps and
bodyweight reps. Deriving all four requested kinds from completed ledger data
keeps the dashboard honest.

**Alternatives considered**:
- Render only existing records. Rejected because the feature explicitly calls
  out e1RM and volume.
- Compute e1RM/volume only in UI. Rejected because records need stable source
  ids and exportable progress evidence.

## Decision: Reuse Completed Workout Evidence Instead of a Separate Evidence Store

**Rationale**: Source evidence is already in completed workout history and set
rows. Looking up the source workout/set keeps PR traceability aligned with the
ledger.

**Alternatives considered**:
- Store snapshots on the PR row. Rejected because it duplicates ledger data.
- Hide evidence links when source data is old. Rejected because trust requires
  traceability or a clear unavailable state.

## Decision: Use Simple Trend Point Lists in V1

**Rationale**: Progress points already exist locally. A chronological list is
enough to validate trend data, source ids, and unit handling without adding a
charting dependency or visual complexity.

**Alternatives considered**:
- Add a chart library now. Rejected to avoid dependency and design-system scope
  expansion.
- Skip trend context entirely. Rejected because the spec requests basic
  progress trends.
