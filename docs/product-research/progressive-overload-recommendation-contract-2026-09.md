# Progressive-Overload Recommendation Contract

**Date:** 2026-09-21
**Status:** Research and proposed product contract. This document does not add
recommendation code, a training plan, analytics, pricing, or health claims.

## Product Question

After a lifter logs a workout, the app should help make the next comparable
session easier to start without silently turning a training notebook into an
opaque coach. A lower-load, higher-rep set can be a legitimate granular PR,
while still not being evidence that a person should add load next session.

The product must therefore distinguish an observed performance, a celebration,
and an optional next-session choice.

## Market Observation

[Hevy's previous-values behavior](https://help.hevyapp.com/hc/en-us/articles/36011896355479-How-to-Use-Previous-Workout-Values-to-Improve-Performance-in-Hevy)
lets people choose whether comparison comes from any prior workout or the same
routine. Its documentation correctly notes that different strength and
hypertrophy routines can make the same exercise values incomparable.
[Strong's Focus Metric](https://help.strongapp.io/article/226-focus-metric)
makes the comparison metric exercise-specific: volume, reps, time, or distance
relative to a prior workout. [Hevy Trainer](https://help.hevyapp.com/hc/en-us/articles/38385724273047-Hevy-Trainer-Explained-How-It-Builds-Your-Workout-Program)
places explicit load/repetition suggestions inside a guided-program product,
rather than presenting every logged set as a prescription.

These sources support a design principle, not their private algorithms: the
comparison context and metric must be visible before a progression suggestion
can be useful.

## Ubiquitous Language

- **Observation:** A source-backed performance fact from a completed workout,
  such as the last logged values or a Personal Record.
- **Comparison Context:** The exercise definition, immutable logging
  configuration, load role, and source scope used to determine whether two
  observations can be compared. A future scope may be `sameRoutine` or
  `anyWorkout`; it is never implicit.
- **Baseline:** The last completed, comparable workout performance selected for
  a next-session decision. It remains visible and linkable.
- **Progression Option:** One optional, user-editable set of starting values
  derived from a qualified baseline and an explicit strategy.
- **Strategy:** A user-controlled rule for a single configured exercise, such
  as repeat values, add a configured load increment, or add repetitions within
  an explicit range.
- **Outcome:** What the person actually logs. It does not retroactively change
  a template, strategy, or default merely because it was logged.
- **Insufficient Evidence:** The honest state when the app cannot make a
  comparable, explainable option.

`Prescription`, `optimal`, `recovery-ready`, `plateau`, and `strength score`
are intentionally not terms in V1. The application has no validated intent,
program adherence, recovery, injury, medical, or coaching model.

## Proposed V1: Repeat First, Offer One Transparent Choice

1. The active workout keeps the existing **Last logged values** as the default.
   That is an observation, not a target.
2. Only when a configured exercise has a qualified comparison context may the
   UI expose a secondary `Next-session option`.
3. A qualified option shows its source: `Based on <date> in <routine/any
   workout>`, the exact baseline values, and the strategy that produced it.
4. The only initial choice is **Use suggested values** versus **Keep last
   values**. Both paths open the normal editable logger. The app never locks a
   user into a suggestion.
5. V1 should use a repeat-first strategy by default. An increment strategy is
   opt-in per exercise/configuration and must state the increment, e.g. `Add
   5 lb when you choose to progress`.
6. If the baseline is incomplete, has a different configuration, has mixed
   measures, was manually backdated without a comparable source, or cannot
   meet the selected strategy's prerequisites, show no option. Keep the normal
   previous-value cue.
7. Saving a template or finishing a workout does not mutate persistent routine
   values or exercise defaults. Persistent changes remain an explicit user
   command, consistent with the current configurable-logging contract.

This deliberately provides assistance without saying a person failed when they
repeat, deload, change equipment, or choose another training objective.

## Comparability Rules

A baseline is eligible only when all of these match:

1. Same exercise definition or stable custom-exercise identity.
2. Same immutable logging-configuration identity and metric roles.
3. Same load interpretation, including bodyweight versus added load.
4. Same user-selected comparison scope, once that setting exists.
5. A completed source workout with logged facts sufficient for the strategy.

Configurations using repetitions and external load may eventually support a
load increment. Bodyweight-plus-added-load, time, distance, and effort fields
must each get a distinct strategy and acceptance criteria. V1 must not convert
one measure into another, infer a bodyweight value, or apply a barbell plate
increment to dumbbells, machines, or bodyweight movements.

## Increment Strategies Are Future Contracts

The product does not yet model a desired rep range, completed working-set
count, warmups, failure, RIR adherence, equipment increment, or program
intent. Therefore no automatic `increase weight` rule is safe to ship now.

Before enabling an increment strategy, its contract must answer:

1. What exact user-declared success condition qualifies a completed session?
2. Is the comparison against any workout, the same routine, or a user-chosen
   session family?
3. What increment applies to this configuration and available equipment?
4. Does the option repeat values after a deload, an incomplete session, or an
   explicit user override?
5. How do edits to completed history recompute or invalidate a pending option?

An implementation should carry the baseline source ID, comparison context, and
strategy version with a generated option. It must be regenerated from current
local data when opening a workout, rather than persisted as an unexplained
future obligation.

## Experience Examples

**Qualified weighted exercise**

```text
Last logged values
3 sets of 7 reps at 25 lb · Sep 18

Next-session option
Keep last values, or try 30 lb
Based on the same routine · load increment 5 lb
```

**Legitimate PR, no prescription**

```text
New record: 8 reps at 20 lb
Last logged values remain 7 reps at 25 lb
No next-session option yet
```

**Bodyweight exercise with added load enabled**

```text
Last logged values
10 reps · added load 25 lb

Next-session option unavailable
Choose a bodyweight-plus-load strategy in exercise settings
```

## Non-Goals and Safety Boundaries

1. No universal score, estimated recovery, injury advice, nutrition advice, or
   claim that a suggestion creates strength gains.
2. No hidden model, remote inference, or collection of workout history.
3. No paywall on recording, history, export, correction, or baseline values.
4. No automatic modification of a workout, template, routine, or exercise
   default.
5. No penalty or negative language for repeating, reducing load, skipping, or
   changing a session.
6. No replacement of the Evidence Ladder or granular PR policy. Those remain
   outcomes and celebrations, not progression instructions.

## Validation Before Implementation

Run a small usability study with people who use different loading styles. Test
whether they can answer, without help:

1. What values are merely historical versus suggested?
2. Why is the option available or unavailable?
3. Which source session produced it?
4. Can they ignore or edit it without believing they broke their plan?

Do not measure completion rate as proof the suggestions work. First verify
comprehension, appropriate override behavior, and absence of misleading claims.
Only after an owner selects a success rule and comparison scope should a narrow
strategy be implemented behind deterministic, source-backed tests.

### Neutral Participant Tasks

Use the preparation and observation discipline in
`docs/product-research/workout-summary-ux-2026-09.md`. Use synthetic data,
reset it between tasks, and record the first unprompted response, assistance,
wrong actions, and active interaction time. Do not tell a participant which
control is intended.

| Neutral prompt | Required understanding | Failure that blocks release |
| --- | --- | --- |
| "What did you do last time for this exercise?" | Finds the source-backed prior values and can identify the source session. | Calls the prior values a required target or cannot identify their source. |
| "What, if anything, is the app offering for this next session?" | Distinguishes a default historical value from an optional suggestion. | Thinks the app has silently changed a routine, current set, or personal record. |
| "Use the values you would prefer today." | Can keep the baseline, accept the option, or manually edit either without friction. | Cannot find a path to unchanged values or believes an override is an error. |
| "You did fewer reps or less load today. What should happen next time?" | Expects no penalty and can explain that a deload or alternate day remains valid. | Reads a negative judgment, missed-plan claim, or automatic escalation. |
| "Why is no option shown for this bodyweight-plus-load or timed exercise?" | Recognizes that an unsupported configuration is intentionally not guessed at. | Interprets the absence as a lost workout, broken record, or failure. |
| "Show the record you earned and explain whether it proves overall improvement." | Finds the granular record and keeps it separate from the Evidence Ladder. | Treats a lower-load/higher-rep PR as an automatic global strength verdict. |

Predeclare the following release gates: every participant must be able to keep
or edit prior values unaided; no participant may report that a suggestion
silently changes a routine; every unavailable state must name its comparison
limitation without medical or motivational judgment; and the source session
must be discoverable. If a gate fails, revise the language or interaction first
rather than broadening the recommendation algorithm.
