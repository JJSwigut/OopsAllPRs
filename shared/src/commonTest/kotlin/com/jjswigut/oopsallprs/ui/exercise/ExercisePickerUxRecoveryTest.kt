package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ExercisePickerUxRecoveryTest {
    @Test
    fun cancelPreservesExistingDraftValuesAndFocus() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        activeWorkout.hydrate(workout.id)
        val exerciseId = activeWorkout.addExercise(workout.id, harness.weightedReference).successValue()
        activeWorkout.updateDraftReps(exerciseId, 7)
        activeWorkout.updateDraftWeight(exerciseId, WeightKg(55.0))
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.dismiss()

        val block = activeWorkout.state.value.workout?.exerciseBlocks?.single()
        assertEquals(exerciseId, activeWorkout.state.value.workout?.focus?.exerciseInstanceId)
        assertEquals(7, block?.draft?.reps)
        assertEquals(55.0, block?.draft?.weight?.value)
    }
}
