# Progress Evidence Units

Date: 2026-09-20

## Scope

Local Evidence Ladder worktree `0303`, based on development `4d7567c`.
No commits, publication, database migration, pricing, telemetry, or gate-policy
changes. Purchase/backup worktrees remain separate.

## Defect And Correction

Progress source rows previously embedded kilogram labels in domain projections,
even when the user's preference was pounds. Estimated strength sources also
showed only the originating set, not the estimate they contributed.

Sources now carry distinct typed measurements for estimated strength, completed
load-times-repetitions, or a completed session. UI presentation converts original
canonical values using the selected unit; it does not parse labels. Both overall
reading details and exercise detail disclosures receive the preference.

Original source loads retain two decimals, including 1.25 kg. Estimates and work
totals keep the existing one-decimal convention. PR source details also preserve
two-decimal loads; unrelated record/chart formatting defaults are unchanged.
Unit changes do not alter sources, percentages, comparison windows, or thresholds.

Explanations distinguish work performed from strength and describe the middle
(median) exercise percentage change without "normalized mature trend" jargon.
The actual comparison mathematics is unchanged.

## Automated Evidence

- Initial RED: four focused tests, three failures (kg-only estimate and work
  labels, missing original fractional set). Completed-session labels passed.
- RED XML: `build/verification/evidence-source-units/red/`.
- Added coverage for switching units while a reading or exercise is selected,
  preserving evidence identity, comparisons and selection; fractional PR source.
- Existing PR-source test was updated from one-decimal 220.5 lb to two-decimal
  220.46 lb for a 100 kg source. No record value or calculation was changed.
- Final `:shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease`
  passed: **456 tests, zero failures/errors/skips**, 206 tasks in 20 seconds.
  Earlier production build passed in 1m19s; a subsequent run correctly caught the
  old PR source expectation before the final passing run.
- Read-only reviewer found no critical remaining mismatch in this bounded slice.

## Runtime Evidence

API-35 `EventideSmoke`, `emulator-5554`; installed exact debug APK without clearing
demo history. SHA-256:
`ba0c475eff18673c0e1aeb01d6447a4cccec5a3e813ba4448eaf3ee790e61209`.

- Overall Capability source labels showed pounds, then changed to kilograms
  after using the actual Profile unit control. Reading selection, dates and
  changes remained intact. The latest source showed 11.5 kg estimated 1RM from
  9.07 kg x 8, matching the recorded 20 lb set after conversion.
- Exercise Capability detail showed kg; switching Profile back to pounds kept
  the selected exercise and showed 25.3 lb estimated 1RM from 20 lb x 8.
- Inspected standard portrait screenshots of both unit-specific reading sources
  and expanded exercise sources. At 130% text in landscape, inspected actual
  exercise comparison dates and Work capacity source rows, including
  `20 lb x 8` and `160 lb load x reps`, with no text overlap.
- Original pounds preference, font scale 1.0, auto-rotation 1 and rotation 0
  restored. Force-stop/relaunch passed, PID 3732; AndroidRuntime error log empty.
- Screenshots/hierarchies: `build/smoke/evidence-source-units/`. These include
  `lb-capability.png`, `kg-capability.png`, `exercise-expanded.png`,
  `landscape-work.png`, and `landscape-work-sources.png`.

## Acceptance Boundaries

Android emulator verification uses debug demo data only; no user data is cleared.
The former Xcode-license blocker is resolved by the iOS 27 simulator launch
and local StoreKit lifecycle checks in the integrated release gate. This
Android-focused evidence is still not combined interaction or public-release
acceptance.
No user-research or profitability outcome is claimed by these tests.
