package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExercisePickerFailureTest {
    @Test
    fun addFailureKeepsPickerOpenAndActiveWorkoutUnchanged() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        activeWorkout.hydrate(workout.id)
        val originalView = activeWorkout.state.value.workout
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(FoundationId("missing-workout"))
        picker.search("bench")
        val result = picker.select(picker.state.value.results.single())

        assertTrue(result is FoundationResult.Failure)
        assertTrue(picker.state.value.isOpen)
        assertNotNull(picker.state.value.errorMessage)
        assertEquals(originalView, activeWorkout.state.value.workout)
    }
}
