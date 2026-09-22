# Workout Summary Verification

Date: 2026-09-21. Local, uncommitted work on `swiggy/launch-integration`, based on `4d7567c68d916850d2dfd3b972e42667f934974c`. No release, store submission, policy change, or new telemetry.

## User Flow

- History: compact heading and rows retain local date/time, distinguishing same-day sessions.
- Completed workout: duration and counts lead into a positive PR recap, with each record attached to its source set. Estimates remain labeled. Granular rep-specific records remain valid; the recap is not an overall-strength verdict.
- Reuse: Save as template has explicit Cancel, required-name validation, busy controls, and named success confirmation. The saved template appears on Train.
- Corrections: Edit and Delete remain available but secondary. Delete retains confirmation. Existing editor semantics are unchanged.
- Navigation: compact tab labels no longer wrap mid-word; the landscape rail scrolls to destinations that otherwise fell below its bounds.

## Implementation Boundaries

History dates now use the same local timezone for list/detail. Fractional load displays retain up to two decimal places; estimated 1RM and volume remain rounded to one. Duplicate record rows do not inflate achievements, while distinct metric/source/value identities remain separate. Sparse stored set positions are not rewritten.

Template save merges the committed result into local state instead of making success depend on a second repository read. In-flight duplicate calls and draft changes are rejected. Cancellation propagates and uncertain outcomes retain guidance to check Templates before retrying. This is not cross-process idempotency: an interrupted, already-committed save can still require user reconciliation.

Review found an enabled-but-ineffective Save action when navigating from saving workout A to B. History now exposes the outstanding save and disables conflicting saves. Read-only re-review found no further issues in this slice. That delayed-save display was source-reviewed; state-layer deferred-save behavior is automated, not a device-injected slow-save test.

## Automated Proof

Final command:

```sh
env JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
  ANDROID_HOME=/Users/swig/Library/Android/sdk \
  ./gradlew --no-daemon :shared:testDebugUnitTest :design-system:testDebugUnitTest \
  :shared:verifySqlDelightMigration :androidApp:assembleDebug :androidApp:assembleRelease
```

Session 79484 completed successfully: 212 tasks, 1m18s. Shared: 674 tests / 180 suites. Design system: 18 tests / 4 suites. Zero failures, errors, or skipped tests. XML retained under `build/verification/workout-summary/final-shared` and `final-design-system`.

New coverage includes timezone boundaries, singular/plural labels, fractional loads, source filtering, marker deduplication, sparse positions, 25-for-7 then 20-for-8 records, template cancellation, concurrent save prevention, uncertain exceptions, cancellation, and success without a dependent list read. These do not establish user comprehension or commercial impact.

## Android Runtime Proof

API 35 `EventideSmoke`, emulator-5580, installed without clearing app data. Final debug APK SHA-256:

`643fede481a27889231f38f89399c9c049f95bdde9579df2e21fac79ab7e08ca`

Baseline and fresh screenshots/XML are under `build/smoke/workout-summary/`. Inspected flows:

1. History to four-exercise summary: 4 PRs across 2 sets; original achievements/values preserved and date consistent.
2. Template cancel and reopen; blank-name validation; successful named save; Train shows exactly one new template with 4 exercises / 4 planned sets.
3. Delete confirmation then Cancel; editor entry then Cancel. No actual workout deletion or correction in this smoke.
4. 320dp-wide portrait at 130% text: summary, scrolling, template form, and fixed one-line History tab label.
5. 640dp-wide landscape at 130% text: summary/scroll and navigation rail. Scrolling the rail exposes Profile; tapping it opens Profile.
6. Final normal-size summary visually inspected. App alive at PID 6262; crash buffer empty.

Key captures: `after/07-template-saved.png`, `17-small-nav-fixed.png`, `18-small-template.png`, `19-rail-scroll.png`, `21-final-history.png`, and `22-final-summary.png`. `11-small-summary.png` actually captures the History list after Android configuration recreation; `12-small-detail.png` is the corresponding detail view.

SQLite integrity was `ok`. Completed workouts, exercise sets, and personal records had zero bidirectional row differences before/after template/edit-cancel/delete-cancel flows. Original 19-workout/2-template demo database restored byte-for-byte (`cmp` passed) after temporary template creation; font scale, size, and rotation settings restored. Emulator session 82406 exited 0. Original integration sources remain unchanged: all 78 source hashes across 75 paths verified.

## Remaining Acceptance

The former Xcode-license blocker is resolved: iOS 27 simulator launch and local
StoreKit lifecycle verification now pass in the integrated release gate. This
pass still does not establish full accessibility compliance, iOS layout
correctness, store purchase acceptance, or improved retention. Research and a
neutral participant task protocol are in
`docs/product-research/workout-summary-ux-2026-09.md`. Public release and
commercial success remain unproven.
