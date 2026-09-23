package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.instant
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ConfigurationDrivenPersonalRecordDerivationTest {
    @Test
    fun positiveAddedBodyweightLoadDerivesLoadedPerformanceMetrics() = runTest {
        val configuration = repsAndLoad("added-load", LoadRole.ADDED_TO_BODYWEIGHT)
        val result = derive(
            configuration,
            loggedSet("loaded-bodyweight", configuration, SetKind.BODYWEIGHT, reps = 5, weight = 20.0)
        )

        assertEquals(
            setOf("weight_for_reps", "estimated_one_rep_max", "volume"),
            result.points.map { it.metricCode.value }.toSet()
        )
        assertEquals(3, result.records.size)
        assertTrue(result.records.all { it.derivationVersion == ProgressDerivationVersions.CURRENT })
        assertFalse(result.points.any { it.metricCode == ProgressEvidenceMetric.REPS.wireCode })
    }

    @Test
    fun zeroAndUnloadedAddedBodyweightLoadDeriveRepsOnly() = runTest {
        val configuration = repsAndLoad("unloaded-bodyweight", LoadRole.ADDED_TO_BODYWEIGHT)
        val result = derive(
            configuration,
            loggedSet("unloaded", configuration, SetKind.WEIGHTED, reps = 10, weight = null),
            loggedSet("zero-added", configuration, SetKind.BODYWEIGHT, reps = 12, weight = 0.0, position = 1)
        )

        assertEquals(listOf("reps", "reps"), result.points.map { it.metricCode.value })
        assertEquals(listOf(10.0, 12.0), result.records.map { it.value })
        assertTrue(result.records.all { it.metricCode.value == "reps" })
    }

    @Test
    fun assistanceLoadRemainsRepsOnly() = runTest {
        val configuration = repsAndLoad("assistance", LoadRole.ASSISTANCE)
        val result = derive(
            configuration,
            loggedSet("assisted", configuration, SetKind.WEIGHTED, reps = 8, weight = 35.0)
        )

        assertEquals(listOf("reps"), result.points.map { it.metricCode.value })
        assertEquals(8.0, result.records.single().value)
    }

    @Test
    fun legacyUnspecifiedLoadRemainsRepsOnly() = runTest {
        val configuration = repsAndLoad("legacy-unspecified", LoadRole.LEGACY_UNSPECIFIED)
        val result = derive(
            configuration,
            loggedSet("legacy", configuration, SetKind.BODYWEIGHT, reps = 9, weight = 15.0)
        )

        assertEquals(listOf("reps"), result.points.map { it.metricCode.value })
        assertEquals(9.0, result.records.single().value)
    }

    @Test
    fun externalResistancePreservesWeightedBestSetE1rmAndVolume() = runTest {
        val configuration = repsAndLoad("external", LoadRole.EXTERNAL_RESISTANCE)
        val result = derive(
            configuration,
            loggedSet("external", configuration, SetKind.WEIGHTED, reps = 5, weight = 100.0)
        )

        assertEquals(100.0, result.point(ProgressEvidenceMetric.WEIGHT_FOR_REPS).value)
        assertTrue(abs(result.point(ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX).value - 116.6666) < 0.01)
        assertEquals(500.0, result.point(ProgressEvidenceMetric.VOLUME).value)
    }

    @Test
    fun durationConfigurationPreservesLongestDurationBehavior() = runTest {
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("duration-only"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DURATION, MeasureRequirement.REQUIRED))
        )
        val result = derive(
            configuration,
            loggedSet(
                id = "duration",
                configuration = configuration,
                setKind = SetKind.WEIGHTED,
                reps = null,
                weight = null,
                durationMs = 75_000L
            )
        )

        assertEquals(75_000.0, result.point(ProgressEvidenceMetric.LONGEST_DURATION).value)
        assertEquals("longest_duration", result.records.single().metricCode.value)
    }

    @Test
    fun distanceConfigurationDerivesLongestDistance() = runTest {
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("distance-only"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        val result = derive(
            configuration,
            loggedSet(
                id = "distance-short",
                configuration = configuration,
                setKind = SetKind.TIMED,
                reps = null,
                weight = null,
                distanceMeters = 1_000.0
            ),
            loggedSet(
                id = "distance-long",
                configuration = configuration,
                setKind = SetKind.BODYWEIGHT,
                reps = null,
                weight = null,
                distanceMeters = 1_500.0,
                position = 1
            )
        )

        assertEquals(2, result.points.size)
        assertEquals(2, result.records.size)
        val record = result.records.maxBy { it.value }
        assertEquals(1_500.0, record.value)
        assertEquals("longest_distance", record.metricCode.value)
        assertEquals(FoundationId("distance-long"), record.sourceSetId)
    }

    @Test
    fun metricCodesAreStableAndLegacyEvidenceGetsVersionOneDefaults() {
        assertEquals(
            listOf(
                "weight_for_reps",
                "reps",
                "estimated_one_rep_max",
                "volume",
                "longest_duration",
                "longest_distance"
            ),
            ProgressEvidenceMetric.entries.map { it.wireCode.value }
        )
        ProgressEvidenceMetric.entries.forEach { metric ->
            assertEquals(metric, ProgressEvidenceMetric.fromWireCode(metric.wireCode.value))
        }

        val legacyPoint = com.jjswigut.oopsallprs.domain.model.ProgressPoint(
            id = FoundationId("legacy-point"),
            exerciseCatalogId = FoundationId("exercise"),
            sourceWorkoutId = FoundationId("workout"),
            sourceSetId = FoundationId("set"),
            metric = com.jjswigut.oopsallprs.domain.model.ProgressMetric.BEST_SET,
            value = 10.0,
            weight = WeightKg(10.0),
            reps = 5,
            recordedAt = instant(1_000)
        )

        assertEquals("weight_for_reps", legacyPoint.metricCode.value)
        assertEquals(ProgressDerivationVersions.LEGACY_SET_KIND, legacyPoint.derivationVersion)
    }
}

private data class DerivationResult(
    val records: List<com.jjswigut.oopsallprs.domain.model.PersonalRecord>,
    val points: List<com.jjswigut.oopsallprs.domain.model.ProgressPoint>
) {
    fun point(metric: ProgressEvidenceMetric) =
        assertNotNull(points.singleOrNull { it.metricCode == metric.wireCode })
}

private suspend fun derive(
    configuration: LoggingConfiguration,
    vararg sets: ExerciseSet
): DerivationResult {
    val store = InMemoryFoundationStore()
    store.saveLoggingConfiguration(configuration)
    PersonalRecordDerivationUseCase(store).rebuildFrom(listOf(completedWorkout(sets.toList())))
    return DerivationResult(store.personalRecords(), store.progressPoints())
}

private fun repsAndLoad(id: String, loadRole: LoadRole): LoggingConfiguration =
    LoggingConfiguration(
        id = LoggingConfigurationId(id),
        schemaVersion = LoggingSchemaVersion(1),
        measures = listOf(
            MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
            MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, loadRole)
        )
    )

private fun completedWorkout(sets: List<ExerciseSet>): CompletedWorkout =
    CompletedWorkout(
        id = FoundationId("completed-workout"),
        sourceActiveWorkoutId = FoundationId("active-workout"),
        startedAt = instant(1_000),
        finishedAt = instant(2_000),
        durationMs = 1_000,
        routineId = null,
        exercises = listOf(
            CompletedExercise(
                id = FoundationId("completed-exercise"),
                completedWorkoutId = FoundationId("completed-workout"),
                exerciseCatalogId = FoundationId("exercise"),
                displayNameSnapshot = "Exercise",
                position = OrderedPosition(0),
                loggedSets = sets
            )
        ),
        createdAt = instant(2_000)
    )

private fun loggedSet(
    id: String,
    configuration: LoggingConfiguration,
    setKind: SetKind,
    reps: Int?,
    weight: Double?,
    durationMs: Long? = null,
    distanceMeters: Double? = null,
    position: Int = 0
): ExerciseSet =
    ExerciseSet(
        id = FoundationId(id),
        exerciseInstanceId = FoundationId("exercise-instance"),
        position = OrderedPosition(position),
        setKind = setKind,
        weight = weight?.let(::WeightKg),
        reps = reps,
        loggedAt = instant(1_500L + position),
        createdAt = instant(1_400L + position),
        updatedAt = instant(1_500L + position),
        durationMs = durationMs,
        captureConfigurationId = configuration.id,
        distanceMeters = distanceMeters
    )
