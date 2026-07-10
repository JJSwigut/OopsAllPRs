package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActiveWorkoutMistakeRecoveryTest {
    @Test
    fun editDraftUpdatesLoggedRowAndLeavesLoggingDraftAvailable() = runTest {
        val harness = FoundationHarness()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val set = harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()

        holder.hydrate(workout.id, now = instant(1_300))
        holder.beginEditSet(set.id)
        holder.updateEditReps(7)
        holder.confirmEditSet(instant(1_500)).successValue()

        val block = holder.state.value.workout!!.exerciseBlocks.single()
        assertNull(holder.state.value.editDraft)
        assertEquals(7, block.loggedRows.single().reps)
        assertEquals(1, block.draft.position.value)
    }

    @Test
    fun undoLastSetRefreshesUndoAvailability() = runTest {
        val harness = FoundationHarness()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200))

        holder.hydrate(workout.id, now = instant(1_300))
        assertTrue(holder.state.value.canUndoLastSet)
        holder.undoLastLoggedSet(instant(1_500)).successValue()

        assertFalse(holder.state.value.canUndoLastSet)
        assertTrue(holder.state.value.workout!!.exerciseBlocks.single().loggedRows.isEmpty())
    }

    @Test
    fun discardConfirmationClearsActiveWorkoutState() = runTest {
        val harness = FoundationHarness()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()

        holder.hydrate(workout.id, now = instant(1_100))
        holder.requestDiscard()
        holder.confirmDiscard(instant(1_500)).successValue()

        assertNull(holder.state.value.workout)
        assertNull(harness.store.activeWorkout(workout.id))
    }

    @Test
    fun finishConfirmationCanBeRequestedAndCanceledWithoutCompletingWorkout() = runTest {
        val harness = FoundationHarness()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()

        holder.hydrate(workout.id, now = instant(1_100))
        holder.requestFinish()

        assertTrue(holder.state.value.isFinishConfirmationVisible)
        assertFalse(holder.state.value.isDiscardConfirmationVisible)

        holder.cancelFinish()

        assertFalse(holder.state.value.isFinishConfirmationVisible)
        assertEquals(workout.id, holder.state.value.workout?.workoutId)
        assertEquals(workout.id, harness.store.activeWorkout(workout.id)?.id)
    }
}
