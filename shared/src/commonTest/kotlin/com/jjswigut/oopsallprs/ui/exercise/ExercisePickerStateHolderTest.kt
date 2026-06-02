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

class ExercisePickerStateHolderTest {
    @Test
    fun selectingSeededExerciseAppendsBlockAndMovesFocus() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        activeWorkout.hydrate(workout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.search("bench")
        val bench = picker.state.value.results.single()
        val exerciseId = picker.select(bench).successValue()

        assertFalse(picker.state.value.isOpen)
        val block = activeWorkout.state.value.workout?.exerciseBlocks?.single()
        assertEquals(exerciseId, block?.exerciseInstanceId)
        assertEquals("Bench Press", block?.displayName)
        assertEquals(exerciseId, activeWorkout.state.value.workout?.focus?.exerciseInstanceId)
    }
}
