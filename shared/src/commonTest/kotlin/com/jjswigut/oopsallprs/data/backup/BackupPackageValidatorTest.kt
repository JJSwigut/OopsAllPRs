package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupPackageValidatorTest {
    @Test
    fun historicalUserDefaultSnapshotsRemainValidAfterCurrentDefaultChanges() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val definition = harness.store.all().single { it.canonicalName == "bench press" }
        val oldDefault = configuration("old-user-default", MeasureRequirement.REQUIRED)
        val newDefault = configuration("new-user-default", MeasureRequirement.OPTIONAL)
        harness.store.saveLoggingConfiguration(oldDefault).successValue()
        harness.store.saveLoggingConfiguration(newDefault).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(definition.id, oldDefault, definition.definitionRevision, instant(1_000))
        ).successValue()
        val oldSnapshot = ResolvedLoggingConfiguration(oldDefault, LoggingConfigurationSource.USER_DEFAULT)
        harness.store.saveRoutine(
            ReusableRoutine(
                FoundationId("routine-old-default"),
                "Old default",
                listOf(
                    RoutineExercise(
                        FoundationId("routine-exercise-old"), FoundationId("routine-old-default"), definition.id,
                        definition.displayName, OrderedPosition(0), plannedSets = listOf(
                            RoutineSetTemplate(
                                FoundationId("routine-set-old"), FoundationId("routine-exercise-old"),
                                OrderedPosition(0), null, 8, setKind = SetKind.WEIGHTED
                            )
                        ),
                        definitionRevisionSnapshot = definition.definitionRevision,
                        seedKeySnapshot = definition.seedKey,
                        resolvedLoggingConfiguration = oldSnapshot
                    )
                ),
                instant(1_100),
                instant(1_100)
            )
        ).successValue()
        harness.store.createActiveWorkout(
            ActiveWorkout(
                FoundationId("active-old-default"), instant(1_200), exercises = listOf(
                    ActiveExercise(
                        FoundationId("active-exercise-old"), FoundationId("active-old-default"),
                        ExerciseReference(
                            definition.id, definition.displayName, definition.isBodyweight, definition.loggingMode,
                            definition.equipment, definition.origin, definition.definitionRevision,
                            definition.seedKey, oldSnapshot
                        ),
                        OrderedPosition(0)
                    )
                ),
                createdAt = instant(1_200),
                updatedAt = instant(1_200)
            )
        ).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(definition.id, newDefault, definition.definitionRevision, instant(2_000))
        ).successValue()
        val reader = BackupSnapshotReader(
            harness.store, harness.store, harness.store, harness.store,
            harness.store, harness.store, harness.store
        )

        val original = reader.createPackage(instant(3_000)).successValue()
        val decoded = BackupPackageCodec().decode(BackupPackageCodec().encode(original).successValue()).successValue()

        assertEquals("new-user-default", decoded.userExerciseConfigurations.single().loggingConfigurationId)
        assertEquals("old-user-default", decoded.routines.single().exercises.single().loggingConfigurationId)
        assertEquals("old-user-default", decoded.activeWorkout!!.exercises.single().loggingConfigurationId)
    }

    private fun configuration(id: String, requirement: MeasureRequirement) = LoggingConfiguration(
        LoggingConfigurationId(id), LoggingSchemaVersion(1),
        listOf(
            MeasureSpec(MeasureKind.REPETITIONS, requirement),
            MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
        )
    )
}
