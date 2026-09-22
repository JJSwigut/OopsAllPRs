package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ProgressComparisonWindow
import com.jjswigut.oopsallprs.domain.model.ProgressTrendState
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EvidenceComparisonDisclosureTest {
    @Test
    fun consistencyDisclosesBoundarySessionUsedByRecentSpacing() = runTest {
        val harness = FoundationHarness()
        listOf(0, 1, 2, 3, 4, 40).forEach { day ->
            harness.finishDisclosureSet(day, harness.weightedReference, 20.0)
        }

        val consistency = harness.disclosureSnapshot().overallReadings.single {
            it.reading == EvidenceReading.CONSISTENCY
        }

        assertEquals(6, consistency.coverage.qualifyingSessions)
        assertEquals(40, consistency.coverage.spanDays)
        // Recent gaps are 1, 1, 36 days only when the day-2 boundary is included.
        assertEquals(ProgressTrendState.STEADY, consistency.state)
        val window = assertNotNull(consistency.comparisonWindow)
        assertEquals(finishedAt(0), window.earlierStart)
        assertEquals(finishedAt(2), window.earlierEnd)
        assertEquals(finishedAt(2), window.recentStart)
        assertEquals(finishedAt(40), window.recentEnd)
    }

    @Test
    fun overallCapabilityDisclosesEachExercisesActualWindowsAndNormalizedChange() = runTest {
        val harness = FoundationHarness()
        val firstExercise = harness.weightedReference
        val secondExercise = firstExercise.copy(
            exerciseCatalogId = FoundationId("comparison-second-exercise"),
            displayNameSnapshot = "Second press"
        )
        val firstSchedule = listOf(0, 1, 2, 3, 4, 28).mapIndexed { index, day ->
            Triple(day, firstExercise, if (index < 3) 20.0 else 24.0)
        }
        // Keep day 0's logged set inside the window ending at day 55's finish.
        val secondSchedule = listOf(10, 20, 30, 40, 50, 55).mapIndexed { index, day ->
            Triple(day, secondExercise, if (index < 3) 100.0 else 90.0)
        }
        (firstSchedule + secondSchedule).sortedBy { it.first }.forEach { (day, exercise, weight) ->
            harness.finishDisclosureSet(day, exercise, weight)
        }

        val capability = harness.disclosureSnapshot().overallReadings.single {
            it.reading == EvidenceReading.CAPABILITY
        }

        assertEquals(2, capability.coverage.contributingExercises)
        assertEquals(5.0, assertNotNull(capability.changePercent), absoluteTolerance = 0.000001)
        assertEquals(2, capability.exerciseComparisons.size)
        val firstComparison = capability.exerciseComparisons.single {
            it.exerciseCatalogId == firstExercise.exerciseCatalogId
        }
        val secondComparison = capability.exerciseComparisons.single {
            it.exerciseCatalogId == secondExercise.exerciseCatalogId
        }
        assertEquals(firstExercise.displayNameSnapshot, firstComparison.exerciseName)
        assertEquals(secondExercise.displayNameSnapshot, secondComparison.exerciseName)
        assertEquals(20.0, firstComparison.changePercent, absoluteTolerance = 0.000001)
        assertEquals(-10.0, secondComparison.changePercent, absoluteTolerance = 0.000001)
        assertEquals(
            ProgressComparisonWindow(loggedAt(0), loggedAt(2), loggedAt(3), loggedAt(28)),
            firstComparison.comparisonWindow
        )
        assertEquals(
            ProgressComparisonWindow(loggedAt(10), loggedAt(30), loggedAt(40), loggedAt(55)),
            secondComparison.comparisonWindow
        )
    }

    private suspend fun FoundationHarness.disclosureSnapshot() = EvidenceLadderUseCase().project(
        workouts = store.completedWorkouts(),
        records = store.personalRecords(),
        points = store.progressPoints()
    )

    private suspend fun FoundationHarness.finishDisclosureSet(
        day: Int,
        reference: ExerciseReference,
        weightKg: Double
    ) {
        val startedAt = 1_000L + day * 86_400_000L
        val workout = lifecycle.startEmpty(instant(startedAt)).successValue()
        val exercise = setLogging.addExercise(
            workout.id,
            reference,
            instant(startedAt + 100)
        ).successValue()
        setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 8,
            weight = WeightKg(weightKg),
            position = 0,
            loggedAt = loggedAt(day)
        ).successValue()
        routines.finishWorkout(workout.id, finishedAt(day)).successValue().workout
    }

    private fun loggedAt(day: Int) = instant(1_200L + day * 86_400_000L)

    private fun finishedAt(day: Int) = instant(2_000L + day * 86_400_000L)
}
