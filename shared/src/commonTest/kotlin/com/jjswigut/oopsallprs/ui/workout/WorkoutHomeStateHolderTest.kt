package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkoutHomeStateHolderTest {
    @Test
    fun startEmptyCreatesResumableActiveSession() = runTest {
        val harness = FoundationHarness()
        val holder = WorkoutHomeStateHolder(harness.lifecycle)

        val workout = holder.startEmpty().successValue()

        assertEquals(workout.id, holder.state.value.activeSession?.activeWorkoutId)
        assertNotNull(harness.lifecycle.restoreActiveSession(instant(2_000)))
    }

    @Test
    fun hydrateSurfacesExistingActiveSession() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle)

        holder.hydrate()

        assertEquals(workout.id, holder.state.value.activeSession?.activeWorkoutId)
    }

    @Test
    fun discardActiveClearsResumableSession() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle)
        holder.hydrate()

        holder.discardActive().successValue()

        assertNull(holder.state.value.activeSession)
        assertNull(harness.lifecycle.restoreActiveSession(instant(2_000))?.activeWorkoutId)
    }

    @Test
    fun hydrateIgnoresRouteOnlySessionAfterWorkoutFinishes() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        harness.routines.finishWorkout(workoutId, instant(2_000)).successValue().workout
        harness.lifecycle.saveLastOpenedRoute("history", instant(2_200)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle)

        holder.hydrate()

        assertNull(holder.state.value.activeSession)
    }
}
