# Research: Completed Workout History and Templates

## Decision: Completed Summary and History Detail Share One Projection

**Rationale**: The same read-only data is needed immediately after finish and
later from History. A shared projection avoids divergent formatting, missing
bodyweight fields, or PR marker mismatches.

**Alternatives considered**:
- Separate finish and History models. Rejected because it doubles mapping work
  and risks summary/detail drift.
- Display raw completed workout domain objects directly. Rejected because UI
  needs scan-friendly counts, labels, and PR markers.

## Decision: Save Templates From Completed Workout Evidence

**Rationale**: Completed workouts already contain logged exercises and sets.
Creating a template from that evidence supports the user's natural workflow:
log first, reuse later. Existing routine domain models already separate
planned targets from logged history.

**Alternatives considered**:
- Add a new template entity separate from routines. Rejected for this slice
  because reusable routines already represent template behavior.
- Require users to build templates manually before logging. Rejected because it
  slows the core gym workflow.

## Decision: Allow Duplicate Template Names

**Rationale**: Stable template ids distinguish records. Rejecting duplicates
adds friction and requires naming policy decisions that are not necessary for
local v1 behavior.

**Alternatives considered**:
- Enforce globally unique names. Rejected because lifters may intentionally use
  repeated names such as "Push" across variants.
- Auto-suffix names. Rejected because silent renaming can surprise the user.

## Decision: Launch Templates Through Existing Routine Lifecycle

**Rationale**: `WorkoutLifecycleUseCases.startFromRoutine` already creates a
new active workout from planned sets and leaves `loggedAt` null. Reusing that
path keeps history, templates, and active workouts separated.

**Alternatives considered**:
- Clone completed workouts directly into active workouts. Rejected because it
  increases risk of copying completed ids, timestamps, or PR markers.
- Build launch entirely in UI state. Rejected because active session creation
  belongs in shared domain lifecycle logic.

## Decision: Keep Train Empty-Start Primary

**Rationale**: The constitution prioritizes fast gym-side logging. Templates
should speed repeat workouts without hiding the fastest path for an unplanned
session.

**Alternatives considered**:
- Make templates the first Train screen. Rejected because it slows starting an
  empty workout.
- Move templates only to History. Rejected because launching a workout belongs
  in Train.

## Decision: Defer Editing and Deletion

**Rationale**: The feature closes the finish-review-save-launch loop. Editing
completed workouts or templates introduces additional ledger and confirmation
rules that deserve their own slice.

**Alternatives considered**:
- Include template edit/delete now. Rejected to keep the slice independently
  shippable and focused.
- Include completed workout delete/edit now. Rejected because data deletion
  has higher release-gate risk.
