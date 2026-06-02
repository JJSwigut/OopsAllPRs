package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkoutLifecycleContractTest {
    @Test
    fun startEmptyCreatesRecoverableActiveSession() = runTest {
        val harness = FoundationHarness()
        val result = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val restored = harness.lifecycle.restoreActiveSession(instant(3_000))
        assertEquals(result.id, restored?.activeWorkoutId)
        assertEquals(2_000, restored?.elapsedMillis(instant(3_000)))
    }

    @Test
    fun missingRoutineReturnsExplicitFailure() = runTest {
        val harness = FoundationHarness()
        val result = harness.lifecycle.startFromRoutine(FoundationId("missing"), instant(1_000))
        assertTrue(result is FoundationResult.Failure)
    }

    @Test
    fun discardClearsActiveSession() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        harness.lifecycle.discard(workout.id, instant(2_000))
        assertEquals(null, harness.store.load())
    }
}
