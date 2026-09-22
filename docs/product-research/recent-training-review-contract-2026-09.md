# Recent Training Review Contract

**Date:** 2026-09-21
**Status:** Proposed implementation contract. This does not add analytics,
pricing, a score, or a recommendation engine.

## Purpose

Give a lifter a quick, honest answer to "what did I do recently?" without
turning granular personal records into a single claim about strength or
consistency.

This is a local read model over completed workouts. It complements the
Evidence Ladder; it does not replace or recompute it.

## Ubiquitous Language

- **Recent Training Window:** A device-local trailing seven-calendar-day
  interval. It includes today and the preceding six local dates.
- **Completed Session Fact:** A count, duration, or logged-set total derived
  from a workout whose `finishedAt` belongs to the window.
- **Window Achievement:** An existing granular personal-record achievement
  whose source completed workout belongs to the window.
- **Progress Reading:** An existing Evidence Ladder Capability, Work capacity,
  or Consistency result. Its own evidence window and maturity rules remain
  authoritative.
- **Recent Training Review:** The presentation-ready composition of a Recent
  Training Window, completed-session facts, window achievements, and existing
  Progress Readings.

`Adherence`, `missed workout`, and `on track` are intentionally not terms in
this model. The product has templates but does not yet model a user's planned
schedule or intent, so it cannot truthfully make those claims.

## Time Boundary

The review accepts an injected `now: Instant` and `timeZone: TimeZone`. A
platform boundary adapter resolves a half-open interval:

```text
startInclusive = start of the local date six days before now's local date
endExclusive   = start of the local date after now's local date
```

A workout is included exactly when:

```text
startInclusive <= workout.finishedAt < endExclusive
```

The UI label is `Last 7 days`, not `This week`, so locale-specific week-start
rules cannot silently alter the result. Boundaries are computed in device local
time, not UTC, including daylight-saving transitions.

## Read Model

The implementation should add a domain projection with this shape. Existing
domain types are reused rather than copied into another persistence model.

```kotlin
data class RecentTrainingWindow(
    val startInclusive: Instant,
    val endExclusive: Instant,
    val label: String = "Last 7 days"
) {
    init { require(startInclusive < endExclusive) }
}

data class RecentTrainingReview(
    val window: RecentTrainingWindow,
    val completedWorkouts: List<CompletedWorkout>,
    val personalRecords: List<PersonalRecord>,
    val progressReadings: List<ProgressionSummary>
)
```

Derived `completedWorkoutCount`, `loggedSetCount`, and `totalDurationMs` are
all zero for an empty window. `loggedSetCount` counts only
`ExerciseSet.isLogged` sets. The duration is the persisted
`CompletedWorkout.durationMs`; it must not be recalculated from set timestamps.

`personalRecords` preserve the existing granular record semantics. A lower-load,
higher-rep PR can still be celebrated independently from a higher-load,
lower-rep PR. The review must not deduplicate them into a synthetic best set or
use them to claim global strength improvement.

`progressReadings` are supplied from `EvidenceLadderSnapshot.overallReadings`
without changing their 28-day evidence window, qualification requirements, or
confidence. A review may say that an Evidence Ladder reading is still building
but must not call that a negative week.

## User Experience

The Progress overview should lead with a compact `Recent training` section:

```text
Last 7 days
3 completed workouts  ·  27 logged sets  ·  2h 14m
2 new records
```

`View records` opens a bounded Recent records list for the window. Each row
opens its existing source-backed PR detail; the review does not invent a second
achievement type. Existing Evidence Ladder cards remain separate and retain
their explanatory copy.

For an empty window, show:

```text
No completed workouts in the last 7 days.
Your history and progress are still here.
```

The only primary next action is `Train`. There is no warning, streak break,
missed-workout claim, prescription, or paywall in this first slice.

## Rules and Non-Goals

1. Include only completed workouts, keyed by `finishedAt`; active and discarded
   workouts are never review facts.
2. A workout that crosses midnight belongs to the date it was completed, not
   the date it was started.
3. Do not show a duration when persisted duration is unavailable or invalid;
   show the other facts instead. Do not manufacture elapsed time.
4. Do not send review data, timestamps, exercise names, or workout contents to
   a remote provider. This is local-only product behavior.
5. Do not create a new database table, migration, backup field, entitlement,
   permission, or public-store disclosure for this projection.
6. Do not alter personal-record derivation, the Evidence Ladder algorithm, or
   the current granular celebration policy.

## Acceptance and Proof

The projection needs focused tests for:

1. Exact lower-bound inclusion and upper-bound exclusion.
2. Device-local midnight and daylight-saving boundaries with injected time and
   timezone.
3. Empty, one-session, and multi-session totals, including only logged sets.
4. Exclusion of active, discarded, future, and stale completed workouts.
5. Preservation of every eligible source-backed PR achievement.
6. Separation from Evidence Ladder maturity and comparison windows.

Android proof should cover a normal populated review, an empty review, and a
320dp/130% text capture. It must use the established temporary fixture and
restore the smoke baseline byte-for-byte. iOS verification now has an available
Xcode 27/iOS 27 simulator gate; the remaining iOS acceptance is interaction
coverage and real-store purchase/restore behavior.

## Implementation Order

1. Add the pure time-window and review projection with injected time/timezone
   tests.
2. Expose the projection through `ProgressStateHolder` beside, not inside, the
   existing Evidence Ladder state.
3. Add the compact Progress overview section and source-backed navigation.
4. Run shared tests, Android assembly, API 35 proof, and the available iOS
   simulator build gate.
