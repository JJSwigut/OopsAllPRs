package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActiveWorkoutErrorTest {
    @Test
    fun invalidWeightedDraftPreservesValuesAndStaysUnlogged() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.weightedReference).successValue()
        holder.updateDraftReps(exerciseId, 7)
        holder.updateDraftWeight(exerciseId, null)

        val result = holder.confirmDraft(exerciseId)

        assertTrue(result is FoundationResult.Failure)
        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(7, block?.draft?.reps)
        assertNull(block?.draft?.weight)
        assertEquals("External resistance is required", block?.draft?.inlineError)
        assertEquals(0, block?.loggedRows?.size)
    }

    @Test
    fun invalidBodyweightDraftPreservesRepsOnlyDraftAndStaysUnlogged() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()
        holder.updateDraftReps(exerciseId, 0)

        val result = holder.confirmDraft(exerciseId)

        assertTrue(result is FoundationResult.Failure)
        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(0, block?.draft?.reps)
        assertNull(block?.draft?.weight)
        assertEquals("Repetitions must be positive", block?.draft?.inlineError)
        assertEquals(0, block?.loggedRows?.size)
    }
}
