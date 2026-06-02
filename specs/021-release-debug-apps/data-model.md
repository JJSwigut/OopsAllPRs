# Data Model: Release and Debug Apps

## Build Channel

Represents the build-time classification of an installed artifact.

**Values**:

- `debug`: developer tooling enabled; artifact distinguishable from release.
- `release`: developer tooling disabled; public Profile has no developer controls.

**Validation rules**:

- Must be derived from platform build configuration.
- Must not be read from persisted user settings.
- Must remain stable for the lifetime of the installed artifact.

## Developer Tools Availability

Represents whether shared app state creates developer-only capabilities.

**Fields**:

- `enabled: Boolean`: true only for debug builds.
- `developerSeeds: DeveloperSeedStateHolder?`: non-null only when enabled.

**State transitions**:

- App launch in debug: `enabled = true`, developer seed state is available.
- App launch in release: `enabled = false`, developer seed state is absent.
- Restart/process death: same state is reconstructed from build configuration.

## App Artifact Identity

Represents platform install/display metadata used by humans and tooling during validation.

**Android fields**:

- Release application id: `com.jjswigut.oopsallprs.android`
- Debug application id: release id plus a debug suffix.
- Debug display name: includes a visible debug suffix.

**iOS fields**:

- Release bundle id: `com.jjswigut.oopsallprs.ios`
- Debug bundle id: release id plus a debug suffix.
- Debug display name: includes a visible debug suffix.

**Data impact**:

- No workout, routine, PR, export, preference, or exercise schema changes.
- Co-installable debug/release identities may naturally use separate platform app containers.
