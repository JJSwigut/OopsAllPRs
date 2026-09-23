fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android internal

```sh
[bundle exec] fastlane android internal
```

Build and upload the Android app bundle to Google Play internal testing

### android listing_package

```sh
[bundle exec] fastlane android listing_package
```

Prepare Google Play listing metadata and artwork without contacting Google Play

### android listing

```sh
[bundle exec] fastlane android listing
```

Validate or upload Google Play listing metadata and artwork only

----


## iOS

### ios beta

```sh
[bundle exec] fastlane ios beta
```

Build the iOS app and upload it to TestFlight

### ios listing

```sh
[bundle exec] fastlane ios listing
```

Upload App Store screenshots only

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
