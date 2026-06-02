package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActiveWorkoutPendingTest {
    @Test
    fun pendingDraftSuppressesDuplicateConfirm() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        val pending = SetRowDraft(
            draftId = FoundationId("draft-pending"),
            exerciseInstanceId = exercise.id,
            position = com.jjswigut.oopsallprs.domain.model.OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(10.0),
            isPending = true
        )
        holder.restoreDraftForTest(pending)

        val result = holder.confirmDraft(exercise.id)

        assertTrue(result is com.jjswigut.oopsallprs.domain.model.FoundationResult.Failure)
        assertEquals(0, holder.state.value.workout?.exerciseBlocks?.single()?.loggedRows?.size)
    }
}
