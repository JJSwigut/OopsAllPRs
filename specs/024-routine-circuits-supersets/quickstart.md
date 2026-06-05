# Quickstart: Routine Circuits & Supersets

## Automated Validation

Validated locally on 2026-06-05:

```sh
./gradlew :shared:compileKotlinMetadata
ANDROID_HOME=/Users/swig/Library/Android/sdk ./gradlew :shared:testDebugUnitTest --tests '*RoutineBuilderCreateTest' --tests '*RoutineBuilderEditTest' --tests '*TemplateLaunchSeparationTest' --tests '*RoutineSeparationTest' --tests '*SqlRoutineManagementPersistenceTest'
ANDROID_HOME=/Users/swig/Library/Android/sdk ./gradlew :shared:testDebugUnitTest
./gradlew :shared:compileKotlinIosSimulatorArm64
```

`./gradlew :shared:allTests --tests ...` is not supported by this Gradle task; use `:shared:testDebugUnitTest` for filtered Android unit tests.

1. Run focused routine editor tests:

   ```sh
   ./gradlew :shared:allTests --tests '*RoutineBuilder*' --tests '*RoutineStateHolder*'
   ```

2. Run focused launch and ledger tests:

   ```sh
   ./gradlew :shared:allTests --tests '*RoutineLaunch*' --tests '*PreviousWorkout*' --tests '*Export*'
   ```

3. Run SQL persistence tests:

   ```sh
   ./gradlew :shared:connectedAndroidTest
   ```

   If no device is attached, run the available Android unit SQL tests used by this repo.

## Manual Validation

1. Open Train and create a routine.
2. Add two exercises, group them, and confirm the label reads as a superset.
3. Add a third adjacent exercise to another group and confirm the label reads as a circuit.
4. Save, reopen the routine editor, and confirm grouping, exercise order, planned sets, and rest values are preserved.
5. Launch the routine and verify group labels are visible without changing the normal set logging path.
6. Log sets, finish the workout, and verify history, PR feedback, and previous values still reflect logged sets.

## Out Of Scope Checks

- Do not expect group-specific rest, automatic round rotation, drag-and-drop grouping, completed-workout grouping, or export group metadata in this slice.
