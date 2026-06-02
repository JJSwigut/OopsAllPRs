package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppShellActiveModeTest {
    @Test
    fun presentActiveWorkoutHidesTopLevelShellUntilDismissed() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val session = harness.lifecycle.restoreActiveSession(instant(1_100))
        val holder = AppNavigationStateHolder(harness.lifecycle)
        holder.hydrate(session, instant(1_100))

        holder.presentActiveWorkout(instant(1_200))

        assertTrue(holder.state.value.isActiveWorkoutPresented)
        assertTrue(holder.state.value.activeWorkoutResume?.activeWorkoutId == workout.id)

        holder.dismissActiveWorkout(instant(1_300))

        assertFalse(holder.state.value.isActiveWorkoutPresented)
    }
}
