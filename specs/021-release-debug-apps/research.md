# Research: Release and Debug Apps

## Decision: Use Existing Shared Capability for Profile Visibility

**Rationale**: `AppState.create(... developerToolsEnabled)` already conditionally creates `developerSeeds`; `ProfileFlow` only renders the Developer card when `developerSeedState` is non-null. This is the right shared contract because release gating happens before UI state is created and is not persisted.

**Alternatives considered**:

- Hide the card directly in `ProfileFlow` with another boolean: rejected because `developerSeedState: DeveloperSeedState?` already expresses capability and avoids duplicated state.
- Remove developer tooling from release source sets: rejected as unnecessary for this scope; build-time capability prevents user access while keeping debug tooling testable.

## Decision: Android Uses `BuildConfig.DEBUG`

**Rationale**: Android already passes `BuildConfig.DEBUG` to `App`, which is the standard build-variant source of truth and cannot be changed by users. Add debug metadata (`applicationIdSuffix`, display name) for distinguishability rather than introducing new runtime state.

**Alternatives considered**:

- Gradle manifest placeholders only: useful for display naming, but insufficient alone because the shared app needs a boolean capability.
- Persist a developer-tools preference: rejected because it could leak into release and violates the spec.

## Decision: iOS Passes Swift Build Configuration Into Shared Host

**Rationale**: The current iOS factory hardcodes `developerToolsEnabled = true`. Swift/Xcode already defines `DEBUG` for Debug configuration, so the Swift host should derive a boolean and pass it into the Kotlin `IosAppViewControllerFactory`. This matches Android’s platform bootstrap boundary.

**Alternatives considered**:

- Detect build type inside Kotlin/Native: rejected because this is a platform build concern and Swift/Xcode already exposes it simply.
- Add separate debug/release Kotlin frameworks immediately: rejected for this scope because the existing simulator host links debug framework today; release app visibility can still be validated through the Swift-provided flag and Xcode release build, while future archive optimization can refine framework linkage.

## Decision: Distinguish Debug Artifacts by Identity and Name

**Rationale**: Debug and release apps need to be hard to confuse during validation. Android should use a debug `applicationIdSuffix` and display-name suffix. iOS should use Debug-specific bundle identifier and display-name build settings consumed by `Info.plist`.

**Alternatives considered**:

- Only rely on build folder names: rejected because installed apps remain ambiguous.
- Put a debug badge inside the app UI: rejected for release safety and because Profile developer controls already prove debug capability.
