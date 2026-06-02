package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ActiveWorkoutPrFeedbackTest {
    @Test
    fun confirmedRecordSetRendersInlineFeedbackOnLoggedRow() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(
            harness.setLogging,
            harness.lifecycle,
            harness.store,
            ActivePrFeedbackUseCase(harness.store)
        )
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.weightedReference).successValue()
        holder.updateDraftWeight(exerciseId, WeightKg(100.0))

        holder.confirmDraft(exerciseId).successValue()

        val feedback = holder.state.value.workout?.exerciseBlocks?.single()?.loggedRows?.single()?.prFeedback
        assertNotNull(feedback)
        assertEquals("New PR: 100 kg x 5", feedback.label)
    }
}
