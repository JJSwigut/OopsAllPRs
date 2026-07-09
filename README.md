# Oops All PRs

Fresh Kotlin Multiplatform foundation for a local-first workout logger.

## Build

```bash
./gradlew :shared:build
./gradlew :androidApp:assembleDebug
./gradlew testDebugUnitTest
./gradlew check
```

Android is the first executable validation target. iOS remains first-class
through shared domain behavior and compileable platform adapter boundaries.

## Codex Workflow

Product and release workflow conventions live in `docs/development/codex-workflow.md`.

Common commands:

```bash
tools/create_feature_worktree.sh feature-name
FROM_ORIGIN=1 tools/create_feature_worktree.sh feature-name
tools/bootstrap_fastlane.sh
tools/fastlane.sh lanes
tools/android_emulator_smoke.sh
tools/release_gate.sh
tools/ios_release_package.sh
```
