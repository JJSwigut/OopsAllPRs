package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActiveWorkoutRestTimerTest {
    @Test
    fun loggingSetAutoStartsRestAndKeepsNextDraftReady() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)

        val logged = holder.confirmDraft(exercise.id).successValue()

        val view = assertNotNull(holder.state.value.workout)
        val rest = assertNotNull(view.activeRest)
        val block = view.exerciseBlocks.single()
        assertEquals(logged.id, rest.originSetId)
        assertTrue(rest.remainingMillis in 1L..120_000L)
        assertEquals(1, block.loggedRows.size)
        assertEquals(1, block.draft.position.value)
        assertEquals(exercise.id, view.focus?.exerciseInstanceId)
    }

    @Test
    fun disabledExerciseRestDoesNotAutoStartAfterLogging() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)

        holder.toggleExerciseRest(exercise.id).successValue()
        assertFalse(assertNotNull(holder.state.value.workout).exerciseBlocks.single().rest.isEnabled)
        holder.confirmDraft(exercise.id).successValue()

        assertNull(holder.state.value.workout?.activeRest)
    }
}
