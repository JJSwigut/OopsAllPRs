# Validation Results: Android Alpha Hardening

## Automated Gates

| Gate | Result | Evidence |
|------|--------|----------|
| `./gradlew :shared:testDebugUnitTest` | Pass | BUILD SUCCESSFUL after adding readiness/recovery tests |
| `./gradlew :androidApp:assembleDebug` | Pass | BUILD SUCCESSFUL; APK installed on Pixel 9 Pro AVD |
| `./gradlew :shared:compileKotlinIosSimulatorArm64` | Pass | BUILD SUCCESSFUL with existing expect/actual beta warnings |
| Material guard | Pass | `rg` returned no forbidden Material 3/shared styling matches |
| `git diff --check -- .` | Pass | No whitespace errors |

## Manual Android Alpha Smoke

| Flow | Expected Result | Outcome | Evidence |
|------|-----------------|---------|----------|
| Launch and top-level navigation | Train, History, Progress, and Profile are reachable with no overlap | Pass on emulator | Pixel 9 Pro AVD launched via `adb`; Train, Progress, and Profile reached |
| Fresh Train | Start/create actions are immediate and thumb-reachable | Partial | AVD retained an active workout from prior data; Train resume/start/create controls were bottom reachable |
| Exercise picker | Search and scrolling work for default catalog | Deferred | Full picker smoke deferred to physical/manual pass |
| Weighted set logging | Set persists only after explicit log action succeeds | Deferred | Covered by existing unit tests; manual logging smoke deferred |
| Bodyweight reps-only logging | Bodyweight set can be logged without required weight | Deferred | Covered by existing unit tests; manual logging smoke deferred |
| Rest timer | Start, skip, completion, and denied-notification paths do not corrupt workout state | Pass automated, partial manual | New expired-rest hydration test passes; manual denied-permission smoke deferred |
| Force-close recovery | Active workout and resume affordance recover after restart | Pass automated, partial manual | Active workout resume banner remained after app relaunch; recovery tests pass |
| Finish/discard | Finished workouts appear in History; discard does not create history row | Deferred | Manual finish/discard smoke deferred |
| Progress | Empty and PR states are truthful and compact | Pass on emulator | Progress empty screenshot checked; stat tile clipping fixed and rechecked |
| Profile | Local storage, sync off, backup, rest alerts, export, and alpha readiness are clear | Pass on emulator | Profile Data card shows all readiness rows with no overlap |
| Export | User-owned local data can be shared/exported where available | Deferred | Automated export tests pass; manual share/export smoke deferred |

## Known Limitations

- Physical Pixel validation was not run because `adb devices` showed no attached devices in this shell.
- Pixel 9 Pro AVD smoke was completed for launch/navigation, active workout resume/dismiss, Progress empty state, and Profile readiness. Temporary screenshots were captured under `/tmp/oops-all-prs-alpha-*.png`.
- Full end-to-end manual logging/export smoke remains a milestone gate for the user's next physical-device pass.
