# Contracts: Routine-Aware Rest Timers

## Log Set Auto-Start

**Given** an active exercise has enabled rest configuration,
**when** a set is confirmed successfully,
**then**:

- The set is persisted before rest starts.
- `restStartedAt` is set to the logged time.
- `restEndsAt` is `logged time + durationSeconds`.
- `restOriginSetId` is the logged set id.
- A platform alert is scheduled when alerts are available.
- The RestBar appears without blocking the next set.

## Rest Disabled

**Given** an active exercise has rest disabled,
**when** a set is confirmed successfully,
**then** no active rest state is created and no alert is scheduled.

## Rest Adjustment

**Given** rest is running,
**when** the user taps +15 seconds,
**then** `restEndsAt` moves later by 15 seconds and alert scheduling updates.

**Given** rest is running,
**when** the user taps -15 seconds,
**then** `restEndsAt` moves earlier by 15 seconds. If the resulting duration is
zero or less, rest clears and pending alerts are canceled.

## Skip Rest

**Given** rest is running,
**when** the user taps Skip,
**then** rest fields are cleared and no completion alert fires for that rest.

## Routine Carryover

**Given** a routine exercise has rest configuration,
**when** a workout starts from that routine,
**then** the active exercise receives the same rest configuration.

## Restart Recovery

**Given** rest is running,
**when** the app restarts before `restEndsAt`,
**then** the RestBar shows remaining wall-clock time for the same origin set.

**Given** rest expired while the app was not active,
**when** the app restores,
**then** expired rest clears and the active workout remains recoverable.

## Deleted Origin

**Given** the current rest references a logged set,
**when** that set is deleted or undone,
**then** rest clears before the UI refreshes and future alerts are canceled.

## Finish Or Discard

**Given** rest is running,
**when** the workout is finished or discarded,
**then** the rest state clears, alert scheduling is canceled, and no resume
timer remains.
