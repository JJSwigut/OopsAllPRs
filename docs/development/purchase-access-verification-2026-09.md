# Purchase Access Verification (2026-09-20)

Local, uncommitted work on `swiggy/purchase-access-ux`, based on development
`4d7567c`. This does not include the separate backup-safety or Evidence Ladder
branches. No pricing, trial allowance, or paid-feature policy was changed.

## Verified

- `:shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease`
  passed with Android Studio JBR and the local Android SDK: 206 tasks, 1m29s.
- 429 tests passed, zero failures, errors, or skips.
- `git diff --check` passed.
- Installed the exact debug APK on EventideSmoke / emulator-5554 (API 35).
  Existing emulator data was retained, including prior demo workouts.
- Profile displayed an unavailable Google Play offer with no invented price.
  UI hierarchy confirms the purchase control is disabled. Retry and restore
  controls remain present. No real purchase was attempted.
- The exhausted-trial Train action opened the unlock modal. The long native
  store errors fit the portrait layout. Tapping Not now dismissed the modal
  and returned to Train navigation.
- Inspected screenshots at `build/smoke/purchase-access/profile.png` and
  `build/smoke/purchase-access/modal.png`; hierarchy proof is alongside them.

## Reconciliation Follow-Up

The same branch now also addresses asynchronous purchase reconciliation (B3).

- Added native observer invalidation and a shared conflated, serial reconciliation
  collector. Android Activity resume and iOS foreground/verified transaction events
  trigger fresh ownership reads, including purchases completing without a caller.
- Store operations and local persistence use separate locks. Network/checkout waits
  do not block local completion; older refresh results cannot race successful
  purchases or overwrite intervening free-workout counts. This is not completion
  idempotency and does not address audit B5's crash window or qualification policy.
- Native Android callbacks have bounded waits, cancellation cleanup, and Activity
  disposal. Cleanup is joined before returning; unrelated successful events leave
  the lifetime waiter intact. Google callbacks still lack a request identifier.
- StoreKit stages verified transactions and finishes only the batch associated with
  the exact saved snapshot token. Expired tokens report failure, not false success.
  Native Swift tasks can outlive a canceled shared call; direct success also emits
  an independent reconciliation signal. No iOS compilation/runtime proof yet.
- Background refresh updates access fields only, preserving settings editors/drafts
  and removing obsolete unlock dialogs or contradictory restore messages.
- Unavailable purchase buttons are visibly dimmed without changing their semantics.

### Red / Green Evidence

1. Focused `FullAccessReconciliationTest` and `AndroidBillingRequestLifecycleTest`
   ran against the extracted existing behavior: 21 tests, 13 failures. Confirmed
   stale refresh overwriting ownership, count/save races, unbounded reads, missing
   delivery completion, dropped unsolicited events, and stuck canceled requests.
2. App-state integration reproduced settings editor dismissal (one failure in two
   tests). A subsequent run reproduced the stale "No lifetime purchase" message
   after ownership was granted (one failure in 26 focused tests).
3. Final `:shared:testDebugUnitTest :androidApp:assembleDebug :androidApp:assembleRelease`
   passes: **472 tests, zero failures/errors/skips**, both APK builds, 206 tasks,
   1m16s. Red XML proof is retained in `build/verification/purchase-reconciliation/red/`.
   An intermediate app compile exposed a private billing SDK supertype in the public
   handoff API; replacing that inheritance with a private listener lambda fixed it.
4. Independent read-only review identified two Swift and two Android race cases;
   all were corrected and reread without further source-confirmed findings. This
   does not replace native/store testing.

### Runtime Evidence

Exact debug APK installed on EventideSmoke/API 35, preserving prior demo data:

- Cold launch, relaunch, Activity recreation and background/resume survived.
  Final relaunched process PID was 3962; AndroidRuntime error log was empty.
- Profile moved from loading to store-unavailable state and purchase was disabled
  in the UI hierarchy. No fabricated price was displayed.
- Open rest editor remained present before, immediately after, and after settling
  a background/resume cycle; XML evidence is in `editor-*.xml`.
- At 130% text size, the portrait modal renders without overlapping text. In
  landscape (2400x1080 pixels), its contents scroll to the visible Not now action;
  tapping it returns to Train. Screenshots were visually inspected.
- Proof directory: `build/smoke/purchase-reconciliation/`. Primary screenshots:
  `large-modal.png`, `landscape-modal.png`, `landscape-modal-bottom.png`.
- Font scale and rotation settings were restored after testing. No purchase was
  attempted; the emulator has no working Google Play store.

Platform guidance used for the implementation:
[Google billing integration](https://developer.android.com/google/play/billing/integrate),
[StoreKit transaction updates](https://developer.apple.com/documentation/storekit/transaction/updates),
[StoreKit finish](https://developer.apple.com/documentation/storekit/transaction/finish()).

## Remaining

- Blocked data-action modal device checks and successful retry/restore with a
  store-capable build remain unverified; shared state coverage is not store proof.
- The former Xcode/CoreSimulator blocker is resolved: a Release simulator app
  package now launches on iOS 26.5. StoreKit purchase/restore flows remain
  unverified; first-launch proof is not transaction proof.
- iOS-specific cancellation/late-result ordering, revocation during delivery, and
  delivery-token expiry need native tests. Swift changes are source-reviewed only.
- Android acknowledgment still precedes local persistence in the existing adapter;
  store restoration can recover entitlement, but this is not exactly-once delivery
  or server-side purchase verification. Sandbox pending/approve/refund/account-change
  acceptance is still required on both platforms.
- No live-store purchase, publication, merge, or cross-branch integration proof.

The emulator is shut down after this verification checkpoint.
