package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ExercisePickerCancelTest {
    @Test
    fun dismissClosesPickerWithoutChangingActiveWorkout() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        activeWorkout.hydrate(workout.id)
        val originalBlocks = activeWorkout.state.value.workout?.exerciseBlocks.orEmpty()
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.search("bench")
        picker.dismiss()

        assertFalse(picker.state.value.isOpen)
        assertEquals(originalBlocks, activeWorkout.state.value.workout?.exerciseBlocks.orEmpty())
    }
}
