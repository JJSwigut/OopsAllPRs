package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveWorkoutRecoveryTest {
    @Test
    fun hydrateRestoresFocusedExerciseAndDraftSnapshot() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        holder.addExercise(workout.id, harness.weightedReference).successValue()
        val bodyweightId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()
        holder.setFocus(bodyweightId, instant(1_500))
        val snapshot = holder.snapshotFocus()

        val recovered = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        recovered.hydrate(workout.id, snapshot)

        assertEquals(bodyweightId, recovered.state.value.workout?.focus?.exerciseInstanceId)
        assertEquals(snapshot, recovered.state.value.focusSnapshot)
    }
}
