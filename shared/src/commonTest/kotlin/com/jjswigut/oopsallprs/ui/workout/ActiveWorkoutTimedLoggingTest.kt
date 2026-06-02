package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutTimedLoggingTest {
    @Test
    fun timedExerciseUsesDurationDraftAndLogsTimeOnlySet() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        holder.hydrate(workout.id)
        val exerciseId = holder.addExercise(workout.id, harness.timedReference).successValue()

        val initialDraft = holder.state.value.workout?.exerciseBlocks?.single()?.draft
        assertEquals(SetKind.TIMED, initialDraft?.setKind)
        assertNull(initialDraft?.reps)
        assertNull(initialDraft?.weight)

        holder.updateDraftDuration(exerciseId, 90_000L)
        val set = holder.confirmDraft(exerciseId).successValue()

        assertEquals(90_000L, set.durationMs)
        assertNull(set.reps)
        assertNull(set.weight)
    }

    @Test
    fun hydrateRestoresTimedDraftDuration() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val firstHolder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        firstHolder.hydrate(workout.id)
        val exerciseId = firstHolder.addExercise(workout.id, harness.timedReference).successValue()
        firstHolder.updateDraftDuration(exerciseId, 60_000L)
        firstHolder.setFocus(exerciseId, instant(1_500))

        val recovered = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        recovered.hydrate(workout.id)
        val draft = recovered.state.value.workout?.exerciseBlocks?.single()?.draft

        assertEquals(SetKind.TIMED, draft?.setKind)
        assertEquals(60_000L, draft?.durationMs)
    }
}
