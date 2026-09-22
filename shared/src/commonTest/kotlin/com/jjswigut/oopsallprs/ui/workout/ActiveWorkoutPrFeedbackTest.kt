package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.usecase.ActivePrFeedbackUseCase
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedbackKind
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
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
        assertEquals("New 5-rep PR: 100 kg x 5", feedback.label)
    }

    @Test
    fun weightedPrDisplayUsesSelectedWeightUnit() {
        val feedback = ActivePrFeedback(
            setId = FoundationId("set-1"),
            exerciseCatalogId = FoundationId("exercise-1"),
            kind = ActivePrFeedbackKind.WEIGHT_FOR_REPS,
            label = "New PR: 100 kg x 5",
            previousValue = null,
            newValue = 100.0
        )

        assertEquals("New 5-rep PR: 220.5 lb x 5", feedback.displayLabel(reps = 5, weightUnit = WeightUnit.POUNDS))
        assertEquals("New 5-rep PR: 100 kg x 5", feedback.displayLabel(reps = 5, weightUnit = WeightUnit.KILOGRAMS))
    }
}
