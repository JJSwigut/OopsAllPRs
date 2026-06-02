package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SetLoggingRecoveryUseCasesTest {
    @Test
    fun editLoggedSetPreservesIdentityAndLoggedAt() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()

        val edited = harness.setLogging.editLoggedSet(
            activeWorkoutId = workout.id,
            setId = logged.id,
            reps = 6,
            weight = WeightKg(102.5),
            now = instant(1_500)
        ).successValue()

        val stored = harness.store.activeWorkout(workout.id)!!
            .exercises.single()
            .sets.single()
        assertEquals(logged.id, edited.id)
        assertEquals(logged.loggedAt, edited.loggedAt)
        assertEquals(instant(1_500), edited.editedAt)
        assertEquals(6, stored.reps)
        assertEquals(WeightKg(102.5), stored.weight)
    }

    @Test
    fun bodyweightEditAllowsRepsOnly() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.BODYWEIGHT,
            reps = 8,
            weight = null,
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()

        val edited = harness.setLogging.editLoggedSet(
            activeWorkoutId = workout.id,
            setId = logged.id,
            reps = 10,
            weight = null,
            now = instant(1_500)
        ).successValue()

        assertEquals(10, edited.reps)
        assertNull(edited.weight)
    }

    @Test
    fun undoLastLoggedSetRemovesMostRecentSet() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val first = harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        val second = harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 4, WeightKg(110.0), 1, instant(1_300)).successValue()

        val undone = harness.setLogging.undoLastLoggedSet(workout.id, instant(1_500)).successValue()

        val remaining = harness.store.activeWorkout(workout.id)!!
            .exercises.single()
            .sets.single()
        assertEquals(second.id, undone.id)
        assertEquals(first.id, remaining.id)
    }
}
