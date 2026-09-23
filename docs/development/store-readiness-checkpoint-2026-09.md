# Store Readiness Checkpoint

Observed 2026-09-21 through the signed-in in-app browser. Read-only inspection;
no fields changed, assets uploaded, declarations answered, or release submitted.

## Google Play

App: Oops All PRs (`com.jjswigut.oopsallprs.android`).

- [Default listing](https://play.google.com/console/u/0/developers/5909048689246924799/app/4973704910551253657/store-listings)
  is marked **Ready to send for review**, last updated July 14, 2026.
- Saved English title: Oops All PRs.
- Saved short description: "Log workouts fast, chase personal records, and keep
  your training data yours."
- Full description includes local-first logging, routines, previous values,
  rest notifications, history/PRs, custom exercises, backup/export, ten free
  completed workouts, and one-time Lifetime Unlock with no subscription.
- Assets present: one app icon, one feature graphic, six phone screenshots,
  two 7-inch tablet screenshots, and two 10-inch tablet screenshots. No video.
- Review step includes an AI asset declaration with neither option selected in
  the observed page. Recheck applicable requirements and asset provenance before
  the next listing save; no declaration was made during this inspection.
- [Publishing overview](https://play.google.com/console/u/0/developers/5909048689246924799/app/4973704910551253657/publishing)
  lists pending listing, content-rating, audience, privacy, ads, Data Safety,
  health-app, and category changes. These are pending changes, not proof of review
  acceptance. Send app for review is disabled pending dashboard steps.
- Dashboard reports **Draft app**, **Production inactive**, and **0 of 5**
  production tasks complete: select countries/regions; create a release;
  preview/confirm; send to Google for review; publish. Managed publishing is off.

## Apple

[App Store Connect](https://appstoreconnect.apple.com/apps/6791339090/distribution)
redirects to login. Current listing assets, submission requirements, production
build status, and acceptance are unverified. TestFlight availability does not
establish public App Store release readiness.

The former local Xcode/CoreSimulator gate is resolved. On 2026-09-22, the
Release simulator app package built and launched on a fresh iOS 27 simulator
without a crash marker. The local StoreKit fixture also passed purchase,
fresh-session entitlement, and refund/revocation lifecycle tests. This does
not verify App Store Connect state, TestFlight delivery, App Store review, or
StoreKit sandbox behavior with an Apple account.

The same host now also produces an unsigned `iphoneos` Release archive that
contains the `RestTimerLiveActivityExtension`. The project resolves only the
arm64 shared framework for device archives and only the simulator framework for
simulator builds; the Release packaging script asserts both paths before its
normal simulator package build. This proves local architecture compatibility,
not distribution signing. The remaining Apple delivery prerequisite is an App
Store provisioning profile for
`com.jjswigut.oopsallprs.ios.RestTimerLiveActivity` and its
`IOS_LIVE_ACTIVITY_PROVISIONING_PROFILE_BASE64` GitHub secret.

On 2026-09-22, GitHub repository-secret inspection found the main-app signing
secrets but not `IOS_LIVE_ACTIVITY_PROVISIONING_PROFILE_BASE64`. The local
provisioning-profile inventory likewise contained only `Oops All PRs App
Store` for `com.jjswigut.oopsallprs.ios`, not the Live Activity extension.
The prior successful TestFlight run predates the extension-profile requirement;
it is not evidence that a current Live Activity build can upload.

## Before Public Release

- Finish local backup safety verification and extend native iOS proof from first
  launch to core interactions and StoreKit sandbox purchase/restore behavior.
- Review refreshed screenshots/copy against the actual shipping build, including
  progress evidence, configurable logging, and payment/recovery disclosures.
- Obtain owner decisions on release markets and the free data-recovery policy.
- Verify real Apple/Google sandbox purchase, restore, and entitlement delivery.
- Select the exact verified build, inspect store-specific validation, and obtain
  explicit submission/publication authorization. Managed publishing being off
  matters when choosing submission timing.

No live commercial performance or profitability is established by this check.
