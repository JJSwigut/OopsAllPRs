# Contract: Profile Developer Tools Visibility

## Scope

The shared app entry point receives developer-tool availability from platform bootstrap code. Profile renders developer tools only when that capability exists.

## Inputs

| Input | Source | Debug Value | Release Value |
|-------|--------|-------------|---------------|
| `developerToolsEnabled` | Android `BuildConfig.DEBUG` | `true` | `false` |
| `developerToolsEnabled` | iOS Swift/Xcode build configuration | `true` | `false` |

## Shared State Contract

- When `developerToolsEnabled = true`, `AppState.developerSeeds` is non-null.
- When `developerToolsEnabled = false`, `AppState.developerSeeds` is null.
- `ProfileFlow` receives `developerSeedState = null` in release and must render no Developer section.
- `ProfileFlow` receives non-null `developerSeedState` in debug and must render the Developer section.

## Release UI Contract

Profile must not show:

- Section label `Developer`
- Developer seed scenario buttons
- Developer seed result/status text
- Any navigation or action that loads developer/demo seed data

## Debug UI Contract

Profile must show:

- Section label `Developer`
- Developer seed scenario controls
- Status feedback after a developer seed scenario completes

## Validation Signals

- Shared UI test validates the Profile contract directly.
- Android release build artifact reports `BuildConfig.DEBUG=false` and Profile has no developer state.
- iOS release build passes `false` from Swift into `IosAppViewControllerFactory`.
- Debug app identities are distinguishable from release identities.
