package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ExercisePickerBodyweightTest {
    @Test
    fun selectedBodyweightExerciseCreatesRepsOnlyDraft() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        activeWorkout.hydrate(workout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.search("pull")
        val pullUp = picker.state.value.results.single()
        val exerciseId = picker.select(pullUp).successValue()

        val draft = activeWorkout.state.value.workout?.exerciseBlocks?.single()?.draft
        assertEquals(SetKind.BODYWEIGHT, draft?.setKind)
        assertNull(draft?.weight)

        val set = activeWorkout.confirmDraft(exerciseId).successValue()
        assertEquals(SetKind.BODYWEIGHT, set.setKind)
        assertNull(set.weight)
    }
}
