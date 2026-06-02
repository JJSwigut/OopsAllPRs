package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutineSeparationTest {
    @Test
    fun completedWorkoutCanBecomeRoutineWithoutLoggedTimestamps() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        val routine = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        assertEquals(completed.id, routine.sourceCompletedWorkoutId)
        assertTrue(routine.exercises.single().plannedSets.all { it.targetReps != null })
    }
}
