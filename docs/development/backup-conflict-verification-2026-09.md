# Backup Conflict Safety Verification

## Scope and State

Verified locally on 2026-09-20 in `swiggy/backup-conflict-safety`, based on
development `4d7567c`. Changes are uncommitted and have not been integrated,
pushed, or deployed. This closes the reproduced conflict-retention defect in
the local patch; it does not certify the entire backup system for release.

## Reproduction and Changes

- Reproduced two unauthorized-overwrite cases before the fix: repeated remote
  conflict checks followed by a local edit, and manual backup bypassing conflict
  detection. Both tests failed with one unexpected write.
- Unresolved remote changes now retain the last successfully synchronized
  baselines across checks, cancellation, coordinator recreation, and provider
  failures. Legacy rows with incorrectly advanced baselines retain their pending
  conflict outcome.
- Manual backup checks for remote changes, then writes the complete current
  snapshot. Review caught a preference-only regression in an intermediate patch;
  the real-SQL test failed with `CLEAN` instead of `LOCAL_WRITTEN`. It now verifies
  that pounds-to-kilograms changes survive backup and restore into another DB.
- Coordinator operations are serialized. Non-explicit overwrites recheck remote
  content after preparing the outgoing snapshot. Successful writes remember the
  revision actually written, not a newer local revision sampled after I/O.
- Restore rejects local changes made while exporting the safety copy. Tests cover
  both revision-changing edits and preference-only edits with unchanged revision.
- Profile preserves pending conflict actions and persisted provider errors across
  recreation rather than presenting a failed check as a clean state.

## First-Pass Proof

Final command, with Android Studio JBR and `ANDROID_HOME` set:

```sh
./gradlew --no-daemon :shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease
```

- Passed: 425 tests, zero failures, errors, or skips; both APK builds succeeded.
- `git diff --check` passed.
- Installed the debug APK on `emulator-5554` (EventideSmoke, API 35).
- Cold launch returned `Status: ok`; process PID 3393 remained running on a later
  check, with no `AndroidRuntime:E` entries. This is launch smoke only, not a
  document-provider end-to-end test.
- Independent review found no additional new regression after the manual-backup
  correction. Its recommended unchanged-revision safety-export test was added
  and is included in the final 425-test result.

## Content Identity and Restore Follow-Up

The next pass replaced count/timestamp identity with `snapshot-v1:` SHA-256 over
canonical snapshot content. The coordinator computes remote identity itself,
rather than trusting the producer's revision label. Preferences, sessions,
drafts, configuration content/order, and same-count/same-timestamp set corrections
now participate. Envelope metadata and harmless entity traversal ordering do not.

Four tests failed before integration: local preference-only detection, remote
content with an unchanged wire revision, envelope-only false conflicts, and
legacy baseline handling. Legacy stored baselines now require an explicit choice
once; cancellation/recreation cannot bypass that prompt. A real-SQL test verifies
that a released V1 backup restores and the next sync is clean without rewriting
the source file.

Review uncovered completed-circuit metadata loss in SQL restore; group fields now
round-trip. SQL snapshot capture also includes export metadata. Retained immutable
configurations remain in the database and in its content identity. Restore reports
a separate local baseline, so this legitimate difference does not automatically
overwrite the source backup.

Restore now unlinks the old backup inside the same transaction as data replacement.
Rollback preserves the old link. If post-commit state capture or relinking fails,
the app reports that data was restored and asks for relinking; it does not claim
the data transaction failed or silently continue using the previous file. Tests
cover returned/thrown metadata failures, post-commit read failure, cancellation,
and rollback. Independent read-only integration review found no new regression.

### Follow-Up Proof

- Full command passed: `:shared:testDebugUnitTest :shared:verifySqlDelightMigration
  :androidApp:assembleDebug :androidApp:assembleRelease` (207 tasks, 1m11s).
- XML reports: 451 tests, zero failures, errors, or skips.
- `tools/android_emulator_smoke.sh` passed against the exact debug APK on
  EventideSmoke/API 35, PID 3081. Inspected the nonblank launch screenshot at
  `build/smoke/backup-content-identity/launch.png`: workout home and existing demo
  templates rendered. This proves launch/rendering, not native backup interaction.
- The smoke script shut down its emulator; `adb devices -l` subsequently showed
  no connected devices. No build or review task remains running.
- `git diff --check` passed. No changes have been committed or published.

## Remaining Release Work

1. Snapshot reads span multiple repository queries and are not transactional.
   Post-restore baseline capture can observe concurrent local edits. A shared
   mutation boundary/transactional snapshot is still needed for that guarantee.
2. Providers expose no atomic compare-and-swap; another device can modify the file
   after the final read. The coordinator mutex only serializes one instance.
3. Local mutations are not transactionally coupled to the final safety check and
   restore. The existing narrow check-to-restore race needs a conditional restore
   or shared mutation boundary.
4. Existing SQL readers reconstruct some active/routine definition metadata,
   configuration source, and null equipment snapshots rather than persisting all
   incoming fields verbatim. Current SQL-generated round trips pass, but arbitrary
   supported backup DTOs can normalize. Audit persistence of those snapshot fields
   separately; do not hide their differences by excluding them from content identity.
5. Native Android document-provider permission/reopen/cancellation flows and
   iOS provider behavior have not been freshly verified. The former Xcode
   blocker is resolved by the iOS 27 release package and simulator launch
   gates; provider interaction coverage remains separate.

Next engineering work should address these recovery limitations before declaring
the backup lane complete. Store purchase recovery and launch review remain
separate pending work; no monetization or analytics policy changed here.
