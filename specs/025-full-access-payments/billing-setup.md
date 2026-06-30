# Lifetime Unlock Billing Setup

This feature is wired for native store purchases, but purchase flows cannot succeed until matching products exist in the stores.

## Product ID

The app uses one shared product ID:

- Lifetime unlock: `lifetime_unlock`

Change it in one place if the store record must use a different ID:

- `shared/src/commonMain/kotlin/com/jjswigut/oopsallprs/domain/model/FullAccessBillingConfig.kt`

Android and iOS both read this shared constant.

## Google Play Console

Create one non-consumable in-app product for the Android release package `com.jjswigut.oopsallprs.android`:

- Product ID: `lifetime_unlock`
- Title: `Lifetime Unlock`
- Price: `$14.99`
- Status: active
- License testers for purchase, cancel, refund, restore, and unavailable-store checks

Validation checklist:

- Product lookup returns the localized store price on the unlock screen.
- Purchase unlocks workout creation/logging immediately after Google Play confirms it.
- Purchase acknowledgement succeeds.
- Restore purchase unlocks when the current Google account owns `lifetime_unlock`.
- Canceled, pending, unavailable, and failed purchases leave local workout data unchanged.

## App Store Connect

Create one non-consumable in-app purchase for bundle ID `com.jjswigut.oopsallprs.ios`:

- Product ID: `lifetime_unlock`
- Reference name: `Lifetime Unlock`
- Price: `$14.99`
- Status: available for sandbox/store testing
- Sandbox testers for purchase, cancel, refund, restore, and unavailable-store checks

Validation checklist:

- Product lookup returns the localized store price on the unlock screen.
- Purchase unlocks workout creation/logging immediately after StoreKit confirms it.
- Restore purchase unlocks when the current Apple account owns `lifetime_unlock`.
- Canceled, pending, unavailable, and failed purchases leave local workout data unchanged.

## Current Boundaries

- No app account, login, subscription, ads, cloud entitlement service, or backend is included.
- The free workout count is local and persists across normal app restarts and updates.
- Deleting/reinstalling the app may reset local app data unless restored through existing device backup behavior.
- Purchase restores are same-store only: Apple account on iOS, Google account on Android.
- Client-side store entitlement checks are less tamper-resistant than server receipt validation, by design for the account-free V1.

## TODO Before Store Release

- TODO: Create the Google Play non-consumable product `lifetime_unlock` and activate the $14.99 price.
- TODO: Create the App Store Connect non-consumable product `lifetime_unlock` and activate the $14.99 price.
- TODO: Confirm the Apple Paid Apps Agreement, banking, and tax setup are complete before relying on sandbox/store testing.
- TODO: Add the iOS In-App Purchase capability to the release app identifier/provisioning profile if App Store Connect or Xcode reports it missing.
- TODO: Run sandbox/license-tester checks for purchase success, cancel, restore, refund/revocation behavior, unavailable store, and localized price loading on both platforms.
- TODO: Capture the final paywall screenshot and review notes required for the first App Store in-app purchase submission.
