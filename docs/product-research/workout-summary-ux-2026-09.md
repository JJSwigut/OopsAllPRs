# Workout Summary UX: Achievements and Repeat Value

**Date and source access date:** 2026-09-21

**Status:** The scoped receipt, granular achievement, correction, and template
reuse behavior is implemented in the current local integration and covered by
focused state/model tests. Visual runtime review and participant usability
evidence remain outstanding.

**Decision:** Make the post-workout screen a clear saved-workout receipt, an abundant specific-achievement recap, and an optional route to reuse. Keep Evidence Ladder interpretation separate.

## Scope and Evidence

Code inspection targets `/Users/swig/Development/oops-all-prs-worktrees/launch-integration`. Existing uncommitted changes belong to the parent and other lanes. This lane changes only this new report; it runs no builds, modifies no product code, and performs no public operations or subdelegation.

The research inputs were read from the MAIN checkout, not copied into this worktree:

- [Workout logging competitors](/Users/swig/Development/oops-all-prs/docs/product-research/workout-logging-competitors-2026-07.md).
- [Progress system options](/Users/swig/Development/oops-all-prs/docs/product-research/progress-system-options-2026-08.md).
- [Launch readiness](/Users/swig/Development/oops-all-prs/docs/product-research/launch-readiness-2026-09.md).

**Observed code facts** below describe source inspected on this date, not an executed app. **Competitor facts** describe accessible official documentation, not independently verified current binaries. **Recommendations and inferences** are proposals requiring verification. No retention improvement, conversion effect, participant result, or scientific confidence claim is established.

The parent reports fresh baseline screenshots labeled `before/01history`, `02summary`, and `03template`. This lane has not inspected their files and makes no runtime or visual claims from them. Parent-owned screenshot and implementation verification remain outstanding for these recommendations.

## Observed Code Facts

- `CompletedWorkoutSummary` contains duration, exercise/set counts, `prCount`, and exercise/set rows. `prCount` sums record markers, not distinct record-setting sets. One set can contribute several records. [HistoryModels.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryModels.kt)
- The detail header shows date, duration, exercises, and sets. A saved-workout
  notice remains visible after commit, and a distinct achievement summary says
  **X PRs across Y sets** before the ledger. Each source set keeps its own
  typed, unit-aware record markers. **Save as template**, **View progress**,
  and **Edit workout** are grouped near the receipt; deletion remains below the
  ledger behind confirmation. [HistoryFlow.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryFlow.kt)
- Completion first presents the committed workout without a dependent database read. That initial summary has no supplied record list; later History refresh supplies records. A completion warning can explicitly state that the workout saved despite secondary failures. Thus an initially empty marker list does not establish that no PR occurred. [HistoryStateHolder.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/history/HistoryStateHolder.kt), [AppState.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/AppState.kt)
- Saving a completed workout creates a new named routine from its recorded
  exercises and sets. The draft has explicit cancellation, rejects duplicate
  submits while saving, retains recoverable failures, and leaves the completed
  workout unchanged on cancellation. Success exposes the saved name with an
  **Open in Train** route that does not start a workout. [RoutineStateHolder.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/routine/RoutineStateHolder.kt), [AppShell.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/ui/navigation/AppShell.kt)
- Record derivation retains running bests, including separate weight-for-specific-repetition series. The summary should preserve those achievements rather than introduce a second, narrower eligibility rule. [PersonalRecordDerivationUseCase.kt](/Users/swig/Development/oops-all-prs-worktrees/launch-integration/shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/usecase/PersonalRecordDerivationUseCase.kt)

## Primary Competitor Sources

All sources below were accessed on **2026-09-21**. Documentation may lag shipping behavior; Strong's cited articles explicitly carry 2021 update dates.

| Source | Documented behavior | Scoped implication, not an outcome claim |
| --- | --- | --- |
| [Strong: Save or update a template](https://help.strongapp.io/article/177-update-template) | Completed workouts can become named templates, including from History. Existing-template choices distinguish keeping the original, updating values, and replacing structure. | Offer reuse after completion without silently changing a user's plan. Do not copy every prompt. |
| [Strong: Records screen](https://help.strongapp.io/article/216-exercise-records-screen) | Actual performances and predicted performances are distinguished in the records table. | Keep recorded facts and estimates visibly distinct; do not adopt competitor record limits as a new Oops rule. |
| [Hevy: Set records versus personal records](https://help.hevyapp.com/hc/en-us/articles/38279531346455-Set-Records-vs-Personal-Records) | PR medals appear on the source set and saved workout information. Exact-repetition set records are tracked separately and do not receive those medals. | Borrow source-linked visibility, not the restriction: Oops should continue celebrating granular rep-specific records. |
| [Hevy: Logging a workout](https://help.hevyapp.com/hc/en-us/articles/35361530647959-How-to-Log-a-Workout-in-the-Hevy-App-Step-by-Step-Guide) | A previously logged workout can be copied to start a new workout. | History can support repeat use. A new direct-repeat engine is not required for this scoped improvement; expose existing template reuse first. |
| [Hevy: Previous versus routine values](https://help.hevyapp.com/hc/en-us/articles/34105442929943-Previous-Workout-Values-Vs-Routine-Values-How-to-Adjust-in-Settings) and [routine update choices](https://help.hevyapp.com/hc/en-us/articles/38387296276375-Update-Routine-vs-Keep-Original-Routine) | Previous-session references differ from routine target values. Structural routine changes have update/keep choices; value behavior is controlled separately. | Do not imply that saving history automatically changes a reusable plan or that a previous value is a prescribed target. |

## Scoped Recommendations

1. **Lead with the saved receipt.** After successful completion, show **Workout saved**, date, duration, exercise count, and logged-set count. Historical visits can retain **Completed workout**. Keep the receipt visible during PR refresh and secondary failure. Model records as loading, available, or unavailable before showing a definitive zero-record message. Do not make secondary failure look like a failed save or invite a duplicate finish.
2. **Celebrate all specific achievements.** Put an exercise-grouped achievement section before the complete ledger. Keep both **7-rep PR: 25 lb x 7** and **8-rep PR: 20 lb x 8** when both qualify. Group several records under their source set, but do not suppress records to create a short highlight list. If displaying a total, use **X records across Y sets**, counting unique source sets separately. Give each achievement a direct route or scroll target to its matching set.
3. **Use precise metric labels.** Keep **Estimated 1RM** explicit. Describe set volume as load x reps with appropriate units, not strength gained. Do not use PR count or summed workout volume as an overall improvement score. Preserve existing domain eligibility and all granular achievements; a display change must not invent new strength claims.
4. **Make reuse an optional next step.** Move **Save as template** near the summary, keep editing secondary, and put deletion in an overflow action with existing confirmation. Use consistent terminology with Train. Provide an explicit cancel action for the naming draft. Saving should show the saved name and **Open in Train** (or the equivalent existing template destination), not silently return to an indistinguishable save action. Disable repeated submissions while saving; failure retains the draft. Cancel changes neither history nor routines. Do not start another workout automatically or add implicit routine replacement.
5. **Keep Evidence Ladder separate.** A secondary **View progress** destination can connect the summary to the existing evidence view. Do not place a new strength verdict beside today's PR count. Preserve the six-session/28-day maturity rule, neutral insufficient-evidence states, and source-qualified comparisons. A no-PR workout still receives a useful saved receipt and reuse actions; it is not a failed workout.

**Inference:** This hierarchy may make saved status, specific achievements, and the next reusable action easier to find. It is not evidence of better comprehension or improved retention. No new scoring, trial behavior, telemetry, ads, sharing, social features, or scientific interpretation is proposed.

## Manual Task Protocol

### Preparation

The parent selects the target participants and declares success criteria before testing. Do not infer a sample size or population effect from this report. Record build/worktree snapshot, platform, device, text size, weight unit, fixture identity, and whether the screen is fresh completion or reopened History. Use synthetic fixture data and reset to the same starting state between independent tasks. Baseline and candidate should use equivalent tasks; counterbalance their order when the same participants see both to reduce learning effects.

Prepare these cases: a first-use state with no workouts or templates; a first
weighted set with an active configured rest timer; two legitimate rep-specific
PRs; several record types on one set; no new records; first/insufficient
history; a post-break state whose newest workout is outside the trailing
seven-day review; mature Holding steady evidence alongside a granular PR; PR
refresh unavailable after successful persistence; a long mixed-exercise
workout; and a workout suitable for template reuse. Establish expected
achievements, source sets, routine counts, saved values, and timer state from
fixtures before observation. Do not fabricate a trend verdict just to fit a
task.

Ask tasks without naming the control to press. Record the first unprompted answer/action before offering help. Log assistance, errors, completion, abandoned attempts, taps, and interaction time manually. Separate active interaction time from physical exercise/rest and deliberate reading time. Report missing observations and failures, not only successful attempts.

| Task and neutral prompt | Expected evidence | Record |
| --- | --- | --- |
| First set: "You are ready to record your workout. Add the specified exercise and record one set at the stated reps and load." Do not name Train, Start, the exercise picker, or the log button. | Participant intentionally starts a workout, finds the correct exercise, changes values if needed, and records the requested set. No duplicate workout or unintended exercise is created. | Time from task start to first persisted set; taps; wrong destinations; accidental starts; value corrections; assistance; completed versus abandoned attempts. |
| Rest after first set: "What changed after you recorded that set? Show what you would do if you needed a little more rest, then continue when ready." | Participant identifies the rest state, can adjust or skip it intentionally, and understands that it follows a logged set rather than setup. | Correct explanation / attempts; time; timer adjustment or skip choice; accidental set changes; assistance. Do not count physical rest time as interaction time. |
| Return after a break: "You have not trained for a week. Find a sensible way to prepare your next workout without starting one yet." | Participant finds the Progress recovery action or Train, reaches templates and the empty-workout choice, and does not create an active session. | Correct planning destination / attempts; time; taps; unintended starts; first unprompted interpretation of the recent-training state; assistance. |
| Saved state: "What happened to this workout? Is anything still unavailable?" Repeat with a PR-refresh failure. | Participant distinguishes a persisted workout from unavailable record information and does not attempt to log it again. | Correct saved-state and refresh-state answers / attempts; duplicate-finish attempts; assistance. |
| Granular PR versus overall strength: "What did you achieve here? What does this tell you, and not tell you, about your overall progress?" Show both rep-specific PRs and separately the existing Holding steady or insufficient-evidence view. | Participant recognizes both specific records without treating them as contradictory, and does not conclude that either proves overall strength improvement. Insufficient evidence is not interpreted as failure or lost records. | Correct specific-record explanation and correct overall-evidence explanation separately / participants attempting the task; verbatim misconceptions without personal workout data. |
| Source-set lookup: "Show the exact set behind this record and tell me what was logged." Include a set with multiple markers. | Participant finds the correct exercise/set and reads the actual values; distinguishes an estimated metric from the recorded load/reps. | Correct source-set finds / attempts; time, taps, wrong-set selections, assistance. |
| Template cancellation: "Prepare this workout for reuse, enter a name, then change your mind without saving it." | Explicit exit restores the summary, creates no routine, and leaves the completed workout unchanged. Reopening the draft follows the declared cancel behavior without an accidental save. | Unaided cancellations / attempts; baseline versus final routine count and workout identity/content; accidental creations. |
| Template save: "Keep this workout so you can use it next time, with this name." | Exactly one routine is saved, with clear success and correct recorded exercise/set targets. Failed-save control retains the draft and does not announce success. | Successful saves / attempts; time, taps, corrections, duplicate routines, assistance; fixture comparison. |
| Open on Train: "Find what you just saved where you would use it for your next workout." | Participant opens the correct saved routine on Train; name/order/targets match. Merely opening it must not silently start a workout. | Correct destination and routine finds / attempts; time, taps, wrong destinations, unintended starts. |
| Repeat-session follow-through, separately consented: "Start the saved workout and record the specified next-session set." | Existing start flow is used intentionally. Planned/previous values are understood and can be corrected; the original History workout is unchanged. | Successful reuse / observed attempts; corrections per persisted set and interaction time. No inference about unobserved return behavior. |

### Acceptance and Interpretation

- Scripted correctness gates: no unexplained omitted eligible records; correct marker-versus-set counts; exact source-set mapping; no confirmed-zero message for unavailable records; no history mutation on template cancel; exactly one routine per successful save; correct Train destination without an unintended workout start.
- Parent visual checks: compare baseline and candidate History, summary, and template states; inspect long names, many records, both weight units, small screens, and large text for truncation, overlap, and displaced actions. Screenshots alone do not prove persistence or navigation behavior.
- Comprehension and usability metrics use explicit numerators/denominators. Report raw counts and individual task times for small studies; summarize distributions only with enough observations to make them meaningful. Choose decision thresholds before results, not after seeing them.
- Compare task execution with the same protocol and account for order effects. Any improvement is limited to the observed task/build/sample. No telemetry implementation, artificial participant counts, retention uplift, conversion claim, or population-level effect is authorized or established.
- For the first-set and post-break tasks, decide the acceptable completion,
  assistance, and interaction-time thresholds before recruitment. Record the
  build identifier and exact fixture for every observation, and do not combine
  first-use results with experienced-user repeat-session results. The results
  can identify a next usability fix; they do not establish retention, revenue,
  or willingness to pay.

## Handoff

The parent owns implementation, test execution, screenshots, runtime verification,
and participant consent. The next evidence-gathering step is to run the
first-set and post-break tasks on a verified build, then use observed errors or
assistance needs to prioritize one bounded usability fix. Keep domain evidence
rules unchanged. This report does not authorize telemetry, pricing changes, or
new health claims.
