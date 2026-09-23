# First-Session Usability Study Kit

**Date:** 2026-09-22
**Status:** Ready to run on a verified build. This is a manual research kit,
not product telemetry and not evidence of retention, conversion, or revenue.

## Purpose

Find the next concrete usability defect in the workout logging loop before
adding remote analytics. This study tests whether a new participant can record
a first set, understand the rest state, correct a completed workout, reuse a
routine, and distinguish granular records from overall progress.

The study must use a synthetic fixture. Do not record participants' real
workout history, health data, payment information, or account identifiers.

## Guardrails

- Production remains local-only. Do not add an analytics SDK, event endpoint,
  crash reporter, or policy language as part of this study.
- Use a participant alias such as `P01`, not a name, email address, or device
  identifier.
- Separate first-use and repeat-session observations. Do not combine them in
  one completion or time metric.
- Record active interaction time only. Do not count physical exercise, actual
  rest, facilitator explanation, or deliberate reading time.
- Ask the prompt verbatim without naming a screen, button, or path. Offer help
  only after the participant has made an unprompted attempt.
- Stop immediately if a participant asks to stop. A difficulty is a product
  finding, not a participant failure.

## Before Recruiting

1. Choose a stable, verified build and record its Git commit, platform,
   operating-system version, device model, text-size setting, and weight unit.
2. Prepare fresh fixture states: no history; a first weighted set; active rest
   after a logged set; a saved circuit routine; a completed workout with two
   rep-specific records; and a completed workout eligible for correction.
3. Reset the fixture between independent participants. Check expected set,
   routine, timer, and record states before every session.
4. Recruit five to eight people who currently lift at least occasionally.
   Include people who have and have not used a workout tracker; report those
   groups separately.
5. Write the decision thresholds below into the study notes before the first
   session. Do not change them after seeing results.

## Proposed Decision Gates

These are provisional product-quality gates, not population claims.

| Task | Pass condition for an initial cohort of five | Escalation condition |
| --- | --- | --- |
| Record first set | At least 4 of 5 complete unaided, with no duplicate workout or unintended exercise; median active interaction time is at most 120 seconds. | Any data-integrity error, or fewer than 4 unaided completions. |
| Rest after logging | At least 4 of 5 correctly explain that rest begins after a logged set and intentionally adjust, skip, or continue it. | The rest state is mistaken for a required setup step, or it causes unintended set changes. |
| Routine reuse | At least 4 of 5 find the saved routine without creating an active workout until asked. | A participant cannot find it, or a routine starts unintentionally. |
| Post-workout correction | At least 4 of 5 find and save a correction without losing another set. | Any unexpected history mutation or lost data. |
| Progress interpretation | At least 4 of 5 identify a rep-specific record without claiming it proves overall strength improvement. | Participants consistently infer a strength verdict the screen does not support. |

If a critical integrity issue occurs, pause wider testing, reproduce it with the
fixture, and prioritize its fix over optimizing task time.

## Facilitator Opening

Read this before tasks begin:

> We are evaluating the app, not you. We will use made-up workout data. Please
> say what you are looking for as you use it. I will not guide you unless you
> ask or get stuck. You can stop at any time, and we will record only anonymous
> notes about what happened in the app.

Start the timer after the participant acknowledges the task. Stop when the
requested state is persisted or the participant abandons the attempt.

## Task Script

Run the first three tasks with a first-use fixture. Run repeat-use tasks only
in a separately labeled session after the participant has completed the
first-use flow.

| ID | Neutral prompt | Success evidence |
| --- | --- | --- |
| F1 | "You are ready to record your workout. Add the specified exercise and record one set at the stated reps and load." | One intended workout and one persisted target set; no unintended exercise. |
| F2 | "What changed after you recorded that set? Show what you would do if you needed a little more rest, then continue when ready." | Participant identifies rest, intentionally adjusts/skips/continues it, and knows it follows logging. |
| F3 | "You have not trained for a week. Find a sensible way to prepare your next workout without starting one yet." | Participant reaches a planning destination or saved routine without an accidental active workout. |
| R1 | "Keep this workout so you can use it next time, with this name." | Exactly one routine is saved with the expected target values. |
| R2 | "Start the saved workout and record the specified next-session set." | Correct routine opens and logging is intentional; original history remains unchanged. |
| H1 | "You realize one set in this completed workout is wrong. Correct it." | Target set changes and other history remains intact. |
| P1 | "What did you achieve here? What does this tell you, and not tell you, about your overall progress?" | Participant explains the granular record and keeps it separate from overall evidence. |

For F2, record the active interaction time around timer controls only; never
time a participant's real or simulated rest interval.

## Observation Sheet

Copy one row per task. Keep verbatim comments short and free of personal
workout details.

| Participant | Session type | Build / fixture | Task | Outcome | Active time (s) | Taps | First wrong turn | Assistance | Integrity issue | Verbatim observation |
| --- | --- | --- | --- | --- | ---: | ---: | --- | --- | --- | --- |
| P01 | First use |  | F1 | Complete / abandon |  |  |  | None / requested | None / describe |  |

Use `Complete unaided`, `Complete with help`, `Abandoned`, or `Blocked by
defect` for outcome. A wrong turn is a navigation or action that moves away
from the task; do not count intentional exploration twice.

## Debrief Questions

Ask after all tasks, not during them:

1. "What felt clear?"
2. "Where did you expect something different to happen?"
3. "What would you change first?"
4. "What would make you trust the progress information more?"

Do not ask leading questions about subscriptions, price, or health outcomes.

## Analysis and Next Action

Report raw outcomes, individual active times, and each requested-help event.
For each task, distinguish a copy/label issue, discoverability issue,
interaction issue, and persistence/data-integrity issue. Keep screenshots or
screen recordings only when participants have explicitly agreed and when they
contain no personal data.

Select one bounded follow-up only when it is supported by repeated observations
or a reproducible integrity defect. A small, convenience sample can identify a
next usability fix; it cannot establish retention, willingness to pay, or an
overall market result.

## Artifact Location

Store completed anonymous sheets beside this kit under
`docs/product-research/usability-sessions/`, using a dated filename. Do not
commit raw recordings or identifying information.
