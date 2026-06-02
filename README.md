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
