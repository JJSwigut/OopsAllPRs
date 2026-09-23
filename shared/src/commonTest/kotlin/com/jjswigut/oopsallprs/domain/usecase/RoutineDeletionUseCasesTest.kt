package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoutineDeletionUseCasesTest {
    @Test
    fun deleteCompletedWorkoutRebuildsPersonalRecordsFromRemainingWorkouts() = runTest {
        val harness = FoundationHarness()
        val first = harness.completedWeightedWorkout(weight = 100.0, finishedAt = 2_000)
        val second = harness.completedWeightedWorkout(weight = 120.0, finishedAt = 4_000)

        assertTrue(harness.store.personalRecords().any { it.sourceWorkoutId == second.id })

        harness.routines.deleteCompletedWorkout(second.id, instant(5_000)).successValue()

        assertNull(harness.store.completedWorkout(second.id))
        assertNotNull(harness.store.completedWorkout(first.id))
        assertTrue(harness.store.personalRecords().all { it.sourceWorkoutId == first.id })
        assertTrue(harness.store.personalRecords().any { it.weight == WeightKg(100.0) })
    }

    @Test
    fun deleteRoutineRetiresTemplateWithoutDeletingSourceWorkout() = runTest {
        val harness = FoundationHarness()
        val completed = harness.completedWeightedWorkout(weight = 100.0, finishedAt = 2_000)
        val routine = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Bench day", instant(2_500)).successValue()

        harness.routines.deleteRoutine(routine.id, instant(3_000)).successValue()

        assertTrue(harness.routines.listRoutines().isEmpty())
        assertNotNull(harness.store.completedWorkout(completed.id))
        assertTrue(harness.lifecycle.startFromRoutine(routine.id, instant(3_500)) is FoundationResult.Failure)
    }
}

private suspend fun FoundationHarness.completedWeightedWorkout(
    weight: Double,
    finishedAt: Long
) = run {
    val workout = lifecycle.startEmpty(instant(finishedAt - 1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(finishedAt - 900)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.WEIGHTED,
        reps = 5,
        weight = WeightKg(weight),
        position = 0,
        loggedAt = instant(finishedAt - 800)
    )
    routines.finishWorkout(workout.id, instant(finishedAt)).successValue().workout
}
