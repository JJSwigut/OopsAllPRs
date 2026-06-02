# Data Model: Android Alpha Hardening

## Alpha Smoke Checklist

Represents the repeatable manual validation gate for internal Android dogfooding.

Fields:

- `flow`: Train, active workout, exercise picker, rest timer, history, progress, profile, export, or recovery.
- `setup`: Initial app/device state required for the step.
- `action`: Tester action.
- `expectedResult`: Observable outcome.
- `evidence`: Screenshot, note, command output, or deferred reason.
- `outcome`: Pass, fail, blocked, or deferred.

Validation:

- Every checklist item must have an expected result.
- Blocked or deferred items must record why and what fallback evidence was used.
- Core workout flows must include at least one weighted set and one bodyweight reps-only set.

## Local Readiness Status

Profile-facing model summarizing local ownership and alpha readiness.

Fields:

- `storageLabel`: Local data storage claim.
- `syncLabel`: Cloud sync status.
- `backupLabel`: Android backup eligibility claim.
- `restNotificationLabel`: Rest notification behavior and permission expectation.
- `exportLabel`: Export availability.
- `alphaGateLabel`: Manual alpha validation state.

Validation:

- Must not imply cloud sync exists.
- Must clearly identify local/offline ownership.
- Must fit in compact Profile layout without overlapping controls.
- Must not expose debug-only jargon to normal users.

## Recovery Validation Evidence

Represents automated and manual proof that recovery-critical state survives app lifecycle interruptions.

Fields:

- `activeWorkoutId`: Stable id of the workout under test.
- `startedAt`: Workout wall-clock start.
- `restEndsAt`: Optional rest wall-clock end.
- `hydratedAt`: Wall-clock time used for restoration.
- `route`: Route or resume affordance expected after hydration.
- `notificationAction`: Scheduled, canceled, ignored, or unavailable.
- `result`: Pass/fail evidence.

Validation:

- Expired rest recovery must preserve `activeWorkoutId`.
- Expired rest recovery must clear `restEndsAt`.
- Expired rest recovery must cancel pending notification state.
- Unexpired rest recovery must preserve remaining rest and resume affordance.
