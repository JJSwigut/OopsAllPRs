package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ActiveWorkoutStateHolderTest {
    @Test
    fun hydrateCreatesDefaultDraftForExercise() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)

        holder.hydrate(workout.id)

        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(exercise.id, block?.exerciseInstanceId)
        assertEquals(5, block?.draft?.reps)
        assertNotNull(block?.draft?.weight)
    }

    @Test
    fun oneTapConfirmMovesDraftToLoggedHistory() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        holder.updateDraftReps(exercise.id, 8)

        holder.confirmDraft(exercise.id).successValue()

        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(1, block?.loggedRows?.size)
        assertEquals(8, block?.loggedRows?.single()?.reps)
        assertEquals(1, block?.draft?.position?.value)
    }

    @Test
    fun confirmingNonFirstExerciseKeepsFocusOnNextDraftForSameExercise() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val first = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val second = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        holder.setFocus(second.id, instant(1_300))

        holder.confirmDraft(second.id).successValue()

        val view = holder.state.value.workout
        assertEquals(listOf(first.id, second.id), view?.exerciseBlocks?.map { it.exerciseInstanceId })
        assertEquals(second.id, view?.focus?.exerciseInstanceId)
        assertEquals(1, view?.exerciseBlocks?.first { it.exerciseInstanceId == second.id }?.draft?.position?.value)
    }
}
