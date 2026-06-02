package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutBodyweightPrTest {
    @Test
    fun bodyweightPrUsesRepsAndNonPrRowsStayPlain() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(
            harness.setLogging,
            harness.lifecycle,
            harness.store,
            ActivePrFeedbackUseCase(harness.store)
        )
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()
        holder.updateDraftReps(exerciseId, 10)
        holder.confirmDraft(exerciseId).successValue()
        holder.updateDraftReps(exerciseId, 8)
        holder.confirmDraft(exerciseId).successValue()

        val rows = holder.state.value.workout?.exerciseBlocks?.single()?.loggedRows.orEmpty()
        assertEquals("New PR: 10 reps", rows[0].prFeedback?.label)
        assertNull(rows[1].prFeedback)
    }
}
