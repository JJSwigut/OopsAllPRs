package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ExercisePickerCustomExerciseTest {
    @Test
    fun createCustomBodyweightExerciseSavesAppendsAndBecomesSearchable() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        activeWorkout.hydrate(workout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.search("Ring Row")
        picker.showCustomCreation()
        picker.updateCustomName("Ring Row")
        picker.updateCustomBodyweight(true)
        picker.createCustom().successValue()

        assertFalse(picker.state.value.isOpen)
        val block = activeWorkout.state.value.workout?.exerciseBlocks?.single()
        assertEquals("Ring Row", block?.displayName)
        assertEquals(SetKind.BODYWEIGHT, block?.draft?.setKind)
        assertNull(block?.draft?.weight)
        assertEquals(listOf("Ring Row"), harness.exerciseCatalog.search("ring").map { it.displayName })
    }
}
