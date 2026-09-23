package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStatus
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SqlFullAccessPersistenceTest {
    @Test
    fun completedFreeWorkoutCountPersistsInSqlStore() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val bundle = harness.repositories()
        bundle.store.updateFullAccess {
            FullAccessState(
                completedFreeWorkouts = FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT,
                storeStatus = FullAccessStoreStatus.AVAILABLE,
                lastError = null
            )
        }.successValue()

        val loaded = SqlFoundationStore(harness.database).loadFullAccess()

        assertEquals(FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT, loaded.completedFreeWorkouts)
        assertEquals(0, loaded.remainingFreeWorkouts)
        assertEquals(FullAccessStatus.EXPIRED, loaded.status)
    }

    @Test
    fun lifetimeEntitlementPersistsInSqlStore() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val bundle = harness.repositories()
        bundle.store.updateFullAccess {
            FullAccessState(
                completedFreeWorkouts = FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT,
                lifetimeUnlocked = true,
                storeStatus = FullAccessStoreStatus.AVAILABLE
            )
        }.successValue()

        val loaded = SqlFoundationStore(harness.database).loadFullAccess()

        assertTrue(loaded.hasFullAccess)
        assertEquals(FullAccessStatus.LIFETIME, loaded.status)
        assertEquals(FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT, loaded.completedFreeWorkouts)
    }

    @Test
    fun unpaidStateDoesNotGrantAccess() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val bundle = harness.repositories()
        bundle.store.updateFullAccess {
            FullAccessState(
                completedFreeWorkouts = 1,
                storeStatus = FullAccessStoreStatus.AVAILABLE
            )
        }.successValue()

        val loaded = SqlFoundationStore(harness.database).loadFullAccess()

        assertEquals(1, loaded.completedFreeWorkouts)
        assertEquals(FullAccessStatus.TRIAL, loaded.status)
    }
}
