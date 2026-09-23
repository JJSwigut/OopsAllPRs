# Launch and Retention Decision Brief

**Date:** 2026-09-21
**Status:** Current product defaults and pending owner decisions. No unapproved
remote collection, price, or policy change is implied by this brief.

## Recommended Defaults

| Decision | Recommendation | Why now | What it does not authorize |
| --- | --- | --- | --- |
| iOS local build gate | **Resolved:** retain the iOS 27 simulator and local StoreKit checks in the release gate. | The development Mac now builds, packages, and launches the iOS app; local StoreKit purchase, fresh-session entitlement, and revocation tests pass. | TestFlight upload, App Store review, real sandbox purchase/restore, or a production release. |
| Rest timer system surface | Keep the outside-app rest timer **off by default**, with a visible opt-in setting. | A rest countdown is useful when the app is backgrounded, but a new user should not receive an unexpected notification permission request after logging their first set. | Notification permission without the platform prompt, or a claim that every platform has identical support. |
| Analytics | Keep production behavior **local-only** through the first usability study. | The product does not yet need remote event data to validate immediate comprehension, and its public privacy statement says no developer collection. | Remote telemetry, a provider account, policy changes, or any transmission of workout data. |
| $14.99 unlock | Label it an **early-adopter lifetime offer** while validating repeat use. | A permanent low-price lifetime promise is hard to reverse and there is no proven ongoing paid value layer yet. | A subscription, price increase, paywall expansion, or store-listing change. |
| Data ownership and recovery | Make **human-readable export free**; require Full Access for every import or restore path, automatic backup, and sync until the allowance model has durable identity. | A free restore or import currently lets a person reinstall, reset the local free-workout counter, and reconstruct their workout data. Free export preserves data ownership without creating that reset path. | Free in-app import or restore, cross-device sync, cloud storage, remote support, or a change to the logging allowance. |
| Progressive overload V1 | Ship **repeat last values only**; do not ship automatic weight increments. | The current configurable logging model correctly supports varied metrics but does not yet model success, rep ranges, equipment increments, or program intent. | Hiding the existing optional previous-value cue, or treating a PR as a prescribed progression. |

## Consequences

With these defaults, the next implementation work remains valuable and low
risk: improve source visibility for prior values, validate the Recent Training
review with lifters, fix iOS compile/runtime defects, and keep the logging loop
fast. It avoids converting a local-first tracker into a generic coaching app
or collecting more user data than the product has disclosed.

### Current Evidence for the Data-Ownership Decision

`FullAccessUseCases.checkGate` allows `EXPORT` for every local state and
requires Full Access for `IMPORT`, `BACKUP_LINK`, `BACKUP_NOW`, `SYNC_NOW`,
and `RESTORE_BACKUP`; the Profile messages tell a non-owner to unlock for
recovery operations. The purchase dialog advertises unlimited logging, backup,
restore, and sync rather than ownership of a readable export. This is a
deliberate commercial policy in code, not a missing UI.

The backup package deliberately excludes `full_access_state`. On a fresh
install, the local allowance state is initialized before restore from an empty
history. Restore replaces workout history but does not re-derive the allowance
counter. Therefore a free restore would currently allow a person to export a
backup, reinstall, regain the free allowance, then restore the same history.

Free CSV-style export is different: the application has no general CSV import
path, so it lets someone keep readable data without resetting their in-app
trial. Any future import path that reconstructs workout history must require
Full Access. It is the right near-term ownership promise for an account-free
product.

An account-free mitigation could make restore set the local allowance to at
least the number of restored completed workouts, but it only deters ordinary
use. Someone can still curate or edit a user-controlled backup. Fraud-resistant
trial accounting requires a durable identity or server-side record, which is a
separate product/privacy decision.

### Implementation Slice After Approval

The change is intentionally bounded:

1. Change `FullAccessUseCases.checkGate` so only `EXPORT` is allowed for every
   local state. Keep `WORKOUT_START`, `BACKUP_LINK`, `BACKUP_NOW`, `SYNC_NOW`,
   `RESTORE_BACKUP`, and every future import action exactly as they are.
2. Remove only the export preflight gate call in `ProfileStateHolder`; retain
   the restore, validation, safety-copy, conflict, and transactional behavior.
3. Update purchase copy so Full Access offers unlimited logging and backup/sync
   convenience, without claiming ownership of human-readable export.
4. Add red/green gate tests for a depleted free allowance: export succeeds;
   restore/import, creating or syncing an automatic backup, and starting
   another workout remain blocked.
5. Run the affected shared suite, Android assembly, and API 35 smoke using an
   exhausted-allowance fixture. Restore the emulator baseline afterward.

This does not require a data migration, entitlement-state rewrite, provider
account, new permission, server, store configuration, or public policy change.

The first later decision for guided progression should be a user-declared
comparison scope: `same routine` versus `any workout`. The second should be a
single success criterion for one weighted configuration. Do not make an
increment engine configurable until those terms have a stable data contract.

## Approval Form

The owner can approve the recommendations as written, or reply with a change
to any row. Each decision will then be recorded in the product contract before
code, privacy, store, or release work begins.
