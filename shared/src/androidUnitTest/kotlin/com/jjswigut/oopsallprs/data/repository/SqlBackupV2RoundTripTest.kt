package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.EffortTarget
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlBackupV2RoundTripTest {
    @Test
    fun v2EncodeDecodeRestorePreservesConfigurableLoggingAndDerivedEvidence() = runTest {
        val sourceHarness = SqlFoundationStoreTestHarness()
        val source = sourceHarness.repositories()
        seedBackupExercises(source)
        val completedId = createCompletedMixedWorkout(source)
        val completed = requireNotNull(source.workouts.completedWorkout(completedId))
        val evidenceSet = completed.exercises.first().loggedSets.single()
        val customExercise = configuredExercise()
        source.store.saveUserExercise(customExercise).successValue()
        source.store.saveLoggingConfiguration(userConfiguration).successValue()
        source.store.saveLoggingConfiguration(workoutOverride).successValue()
        source.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                customExercise.id, userConfiguration, customExercise.definitionRevision, instant(3_000)
            )
        ).successValue()
        source.store.saveRoutine(configuredRoutine(customExercise)).successValue()
        source.store.createActiveWorkout(configuredWorkout(customExercise)).successValue()
        source.store.saveSetDraft(configuredDraft()).successValue()
        source.store.replaceRecords(
            records = listOf(
                PersonalRecord(
                    FoundationId("distance-record"), completed.exercises.first().exerciseCatalogId,
                    PersonalRecordKind.TIME, null, null, 5000.0, completed.id, evidenceSet.id,
                    instant(2_000), instant(2_100), ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode,
                    ProgressDerivationVersions.CONFIGURATION_CAPTURE
                )
            ),
            points = listOf(
                ProgressPoint(
                    FoundationId("distance-point"), completed.exercises.first().exerciseCatalogId,
                    completed.id, evidenceSet.id, ProgressMetric.TIME, 5000.0, null, null,
                    instant(2_000), ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode,
                    ProgressDerivationVersions.CONFIGURATION_CAPTURE
                )
            )
        ).successValue()
        val sourceBackup = SqlBackupRepository(sourceHarness.database, source.store)
        val sourcePackage = sourceBackup.createPackage().successValue()
        val encoded = sourceBackup.encodePackage(sourcePackage).successValue()
        val decoded = sourceBackup.decodePackage(encoded).successValue()
        assertTrue(encoded.contains("\"origin\": \"user\""))
        assertTrue(encoded.contains("\"loggingConfigurationSource\": \"workout_override\""))
        assertTrue(encoded.contains("\"observedEffortKinds\": ["))
        assertTrue(encoded.contains("\"failure_outcome\""))
        assertTrue(encoded.contains("\"metricCode\": \"longest_distance\""))
        assertTrue(!encoded.contains("\"loggingConfigurationSource\": \"WORKOUT_OVERRIDE\""))

        val destinationHarness = SqlFoundationStoreTestHarness()
        val destination = destinationHarness.repositories()
        SqlBackupRepository(destinationHarness.database, destination.store).restore(decoded).successValue()
        val restoredPackage = SqlBackupRepository(destinationHarness.database, destination.store).createPackage().successValue()

        assertEquals(
            sourcePackage.copy(
                createdAt = restoredPackage.createdAt,
                lastLocalRevision = restoredPackage.lastLocalRevision
            ),
            restoredPackage
        )
        val restoredActive = requireNotNull(destination.store.currentActiveWorkout())
        assertEquals(workoutOverride.id, restoredActive.exercises.single().resolvedLoggingConfiguration.configuration.id)
        assertEquals(42.0, restoredActive.exercises.single().sets.single().distanceMeters)
        assertEquals(FailureOutcome.NOT_REACHED, restoredActive.exercises.single().sets.single().observedEffort?.failureOutcome)
        assertEquals(3, destination.store.routine(FoundationId("configured-routine"))!!.exercises.single().plannedSets.size)
        assertEquals(ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode, destination.store.personalRecords().single().metricCode)
        assertEquals(ProgressDerivationVersions.CONFIGURATION_CAPTURE, destination.store.progressPoints().single().derivationVersion)
    }

    private fun configuredExercise() = ExerciseCatalogItem(
        FoundationId("configured-exercise"), "sled drag", "Sled Drag", "Full Body", "Sled", "Carry",
        "Conditioning", "Intermediate", "Full Body", false, ExerciseLoggingMode.WEIGHTED,
        isUserCreated = true, createdAt = instant(1_000), updatedAt = instant(1_000),
        definitionRevision = ExerciseDefinitionRevision(3)
    )

    private fun configuredRoutine(exercise: ExerciseCatalogItem) = ReusableRoutine(
        FoundationId("configured-routine"), "Configured routine",
        listOf(
            RoutineExercise(
                FoundationId("configured-routine-exercise"), FoundationId("configured-routine"), exercise.id,
                exercise.displayName, OrderedPosition(0), plannedSets = listOf(
                    target("target-rpe", 0, EffortTarget.Rpe(80)),
                    target("target-rir", 1, EffortTarget.Rir(2)),
                    target("target-failure", 2, EffortTarget.ToFailure)
                ),
                definitionRevisionSnapshot = exercise.definitionRevision,
                resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                    userConfiguration, LoggingConfigurationSource.USER_DEFAULT
                )
            )
        ), instant(3_100), instant(3_100)
    )

    private fun target(id: String, position: Int, effort: EffortTarget) = RoutineSetTemplate(
        FoundationId(id), FoundationId("configured-routine-exercise"), OrderedPosition(position),
        WeightKg(20.0), 10, setKind = SetKind.WEIGHTED, targetDistanceMeters = 100.0 + position,
        effortTarget = effort
    )

    private fun configuredWorkout(exercise: ExerciseCatalogItem): ActiveWorkout {
        val workoutId = FoundationId("configured-active")
        val exerciseId = FoundationId("configured-active-exercise")
        return ActiveWorkout(
            workoutId, instant(4_000), exercises = listOf(
                ActiveExercise(
                    exerciseId, workoutId,
                    ExerciseReference(
                        exercise.id, exercise.displayName, false, exercise.loggingMode, exercise.equipment,
                        exercise.origin, exercise.definitionRevision, exercise.seedKey,
                        ResolvedLoggingConfiguration(workoutOverride, LoggingConfigurationSource.WORKOUT_OVERRIDE)
                    ),
                    OrderedPosition(0), sets = listOf(
                        ExerciseSet(
                            FoundationId("configured-active-set"), exerciseId, OrderedPosition(0), SetKind.WEIGHTED,
                            WeightKg(25.0), 12, instant(4_200), instant(4_100), instant(4_200),
                            captureConfigurationId = workoutOverride.id, distanceMeters = 42.0,
                            observedEffort = Effort(90, 1, FailureOutcome.NOT_REACHED)
                        )
                    )
                )
            ), createdAt = instant(4_000), updatedAt = instant(4_200)
        )
    }

    private fun configuredDraft() = PersistedSetDraft(
        FoundationId("configured-draft"), FoundationId("configured-active"),
        FoundationId("configured-active-exercise"), OrderedPosition(1), SetKind.WEIGHTED,
        10, WeightKg(30.0), updatedAt = instant(4_300), captureConfigurationId = workoutOverride.id,
        distanceMeters = 50.0, observedEffort = Effort(85, 2, FailureOutcome.REACHED)
    )

    private companion object {
        val userConfiguration = LoggingConfiguration(
            LoggingConfigurationId("configured-user-default"), LoggingSchemaVersion(1),
            listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.EXTERNAL_RESISTANCE),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
            ),
            ObservedEffortSpec(listOf(EffortKind.RPE, EffortKind.RIR, EffortKind.FAILURE_OUTCOME))
        )
        val workoutOverride = LoggingConfiguration(
            LoggingConfigurationId("configured-workout-override"), LoggingSchemaVersion(1),
            listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.REQUIRED, LoadRole.ASSISTANCE),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED)
            ),
            ObservedEffortSpec(listOf(EffortKind.FAILURE_OUTCOME, EffortKind.RIR, EffortKind.RPE))
        )
    }
}
