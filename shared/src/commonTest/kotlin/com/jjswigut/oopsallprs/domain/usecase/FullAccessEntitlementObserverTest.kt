package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.platform.FullAccessBillingObserver
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FullAccessEntitlementObserverTest {
    @Test
    fun registersBeforeInitialSignalAndInvalidationsDoNotGrantAccessByThemselves() = runTest {
        val repository = InMemoryFoundationStore()
        val billing = ObserverBilling()
        val access = FullAccessUseCases(repository, billing)
        var signals = 0
        val collector = launch {
            access.observeEntitlementChanges().collect {
                assertNotNull(billing.observer)
                signals++
            }
        }
        runCurrent()
        assertEquals(1, signals)
        assertEquals(1, billing.registrations)

        billing.signal()
        runCurrent()
        assertEquals(2, signals)
        assertEquals(0, billing.refreshCalls)
        assertFalse(repository.loadFullAccess().hasFullAccess)

        val lateObserver = billing.observer
        collector.cancelAndJoin()
        assertNull(billing.observer)
        assertEquals(1, billing.unregistrations)
        lateObserver?.onEntitlementsChanged()
        runCurrent()
        assertEquals(2, signals)
    }

    @Test
    fun eventsDuringInitialRefreshQueueOneSerialFollowUpWithoutCancelingTheRead() = runTest {
        val repository = InMemoryFoundationStore()
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var firstReadFinished = false
        val billing = ObserverBilling().apply {
            refresh = {
                if (refreshCalls == 1) {
                    entered.complete(Unit)
                    finish.await()
                    firstReadFinished = true
                    foundationSuccess(FullAccessEntitlementSnapshot())
                } else {
                    foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
                }
            }
        }
        val access = FullAccessUseCases(repository, billing)
        var results = 0
        val collector = launch {
            access.observeEntitlementChanges().collect {
                access.refreshEntitlements().successValue()
                results++
            }
        }
        entered.await()
        repeat(20) { billing.signal() }
        runCurrent()
        assertEquals(1, billing.refreshCalls)
        assertFalse(firstReadFinished)

        finish.complete(Unit)
        runCurrent()
        assertTrue(firstReadFinished)
        assertEquals(2, billing.refreshCalls)
        assertEquals(2, results)
        assertTrue(repository.loadFullAccess().hasFullAccess)
        collector.cancelAndJoin()
    }

    @Test
    fun signalsDuringPersistenceDoNotCancelAnInFlightSave() = runTest {
        val store = InMemoryFoundationStore()
        val enteredSave = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        var updateCalls = 0
        var mutationTransforms = 0
        var identityTransforms = 0
        var firstCommitSettled = false
        var firstUpdateCanceled = false
        val repository = object : FullAccessRepository by store {
            override suspend fun updateFullAccess(transform: (FullAccessState) -> FullAccessState): FoundationResult<FullAccessState> {
                val firstUpdate = ++updateCalls == 1
                try {
                    if (firstUpdate) {
                        enteredSave.complete(Unit)
                        finishSave.await()
                    } else {
                        assertTrue(firstCommitSettled, "Queued updates must not overtake the blocked commit")
                    }
                    val result = store.updateFullAccess { current ->
                        val updated = transform(current)
                        if (updated === current) identityTransforms++ else mutationTransforms++
                        updated
                    }
                    if (firstUpdate) {
                        result.successValue()
                        firstCommitSettled = true
                    }
                    return result
                } catch (cancellation: CancellationException) {
                    if (firstUpdate) firstUpdateCanceled = true
                    throw cancellation
                }
            }
        }
        val billing = ObserverBilling()
        val access = FullAccessUseCases(repository, billing)
        var results = 0
        val collector = launch {
            access.observeEntitlementChanges().collect {
                access.refreshEntitlements().successValue()
                results++
            }
        }
        enteredSave.await()
        repeat(20) { billing.signal() }
        runCurrent()
        assertEquals(1, updateCalls)
        assertEquals(0, mutationTransforms)
        assertEquals(0, identityTransforms)
        assertEquals(0, results)
        assertTrue(billing.deliveredTokens.isEmpty())
        assertFalse(firstCommitSettled)
        assertFalse(firstUpdateCanceled)
        assertEquals(1, billing.refreshCalls)
        finishSave.complete(Unit)
        runCurrent()
        assertTrue(firstCommitSettled)
        assertFalse(firstUpdateCanceled)
        assertEquals(2, mutationTransforms, "Each refresh must persist its entitlement snapshot")
        assertEquals(2, identityTransforms, "Each successful delivery must read current state atomically")
        assertEquals(4, updateCalls)
        assertEquals(2, billing.refreshCalls)
        assertEquals(2, billing.deliveredTokens.size)
        assertEquals(2, results)
        collector.cancelAndJoin()
    }

    @Test
    fun canceledPurchaseCannotApplyLateResultButIndependentSignalRefreshesAuthoritativeAccess() = runTest {
        val repository = InMemoryFoundationStore()
        val enteredPurchase = CompletableDeferred<Unit>()
        val billing = ObserverBilling().apply {
            purchase = {
                enteredPurchase.complete(Unit)
                try {
                    awaitCancellation()
                } catch (_: CancellationException) {
                    // Simulate a native bridge returning a late result to a canceled caller.
                    foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true, deliveryToken = "canceled"))
                }
            }
        }
        val access = FullAccessUseCases(repository, billing)
        val collector = launch {
            access.observeEntitlementChanges().collect { access.refreshEntitlements().successValue() }
        }
        runCurrent()
        val beforePurchase = repository.loadFullAccess()
        billing.deliveredTokens.clear()
        val purchase = async { access.purchaseLifetimeUnlock() }
        enteredPurchase.await()
        purchase.cancelAndJoin()
        assertTrue(purchase.isCancelled)
        assertEquals(beforePurchase, repository.loadFullAccess())
        assertTrue(billing.deliveredTokens.isEmpty())

        billing.refresh = {
            foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true, deliveryToken = "verified-refresh"))
        }
        billing.signal()
        runCurrent()
        assertEquals(2, billing.refreshCalls)
        assertTrue(repository.loadFullAccess().hasFullAccess)
        assertEquals(listOf<String?>("verified-refresh"), billing.deliveredTokens)
        collector.cancelAndJoin()
    }

    @Test
    fun cancelingOneCollectorDoesNotDetachAnotherCollectorsNativeObserver() = runTest {
        val billing = ObserverBilling()
        val access = FullAccessUseCases(InMemoryFoundationStore(), billing)
        var firstSignals = 0
        var secondSignals = 0
        val first = launch { access.observeEntitlementChanges().collect { firstSignals++ } }
        val second = launch { access.observeEntitlementChanges().collect { secondSignals++ } }
        runCurrent()
        assertEquals(1, billing.registrations)
        assertEquals(1, firstSignals)
        assertEquals(1, secondSignals)

        first.cancelAndJoin()
        assertNotNull(billing.observer)
        assertEquals(0, billing.unregistrations)
        billing.signal()
        runCurrent()
        assertEquals(1, firstSignals)
        assertEquals(2, secondSignals)
        second.cancelAndJoin()
        assertNull(billing.observer)
        assertEquals(1, billing.unregistrations)
    }

    @Test
    fun partialRegistrationFailureUnregistersAndAllowsLaterCollection() = runTest {
        val billing = ObserverBilling().apply { failRegistration = true }
        val access = FullAccessUseCases(InMemoryFoundationStore(), billing)
        assertFailsWith<IllegalStateException> { access.observeEntitlementChanges().first() }
        assertNull(billing.observer)
        assertEquals(1, billing.unregistrations)

        billing.failRegistration = false
        assertEquals(Unit, access.observeEntitlementChanges().first())
        assertNull(billing.observer)
        assertEquals(2, billing.registrations)
        assertEquals(2, billing.unregistrations)
    }

    @Test
    fun missingAdapterStillSuppliesInitialSignalForRecoverableCachedStateRefresh() = runTest {
        val access = FullAccessUseCases(InMemoryFoundationStore())
        assertEquals(Unit, access.observeEntitlementChanges().first())
        assertIs<FoundationResult.Failure>(access.refreshEntitlements())
    }
}

private class ObserverBilling : FullAccessBillingAdapter {
    var observer: FullAccessBillingObserver? = null
    var registrations = 0
    var unregistrations = 0
    var refreshCalls = 0
    var failRegistration = false
    val deliveredTokens = mutableListOf<String?>()
    var purchase: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
    }
    var refresh: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot())
    }

    fun signal() { observer?.onEntitlementsChanged() }
    override fun setEntitlementObserver(observer: FullAccessBillingObserver?) {
        this.observer = observer
        if (observer == null) {
            unregistrations++
        } else {
            registrations++
            if (failRegistration) error("Observer registration failed")
        }
    }
    override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> = foundationSuccess(emptyList())
    override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot> {
        refreshCalls++
        return refresh()
    }
    override suspend fun purchaseLifetimeUnlock() = purchase()
    override suspend fun restorePurchases() = refreshEntitlements()
    override suspend fun completeEntitlementDelivery(deliveryToken: String?): FoundationResult<Unit> {
        deliveredTokens += deliveryToken
        return foundationSuccess(Unit)
    }
}
