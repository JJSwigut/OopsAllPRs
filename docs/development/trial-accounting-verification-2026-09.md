# Trial Accounting and Workout Completion Verification

Date: 2026-09-21. Local branch: `swiggy/launch-integration`.

This is local implementation evidence, not store acceptance or launch approval.
Nothing was committed, pushed, merged, or published during this pass.

## Defects Reproduced

The original SQL-backed use-case regression suite ran seven tests with two failures:

- A first completed workout with no allowance row counted as two trial workouts.
- A saved workout counted as zero when notification cancellation threw afterward.

Original failing XML is retained in `build/verification/trial-accounting/red/`.
An additional observer-test failure during integration distinguished a changed API
contract from a product regression: two state mutations plus two identity transforms
are now verified separately, with no cancellation or overtaking of the first commit.

## Implemented Contract

- `finishActiveWorkout` writes completed history, clears recovery state, stores a
  source-workout receipt, and updates trial usage in one database transaction.
- Retrying a completed source returns the original saved workout, without another
  charge or another timer cancellation. Ambiguous historical duplicates are rejected
  without choosing or deleting a row.
- Receipts survive history deletion and backup restore. Deleting history does not
  refund usage; restoring an older active source already accounted for does not
  charge twice. Imported completed history does not itself consume trial usage.
- Schema 14 initializes missing legacy allowance from pre-mutation history once.
  Persisted allowance and entitlement values are retained. Prior import provenance
  cannot be reconstructed, so the historical fallback policy is unchanged.
- Entitlement updates transform transactional current state rather than replacing
  the allowance with a stale whole-row snapshot. SQLite WAL contention fails safely
  and can be retried; the implementation does not promise automatic busy retries.
- Cancellation before commit rolls back SQL changes. In-memory accounting also
  checks cancellation before mutations and after entitlement transforms.
- Timer/progress follow-up failures return explicit warnings alongside the committed
  workout. Cancellation still propagates. History can display the receipt without
  requiring another database read, and secondary refresh failures retain that view.
- Actual finish failures show a retry message. Successful completion clears the
  finish confirmation/error before another workout is hydrated. History destination
  refresh catches ordinary read errors without swallowing cancellation.

The ten-workout allowance, paid bypass, and current empty-workout counting policy
are unchanged. Free export/recovery and zero-set trial policy await owner decisions.

## Automated Evidence

- Final suite: 656 tests across 178 suites, no failures/errors/skips.
- Native Android API 35: 16 tests, no failures/errors/skips, using isolated test
  databases rather than the installed app's data.
- Shared tests, SQLDelight migrations, and Android debug/release builds passed.
- Final review added three regressions for History refresh failure/cancellation and
  finish-failure/success/new-workout state. The combined full/native/migration/debug/
  release rerun passed: 236 Gradle tasks in 1m21s (session 59945, exit 0).
- Original 78 source hashes across the three source worktrees remain unchanged.

Proof directories: `build/verification/trial-accounting/green/` and
`build/smoke/trial-accounting/`.

## Installed-App Evidence

The existing emulator database upgraded from schema 13 to 14 without clearing data:
19 workouts remained, allowance stayed at 10, and 19 source receipts were backfilled.
The Progress screen still displayed the existing demo evidence.

A temporary copy of that emulator database set usage to nine for a boundary check.
Through the real UI, a workout was started, Wide-Grip Pull-Up was added, and one set
of 12 reps was logged. Finish opened History with that exact exercise/set. The
database contained 20 workouts, 20 receipts, usage 10, and no active workout. The next
start opened the trial gate. Google Play billing was unavailable on this emulator;
no purchase or actual entitlement validation was attempted.

The original upgraded demo database was restored afterward and compared byte-for-byte
with its saved baseline. Notification permission was denied during the test prompt;
the newly set user decision flags were cleared afterward. No phone or store data changed.

Screenshots: `upgrade-launch.png`, `completed.png`, and `limit.png` in the smoke folder.
The post-completion screenshot also records minor existing copy polish to address:
singular counts currently render as `1 exercises` and `1 sets`.

Final APK SHA-256: `1067d04a7d61937674100ec98ebaf5b6e584c4c59e9b3205b50c1e58dd661409`.
That exact APK was reinstalled after the final review fixes and cold-launched in
1,406ms with the restored demo data, PID 4601, and no AndroidRuntime errors. Its
inspected screenshot/XML is `final-launch.*`. The completion/limit screenshots
precede only the final guarded-History-refresh and cleared-finish-state fixes;
those transitions have focused automated regression coverage.

The emulator was explicitly shut down after verification. Its host process exited
134 while saving the shutdown snapshot, after reporting `saving done`; this is not
an app crash or a test-run failure. All verification/build sessions are terminal.

## Remaining Boundaries

- iOS compilation/runtime and real Google/Apple purchase acceptance remain separate
  release gates; this Android/JVM evidence does not prove them.
- SQLDelight can invoke query listeners after commit. A throwing listener could
  produce an apparent failure after durable storage. No production listener
  registrations were found in this review; source-receipt retries remain idempotent.
- Postcommit cancellation cannot undo a saved workout; retry returns the receipt.
- This pass does not establish profitability, retention, pricing suitability, or
  public-launch readiness. Those require owner decisions, store gates, and real-user
  evidence.
