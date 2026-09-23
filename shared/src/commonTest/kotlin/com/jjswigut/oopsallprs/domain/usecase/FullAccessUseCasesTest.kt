package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessGate
import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStatus
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertIs
import kotlin.test.assertFailsWith

class FullAccessUseCasesTest {
    @Test
    fun unpaidUserCanStartUntilConfiguredFreeCompletedWorkouts() = runTest {
        val harness = FoundationHarness()
        val store = harness.store
        val access = FullAccessUseCases(store)

        repeat(FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT) { index ->
            val gate = access.checkGate(FullAccessGate.WORKOUT_START)
            assertTrue(gate.allowed, "workout ${index + 1} should be allowed")
            val activeId = harness.workoutWithLoggedWeightedSet()
            store.finishActiveWorkout(activeId, instant(index + 1L)).successValue()
        }

        val blocked = access.checkGate(FullAccessGate.WORKOUT_START)
        assertFalse(blocked.allowed)
        assertEquals(FullAccessStatus.EXPIRED, blocked.state.status)
    }

    @Test
    fun lifetimeUnlockedUserCanStartAndUseDataToolsAfterFreeLimit() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(
            FullAccessState(
                completedFreeWorkouts = FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT,
                lifetimeUnlocked = true
            )
        ).successValue()
        val access = FullAccessUseCases(store)

        assertTrue(access.checkGate(FullAccessGate.WORKOUT_START).allowed)
        assertTrue(access.checkGate(FullAccessGate.EXPORT).allowed)
        assertTrue(access.checkGate(FullAccessGate.RESTORE_BACKUP).allowed)
    }

    @Test
    fun unpaidUserCanExportButCannotRestoreOrImportDataEvenWithinFreeWorkoutAllowance() = runTest {
        val access = FullAccessUseCases(InMemoryFoundationStore())

        assertTrue(access.checkGate(FullAccessGate.EXPORT).allowed)
        assertFalse(access.checkGate(FullAccessGate.IMPORT).allowed)
        assertFalse(access.checkGate(FullAccessGate.BACKUP_LINK).allowed)
        assertFalse(access.checkGate(FullAccessGate.BACKUP_NOW).allowed)
        assertFalse(access.checkGate(FullAccessGate.SYNC_NOW).allowed)
        assertFalse(access.checkGate(FullAccessGate.RESTORE_BACKUP).allowed)
    }

    @Test
    fun restorePurchasesSavesLifetimeEntitlement() = runTest {
        val store = InMemoryFoundationStore()
        val access = FullAccessUseCases(store, billing = GrantingBillingAdapter(lifetimeUnlocked = true))

        val restored = access.restorePurchases().successValue()

        assertTrue(restored.hasFullAccess)
        assertEquals(FullAccessStatus.LIFETIME, store.loadFullAccess().status)
    }

    @Test
    fun offersExposeLifetimeUnlock() = runTest {
        val access = FullAccessUseCases(
            InMemoryFoundationStore(),
            billing = OfferingBillingAdapter(
                listOf(
                    FullAccessStoreOffer(
                        title = "Lifetime",
                        priceLabel = "${'$'}14.99",
                        termsLabel = "One-time purchase."
                    )
                )
            )
        )

        val available = assertIs<FullAccessOfferState.Available>(access.loadLifetimeOffer())

        assertEquals("${'$'}14.99", available.offer.priceLabel)
        assertEquals("One-time purchase.", available.offer.termsLabel)
    }

    @Test
    fun billingFailureDoesNotGrantAccess() = runTest {
        val store = InMemoryFoundationStore()
        val access = FullAccessUseCases(store, billing = FailingBillingAdapter)

        access.purchaseLifetimeUnlock()

        val state = store.loadFullAccess()
        assertFalse(state.hasFullAccess)
        assertEquals("Store unavailable", state.lastError)
    }

    @Test
    fun missingBillingDoesNotFabricateAnOffer() = runTest {
        val access = FullAccessUseCases(InMemoryFoundationStore())

        assertIs<FullAccessOfferState.Unavailable>(access.loadLifetimeOffer())
    }

    @Test
    fun emptyFailedAndBlankPriceResponsesAreUnavailable() = runTest {
        val store = InMemoryFoundationStore()
        assertIs<FullAccessOfferState.Unavailable>(FullAccessUseCases(store, OfferingBillingAdapter(emptyList())).loadLifetimeOffer())
        assertEquals("Store unavailable", assertIs<FullAccessOfferState.Unavailable>(
            FullAccessUseCases(store, FailingBillingAdapter).loadLifetimeOffer()
        ).message)
        assertIs<FullAccessOfferState.Unavailable>(FullAccessUseCases(store, OfferingBillingAdapter(listOf(
            FullAccessStoreOffer("Lifetime", " ", "One-time purchase")
        ))).loadLifetimeOffer())
    }

    @Test
    fun thrownOfferFailureIsRecoverableButCancellationPropagates() = runTest {
        var failure: Exception = IllegalStateException("Store disconnected")
        val billing = object : FullAccessBillingAdapter by FailingBillingAdapter {
            override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> = throw failure
        }
        val access = FullAccessUseCases(InMemoryFoundationStore(), billing)
        assertEquals("Store disconnected", assertIs<FullAccessOfferState.Unavailable>(access.loadLifetimeOffer()).message)

        failure = CancellationException("Canceled")
        assertFailsWith<CancellationException> { access.loadLifetimeOffer() }
    }

    @Test
    fun enclosingCallerTimeoutIsNotConvertedIntoStoreUnavailability() = runTest {
        val billing = object : FullAccessBillingAdapter by FailingBillingAdapter {
            override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> = awaitCancellation()
        }
        val access = FullAccessUseCases(InMemoryFoundationStore(), billing)

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { access.loadLifetimeOffer() }
        }
    }

    private class GrantingBillingAdapter(
        private val lifetimeUnlocked: Boolean = false
    ) : FullAccessBillingAdapter {
        override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> =
            foundationSuccess(emptyList())

        override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = lifetimeUnlocked))

        override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))

        override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> =
            refreshEntitlements()
    }

    private class OfferingBillingAdapter(
        private val offers: List<FullAccessStoreOffer>
    ) : FullAccessBillingAdapter {
        override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> =
            foundationSuccess(offers)

        override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationSuccess(FullAccessEntitlementSnapshot(storeStatus = FullAccessStoreStatus.AVAILABLE))

        override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))

        override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> =
            refreshEntitlements()
    }

    private object FailingBillingAdapter : FullAccessBillingAdapter {
        override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> =
            foundationFailure(FoundationError.Platform("Store unavailable"))

        override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationFailure(FoundationError.Platform("Store unavailable"))

        override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationFailure(FoundationError.Platform("Store unavailable"))

        override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> =
            foundationFailure(FoundationError.Platform("Store unavailable"))
    }
}
