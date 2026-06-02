package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutBodyweightUxTest {
    @Test
    fun bodyweightDraftDefaultsToRepsOnlyAndConfirmsWithoutWeight() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()

        val draft = holder.state.value.workout?.exerciseBlocks?.single()?.draft
        assertEquals(SetKind.BODYWEIGHT, draft?.setKind)
        assertNull(draft?.weight)

        holder.updateDraftReps(exerciseId, 10)
        holder.confirmDraft(exerciseId).successValue()

        val logged = holder.state.value.workout?.exerciseBlocks?.single()?.loggedRows?.single()
        assertEquals(10, logged?.reps)
        assertNull(logged?.weight)
    }
}
