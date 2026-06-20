# Store Deployment Setup

GitHub Actions now has two workflows:

- `CI`: runs Gradle checks, Android debug/release assembly, and an iOS simulator build for pull requests and pushes to `main`.
- `Deploy Stores`: runs release gates on pushes to `main`, uploads Android to Google Play internal testing, and uploads iOS to TestFlight once the Apple signing secrets exist.

## Android

The Android workflow expects these GitHub secrets:

- `ANDROID_UPLOAD_KEYSTORE_BASE64`
- `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
- `ANDROID_UPLOAD_KEY_ALIAS`
- `ANDROID_UPLOAD_KEY_PASSWORD`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`

The Google Play service account needs Play Console access for:

- `View app information and download bulk reports (read-only)`
- `Release apps to testing tracks`
- `Manage store presence` for listing image and screenshot uploads

The Play Console app must exist before the first Fastlane upload. Use package name `com.jjswigut.oopsallprs.android`.

Upload only the Google Play listing artwork:

```sh
bundle exec fastlane android listing
```

Set `ANDROID_PLAY_VALIDATE_ONLY=true` to validate the Play edit without publishing it.

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
bundle exec fastlane ios listing
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
