package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class ActiveWorkoutConflictTest {
    @Test
    fun startEmptyRejectsSecondActiveWorkout() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()

        val result = harness.lifecycle.startEmpty(instant(2_000))

        assertTrue(result is FoundationResult.Failure)
    }

    @Test
    fun startFromRoutineRejectsSecondActiveWorkout() = runTest {
        val harness = FoundationHarness()
        val sourceWorkoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(sourceWorkoutId, instant(2_000)).successValue()
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        harness.lifecycle.startEmpty(instant(4_000)).successValue()

        val result = harness.lifecycle.startFromRoutine(template.id, instant(5_000))

        assertTrue(result is FoundationResult.Failure)
    }
}
