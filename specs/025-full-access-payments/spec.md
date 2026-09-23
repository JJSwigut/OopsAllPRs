# Feature Specification: Lifetime Unlock Monetization

**Feature Branch**: `payments-pricing-options`

**Created**: 2026-06-20

**Updated**: 2026-09-22

**Status**: Implemented

**Input**: Users can download and use Oops All PRs for free until they complete a configurable number of workouts. After the limit, workout creation/logging requires a one-time $14.99 lifetime unlock through the platform store. The app remains account-free, subscription-free, ad-free, local-first, and backend-free.

## User Scenarios & Testing

### User Story 1 - Try Workout Logging Before Paying (Priority: P1)

As a new lifter, I want to complete several real workouts before paying so I can decide whether the logging loop is worth buying.

**Independent Test**: Fresh install, complete workouts until the configured free limit, verify each can be started/logged/finished/reviewed, then attempt the next workout and verify the lifetime unlock prompt appears.

**Acceptance Scenarios**:

1. **Given** a user has no lifetime unlock and zero completed free workouts, **When** they start and finish a workout, **Then** the workout is saved and their free workout count increases.
2. **Given** a user has completed one fewer than the free limit, **When** they start and finish another workout, **Then** the workout is saved and the next workout start requires unlock.
3. **Given** a user has reached the free limit without lifetime unlock, **When** they try to start an empty workout or routine workout, **Then** the app blocks creation and shows the lifetime unlock prompt.
4. **Given** a user is actively logging a workout that will reach the limit, **When** they finish it, **Then** the app saves the workout and gates only the next workout creation attempt.

### User Story 2 - Buy or Restore Lifetime Unlock (Priority: P2)

As a user who wants to keep using the app, I want to buy one durable unlock with no account, no subscription, and a visible restore path.

**Independent Test**: Reach the unlock screen from Profile and from a blocked workout start, verify the lifetime offer uses the store price, complete purchase in a store test environment, reinstall or clear entitlement cache, restore purchase, and verify workout creation is unlocked.

**Acceptance Scenarios**:

1. **Given** a user has no lifetime unlock at any trial count, **When** they open Profile or attempt a paid data action, **Then** they can choose the optional unlock without exhausting their free workouts. An available offer shows the store-provided price, `Unlock forever`, and `Restore purchase`.
2. **Given** a user buys the lifetime unlock, **When** the purchase succeeds, **Then** workout creation/logging and paid data tools unlock for that store ecosystem.
3. **Given** a user previously bought the lifetime unlock on the same store account, **When** they choose Restore purchase, **Then** the app restores the entitlement and removes the paywall.
4. **Given** a user cancels, has a pending purchase, or the store is unavailable, **When** the purchase flow returns, **Then** the app keeps existing data unchanged and shows a recoverable message.
5. **Given** the store has not returned a valid offer, **When** the unlock UI opens, **Then** no price is invented, purchase is disabled, and loading or a recoverable unavailable state is shown. Offer failure leaves retry and purchase restoration accessible.
6. **Given** purchase restoration succeeds without finding ownership, **Then** the app explicitly reports that no lifetime purchase was found for that store account rather than silently clearing the busy state.
7. **Given** a gated data action opens unlock UI, **When** access is granted, **Then** the unlock UI closes but the original data action is not automatically replayed. The user remains in control of export and restore.

### User Story 3 - Preserve Local Data Access (Priority: P3)

As a local-first user, I want my existing data to remain viewable after the free limit so the app still feels trustworthy.

**Independent Test**: Use an unpaid app state with saved workouts and the free limit reached, open History, Progress, and Profile, then attempt workout creation and paid data tools.

**Acceptance Scenarios**:

1. **Given** an unpaid user has completed workouts, **When** they open History or Progress, **Then** previously logged local data remains viewable.
2. **Given** an unpaid user is under the limit, **When** they export their human-readable data, **Then** the export proceeds without requiring an unlock. **When** they attempt import, backup link, Backup now, Sync now, or restore-from-backup, **Then** the app requires lifetime unlock before the data operation starts.
3. **Given** an unpaid user has reached the limit, **When** they attempt workout creation or a paid data tool, **Then** the app requires lifetime unlock and does not mutate data if dismissed. Export remains available.
4. **Given** a lifetime-unlocked user opens Profile, **When** they choose export, backup, sync, or restore-from-backup, **Then** the existing data tool flow proceeds normally.

## Requirements

- **FR-001**: The app MUST provide a local entitlement state that distinguishes free, limit reached, lifetime unlocked, and store unavailable/error states.
- **FR-002**: The app MUST make the free completed workout limit configurable in one shared place and default it to 10.
- **FR-003**: The free count MUST increase only when a workout is completed, not when a workout is started, discarded, resumed, edited, or reviewed.
- **FR-004**: The app MUST NOT interrupt an active workout because the user reaches the free limit; gating MUST happen before creating the next workout.
- **FR-005**: Users without lifetime unlock and at the free limit MUST be blocked from creating empty workouts and routine-based workouts.
- **FR-006**: Users without lifetime unlock MUST be able to view existing local workouts, progress, Profile settings, purchase status, and restore purchase actions.
- **FR-007**: Users without lifetime unlock MUST be able to export human-readable CSV data. They MUST NOT be able to import workout data, link a backup file, run Backup now, run Sync now, or restore from backup.
- **FR-008**: If an unpaid user attempts a gated action and cancels purchase, the app MUST leave workout data, backup link state, and sync state unchanged.
- **FR-009**: Lifetime-unlocked users MUST retain the existing workout creation, export, backup link, Backup now, Sync now, and restore-from-backup flows.
- **FR-010**: The unlock screen MUST offer only a one-time lifetime unlock.
- **FR-011**: Store-facing price shown in the app MUST come from an available platform-store offer. Missing, empty, failed, or invalid offers MUST NOT be replaced by a hardcoded price. Purchase remains disabled until a valid offer is loaded.
- **FR-012**: The lifetime unlock MUST permanently unlock workout creation/logging and gated data tools for the store ecosystem that reports the purchase.
- **FR-013**: Users MUST be able to restore purchases from Profile and from the unlock screen.
- **FR-014**: Restored purchases MUST unlock only the lifetime entitlement reported for the current platform store account.
- **FR-015**: The app MUST clearly communicate the one-time/no-subscription model and distinguish no app account requirement from the store account used for purchase restoration.
- **FR-016**: The trial/free usage state MUST remain local-only and MUST NOT require sign-in, app accounts, email, hosted entitlement service, device fingerprinting, subscriptions, or ads.
- **FR-017**: Payment failures, canceled purchases, unavailable store configuration, and restore failures MUST show recoverable messaging and MUST NOT change local workout data.
- **FR-018**: Full Access gating MUST preserve active session recovery, workout ledger durability, PR derivation, history viewing, progress viewing, settings, and local data ownership behavior.
- **FR-019**: Profile MUST expose optional purchase throughout the trial. Blocked paid data actions MUST provide a dismissible unlock path without stacking setup dialogs.
- **FR-020**: Local settings hydration MUST not wait for store offer loading. Offer requests MUST have a bounded timeout; failed or canceled store operations MUST release busy UI and permit retry.
- **FR-021**: Store transaction updates and foreground entry MUST trigger authoritative entitlement reconciliation independently of an open checkout or Profile screen. Signals MUST NOT grant access by themselves, cancel an in-flight persistence operation, or replay a previously blocked workout/export/backup action.
- **FR-022**: Entitlement-changing store operations MUST be ordered so an older refresh cannot overwrite a newer successful purchase. Local entitlement/count read-modify-write operations MUST be serialized without holding the persistence lock during network or checkout waits. This does not define completion idempotency or trial qualification.
- **FR-023**: Store failures MUST preserve cached ownership; authoritative successful store reads MAY revoke it. External cancellation MUST propagate, native wait resources MUST be released, and a purchase watchdog MUST state that the purchase may still complete.
- **FR-024**: StoreKit transactions MUST finish only after the corresponding entitlement snapshot has been saved. Delivery tokens MUST identify that exact snapshot; missing/expired tokens MUST yield a recoverable error rather than false completion. Delivery failure MUST NOT erase already persisted ownership.
- **FR-025**: Background access refresh MUST preserve open settings editors and drafts. It MUST close an obsolete unlock prompt and clear contradictory restore messages when ownership changes.

## Key Entities

- **Entitlement State**: Local access state including completed free workouts, configured free limit, lifetime unlock, store status, and last error.
- **Store Offer**: Store-returned lifetime unlock offer details, including localized price and terms.
- **Purchase Entitlement**: Store-reported ownership of `lifetime_unlock`.
- **Gated Action**: A user action that requires lifetime unlock, including workout creation after the free limit, importing workout data, backup link, Backup now, Sync now, and restore-from-backup. Human-readable export is deliberately not gated.

## Success Criteria

- **SC-001**: Fresh install starts in free mode with 0 completed workouts.
- **SC-002**: Unpaid users can complete workouts until the configured limit and are blocked from creating the next workout.
- **SC-003**: Users at the limit can still view previous workouts and charts.
- **SC-004**: Completing the lifetime purchase unlocks unlimited workout creation/logging.
- **SC-005**: Previously purchased users are unlocked after entitlement refresh or restore.
- **SC-006**: Canceled, pending, unavailable, failed purchase, and failed restore scenarios leave existing local workout data unchanged.
- **SC-007**: No account creation, login screen, subscription, ads, or backend dependency is added.
- **SC-008**: Entitlement business rules have unit test coverage.

## Assumptions

- Product ID is `lifetime_unlock`.
- Store price target is `$14.99`, configured in Play Console and App Store Connect rather than hardcoded as business logic.
- Deleting/reinstalling the app may reset local workout data unless existing backup behavior restores it.
- Cross-platform unlock is out of scope without app accounts or a hosted entitlement service.
