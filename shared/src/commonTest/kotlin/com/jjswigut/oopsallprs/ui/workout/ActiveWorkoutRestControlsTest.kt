package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ActiveWorkoutRestControlsTest {
    @Test
    fun activeRestCanBeAdjustedAndSkippedWithoutDroppingWorkoutFocus() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        harness.lifecycle.startRestTimer(workout.id, logged.id, durationSeconds = 120, now = instant(1_200)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id, now = instant(2_000))

        assertEquals(119_200L, holder.state.value.workout?.activeRest?.remainingMillis)
        holder.adjustActiveRest(deltaSeconds = 15, now = instant(2_000)).successValue()
        assertEquals(134_200L, holder.state.value.workout?.activeRest?.remainingMillis)
        holder.adjustActiveRest(deltaSeconds = -30, now = instant(2_000)).successValue()
        assertEquals(104_200L, holder.state.value.workout?.activeRest?.remainingMillis)
        holder.skipActiveRest(now = instant(2_100)).successValue()

        val view = assertNotNull(holder.state.value.workout)
        assertNull(view.activeRest)
        assertEquals(exercise.id, view.focus?.exerciseInstanceId)
    }

    @Test
    fun deletingOriginSetClearsActiveRest() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            workout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        harness.lifecycle.startRestTimer(workout.id, logged.id, durationSeconds = 120, now = instant(1_200)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id, now = instant(2_000))

        holder.deleteLoggedSet(logged.id, now = instant(2_500)).successValue()

        assertNull(holder.state.value.workout?.activeRest)
        assertNull(harness.store.load()?.restEndsAt)
    }
}
