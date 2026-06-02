package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWorkoutUxRecoveryTest {
    @Test
    fun hydrateRestoresPersistedFocusAndDraftValues() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val firstHolder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        firstHolder.hydrate(workout.id)
        val exerciseId = firstHolder.addExercise(workout.id, harness.weightedReference).successValue()
        firstHolder.updateDraftReps(exerciseId, 12)
        firstHolder.updateDraftWeight(exerciseId, com.jjswigut.oopsallprs.domain.model.WeightKg(80.0))
        firstHolder.setFocus(exerciseId, instant(1_500))

        val recovered = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        recovered.hydrate(workout.id)
        val block = recovered.state.value.workout?.exerciseBlocks?.single()

        assertEquals(exerciseId, recovered.state.value.focusSnapshot?.exerciseInstanceId)
        assertEquals(12, block?.draft?.reps)
        assertEquals(80.0, block?.draft?.weight?.value)
    }
}
