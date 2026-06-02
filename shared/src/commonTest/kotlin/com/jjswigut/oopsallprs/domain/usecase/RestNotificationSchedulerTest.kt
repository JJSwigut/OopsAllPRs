package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class RestNotificationSchedulerTest {
    @Test
    fun restLifecycleSchedulesWithSoundPreferenceAndCancelsOnClearAndDiscard() = runTest {
        val store = InMemoryFoundationStore()
        store.setRestSoundEnabled(false).successValue()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, preferences = store, notifications = scheduler)
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()

        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), durationSeconds = 60, now = instant(2_000)).successValue()

        assertEquals(instant(62_000), scheduler.scheduledEndsAt)
        assertEquals(false, scheduler.scheduledSoundEnabled)
        lifecycle.clearRestTimer(workout.id, instant(3_000)).successValue()
        assertEquals(1, scheduler.cancelCount)

        lifecycle.startRestTimer(workout.id, FoundationId("set-2"), durationSeconds = 60, now = instant(4_000)).successValue()
        lifecycle.discard(workout.id, instant(5_000)).successValue()

        assertEquals(2, scheduler.cancelCount)
    }

    private class FakeRestAlertScheduler : RestAlertScheduler {
        var scheduledEndsAt: Instant? = null
        var scheduledSoundEnabled: Boolean? = null
        var cancelCount: Int = 0

        override fun schedule(restEndsAt: Instant, soundEnabled: Boolean) {
            scheduledEndsAt = restEndsAt
            scheduledSoundEnabled = soundEnabled
        }

        override fun cancel() {
            cancelCount += 1
        }
    }
}
