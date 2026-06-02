package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutLifecycleRouteTest {
    @Test
    fun startingWorkoutPersistsActiveWorkoutRoute() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()

        assertEquals("active-workout", harness.store.load()?.lastOpenedRoute)
    }

    @Test
    fun savingLastOpenedRoutePreservesActiveWorkoutIdentity() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()

        val saved = harness.lifecycle.saveLastOpenedRoute("history", instant(2_000)).successValue()

        assertEquals(workout.id, saved.activeWorkoutId)
        assertEquals(workout.startedAt, saved.startedAt)
        assertEquals("history", saved.lastOpenedRoute)
    }

    @Test
    fun restTimerUpdatesPreserveLastOpenedRoute() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.lifecycle.saveLastOpenedRoute("progress", instant(1_500)).successValue()

        val result = harness.lifecycle.updateRestTimer(
            activeWorkoutId = workout.id,
            originSetId = null,
            restEndsAt = instant(10_000),
            now = instant(2_000)
        )

        assertTrue(result is FoundationResult.Success)
        assertEquals("progress", harness.store.load()?.lastOpenedRoute)
    }
}
