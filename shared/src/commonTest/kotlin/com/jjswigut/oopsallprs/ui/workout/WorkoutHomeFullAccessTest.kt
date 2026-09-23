package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.ui.profile.DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutHomeFullAccessTest {
    @Test
    fun workoutStartIsAllowedBeforeDefaultFreeLimit() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(
            FullAccessState(completedFreeWorkouts = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT - 1)
        ).successValue()
        val fullAccess = FullAccessUseCases(store)
        val lifecycle = WorkoutLifecycleUseCases(store, store, store)
        val holder = WorkoutHomeStateHolder(lifecycle, fullAccess = fullAccess)

        val result = holder.startEmpty()

        assertFalse(holder.state.value.isFullAccessPaywallVisible)
        assertTrue(result is FoundationResult.Success)
    }

    @Test
    fun workoutStartIsBlockedAtDefaultFreeLimit() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(
            FullAccessState(completedFreeWorkouts = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT)
        ).successValue()
        val fullAccess = FullAccessUseCases(store)
        val lifecycle = WorkoutLifecycleUseCases(store, store, store)
        val holder = WorkoutHomeStateHolder(lifecycle, fullAccess = fullAccess)

        val result = holder.startEmpty()

        assertTrue(result is FoundationResult.Failure)
        assertTrue(holder.state.value.isFullAccessPaywallVisible)
        assertEquals("You've used your free workouts.", holder.state.value.fullAccessMessage)
        assertEquals(DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT, holder.state.value.fullAccess.completedFreeWorkouts)
    }

    @Test
    fun workoutStartUsesTheSharedFreeLimitInsteadOfAUiOverride() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(
            FullAccessState(completedFreeWorkouts = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT - 1)
        ).successValue()
        val fullAccess = FullAccessUseCases(store)
        val lifecycle = WorkoutLifecycleUseCases(store, store, store)
        val holder = WorkoutHomeStateHolder(lifecycle = lifecycle, fullAccess = fullAccess)

        val result = holder.startEmpty()

        assertTrue(result is FoundationResult.Success)
        assertFalse(holder.state.value.isFullAccessPaywallVisible)
    }

    @Test
    fun paidUserCanStartAfterTrialLimit() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(
            FullAccessState(
                completedFreeWorkouts = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT,
                lifetimeUnlocked = true
            )
        ).successValue()
        val fullAccess = FullAccessUseCases(store)
        val lifecycle = WorkoutLifecycleUseCases(store, store, store)
        val holder = WorkoutHomeStateHolder(lifecycle, fullAccess = fullAccess)

        val result = holder.startEmpty()

        assertFalse(holder.state.value.isFullAccessPaywallVisible)
        assertTrue(result is FoundationResult.Success)
    }
}
