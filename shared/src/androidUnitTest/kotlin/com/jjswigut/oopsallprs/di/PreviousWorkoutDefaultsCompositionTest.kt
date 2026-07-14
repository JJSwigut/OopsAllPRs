package com.jjswigut.oopsallprs.di

import com.jjswigut.oopsallprs.data.repository.SqlFoundationStore
import com.jjswigut.oopsallprs.data.repository.SqlFoundationStoreTestHarness
import com.jjswigut.oopsallprs.data.repository.SqlWorkoutRepository
import com.jjswigut.oopsallprs.data.repository.instant
import com.jjswigut.oopsallprs.data.repository.successValue
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import kotlinx.coroutines.test.runTest
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreviousWorkoutDefaultsCompositionTest {
    @Test
    fun foundationModuleInjectsFoundationStoreForNonLegacyCaptureResolution() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val application = koinApplication {
            allowOverride(true)
            modules(
                foundationModule,
                module { single<WorkoutDatabase> { harness.database } }
            )
        }
        val store = application.koin.get<SqlFoundationStore>()
        val workouts = application.koin.get<SqlWorkoutRepository>()
        val defaults = application.koin.get<PreviousWorkoutDefaultsUseCase>()
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("composition-distance-v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        store.saveLoggingConfiguration(configuration).successValue()
        val completedId = FoundationId("composition-completed")
        workouts.finishWorkout(
            CompletedWorkout(
                id = completedId,
                sourceActiveWorkoutId = FoundationId("composition-active"),
                startedAt = instant(1_000),
                finishedAt = instant(2_000),
                durationMs = 1_000,
                routineId = null,
                exercises = listOf(
                    CompletedExercise(
                        id = FoundationId("composition-exercise"),
                        completedWorkoutId = completedId,
                        exerciseCatalogId = FoundationId("composition-distance-exercise"),
                        displayNameSnapshot = "Distance",
                        position = OrderedPosition(0),
                        loggedSets = listOf(
                            ExerciseSet(
                                id = FoundationId("composition-distance-set"),
                                exerciseInstanceId = FoundationId("composition-active-exercise"),
                                position = OrderedPosition(0),
                                setKind = SetKind.BODYWEIGHT,
                                weight = null,
                                reps = null,
                                loggedAt = instant(1_500),
                                createdAt = instant(1_400),
                                updatedAt = instant(1_500),
                                captureConfigurationId = configuration.id,
                                distanceMeters = 800.0
                            )
                        )
                    )
                ),
                createdAt = instant(2_000)
            )
        ).successValue()

        val previous = defaults.valueFor(
            exerciseCatalogId = FoundationId("composition-distance-exercise"),
            isBodyweight = true,
            setIndex = 0,
            loggingConfiguration = configuration
        )

        assertNull(previous?.reps)
        assertEquals(800.0, previous?.distanceMeters)
        application.close()
    }
}
