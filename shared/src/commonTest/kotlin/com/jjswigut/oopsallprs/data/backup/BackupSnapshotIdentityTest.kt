package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupSnapshotIdentityTest {
    @Test
    fun canonicalHashGoldenVectorsPreserveConfigurationOrdering() {
        val configuration = LoggingConfigurationDto("config", 1, "fixed", listOf(
            LoggingMeasureDto("reps", "required", "count", null),
            LoggingMeasureDto("load", "required", "kg", "external")
        ), listOf("rpe", "rir"))
        val pkg = BackupPackage(
            formatVersion = BACKUP_FORMAT_VERSION, createdAt = 1, deviceId = "test", lastLocalRevision = "ignored",
            appSchemaVersion = 1, summary = SnapshotSummary().toDto(),
            preferences = PreferencesSnapshotDto("POUNDS", 5.0, 2.5, 120, true),
            loggingConfigurations = listOf(configuration), userExerciseConfigurations = emptyList(),
            exercises = emptyList(), routines = emptyList(), activeWorkout = null, activeSession = null,
            activeUxSession = null, activeSetDrafts = emptyList(), completedWorkouts = emptyList(),
            personalRecords = emptyList(), progressPoints = emptyList(), exportMetadata = emptyList()
        )

        assertEquals("snapshot-v1:8bd14c4a0696cd445e009dc6b72043a1783e0927c673ec257d26ea75f9f3d5b6", identity(pkg))
        assertEquals("snapshot-v1:1fa11330553243eaaeb245d9d72f0fb61011db56f694d54aa67812a5d0369fee",
            identity(pkg.copy(loggingConfigurations = listOf(configuration.copy(measures = configuration.measures.reversed())))))
        assertEquals("snapshot-v1:9eb527490b6e54dbab3b4ff500020534ff44ff2c8e9403999b3d9b3ff4e8b4f2",
            identity(pkg.copy(loggingConfigurations = listOf(configuration.copy(observedEffortKinds = configuration.observedEffortKinds.reversed())))))
    }

    @Test
    fun onlyTopLevelEnvelopeAndSummaryAreIgnored() = runTest {
        val pkg = snapshot()
        val metadataOnly = pkg.copy(
            createdAt = pkg.createdAt + 1,
            deviceId = "another-device",
            lastLocalRevision = "untrusted-wire-revision",
            appSchemaVersion = pkg.appSchemaVersion + 1,
            formatVersion = pkg.formatVersion + 1,
            summary = pkg.summary.copy(workoutCount = 999, latestUpdatedTimestamp = 999)
        )

        assertEquals(identity(pkg), identity(metadataOnly))
        assertTrue(BackupSnapshotIdentity.isContentRevision(identity(pkg)))
        assertFalse(BackupSnapshotIdentity.isContentRevision(null))
        assertFalse(BackupSnapshotIdentity.isContentRevision("legacy-1"))
        assertFalse(BackupSnapshotIdentity.isContentRevision(BackupSnapshotIdentity.PREFIX))
        assertFalse(BackupSnapshotIdentity.isContentRevision(BackupSnapshotIdentity.PREFIX + "g".repeat(64)))

        assertChanges(pkg, "nested export timestamp", pkg.copy(exportMetadata = pkg.exportMetadata.map {
            it.copy(createdAt = it.createdAt + 1)
        }))
        assertChanges(pkg, "nested export format", pkg.copy(exportMetadata = pkg.exportMetadata.map {
            it.copy(formatVersion = it.formatVersion + 1)
        }))
        assertChanges(pkg, "configuration schema", pkg.copy(loggingConfigurations = pkg.loggingConfigurations.map {
            it.copy(schemaVersion = it.schemaVersion + 1)
        }))
    }

    @Test
    fun everyPreferenceChangesIdentityWithoutChangingSummary() = runTest {
        val pkg = snapshot()
        val preferences = pkg.preferences
        val changes = listOf(
            preferences.copy(weightUnit = "KILOGRAMS"),
            preferences.copy(weightStepPounds = preferences.weightStepPounds + 0.25),
            preferences.copy(weightStepKilograms = preferences.weightStepKilograms + 0.25),
            preferences.copy(defaultRestSeconds = preferences.defaultRestSeconds + 1),
            preferences.copy(restSoundEnabled = !preferences.restSoundEnabled),
            preferences.copy(startWorkoutTimerWithFirstSet = !preferences.startWorkoutTimerWithFirstSet),
            preferences.copy(restTimerSurfaceEnabled = !preferences.restTimerSurfaceEnabled)
        )
        changes.forEach { assertChanges(pkg, "preference $it", pkg.copy(preferences = it)) }
    }

    @Test
    fun sessionsDraftsConfigurationsAndAssignmentsAreContent() = runTest {
        val pkg = snapshot()
        assertChanges(pkg, "session", pkg.copy(activeSession = pkg.activeSession!!.copy(restEndsAt = 99)))
        assertChanges(pkg, "UX session", pkg.copy(activeUxSession = pkg.activeUxSession!!.copy(focusedDraftId = "other")))
        assertChanges(pkg, "draft", pkg.copy(activeSetDrafts = pkg.activeSetDrafts.map { it.copy(reps = 12) }))
        assertChanges(pkg, "draft timer", pkg.copy(activeSetDrafts = pkg.activeSetDrafts.map { it.copy(timerStartedAt = 99) }))
        assertChanges(pkg, "configuration content despite same hash", pkg.copy(loggingConfigurations = pkg.loggingConfigurations.map {
            it.copy(measures = it.measures.map { measure -> measure.copy(requirement = "optional") })
        }))
        assertChanges(pkg, "user configuration", pkg.copy(userExerciseConfigurations = pkg.userExerciseConfigurations.map {
            it.copy(basedOnDefinitionRevision = it.basedOnDefinitionRevision + 1)
        }))
    }

    @Test
    fun sameCountSameTimestampSetEditsChangeIdentity() = runTest {
        val pkg = snapshot()
        val active = pkg.activeWorkout!!
        val activeEdit = pkg.copy(activeWorkout = active.copy(exercises = active.exercises.map {
            it.copy(sets = it.sets.map { set -> set.copy(weightKg = 101.0) })
        }))
        val completedEdit = pkg.copy(completedWorkouts = pkg.completedWorkouts.map { workout ->
            workout.copy(exercises = workout.exercises.map {
                it.copy(loggedSets = it.loggedSets.map { set -> set.copy(reps = 7) })
            })
        })

        assertEquals(pkg.summary, activeEdit.summary)
        assertEquals(pkg.summary, completedEdit.summary)
        assertChanges(pkg, "active set value", activeEdit)
        assertChanges(pkg, "completed set value", completedEdit)
        assertChanges(pkg, "set timestamp", pkg.copy(activeWorkout = active.copy(exercises = active.exercises.map {
            it.copy(sets = it.sets.map { set -> set.copy(editedAt = 99) })
        })))
    }

    @Test
    fun unorderedCollectionsAndNestedEntityTraversalOrderDoNotChangeIdentity() = runTest {
        val pkg = snapshot()
        val reordered = pkg.copy(
            loggingConfigurations = pkg.loggingConfigurations.reversed(),
            userExerciseConfigurations = pkg.userExerciseConfigurations.reversed(),
            exercises = pkg.exercises.reversed(),
            routines = pkg.routines.reversed().map { routine ->
                routine.copy(exercises = routine.exercises.reversed().map {
                    it.copy(plannedSets = it.plannedSets.reversed())
                })
            },
            activeWorkout = pkg.activeWorkout!!.let { workout ->
                workout.copy(exercises = workout.exercises.reversed().map { it.copy(sets = it.sets.reversed()) })
            },
            activeSetDrafts = pkg.activeSetDrafts.reversed(),
            completedWorkouts = pkg.completedWorkouts.reversed().map { workout ->
                workout.copy(exercises = workout.exercises.reversed().map {
                    it.copy(loggedSets = it.loggedSets.reversed())
                })
            },
            personalRecords = pkg.personalRecords.reversed(),
            progressPoints = pkg.progressPoints.reversed(),
            exportMetadata = pkg.exportMetadata.reversed()
        )

        assertEquals(identity(pkg), identity(reordered))
    }

    @Test
    fun explicitPositionsRemainSignificant() = runTest {
        val pkg = snapshot()
        val active = pkg.activeWorkout!!
        assertChanges(pkg, "active exercise position", pkg.copy(activeWorkout = active.copy(exercises = active.exercises.map {
            it.copy(position = it.position + 1)
        })))
        assertChanges(pkg, "active set position", pkg.copy(activeWorkout = active.copy(exercises = active.exercises.map {
            it.copy(sets = it.sets.map { set -> set.copy(position = set.position + 1) })
        })))
        assertChanges(pkg, "routine exercise position", pkg.copy(routines = pkg.routines.map { routine ->
            routine.copy(exercises = routine.exercises.map { it.copy(position = it.position + 1) })
        }))
        assertChanges(pkg, "planned set position", pkg.copy(routines = pkg.routines.map { routine ->
            routine.copy(exercises = routine.exercises.map {
                it.copy(plannedSets = it.plannedSets.map { set -> set.copy(position = set.position + 1) })
            })
        }))
        assertChanges(pkg, "completed exercise position", pkg.copy(completedWorkouts = pkg.completedWorkouts.map { workout ->
            workout.copy(exercises = workout.exercises.map { it.copy(position = it.position + 1) })
        }))
        assertChanges(pkg, "completed set position", pkg.copy(completedWorkouts = pkg.completedWorkouts.map { workout ->
            workout.copy(exercises = workout.exercises.map {
                it.copy(loggedSets = it.loggedSets.map { set -> set.copy(position = set.position + 1) })
            })
        }))
        assertChanges(pkg, "draft position", pkg.copy(activeSetDrafts = pkg.activeSetDrafts.map {
            it.copy(position = it.position + 1)
        }))
    }

    @Test
    fun orderedConfigurationMeasuresAndEffortKindsAreNotNormalizedAway() = runTest {
        val pkg = snapshot()
        assertChanges(pkg, "measure order", pkg.copy(loggingConfigurations = pkg.loggingConfigurations.map {
            it.copy(measures = it.measures.reversed())
        }))
        assertChanges(pkg, "effort order", pkg.copy(loggingConfigurations = pkg.loggingConfigurations.map {
            it.copy(observedEffortKinds = it.observedEffortKinds.reversed())
        }))
    }

    @Test
    fun omittedAndExplicitDefaultsHaveTheSameIdentity() = runTest {
        val pkg = snapshot()
        val json = Json { encodeDefaults = true }
        val encoded = json.encodeToJsonElement(BackupPackageDto.serializer(), pkg).jsonObject
        val withoutDefaults = JsonObject(encoded + ("preferences" to JsonObject(
            encoded.getValue("preferences").jsonObject.filterKeys {
                it != "startWorkoutTimerWithFirstSet" && it != "restTimerSurfaceEnabled"
            }
        )))

        assertEquals(identity(pkg), identity(json.decodeFromJsonElement(BackupPackageDto.serializer(), withoutDefaults)))
    }

    private fun identity(pkg: BackupPackage): String = BackupSnapshotIdentity.revision(pkg)

    private fun assertChanges(original: BackupPackage, label: String, changed: BackupPackage) {
        assertNotEquals(identity(original), identity(changed), label)
    }

    private suspend fun snapshot(): BackupPackage {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        harness.workoutWithLoggedWeightedSet()
        val pkg = BackupSnapshotReader(
            harness.store, harness.store, harness.store, harness.store,
            harness.store, harness.store, harness.store
        ).createPackage(instant(3_000)).successValue()
        val active = pkg.activeWorkout!!
        val sourceExercise = active.exercises.single()
        val sourceSet = sourceExercise.sets.single()
        val activeExercises = listOf("exercise-a", "exercise-b").map { id ->
            sourceExercise.copy(id = id, position = 0, sets = listOf("set-a", "set-b").map { setId ->
                sourceSet.copy(id = "$id-$setId", exerciseInstanceId = id, position = 0)
            })
        }
        val configurations = listOf("configuration-a", "configuration-b").map { id ->
            LoggingConfigurationDto(id, 1, "unchanged-hash", listOf(
                LoggingMeasureDto("reps", "required", "count", null),
                LoggingMeasureDto("load", "required", "kg", "external")
            ), listOf("rpe", "rir"))
        }
        val routines = listOf("routine-a", "routine-b").map { routineId ->
            RoutineDto(routineId, "Routine", activeExercises.map { exercise ->
                RoutineExerciseDto(
                    id = "$routineId-${exercise.id}", routineId = routineId,
                    exerciseCatalogId = exercise.exerciseCatalogId, displayNameSnapshot = "Exercise", position = 0,
                    plannedSets = exercise.sets.map { set -> RoutineSetTemplateDto(
                        id = "$routineId-${set.id}", routineExerciseId = "$routineId-${exercise.id}", position = 0,
                        targetWeightKg = 100.0, targetReps = 5, targetDurationMs = null, setKind = "WEIGHTED",
                        loggingConfigurationId = "configuration-a", targetDistanceMeters = null,
                        effortTargetKind = null, targetRpeTenths = null, targetRir = null
                    ) },
                    rest = RestConfigurationDto(120, true), definitionRevisionSnapshot = 1,
                    seedKeySnapshot = null, loggingConfigurationId = "configuration-a", loggingConfigurationSource = "default"
                )
            }, 1, 2, null, null)
        }
        return pkg.copy(
            loggingConfigurations = configurations,
            userExerciseConfigurations = pkg.exercises.take(2).map {
                UserExerciseConfigurationDto(it.id, "configuration-a", 1, 2)
            },
            routines = routines,
            activeWorkout = active.copy(exercises = activeExercises),
            activeSession = ActiveSessionDto(active.id, 1, 2, 1, "set-a", "workout", 2),
            activeUxSession = ActiveWorkoutUxSessionDto(active.id, "exercise-a", "draft-a", 2),
            activeSetDrafts = listOf("draft-a", "draft-b").map { id ->
                PersistedSetDraftDto(id, active.id, "exercise-a", 0, "WEIGHTED", 5, 100.0,
                    null, null, 2, "configuration-a", null, null, null, null)
            },
            completedWorkouts = listOf("completed-a", "completed-b").map { id ->
                CompletedWorkoutDto(id, active.id, 1, 2, 1, null, activeExercises.map { exercise ->
                    CompletedExerciseDto(exercise.id, id, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
                        exercise.position, exercise.sets, exercise.rest)
                }, 2)
            },
            personalRecords = listOf("record-a", "record-b").map { id ->
                PersonalRecordDto(id, "exercise-a", "weight", 5, 100.0, 100.0,
                    "completed-a", "set-a", 2, 2, "weight", 1)
            },
            progressPoints = listOf("point-a", "point-b").map { id ->
                ProgressPointDto(id, "exercise-a", "completed-a", "set-a", "weight", 100.0, 100.0, 5, 2, "weight", 1)
            },
            exportMetadata = listOf("export-a", "export-b").map { id -> ExportSnapshotDto(id, "workouts", 2, "POUNDS", 2, 1) }
        )
    }
}
