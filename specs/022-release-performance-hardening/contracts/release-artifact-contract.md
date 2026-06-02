# Contract: Release Artifact Evidence

## Android Release Contract

Android release validation must produce:

| Evidence | Expected |
|----------|----------|
| Release artifact | APK or AAB path under `androidApp/build/outputs/` |
| Package id | `com.jjswigut.oopsallprs.android` |
| Display name | `Oops All PRs` |
| Developer tools | Disabled |
| Code shrinking | Enabled |
| Resource shrinking | Enabled |
| Mapping file | Present for minified release |
| Keep rules | App-owned `androidApp/proguard-rules.pro` is included |
| Smoke flow | Startup, Profile, export availability, start/log/finish or discard workout |

## Android Debug Contract

Debug validation must confirm:

- Package id remains debug-suffixed.
- Display name remains debug-suffixed.
- Developer tools remain enabled.
- Minification/resource shrinking do not apply to debug.

## iOS Release Contract

iOS release validation must produce:

| Evidence | Expected |
|----------|----------|
| Release app bundle/archive | Simulator app bundle at minimum; archive when signing assets are available |
| Bundle id | `com.jjswigut.oopsallprs.ios` |
| Display name | `Oops All PRs` |
| Developer tools | Disabled |
| Shared framework | Release KMP framework path linked; debug framework paths are not scanned by Release |
| Symbol evidence | `DEBUG_INFORMATION_FORMAT=dwarf-with-dsym` and dSYM/symbol artifact present or blocker documented |
| Smoke flow | Startup, Profile, export availability, start/log/finish or discard workout |

## Blocker Contract

If validation cannot finish, the result must record:

- Failed command
- Last known artifact state
- Environment blocker
- Concrete next action

Known validation blockers after implementation:

- Android release install/smoke was not run because the local release APK is unsigned.
- iOS archive validation was not run because signing assets/provisioning were not validated locally.
- iOS simulator Release framework linked successfully. The separate `:shared:linkReleaseFrameworkIosArm64` device-framework validation became idle in Gradle/Kotlin Native and was terminated with exit 143 before producing the framework binary.
