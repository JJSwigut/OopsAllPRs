# Data Model: Release Performance Hardening

## Release Optimization Profile

Represents build-time optimization settings for each platform.

**Fields**:

- `platform`: Android or iOS.
- `buildChannel`: Release only for optimization enforcement; debug remains unoptimized for local iteration.
- `codeShrinkingEnabled`: Android release only.
- `resourceShrinkingEnabled`: Android release only.
- `releaseFrameworkLinked`: iOS Release must link release KMP framework output.
- `symbolEvidenceAvailable`: dSYM/mapping/symbol artifacts are present or blocker documented.

**Validation rules**:

- Debug builds must not inherit release minification behavior.
- Release builds must keep developer tools disabled.
- Optimization must not alter app data models or user-visible workout behavior.

## Release Artifact Evidence

Represents the evidence collected after a release-hardening validation run.

**Fields**:

- `artifactPath`
- `platform`
- `variantOrConfiguration`
- `bundleOrPackageId`
- `displayName`
- `optimizationState`
- `symbolOrMappingPath`
- `smokeTestStatus`
- `blocker`, if validation cannot complete

**Lifecycle**:

1. Build starts after prerequisites pass.
2. Artifact metadata is inspected.
3. Smoke test runs where device/simulator is available.
4. Evidence is recorded in quickstart output or task notes.

## Validation Environment

Represents local prerequisites needed before release validation.

**Fields**:

- `freeDiskSpace`
- `androidSdkAvailable`
- `iosRuntimeAvailable`
- `signingAssetsAvailable`
- `physicalDeviceOrSimulatorAvailable`

**Validation rules**:

- Large iOS release builds should not start unless enough disk is free.
- Missing signing assets block archive/distribution validation but not simulator Release validation.
