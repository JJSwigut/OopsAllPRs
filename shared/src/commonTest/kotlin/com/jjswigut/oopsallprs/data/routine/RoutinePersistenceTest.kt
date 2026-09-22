package com.jjswigut.oopsallprs.data.routine

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutinePersistenceTest {
    @Test
    fun routineOrderingIsStable() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000))
        assertEquals(0, harness.store.routines().single().exercises.single().position.value)
    }
}
