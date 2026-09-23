# Store Deployment Setup

GitHub Actions has three workflows:

- `CI`: runs the release-equivalent Android checks and an iOS Release simulator
  package for pull requests and pushes to `development` or `main`.
- `Deploy Stores`: runs `tools/release_gate.sh --skip-ios` on pushes to `main`, uploads Android release artifacts and release-gate proof files to GitHub Actions, creates a GitHub Release, attempts a timeout-bounded iOS Release simulator package, uploads Android to Google Play internal testing when secrets exist, and uploads iOS to TestFlight once Apple signing secrets exist.
- `Google Play Listing`: a manual-only workflow for validating or uploading Android store-presence metadata and artwork. It has no push or pull-request trigger.

Manual dispatch accepts:

- `release_tag`: optional GitHub Release tag override, such as `v1.0.1001`.
- `publish_stores`: attempts Google Play/TestFlight uploads when secrets are present. Store jobs also run on `main` pushes.

## GitHub Release Package

Every successful `main` push creates a GitHub Release with an automatic `v1.0.<run-number + 1000>` tag and attaches available release packages:

- Android release APK.
- Android release AAB.
- Android R8/resource shrink mapping files.
- Release-gate proof files from `build/release-gate/`.
- iOS Release simulator zip from `tools/ios_release_package.sh` when the best-effort iOS package job succeeds.

The deployment iOS package job is best-effort only to protect Android/GitHub
artifact availability from GitHub macOS runner infrastructure failures. Local
and pull-request CI both require a successful iOS Release simulator package;
investigate a failed package job before treating an iOS release as ready.

Before promoting `development` to `main`, run `tools/release_gate.sh --android-smoke`. The iOS portion packages the Release app and runs the local StoreKit entitlement lifecycle smoke. Successful local release gates write `build/release-gate/summary.txt`, `build/release-gate/artifacts.txt`, and `build/release-gate/proof.env` for PR or release notes.

## Android

For local Fastlane checks, use Ruby 3.1 or newer and install the bundle into
`vendor/bundle`. The repo includes `.ruby-version` for Ruby version managers,
and the bootstrap script auto-detects Homebrew `ruby@3.3` or `ruby@3.4` when
the shell still points at macOS system Ruby:

```sh
tools/bootstrap_fastlane.sh
tools/fastlane.sh lanes
```

The macOS system Ruby 2.6 is too old for the repo Fastlane environment.

The Android store upload expects these GitHub secrets:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

The Google Play service account needs Play Console access for:

- `View app information and download bulk reports (read-only)`
- `Release apps to testing tracks`
- `Manage store presence` for listing image and screenshot uploads

The deploy workflow skips Android store upload with a GitHub Actions notice until all required Android secrets are present. The Play Console app must exist before the first Fastlane upload. Use package name `com.jjswigut.oopsallprs.android`.

Validate the source-controlled listing dimensions and prepare the exact Supply directory without contacting Google Play:

```sh
ruby tools/validate_google_play_assets.rb
tools/fastlane.sh android listing_package
```

Then validate the complete Google Play listing edit through the Play API:

```sh
ANDROID_PLAY_VALIDATE_ONLY=true tools/fastlane.sh android listing
```

The lane assembles `fastlane/build/metadata/android` from the source-controlled English listing copy, Play icon, feature graphic, phone screenshots, seven-inch tablet screenshots, and ten-inch tablet screenshots. The generated directories follow Fastlane Supply conventions (`en-US/images/phoneScreenshots`, `sevenInchScreenshots`, and `tenInchScreenshots`). Validation is the lane default and asks Google Play to validate the edit without committing it.

Before preparing the upload, the lane also rejects empty or over-limit listing
copy and validates the source-controlled Google Play icon, feature graphic,
phone screenshots, and App Store iPhone screenshots against their expected PNG
dimensions. Run the local equivalent with:

```sh
tools/fastlane.sh android listing_package
```

Fastlane 2.237.0 requires listing-only uploads to identify an existing release even when changelog upload is disabled. Before opening the listing edit, the lane reads version codes from the `internal` track, selects the highest existing version code, and passes that track and version code to Supply. Set `ANDROID_PLAY_LISTING_TRACK` to another populated track only when necessary. Both validation and upload fail before changing listing data if the selected track has no existing release.

To commit only the listing edit, explicitly opt into upload mode:

```sh
ANDROID_PLAY_VALIDATE_ONLY=false tools/fastlane.sh android listing
```

The existing release lookup does not upload a binary, changelog, or release. The lane never builds or uploads an APK/AAB and never changes a release track. It requests `changes_not_sent_for_review` and enables Fastlane's compatibility retry: if Play says that query parameter must not be set, Fastlane retries the edit commit without it; if Play requires the parameter, Fastlane retries with it enabled. For this app's current Play publishing configuration, a successful listing commit saves the changes under **Changes not yet submitted for review** in Publishing overview. It does not press **Send app for review**; that remains a separate manual owner action. Upload mode is still a Google Play store mutation and requires explicit owner approval.

The `Google Play Listing` GitHub Actions workflow is `workflow_dispatch` only. Its `mode` input defaults to `validate`; selecting `upload` commits the same listing-only edit. Both modes use the existing `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret, and neither mode submits an app or release for review. Do not dispatch `upload` unless the owner has approved the store mutation.

## iOS

The iOS TestFlight job is intentionally skipped until all Apple secrets are present:

- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `IOS_LIVE_ACTIVITY_PROVISIONING_PROFILE_BASE64`
- `APPLE_TEAM_ID`
- `KEYCHAIN_PASSWORD`

The App Store Connect app and Apple Developer bundle ID must use `com.jjswigut.oopsallprs.ios`.

### Local StoreKit Fixture

`iosApp/iosApp/FullAccess.storekit` is a local Xcode-only definition of the
single `lifetime_unlock` non-consumable at the current $14.99 offer. It is not
an App Store Connect product record, does not ship as a production entitlement,
and does not contact Apple.

Validate that it still matches the shared product identifier before using it:

```sh
tools/verify_ios_storekit_fixture.sh
```

For a developer-side local transaction session, open the project in Xcode,
select the `OopsAllPRs` scheme, then choose the fixture under **Run > Options >
StoreKit Configuration**. Use the Xcode StoreKit transaction controls to reset
state between purchase, cancellation, and restore scenarios. Local StoreKit
results are not a substitute for Apple sandbox or TestFlight validation.

The shared Debug scheme already references this fixture. The focused
`OopsAllPRsStoreKitTests` target uses `SKTestSession` to verify purchase,
entitlement persistence through a fresh StoreKit session, and refund
revocation. `tools/ios_storekit_smoke.sh` runs this suite and is required by
the local iOS release gate. It defaults to an iOS 27 `iPhone 18 Pro` simulator;
use `IOS_STOREKIT_DESTINATION` to select a different installed simulator. The
local result is still not a substitute for an App Store sandbox purchase or the
in-app restore-button flow.

Upload only the App Store screenshots:

```sh
tools/fastlane.sh ios listing
```

This lane uses only the App Store Connect API secrets above; it does not need the distribution certificate or provisioning profile because it does not upload a build. Set `IOS_APP_VERSION` if Fastlane should target a specific editable App Store version.

To create the remaining iOS secrets:

1. Create an App Store Connect API key with App Manager access. Save the key ID, issuer ID, and `.p8` file.
2. Create or export an Apple Distribution certificate as a `.p12` file and record its password.
3. Create App Store provisioning profiles using that distribution certificate for both `com.jjswigut.oopsallprs.ios` and `com.jjswigut.oopsallprs.ios.RestTimerLiveActivity`.
4. Base64 encode the `.p8`, `.p12`, and both `.mobileprovision` files, then add them as the GitHub secrets above. Use `IOS_PROVISIONING_PROFILE_BASE64` for the app and `IOS_LIVE_ACTIVITY_PROVISIONING_PROFILE_BASE64` for the extension.

Example encoding command:

```sh
base64 -i path/to/file -o path/to/file.base64
```
