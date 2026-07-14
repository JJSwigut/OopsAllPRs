# Store Deployment Setup

GitHub Actions has three workflows:

- `CI`: runs Gradle checks, Android debug/release assembly, and an iOS simulator build for pull requests and pushes to `development` or `main`.
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

The iOS package job is best-effort while local Release framework linking is slow/unresolved. Android release artifacts and the GitHub Release should still be available when the release gate passes.

Before promoting `development` to `main`, run `tools/release_gate.sh --android-smoke`. Successful local release gates write `build/release-gate/summary.txt`, `build/release-gate/artifacts.txt`, and `build/release-gate/proof.env` for PR or release notes.

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

To commit only the listing edit, explicitly opt into upload mode:

```sh
ANDROID_PLAY_VALIDATE_ONLY=false tools/fastlane.sh android listing
```

The lane never builds or uploads an APK/AAB, never changes a release track, and sets `changes_not_sent_for_review` so the edit is not submitted for review. Upload mode is still a Google Play store mutation and requires explicit owner approval.

The `Google Play Listing` GitHub Actions workflow is `workflow_dispatch` only. Its `mode` input defaults to `validate`; selecting `upload` commits the same listing-only edit. Both modes use the existing `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret, and neither mode submits an app or release for review. Do not dispatch `upload` unless the owner has approved the store mutation.

## iOS

The iOS TestFlight job is intentionally skipped until all Apple secrets are present:

- `APP_STORE_CONNECT_API_KEY_ID`
- `APP_STORE_CONNECT_ISSUER_ID`
- `APP_STORE_CONNECT_API_KEY_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_BASE64`
- `IOS_DISTRIBUTION_CERTIFICATE_PASSWORD`
- `IOS_PROVISIONING_PROFILE_BASE64`
- `APPLE_TEAM_ID`
- `KEYCHAIN_PASSWORD`

The App Store Connect app and Apple Developer bundle ID must use `com.jjswigut.oopsallprs.ios`.

Upload only the App Store screenshots:

```sh
tools/fastlane.sh ios listing
```

This lane uses only the App Store Connect API secrets above; it does not need the distribution certificate or provisioning profile because it does not upload a build. Set `IOS_APP_VERSION` if Fastlane should target a specific editable App Store version.

To create the remaining iOS secrets:

1. Create an App Store Connect API key with App Manager access. Save the key ID, issuer ID, and `.p8` file.
2. Create or export an Apple Distribution certificate as a `.p12` file and record its password.
3. Create an App Store provisioning profile for bundle ID `com.jjswigut.oopsallprs.ios` using that distribution certificate.
4. Base64 encode the `.p8`, `.p12`, and `.mobileprovision` files and add them as the GitHub secrets above.

Example encoding command:

```sh
base64 -i path/to/file -o path/to/file.base64
```
