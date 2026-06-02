# Quickstart: Durable Local Persistence

## Automated Validation

Run the shared Android unit suite:

```bash
./gradlew :shared:testDebugUnitTest
```

Run Android build validation:

```bash
./gradlew :shared:compileDebugKotlinAndroid :androidApp:assembleDebug
```

Run iOS shared compile validation:

```bash
./gradlew :shared:compileKotlinIosSimulatorArm64
```

Run design-system dependency scan:

```bash
rg -n "material3|MaterialTheme|androidx\\.compose\\.material3|org\\.jetbrains\\.compose\\.material3|\\bSurface\\b" shared/src shared/build.gradle.kts
```

Run whitespace validation:

```bash
git diff --check -- .
```

## Persistence Scenarios

1. Start an empty workout, add Bench Press, log one weighted set, save a draft,
   save active UX focus, save session/rest anchors, recreate SQL repositories
   against the same database, and verify the active workout and session reload.
2. Finish a workout with weighted and bodyweight sets, derive PRs, save it as a
   template, recreate SQL repositories, and verify History, Templates, Progress,
   and PR evidence reload from durable data.
3. Seed exercises, create a user exercise, seed again, recreate SQL
   repositories, and verify duplicate seed rows are not created and user-created
   exercises survive.
4. Change weight unit, recreate SQL repositories, and verify the preference is
   durable while set/PR weights remain canonical kilograms.
5. Export workouts/routines/exercises/PRs after repository recreation and
   verify rows come from durable data.

## Manual Gate Discovery

Attempt Android manual target discovery:

```bash
~/Library/Android/sdk/platform-tools/adb devices -l
~/Library/Android/sdk/emulator/emulator -list-avds
```

If a device/emulator is available, launch the debug app, create a workout,
force-close/reopen, and verify active workout recovery. If no target is
available, document the manual gate as deferred for milestone review.
