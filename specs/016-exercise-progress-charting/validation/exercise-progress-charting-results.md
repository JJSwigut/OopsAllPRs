# Validation Results: Exercise Progress Charting

## Automated Gates

| Gate | Result | Evidence |
|------|--------|----------|
| `./gradlew :shared:testDebugUnitTest` | Pass | BUILD SUCCESSFUL; includes chart model/state-holder coverage |
| `./gradlew :androidApp:assembleDebug` | Pass | BUILD SUCCESSFUL |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | Pass | BUILD SUCCESSFUL with existing expect/actual beta warnings |
| Material guard | Pass | `rg` returned no forbidden Material 3/shared styling matches |
| `git diff --check -- .` | Pass | No whitespace errors |

## Manual Android Smoke

| Flow | Expected Result | Outcome | Evidence |
|------|-----------------|---------|----------|
| Progress chart launch | Exercise detail renders chart for an exercise with progress points | Deferred | Automated chart state/UI compile coverage passes; manual data setup deferred |
| Metric switching | Chart and rows update to selected metric | Pass automated | `ProgressChartStateHolderTest` verifies metric switching filters chart points |
| Evidence routing | Selecting point/row opens existing source evidence | Pass automated | `ProgressChartModelTest` verifies source record id preservation; existing evidence flow reused |
| Compact layout | Chart does not overlap resume banner or bottom nav | Deferred | Requires manual dataset with completed progress points on emulator/device |

## Known Limitations

- Time range filters are intentionally deferred.
- Manual chart screenshot is deferred until the app has completed-workout progress data on the test device/emulator.
