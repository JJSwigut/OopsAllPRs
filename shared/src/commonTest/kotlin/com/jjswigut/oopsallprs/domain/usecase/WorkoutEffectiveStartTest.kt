package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class WorkoutEffectiveStartTest {
    @Test
    fun workoutPreparedDayBeforeStartsAtFirstLoggedSetByDefault() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 86_401_000)

        val completed = harness.routines.finishWorkout(workout.id, instant(86_461_000)).successValue().workout

        assertEquals(instant(86_401_000), completed.startedAt)
        assertEquals(60_000L, completed.durationMs)
    }

    @Test
    fun preferenceOffKeepsCreationTimeBehavior() = runTest {
        val harness = FoundationHarness()
        harness.store.setStartWorkoutTimerWithFirstSet(false).successValue()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 86_401_000)

        val completed = harness.routines.finishWorkout(workout.id, instant(86_461_000)).successValue().workout

        assertEquals(instant(1_000), completed.startedAt)
        assertEquals(86_460_000L, completed.durationMs)
    }

    @Test
    fun earliestLoggedSetAcrossExercisesBecomesStart() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 5_000)
        harness.logSet(workout.id, harness.bodyweightReference, loggedAtMs = 3_000)

        val completed = harness.routines.finishWorkout(workout.id, instant(8_000)).successValue().workout

        assertEquals(instant(3_000), completed.startedAt)
        assertEquals(5_000L, completed.durationMs)
    }

    @Test
    fun deletingEarliestSetMovesStartToNextRemainingSet() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val earliest = harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 3_000)
        harness.logSet(workout.id, harness.bodyweightReference, loggedAtMs = 5_000)
        harness.setLogging.deleteLoggedSet(workout.id, earliest.id, instant(6_000)).successValue()

        val completed = harness.routines.finishWorkout(workout.id, instant(8_000)).successValue().workout

        assertEquals(instant(5_000), completed.startedAt)
        assertEquals(3_000L, completed.durationMs)
    }

    @Test
    fun noLoggedSetsCannotCreateAnEmptyCompletedWorkout() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()

        val result = harness.routines.finishWorkout(workout.id, instant(4_000))

        assertIs<com.jjswigut.oopsallprs.domain.model.FoundationResult.Failure>(result)
        assertNotNull(harness.store.activeWorkout(workout.id))
        assertEquals(emptyList(), harness.store.completedWorkouts())
    }

    @Test
    fun recoveredActiveWorkoutRecomputesStartFromPersistedLoggedSets() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 10_000)
        val recoveredRoutines = RoutineUseCases(
            workouts = harness.store,
            routines = harness.store,
            activeUx = harness.store,
            preferences = harness.store
        )

        val recoveredSession = harness.lifecycle.restoreActiveSession(instant(15_000))

        val completed = recoveredRoutines.finishWorkout(workout.id, instant(20_000)).successValue().workout

        assertEquals(instant(10_000), recoveredSession?.startedAt)
        assertEquals(instant(10_000), completed.startedAt)
        assertEquals(10_000L, completed.durationMs)
    }

    @Test
    fun changingPreferenceDoesNotRewriteHistoricalWorkout() = runTest {
        val harness = FoundationHarness()
        harness.store.setStartWorkoutTimerWithFirstSet(false).successValue()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.logSet(workout.id, harness.weightedReference, loggedAtMs = 3_000)
        val completed = harness.routines.finishWorkout(workout.id, instant(5_000)).successValue().workout

        harness.store.setStartWorkoutTimerWithFirstSet(true).successValue()
        val historical = harness.store.completedWorkout(completed.id)

        assertEquals(instant(1_000), historical?.startedAt)
        assertEquals(4_000L, historical?.durationMs)
    }
}

private suspend fun FoundationHarness.logSet(
    workoutId: FoundationId,
    reference: ExerciseReference,
    loggedAtMs: Long
): ExerciseSet {
    val exercise = setLogging.addExercise(workoutId, reference, instant(loggedAtMs - 100)).successValue()
    return setLogging.confirmSet(
        activeWorkoutId = workoutId,
        exerciseInstanceId = exercise.id,
        setKind = if (reference.isBodyweight) SetKind.BODYWEIGHT else SetKind.WEIGHTED,
        reps = 5,
        weight = if (reference.isBodyweight) null else WeightKg(100.0),
        position = 0,
        loggedAt = instant(loggedAtMs)
    ).successValue()
}
