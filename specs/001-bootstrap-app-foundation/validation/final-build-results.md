# Final Build Results

Run date: 2026-05-29

## Passed

- `./gradlew :shared:compileKotlinMetadata`
- `./gradlew :shared:testDebugUnitTest`
- `./gradlew :androidApp:assembleDebug`
- `./gradlew :shared:compileKotlinIosSimulatorArm64`
- `./gradlew testDebugUnitTest`

## Limited / Not Clean

- `./gradlew :shared:build` compiled common, Android, and iOS Kotlin, then the Gradle daemon stopped during Kotlin/Native framework linking because the JVM garbage collector was thrashing.
- `./gradlew check` reached iOS simulator test linking and was stopped after a long native link with no additional compiler errors. The lighter iOS compile task passed.

## Follow-Up

Before release, rerun the full native framework/test-link gates on a machine or Gradle configuration with enough memory for Kotlin/Native linking.
