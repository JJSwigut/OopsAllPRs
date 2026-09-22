# Backup Atomicity Verification

Date: 2026-09-21. Local `swiggy/launch-integration`, base `4d7567c`.

## Required Invariants

- One backup package, summary, or revision describes one consistent database
  snapshot, including preferences, active/completed history, drafts, configurations,
  progress, and export metadata.
- Both user-facing restore paths pass the canonical identity of the exact safety
  package successfully saved to the document provider. A later/current identity
  must not stand in for data missing from that file.
- SQL checks that identity inside the same transaction as replacement. A mismatch
  leaves local edits, the existing link, and agreed baselines untouched.
- The exact restored local revision is captured before committing replacement;
  a later local edit cannot be acknowledged as already backed up.
- Unreadable persisted rows must cause a recoverable snapshot failure, not silent
  loss of a workout or circuit from a supposedly successful safety copy.
- Pre-commit snapshot/restore failure rolls back. A post-commit provider/link
  metadata failure remains an explicitly reported partial success.
- Archived definitions, overrides, and routines remain in the backup, while
  normal pickers continue hiding them. Multiple active workouts and orphaned
  recoverable rows stop capture instead of being silently omitted.
- Caller job cancellation is checked before work and before commit, without
  suspending inside SQLDelight's synchronous transaction callback.
- Removing a local active workout warns even when the incoming backup has no
  active workout; the original remains in the saved safety package.

## Reproduction

All commands run with Android Studio JBR and `ANDROID_HOME` set to the local SDK.

1. `:shared:testDebugUnitTest --tests '*BackupRestoreRevisionGuardTest'`:
   two behavioral failures before coordinator changes. Edits after the early
   safety check were overwritten by both file and conflict restore.
2. Focused `SqlBackupTransactionalSnapshotTest`, `SqlBackupSnapshotIdentityRoundTripTest`,
   `SqlBackupSnapshotIntegrityTest`, and `BackupRestoreRevisionGuardTest`:
   29 tests, 17 failures before SQL changes. A real second JDBC/WAL connection
   caused torn preference/export snapshots and active/completed double-counting.
   The stale guard was ignored; a post-commit edit entered the returned restored
   baseline. Capture failure/cancellation left replacement committed. Malformed
   rows were omitted from successful snapshots.
3. `:shared:connectedDebugAndroidTest
   -Pandroid.testInstrumentationRunnerArguments.class=com.jjswigut.oopsallprs.data.repository.NativeSqlBackupAtomicContractTest`:
   six tests on API 35, two behavioral failures before SQL changes. Native
   AndroidSqliteDriver ignored stale expected revisions, including a committed
   edit from a second connection/thread. Four round-trip/rollback/baseline tests
   passed, establishing a working native fixture.
4. Follow-up boundary tests plus Profile removal-warning tests: 11 tests, seven
   failures before the archive/cancellation/coverage fix. Archived definitions
   and overrides disappeared or invalidated history; archived routine templates
   disappeared; canceled jobs still performed snapshot/restore work; orphan
   logged sets were omitted. Profile warning tests and normal discard passed.
5. Native follow-up: nine tests, two failures, reproducing archived exercise and
   archived routine loss with normal store archive operations on API 35.
6. Isolated multiple-active test: one behavioral failure after clearing unrelated
   session state, proving a second active workout could be silently omitted.

One intermediate run failed to compile nullable-list assertions in new tests;
those assertions were corrected. That failure is not counted as behavioral proof.

RED reports: `build/verification/backup-atomicity/red/`, including native runner
XML/logs under `android-connected/`.

## Final Verification

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
ANDROID_HOME=/Users/swig/Library/Android/sdk \
./gradlew --no-daemon :shared:testDebugUnitTest \
  :shared:verifySqlDelightMigration :androidApp:assembleDebug :androidApp:assembleRelease
```

- Final full run: **613 tests in 171 suites**, zero failures/errors/skips;
  migration verification and Android debug/release builds passed. 207 tasks,
  55 seconds. XML retained under `green/full/` relative to the proof directory.
- Final API 35 native run: **nine tests**, zero failures/errors/skips; 74 tasks,
  19 seconds. Reports/logs retained under `green/native-final/`.
- Initial full run had one stale Profile fixture: it asserted replacement but
  supplied no incoming active workout. That fixture now supplies a real active
  workout, retaining distinct coverage for replacement and removal messages.
- Normal discard regression includes a logged exercise. Discard explicitly
  deletes child exercise rows, so cleanup does not rely on enabled FK cascades.
- Independent read-only review found no further blockers in these changes.
- Final debug APK installed with `adb install -r`, without clearing data or
  reseeding. Cold launch succeeded in 696ms; existing demo Progress content
  rendered. PID 4231 stayed live and its AndroidRuntime error log was empty.
  Screenshot/XML: `build/smoke/backup-atomicity/launch.png` and `launch.xml`.
- Debug APK SHA256:
  `f6aaf82e28cf9ff1184d4d9f09e39c280d9901dff98f5d7911253f14a09e20f0`.
- Original source-worktree manifest check: 78 recorded source hashes across
  75 paths still match. Existing source worktrees were not modified.

## Verification Boundaries

- First GREEN checkpoint: 31 focused tests across four suites, zero failures,
  errors, or skips. Native AndroidSqliteDriver verification: seven tests passed
  on API 35, including malformed-row rejection without mutation. Reports are
  retained under `build/verification/backup-atomicity/green/focused/` and
  `green/native-first-pass/`. These are not yet the final integration gate.
- Archived catalog/routine, cooperative cancellation, multiple-active detection,
  and Profile warning follow-ups are included in the final verification above.
- This is a backup of recoverable domain content, not a raw database image.
  Completed workouts intentionally omit unlogged content. Pre-existing corrupt
  or orphaned rows fail closed; automatic data repair is not implemented.
- Cancellation observed before commit rolls back; cancellation arriving after
  commit cannot imply that already committed data was rolled back.
- JDBC interleaving tests use two real connections to a temporary WAL database.
- Native tests use AndroidSqliteDriver and unique named databases owned by the
  dedicated instrumentation package, not the installed workout app's database.
  The cross-thread test joins a completed write; it is not arbitrary schedule
  stress testing. Connections close before those owned files are deleted.
- Synchronous SQLDelight transaction blocks must not suspend or bridge to
  `runBlocking`. Test fixtures may use `runBlocking` outside transaction callbacks.
  See [SQLDelight transactions](https://sqldelight.github.io/sqldelight/2.0.0-alpha05/native_sqlite/transactions/).
- Test infrastructure uses the documented
  [Kotlin Android source layout](https://kotlinlang.org/docs/multiplatform/multiplatform-android-layout.html)
  and [AndroidX test artifacts](https://developer.android.com/jetpack/androidx/releases/test).
- No cloud-provider compare-and-swap guarantee is created by these local changes.
  Concurrent remote writers and remaining metadata-fidelity gaps need separate
  acceptance work. Native iOS execution remains unverified while Xcode requires
  owner license acceptance.
- No public mutations, pricing/gate changes, telemetry, or actual user data
  modifications are part of this work.
