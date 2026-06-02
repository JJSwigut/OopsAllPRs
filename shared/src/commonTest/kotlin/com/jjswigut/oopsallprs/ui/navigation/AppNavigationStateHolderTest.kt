package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppNavigationStateHolderTest {
    @Test
    fun hydratePresentsActiveWorkoutWhenLastRouteWasActive() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = AppNavigationStateHolder(harness.lifecycle)

        holder.hydrate(harness.store.load(), instant(3_000))

        val state = holder.state.value
        assertTrue(state.isActiveWorkoutPresented)
        assertEquals(workout.id, state.activeWorkoutResume?.activeWorkoutId)
        assertEquals(2_000, state.activeWorkoutResume?.elapsedMillis)
    }

    @Test
    fun selectingDestinationHidesOverlayButKeepsResumeBanner() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = AppNavigationStateHolder(harness.lifecycle)
        holder.hydrate(harness.store.load(), instant(2_000))

        holder.selectDestination(TopLevelDestination.HISTORY, instant(3_000))

        val state = holder.state.value
        assertEquals(TopLevelDestination.HISTORY, state.selectedDestination)
        assertFalse(state.isActiveWorkoutPresented)
        assertNotNull(state.activeWorkoutResume)
        assertEquals("history", harness.store.load()?.lastOpenedRoute)
    }

    @Test
    fun resumeAndDismissRoundTripPersistsRoutes() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = AppNavigationStateHolder(harness.lifecycle)
        holder.hydrate(harness.store.load(), instant(2_000))
        holder.selectDestination(TopLevelDestination.PROGRESS, instant(3_000))

        holder.presentActiveWorkout(instant(4_000))

        assertTrue(holder.state.value.isActiveWorkoutPresented)
        assertEquals("active-workout", harness.store.load()?.lastOpenedRoute)

        holder.dismissActiveWorkout(instant(5_000))

        assertFalse(holder.state.value.isActiveWorkoutPresented)
        assertEquals(TopLevelDestination.PROGRESS, holder.state.value.selectedDestination)
        assertEquals("progress", harness.store.load()?.lastOpenedRoute)
    }
}
