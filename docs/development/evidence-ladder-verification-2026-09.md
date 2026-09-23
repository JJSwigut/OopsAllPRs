# Evidence Ladder Verification

Date: 2026-09-20

## Scope

Local uncommitted Evidence Ladder work in the Codex worktree `0303`, based on
development commit `4d7567c`. This is not integrated with the separate purchase
and backup worktrees and is not a release or store acceptance report.

The product contract is the main checkout's
`docs/product-research/progress-system-options-2026-08.md`: preserve specific
achievements independently from conservative, source-backed overall readings.

## Reproduced Defects

- Seeded Progress displayed unavailable Work capacity despite 16 sessions and
  53 days. A bodyweight/timed exercise or rotated exercise mix vetoed the whole
  comparison. Raw session volume also allowed large-volume exercises to dominate.
- Tapping Capability outside its info icon did nothing. The before and after
  tap hierarchy files were byte-identical.
- Demo tests checked only mature coverage, not whether an actual reading existed.
- Review found that combined capability date ranges obscured different actual
  per-exercise windows, and Consistency omitted its shared boundary session from
  the displayed recent interval.

## Corrections

- Compare completed load-times-reps within each stable exercise. Require six
  qualifying sessions spanning 28 days for each exercise independently, within
  the existing 56-day window ending at the latest completed workout.
- Aggregate the median of per-exercise percentage changes, not raw volume.
  Unsupported, sparse, or changed-configuration exercises do not veto unrelated
  mature exercises. Exclusions and source coverage remain explicit.
- Make entire overview reading rows tappable. Show source evidence directly in
  the detail view instead of requiring a second info-toggle interaction.
- Display contributing exercises' actual percentage changes and date comparisons.
  Work performed does not claim greater strength or physiological improvement.

## Verification Evidence

- Baseline debug build/shared tests succeeded with cached tasks.
- First focused RED run executed 29 tests with 7 failures: six work-capacity
  regressions and the strengthened demo assertion. XML is retained under
  `build/verification/evidence-ladder-ui/red/`.
- API-35 emulator `emulator-5554`, AVD `EventideSmoke`: installed the exact debug
  APK without clearing existing demo data. Captured Progress before and after.
- Corrected full-row tap opened Capability details. Source rows were visible
  immediately; tapping the latest triceps source opened the matching completed
  workout dated 2024-07-03 with its 20 lb x 8 set.
- At 130% text, portrait overview and landscape scrolling were inspected.
  A landscape Consistency row tap opened its detail view. Original font scale
  and rotation settings were restored.
- Runtime screenshots/hierarchies: `build/smoke/evidence-ladder-ui/`.

## Final Checkpoint

- Second RED run: 31 focused tests, precisely 2 disclosure failures; all 29
  work-capacity/demo tests passed. Disclosure XML is retained with the first RED.
- Final command (Android Studio JBR, local Android SDK):
  `./gradlew --no-daemon :shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease`.
  Passed in 1m21s, 206 tasks. XML totals: **450 tests, zero failures/errors/skips**.
- Independent source review confirmed both disclosure fixes and found no
  significant remaining issues in this bounded change. It was not native runtime
  or store acceptance.
- Exact final APK installed and inspected. Overview now reads Work capacity
  `Holding steady`. Its detail shows 4 contributing exercises, excludes 2
  unsupported configurations, and shows Bench Press +14%, Triceps 0%, Squat 0%,
  Deadlift -66.7%, with their separate actual date comparisons. Overall median
  remains 0%; this does not hide the individual changes.
- Final full-row navigation and source-workout tap passed. Scrolling exposes
  sources directly; the selected triceps row opened 2024-07-03 with 20 lb x 8.
- Final 130% portrait text/detail/comparison/source views were inspected with no
  text overlap. Earlier landscape checks verified the same row/detail/scroll
  structure before the per-exercise disclosure section was added; final landscape
  disclosure-section verification is not claimed.
- Force-stop/relaunch passed with PID 4229; AndroidRuntime error log empty.
  Original font scale (1.0), accelerometer rotation (1), and user rotation (0)
  restored. `git diff --check` passed. No history clear/reseed was performed.
- Final screenshots: `final-overview.png`, `final-work-capacity.png`,
  `final-large-detail.png`, `final-large-comparisons.png` in the smoke folder.

## Remaining Acceptance

- Native iOS compilation/runtime and live-store acceptance are not verified.
- Source-unit and landscape-disclosure follow-up completed in
  `evidence-source-units-verification-2026-09.md`; that newer proof supersedes
  the corresponding earlier acceptance gaps.
- Generalized capability for bodyweight/timed/assisted configurations remains
  deliberately unclaimed. Existing specific achievements still count.
- No publication, pricing, trial policy, telemetry, or recovery-policy change.
- User comprehension, retention, willingness to pay, and profitability require
  real user/commercial evidence. Passing this suite does not prove those outcomes.
