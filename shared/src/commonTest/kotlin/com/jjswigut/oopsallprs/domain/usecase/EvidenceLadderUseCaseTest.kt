package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EvidenceLadderUseCaseTest {
    @Test
    fun overallCapabilityNormalizesOnlyMatureExerciseTrends() = runTest {
        val harness = FoundationHarness()
        listOf(0, 7, 14, 21, 28, 35).forEach { day ->
            harness.finishWeightedSet(reps = 8, weightKg = 25.0, day = day)
        }
        val sparseReference = harness.weightedReference.copy(
            exerciseCatalogId = FoundationId("sparse-weighted-exercise"),
            displayNameSnapshot = "Sparse press"
        )
        harness.finishWeightedSet(reps = 8, weightKg = 10.0, day = 0, reference = sparseReference)
        harness.finishWeightedSet(reps = 8, weightKg = 100.0, day = 35, reference = sparseReference)

        val capability = harness.evidenceLadder().overallReadings.single {
            it.reading == EvidenceReading.CAPABILITY
        }

        assertEquals(ProgressTrendState.HOLDING_STEADY, capability.state)
        assertEquals(1, capability.coverage.contributingExercises)
    }

    @Test
    fun longReturnGapKeepsAllTimeAchievementButNeedsNewRecentEvidence() = runTest {
        val harness = FoundationHarness()
        listOf(0, 7, 14, 21, 28, 90).forEach { day ->
            harness.finishWeightedSet(reps = 8, weightKg = 25.0, day = day)
        }

        val ladder = harness.evidenceLadder()

        assertTrue(ladder.achievements.any { it.reps == 8 && it.weight?.value == 25.0 })
        assertEquals(
            ProgressTrendState.BUILDING_TREND,
            ladder.exerciseProgressions.single().capability.state
        )
        assertEquals(
            ProgressTrendState.BUILDING_TREND,
            ladder.overallReadings.single { it.reading == EvidenceReading.CONSISTENCY }.state
        )
        assertEquals(1, ladder.overallReadings.single { it.reading == EvidenceReading.CONSISTENCY }
            .coverage.qualifyingSessions)
    }

    @Test
    fun exactRepAchievementCanExpandWhileMatureCapabilityHoldsSteady() = runTest {
        val harness = FoundationHarness()
        listOf(0, 7, 14, 21, 28).forEach { day ->
            harness.finishWeightedSet(reps = 7, weightKg = 25.0, day = day)
        }
        harness.finishWeightedSet(reps = 8, weightKg = 20.0, day = 35)

        val progression = harness.evidenceLadder().exerciseProgressions.single()

        assertEquals(ProgressTrendState.HOLDING_STEADY, progression.capability.state)
        assertTrue(progression.capability.explanation.contains("8-rep benchmark expanded"))
        assertEquals(setOf(7, 8), progression.currentBenchmarks.mapNotNull { it.series.reps }.toSet())
    }

    @Test
    fun overallWorkCapacityAndConsistencyUseTransparentSessionEvidence() = runTest {
        val harness = FoundationHarness()
        listOf(0, 7, 14).forEach { day ->
            harness.finishWeightedSet(reps = 8, weightKg = 20.0, day = day)
        }
        listOf(21, 28, 35).forEach { day ->
            harness.finishWeightedSet(reps = 8, weightKg = 25.0, day = day)
        }

        val ladder = harness.evidenceLadder()
        val workCapacity = ladder.overallReadings.single { it.reading == EvidenceReading.WORK_CAPACITY }
        val consistency = ladder.overallReadings.single { it.reading == EvidenceReading.CONSISTENCY }

        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, workCapacity.state)
        assertTrue(workCapacity.explanation.contains("load × reps"))
        assertEquals(6, workCapacity.evidence.map { it.sourceWorkoutId }.distinct().size)
        assertTrue(workCapacity.evidence.all { it.sourceSetId != null })
        assertEquals(ProgressTrendState.STEADY, consistency.state)
        assertEquals(6, consistency.evidence.map { it.sourceWorkoutId }.distinct().size)
    }

    @Test
    fun capabilityStaysBuildingUntilSixQualifiedSessionsSpanTwentyEightDays() = runTest {
        val harness = FoundationHarness()
        listOf(0, 7, 14, 21, 27).forEachIndexed { index, day ->
            harness.finishWeightedSet(reps = 8, weightKg = 20.0 + index, day = day)
        }

        val fiveSessions = harness.evidenceLadder()
            .exerciseProgressions.single().capability
        assertEquals(ProgressTrendState.BUILDING_TREND, fiveSessions.state)
        assertEquals(5, fiveSessions.coverage.qualifyingSessions)

        harness.finishWeightedSet(reps = 8, weightKg = 26.0, day = 28)
        val mature = harness.evidenceLadder()
            .exerciseProgressions.single().capability

        assertEquals(ProgressTrendState.RECENT_RANGE_HIGHER, mature.state)
        assertEquals(6, mature.coverage.qualifyingSessions)
        assertEquals(28, mature.coverage.spanDays)
        assertEquals(6, mature.evidence.size)
        assertTrue(mature.comparisonWindow != null)
    }

    @Test
    fun lowerLoadAtNewRepCountCreatesASecondAchievementAndBenchmark() = runTest {
        val harness = FoundationHarness()
        harness.finishWeightedSet(reps = 7, weightKg = 25.0, day = 0)
        harness.finishWeightedSet(reps = 8, weightKg = 20.0, day = 7)

        val snapshot = EvidenceLadderUseCase().project(
            workouts = harness.store.completedWorkouts(),
            records = harness.store.personalRecords(),
            points = harness.store.progressPoints()
        )
        val weightedAchievements = snapshot.achievements.filter {
            it.series.metric == ProgressEvidenceMetric.WEIGHT_FOR_REPS
        }

        assertEquals(setOf(7, 8), weightedAchievements.map { it.series.reps }.toSet())
        assertEquals(setOf(7, 8), snapshot.currentBenchmarks.mapNotNull { it.series.reps }.toSet())
        assertTrue(weightedAchievements.any { it.weight?.value == 25.0 && it.reps == 7 })
        assertTrue(weightedAchievements.any { it.weight?.value == 20.0 && it.reps == 8 })
    }
}

private suspend fun FoundationHarness.evidenceLadder() = EvidenceLadderUseCase().project(
    workouts = store.completedWorkouts(),
    records = store.personalRecords(),
    points = store.progressPoints()
)

private suspend fun FoundationHarness.finishWeightedSet(
    reps: Int,
    weightKg: Double,
    day: Int,
    reference: ExerciseReference? = null
) {
    val startedAt = 1_000L + day * 86_400_000L
    val workout = lifecycle.startEmpty(instant(startedAt)).successValue()
    val exercise = setLogging.addExercise(
        workout.id,
        reference ?: weightedReference,
        instant(startedAt + 100)
    ).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        SetKind.WEIGHTED,
        reps,
        WeightKg(weightKg),
        0,
        instant(startedAt + 200)
    ).successValue()
    routines.finishWorkout(workout.id, instant(startedAt + 1_000)).successValue().workout
}
