package com.jjswigut.oopsallprs.data.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

class FinishWorkoutLedgerTest {
    @Test
    fun finishPreservesLoggedSetsOnly() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        assertEquals(1, completed.exercises.single().loggedSets.size)
        assertEquals(800, completed.durationMs)
    }

    @Test
    fun emptyActiveWorkoutCannotFinishOrConsumeFreeAllowance() = runTest {
        val harness = FoundationHarness()
        val active = harness.lifecycle.startEmpty(instant(1_000)).successValue()

        val result = harness.store.finishActiveWorkout(active.id, instant(2_000))

        assertIs<FoundationResult.Failure>(result)
        assertNotNull(harness.store.activeWorkout(active.id))
        assertEquals(0, harness.store.completedWorkouts().size)
        assertEquals(0, harness.store.loadFullAccess().completedFreeWorkouts)
    }
}
