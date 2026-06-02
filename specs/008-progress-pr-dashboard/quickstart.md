# Quickstart: Progress and PR Dashboard V1

## Build and Test

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
```

## Material Scan

```bash
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts
```

Expected result: no matches.

## Android Manual Milestone

1. Install and launch the debug app on a Pixel-class phone or emulator.
2. Complete one weighted workout that produces a weighted PR.
3. Complete one bodyweight workout that produces a reps-only PR.
4. Open Progress and confirm recent PRs are visible first.
5. Confirm weighted, bodyweight, estimated one-rep max, and volume records use
   readable labels when present.
6. Browse records by exercise and confirm only that exercise's records appear.
7. Open source evidence for one PR and confirm the source set values match.
8. Review simple trend context for one exercise.
9. Restart the app and confirm Progress data is still visible without network.

## Completion Evidence

- Shared unit test results for PR rows, grouping, evidence, missing evidence,
  bodyweight records, unit display, trends, and non-mutation.
- Android debug build result.
- iOS simulator shared compile result.
- Material scan result.
- Manual screenshots or notes for empty Progress, recent PRs, exercise detail,
  source evidence, and trend context.
