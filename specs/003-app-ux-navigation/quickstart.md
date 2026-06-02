# Quickstart: App UX Navigation Shell

Use this after `/speckit-tasks` and implementation of the first feature 003
slice.

## Build and Test

From the repository root:

```bash
./gradlew :design-system:testDebugUnitTest
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileKotlinMetadata
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

## Material Retirement Check

Shared UI must not use Material 3 after this slice:

```bash
rg -n "material3|MaterialTheme|Surface|androidx.compose.material3|org.jetbrains.compose.material3" shared/src shared/build.gradle.kts
```

Expected result: no shared source imports/usages and no Material 3 dependency in
`shared/build.gradle.kts`.

If `vico-multiplatform-m3` is still present in shared dependencies, document why
it remains isolated and unused; otherwise remove it until the Progress chart
slice plans a Material-free chart strategy.

## Manual Android Milestone Check

1. Install the debug Android build on a phone-size emulator or device.
2. Launch the app and confirm the Neo-Glass `FitTheme` shell appears.
3. Switch Train, History, Progress, and Profile from the bottom tab bar.
4. Start or simulate an active workout from Train.
5. Leave the active-workout overlay and confirm the resume banner remains
   visible above navigation.
6. Tap Resume and confirm the active-workout overlay returns with session state.
7. Kill and relaunch the app during the active workout. Confirm either the
   active overlay or the resume banner recovers according to `last_opened_route`.
8. Run on a tablet/foldable emulator or resized desktop target if available and
   confirm the navigation rail layout is selected.
9. Enable reduce motion and haptics disabled where platform settings/adapters
   expose them, then confirm the shell remains usable without losing visible
   feedback.

## Expected Scope

This slice validates shell/navigation/theme behavior only. Full one-tap set
logging, roller editing, rest notification UI, PR celebration, history detail,
progress charts, profile preference editing, and export UI remain future
feature 003 slices.
