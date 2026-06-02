package com.jjswigut.oopsallprs.session

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidSessionRecoveryTest {
    @Test
    fun activeSessionHydratesForAndroidStartup() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val restored = harness.lifecycle.restoreActiveSession(instant(2_000))
        assertEquals(workout.id, restored?.activeWorkoutId)
    }
}
