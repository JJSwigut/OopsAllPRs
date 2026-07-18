package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.platform.RestAlertScheduleResult
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

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
        assertEquals(true, scheduler.scheduledPersistentSurfaceEnabled)
        lifecycle.clearRestTimer(workout.id, instant(3_000)).successValue()
        assertEquals(1, scheduler.cancelCount)

        lifecycle.startRestTimer(workout.id, FoundationId("set-2"), durationSeconds = 60, now = instant(4_000)).successValue()
        lifecycle.discard(workout.id, instant(5_000)).successValue()

        assertEquals(2, scheduler.cancelCount)
    }

    @Test
    fun finishingWorkoutCancelsActiveRestSurface() = runTest {
        val store = InMemoryFoundationStore()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, preferences = store, notifications = scheduler)
        val routines = RoutineUseCases(
            workouts = store,
            routines = store,
            preferences = store,
            restNotifications = scheduler
        )
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()
        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), durationSeconds = 60, now = instant(2_000)).successValue()

        routines.finishWorkout(workout.id, instant(3_000)).successValue()

        assertEquals(1, scheduler.cancelCount)
    }

    @Test
    fun timerReplacementUpdatesOnePersistentSurfaceWithoutChangingSessionTimer() = runTest {
        val store = InMemoryFoundationStore()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, preferences = store, notifications = scheduler)
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()

        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), 60, instant(2_000)).successValue()
        lifecycle.adjustRestTimer(workout.id, 30, instant(3_000)).successValue()

        assertEquals(listOf(instant(62_000), instant(92_000)), scheduler.scheduledEndsAtHistory)
        assertEquals(instant(92_000), assertNotNull(store.load()).restEndsAt)
    }

    @Test
    fun disabledPersistentSurfaceStillSchedulesCompletionAndKeepsInAppTimer() = runTest {
        val store = InMemoryFoundationStore()
        store.setRestTimerSurfaceEnabled(false).successValue()
        val scheduler = FakeRestAlertScheduler()
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, preferences = store, notifications = scheduler)
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()

        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), 60, instant(2_000)).successValue()

        assertEquals(false, scheduler.scheduledPersistentSurfaceEnabled)
        assertEquals(instant(62_000), assertNotNull(store.load()).restEndsAt)
    }

    @Test
    fun permissionDeniedSurfaceDoesNotCorruptPersistedRestOrRecovery() = runTest {
        val store = InMemoryFoundationStore()
        val scheduler = FakeRestAlertScheduler(scheduleResult = RestAlertScheduleResult.PERMISSION_DENIED)
        val lifecycle = WorkoutLifecycleUseCases(store, store, store, preferences = store, notifications = scheduler)
        val workout = lifecycle.startEmpty(instant(1_000)).successValue()

        lifecycle.startRestTimer(workout.id, FoundationId("set-1"), 60, instant(2_000)).successValue()
        val recoveredLifecycle = WorkoutLifecycleUseCases(
            store,
            store,
            store,
            preferences = store,
            notifications = scheduler
        )
        val recovered = recoveredLifecycle.restoreActiveSession(instant(30_000))

        assertEquals(instant(62_000), recovered?.restEndsAt)
        assertEquals(2, scheduler.scheduleCount)
        assertEquals(0, scheduler.cancelCount)
    }

    private class FakeRestAlertScheduler(
        private val scheduleResult: RestAlertScheduleResult = RestAlertScheduleResult.SCHEDULED
    ) : RestAlertScheduler {
        var scheduledEndsAt: Instant? = null
        var scheduledSoundEnabled: Boolean? = null
        var scheduledPersistentSurfaceEnabled: Boolean? = null
        val scheduledEndsAtHistory = mutableListOf<Instant>()
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
            scheduledEndsAtHistory += restEndsAt
            scheduleCount += 1
            return scheduleResult
        }

        override fun cancel() {
            cancelCount += 1
        }
    }
}
