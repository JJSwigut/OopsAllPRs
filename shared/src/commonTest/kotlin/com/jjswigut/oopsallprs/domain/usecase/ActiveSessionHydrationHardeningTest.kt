package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.platform.RestAlertScheduleResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ActiveSessionHydrationHardeningTest {
    @Test
    fun unexpiredRestHydrationKeepsWorkoutRestAndRouteRecoverable() = runTest {
        val harness = FoundationHarness()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(
            harness.store,
            harness.store,
            harness.store,
            activeUx = harness.store,
            preferences = harness.store,
            notifications = scheduler
        )
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()
        lifecycle.saveLastOpenedRoute("progress", instant(1_500)).successValue()
        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), durationSeconds = 60, now = instant(2_000)).successValue()

        val restored = lifecycle.restoreActiveSession(instant(30_000))

        assertEquals(workout.id, restored?.activeWorkoutId)
        assertEquals(instant(62_000), restored?.restEndsAt)
        assertEquals(32_000, restored?.restRemainingMillis(instant(30_000)))
        assertEquals("progress", restored?.lastOpenedRoute)
        assertEquals(0, scheduler.cancelCount)
        assertEquals(1, scheduler.scheduleCount)
    }

    @Test
    fun expiredRestHydrationClearsRestCancelsNotificationAndKeepsWorkoutRecoverable() = runTest {
        val harness = FoundationHarness()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(
            harness.store,
            harness.store,
            harness.store,
            activeUx = harness.store,
            preferences = harness.store,
            notifications = scheduler
        )
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()
        lifecycle.saveLastOpenedRoute("active-workout", instant(1_500)).successValue()
        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), durationSeconds = 60, now = instant(2_000)).successValue()

        val restored = lifecycle.restoreActiveSession(instant(70_000))

        assertEquals(workout.id, restored?.activeWorkoutId)
        assertEquals(workout.startedAt, restored?.startedAt)
        assertEquals(null, restored?.restEndsAt)
        assertEquals(null, restored?.restStartedAt)
        assertEquals(null, restored?.restOriginSetId)
        assertEquals("active-workout", restored?.lastOpenedRoute)
        assertEquals(1, scheduler.cancelCount)
        assertEquals(restored, harness.store.load())
    }

    private class FakeRestAlertScheduler : RestAlertScheduler {
        var scheduledEndsAt: Instant? = null
        var scheduledSoundEnabled: Boolean? = null
        var scheduledPersistentSurfaceEnabled: Boolean? = null
        var scheduleCount: Int = 0
        var cancelCount: Int = 0

        override fun schedule(
            restEndsAt: Instant,
            soundEnabled: Boolean,
            persistentSurfaceEnabled: Boolean
        ): RestAlertScheduleResult {
            scheduledEndsAt = restEndsAt
            scheduledSoundEnabled = soundEnabled
            scheduledPersistentSurfaceEnabled = persistentSurfaceEnabled
            scheduleCount += 1
            return RestAlertScheduleResult.SCHEDULED
        }

        override fun cancel() {
            cancelCount += 1
        }
    }
}
