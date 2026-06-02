package com.jjswigut.oopsallprs.session

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class SessionRestorePerformanceTest {
    @Test
    fun restoreHelperCompletesWithinTwoSecondBudget() = runTest {
        val harness = FoundationHarness()
        harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val started = kotlin.time.TimeSource.Monotonic.markNow()
        harness.lifecycle.restoreActiveSession(instant(2_000))
        assertTrue(started.elapsedNow().inWholeMilliseconds < 2_000)
    }
}
