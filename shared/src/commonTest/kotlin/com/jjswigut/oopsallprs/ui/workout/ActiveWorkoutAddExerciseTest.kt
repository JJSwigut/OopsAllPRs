package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutAddExerciseTest {
    @Test
    fun addExerciseAppendsBlockAndMovesFocusToNewDraft() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)

        val weightedId = holder.addExercise(workout.id, harness.weightedReference).successValue()
        val bodyweightId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()

        val view = holder.state.value.workout
        assertEquals(listOf(weightedId, bodyweightId), view?.exerciseBlocks?.map { it.exerciseInstanceId })
        assertEquals(bodyweightId, view?.focus?.exerciseInstanceId)
    }

    @Test
    fun bodyweightExerciseLogsRepsWithoutWeight() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()

        val initialDraft = holder.state.value.workout?.exerciseBlocks?.single()?.draft
        assertEquals(SetKind.BODYWEIGHT, initialDraft?.setKind)
        assertNull(initialDraft?.weight)

        holder.updateDraftReps(exerciseId, 12)
        val set = holder.confirmDraft(exerciseId).successValue()

        assertEquals(SetKind.BODYWEIGHT, set.setKind)
        assertEquals(12, set.reps)
        assertNull(set.weight)
        val logged = holder.state.value.workout?.exerciseBlocks?.single()?.loggedRows?.single()
        assertNull(logged?.weight)
    }
}
