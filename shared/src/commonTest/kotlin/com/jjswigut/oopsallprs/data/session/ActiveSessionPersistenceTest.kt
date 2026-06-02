package com.jjswigut.oopsallprs.data.session

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveSessionPersistenceTest {
    @Test
    fun restTimerUsesWallClockAnchor() = runTest {
        val harness = FoundationHarness()
        val session = ActiveSessionState(
            activeWorkoutId = FoundationId("workout"),
            startedAt = instant(1_000),
            restEndsAt = instant(11_000),
            restStartedAt = instant(6_000),
            restOriginSetId = FoundationId("set"),
            updatedAt = instant(6_000)
        )
        harness.store.save(session)
        val hydrated = ActiveSessionCoordinator(harness.store).hydrate(instant(8_000))
        assertEquals(7_000, hydrated?.elapsedMillis)
        assertEquals(3_000, hydrated?.restRemainingMillis)
    }
}
