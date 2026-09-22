package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.recordLocalCompletion
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class FullAccessReconciliationTest {
    @Test
    fun successfulStoreOperationsCompleteDeliveryOnlyAfterOwnershipIsPersisted() = runTest {
        for (operation in listOf("refresh", "purchase", "restore")) {
            val repository = ReconciliationRepository()
            val token = "delivery-$operation"
            val snapshot = FullAccessEntitlementSnapshot(lifetimeUnlocked = true, deliveryToken = token)
            val billing = ReconciliationBilling().apply {
                refresh = { foundationSuccess(snapshot) }
                purchase = { foundationSuccess(snapshot) }
                restore = { foundationSuccess(snapshot) }
                completeDelivery = { deliveredToken ->
                    assertTrue(repository.state.hasFullAccess, "Ownership must be persisted before delivery finishes")
                    assertEquals(token, deliveredToken, "Delivery must acknowledge this exact saved snapshot")
                    foundationSuccess(Unit)
                }
            }
            val access = FullAccessUseCases(repository, billing)

            when (operation) {
                "refresh" -> access.refreshEntitlements()
                "purchase" -> access.purchaseLifetimeUnlock()
                else -> access.restorePurchases()
            }.successValue()

            assertTrue(repository.state.hasFullAccess)
            assertEquals(1, billing.deliveryCalls, "The verified $operation must complete transaction delivery")
            assertEquals(listOf<String?>(token), billing.deliveredTokens)
        }
    }

    @Test
    fun failedOwnershipPersistenceNeverCompletesStoreDelivery() = runTest {
        val repository = ReconciliationRepository().apply {
            saveFailure = FoundationError.Persistence("Disk full")
        }
        val billing = ReconciliationBilling()
        val access = FullAccessUseCases(repository, billing)

        assertIs<FoundationResult.Failure>(access.purchaseLifetimeUnlock())
        assertFalse(repository.state.hasFullAccess)
        assertEquals(0, billing.deliveryCalls)
    }

    @Test
    fun deliveryFailurePreservesSavedOwnershipAndCanBeRetriedByRefresh() = runTest {
        val repository = ReconciliationRepository()
        val billing = ReconciliationBilling().apply {
            completeDelivery = { foundationFailure(FoundationError.Platform("Delivery could not finish. Try again.")) }
        }
        val access = FullAccessUseCases(repository, billing)

        access.purchaseLifetimeUnlock()

        assertTrue(repository.state.hasFullAccess)
        assertEquals(1, billing.deliveryCalls)
        assertEquals("Delivery could not finish. Try again.", repository.state.lastError)
        billing.completeDelivery = { foundationSuccess(Unit) }
        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        access.refreshEntitlements().successValue()
        assertEquals(2, billing.deliveryCalls)
        assertTrue(repository.state.hasFullAccess)
        assertNull(repository.state.lastError)
    }

    @Test
    fun thrownDeliveryErrorAlsoPreservesAlreadySavedOwnership() = runTest {
        val repository = ReconciliationRepository()
        val billing = ReconciliationBilling().apply { completeDelivery = { error("Store delivery interrupted") } }
        val access = FullAccessUseCases(repository, billing)

        access.purchaseLifetimeUnlock()

        assertTrue(repository.state.hasFullAccess)
        assertEquals(1, billing.deliveryCalls)
        assertEquals("Store delivery interrupted", repository.state.lastError)
    }

    @Test
    fun hungDeliveryPreservesOwnershipIsBoundedAndRetriesWithTheNewSnapshotsToken() = runTest {
        val repository = ReconciliationRepository()
        val billing = ReconciliationBilling().apply {
            purchase = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true, deliveryToken = "first")) }
            completeDelivery = { awaitCancellation() }
        }
        val access = FullAccessUseCases(repository, billing)
        val purchase = async { access.purchaseLifetimeUnlock() }
        runCurrent()
        assertTrue(repository.state.hasFullAccess)
        advanceTimeBy(60_001)
        runCurrent()
        val completedWithinBound = purchase.isCompleted
        if (!completedWithinBound) purchase.cancelAndJoin()
        assertTrue(completedWithinBound)
        assertTrue(purchase.await().successValue().hasFullAccess)
        assertEquals(FullAccessStoreStatus.ERROR, repository.state.storeStatus)

        billing.completeDelivery = { foundationSuccess(Unit) }
        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true, deliveryToken = "retry")) }
        access.refreshEntitlements().successValue()
        assertEquals(listOf<String?>("first", "retry"), billing.deliveredTokens)
        assertNull(repository.state.lastError)
    }

    @Test
    fun purchaseWatchdogWarnsThatPurchaseMayStillCompleteAndAllowsReconciliation() = runTest {
        val repository = ReconciliationRepository()
        val billing = ReconciliationBilling().apply { purchase = { awaitCancellation() } }
        val access = FullAccessUseCases(repository, billing)
        val purchase = async { access.purchaseLifetimeUnlock() }
        runCurrent()
        advanceTimeBy(300_001)
        runCurrent()
        val completedWithinBound = purchase.isCompleted
        if (!completedWithinBound) purchase.cancelAndJoin()
        assertTrue(completedWithinBound)
        val failure = assertIs<FoundationResult.Failure>(purchase.await())
        assertTrue(failure.error.message.contains("may still complete"))
        assertTrue(failure.error.message.contains("restore access"))
        assertFalse(repository.state.hasFullAccess)

        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        assertTrue(access.refreshEntitlements().successValue().hasFullAccess)
    }

    @Test
    fun cancelingQueuedRefreshDoesNotCancelPurchaseOrBlockLocalCompletion() = runTest {
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var refreshCalls = 0
        val repository = ReconciliationRepository()
        val billing = ReconciliationBilling().apply {
            purchase = {
                entered.complete(Unit)
                finish.await()
                foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
            }
            refresh = {
                refreshCalls++
                foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
            }
        }
        val access = FullAccessUseCases(repository, billing)
        val purchase = async { access.purchaseLifetimeUnlock() }
        entered.await()
        val refresh = async { access.refreshEntitlements() }
        runCurrent()
        refresh.cancelAndJoin()
        assertEquals(0, refreshCalls)
        assertEquals(1, repository.completeLocally().successValue().completedFreeWorkouts)
        finish.complete(Unit)
        assertTrue(purchase.await().successValue().hasFullAccess)
        access.refreshEntitlements().successValue()
        assertEquals(1, refreshCalls)
        assertEquals(1, repository.state.completedFreeWorkouts)
    }

    @Test
    fun staleRefreshCannotOverwriteAnOverlappingSuccessfulPurchase() = runTest {
        val repository = ReconciliationRepository()
        val enteredRefresh = CompletableDeferred<Unit>()
        val finishRefresh = CompletableDeferred<Unit>()
        val billing = ReconciliationBilling().apply {
            refresh = {
                enteredRefresh.complete(Unit)
                finishRefresh.await()
                foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = false))
            }
        }
        val access = FullAccessUseCases(repository, billing)
        val refresh = async { access.refreshEntitlements() }
        enteredRefresh.await()
        val purchase = async { access.purchaseLifetimeUnlock() }
        runCurrent()

        // Before serialization, purchase saves ownership before this old read finishes.
        finishRefresh.complete(Unit)
        purchase.await().successValue()
        refresh.await().successValue()

        assertTrue(repository.state.hasFullAccess, "The older refresh must not erase the successful purchase")
    }

    @Test
    fun thrownRefreshPreservesCachedOwnershipAndReturnsRecoverableFailureBeforeRetry() = runTest {
        val repository = ReconciliationRepository(FullAccessState(lifetimeUnlocked = true, completedFreeWorkouts = 4))
        val billing = ReconciliationBilling().apply { refresh = { error("Store disconnected") } }
        val access = FullAccessUseCases(repository, billing)

        assertIs<FoundationResult.Failure>(access.refreshEntitlements())
        assertTrue(repository.state.hasFullAccess)
        assertEquals(4, repository.state.completedFreeWorkouts)
        assertEquals(FullAccessStoreStatus.ERROR, repository.state.storeStatus)
        assertEquals("Store disconnected", repository.state.lastError)

        // A later successful authoritative read may revoke ownership; failures may not.
        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = false)) }
        assertFalse(access.refreshEntitlements().successValue().hasFullAccess)
        assertEquals(4, repository.state.completedFreeWorkouts)
    }

    @Test
    fun hungRefreshHasABoundedFailurePreservesOwnershipAndAllowsLaterRetry() = runTest {
        val repository = ReconciliationRepository(FullAccessState(lifetimeUnlocked = true))
        val billing = ReconciliationBilling().apply { refresh = { awaitCancellation() } }
        val access = FullAccessUseCases(repository, billing)
        val refresh = async { access.refreshEntitlements() }
        runCurrent()
        advanceTimeBy(60_001)
        runCurrent()
        val completedWithinBound = refresh.isCompleted
        if (!completedWithinBound) refresh.cancelAndJoin()

        assertTrue(completedWithinBound, "Refresh must return a recoverable failure within one minute")
        assertIs<FoundationResult.Failure>(refresh.await())
        assertTrue(repository.state.hasFullAccess)
        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        assertTrue(access.refreshEntitlements().successValue().hasFullAccess)
        assertEquals(FullAccessStoreStatus.AVAILABLE, repository.state.storeStatus)
    }

    @Test
    fun entitlementSaveCannotDropAConcurrentFreeCompletion() = runTest {
        val repository = ReconciliationRepository()
        val enteredSave = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        repository.pauseNextSave(enteredSave, finishSave)
        val access = FullAccessUseCases(repository, ReconciliationBilling())
        val refresh = async { access.refreshEntitlements() }
        enteredSave.await()
        val completion = async { repository.completeLocally() }
        runCurrent()
        finishSave.complete(Unit)

        refresh.await().successValue()
        completion.await().successValue()
        assertEquals(1, repository.state.completedFreeWorkouts, "Store state must not overwrite the completion count")
    }

    @Test
    fun completionSaveCannotEraseConcurrentPurchaseAndPurchaseKeepsCompletionCount() = runTest {
        val repository = ReconciliationRepository()
        val enteredSave = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        repository.pauseNextSave(enteredSave, finishSave)
        val access = FullAccessUseCases(repository, ReconciliationBilling())
        val completion = async { repository.completeLocally() }
        enteredSave.await()
        val purchase = async { access.purchaseLifetimeUnlock() }
        runCurrent()
        finishSave.complete(Unit)

        completion.await().successValue()
        purchase.await().successValue()
        assertEquals(1, repository.state.completedFreeWorkouts)
        assertTrue(repository.state.hasFullAccess, "Saving an earlier free completion must not erase ownership")
    }

    @Test
    fun storeErrorPersistenceCannotDropAConcurrentFreeCompletion() = runTest {
        val repository = ReconciliationRepository()
        val enteredSave = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        repository.pauseNextSave(enteredSave, finishSave)
        val billing = ReconciliationBilling().apply {
            refresh = { foundationFailure(FoundationError.Platform("Store offline")) }
        }
        val access = FullAccessUseCases(repository, billing)
        val refresh = async { access.refreshEntitlements() }
        enteredSave.await()
        val completion = async { repository.completeLocally() }
        runCurrent()
        finishSave.complete(Unit)

        assertIs<FoundationResult.Failure>(refresh.await())
        completion.await().successValue()
        assertEquals(1, repository.state.completedFreeWorkouts)
        assertEquals("Store offline", repository.state.lastError)
    }

    @Test
    fun simultaneousFreeCompletionsEachIncrementAllowanceUsage() = runTest {
        val repository = ReconciliationRepository()
        val enteredSave = CompletableDeferred<Unit>()
        val finishSave = CompletableDeferred<Unit>()
        repository.pauseNextSave(enteredSave, finishSave)
        val access = FullAccessUseCases(repository, ReconciliationBilling())
        val first = async { repository.completeLocally() }
        enteredSave.await()
        val second = async { repository.completeLocally() }
        runCurrent()
        finishSave.complete(Unit)

        first.await().successValue()
        second.await().successValue()
        assertEquals(2, repository.state.completedFreeWorkouts)
    }

    @Test
    fun storeNetworkAndUserPurchaseWaitsDoNotBlockLocalWorkoutCompletion() = runTest {
        for (operation in listOf("refresh", "purchase", "restore")) {
            val repository = ReconciliationRepository(FullAccessState(completedFreeWorkouts = 1))
            val enteredNetwork = CompletableDeferred<Unit>()
            val finishNetwork = CompletableDeferred<Unit>()
            val network: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
                enteredNetwork.complete(Unit)
                finishNetwork.await()
                foundationSuccess(FullAccessEntitlementSnapshot())
            }
            val billing = ReconciliationBilling().apply {
                when (operation) {
                    "refresh" -> refresh = network
                    "purchase" -> purchase = network
                    else -> restore = network
                }
            }
            val access = FullAccessUseCases(repository, billing)
            val pending = async {
                when (operation) {
                    "refresh" -> access.refreshEntitlements()
                    "purchase" -> access.purchaseLifetimeUnlock()
                    else -> access.restorePurchases()
                }
            }
            enteredNetwork.await()
            val completion = async { repository.completeLocally() }
            runCurrent()
            val completedBeforeNetwork = completion.isCompleted
            finishNetwork.complete(Unit)
            pending.await().successValue()
            completion.await().successValue()

            assertTrue(completedBeforeNetwork, "Local completion must not wait for $operation")
            assertEquals(2, repository.state.completedFreeWorkouts)
        }
    }

    @Test
    fun externalRefreshCancellationPropagatesPreservesOwnershipAndAllowsRetry() = runTest {
        val repository = ReconciliationRepository(FullAccessState(lifetimeUnlocked = true))
        val before = repository.state
        val billing = ReconciliationBilling().apply { refresh = { awaitCancellation() } }
        val access = FullAccessUseCases(repository, billing)

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { access.refreshEntitlements() }
        }
        assertEquals(before, repository.state)
        billing.refresh = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        assertTrue(access.refreshEntitlements().successValue().hasFullAccess)
    }

    @Test
    fun canceledPersistenceReleasesLockForNextLocalCompletion() = runTest {
        val repository = ReconciliationRepository()
        repository.pauseNextSave(CompletableDeferred(), CompletableDeferred())
        val access = FullAccessUseCases(repository, ReconciliationBilling())

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { repository.completeLocally() }
        }
        assertEquals(0, repository.state.completedFreeWorkouts)
        repository.completeLocally().successValue()
        assertEquals(1, repository.state.completedFreeWorkouts)
    }
}

private class ReconciliationRepository(var state: FullAccessState = FullAccessState()) : FullAccessRepository {
    private val mutex = Mutex()
    var beforeSave: suspend () -> Unit = {}
    var saveFailure: FoundationError? = null
    override suspend fun loadFullAccess(): FullAccessState = state
    override suspend fun updateFullAccess(transform: (FullAccessState) -> FullAccessState): FoundationResult<FullAccessState> = mutex.withLock {
        beforeSave()
        saveFailure?.let { return@withLock foundationFailure(it) }
        state = transform(state)
        foundationSuccess(state)
    }

    // Models the completion transaction sharing the repository's accounting lock.
    suspend fun completeLocally(): FoundationResult<FullAccessState> =
        updateFullAccess { it.recordLocalCompletion(Clock.System.now()) }

    fun pauseNextSave(entered: CompletableDeferred<Unit>, finish: CompletableDeferred<Unit>) {
        var pending = true
        beforeSave = {
            if (pending) {
                pending = false
                entered.complete(Unit)
                finish.await()
            }
        }
    }
}

private class ReconciliationBilling : FullAccessBillingAdapter {
    var deliveryCalls = 0
    val deliveredTokens = mutableListOf<String?>()
    var completeDelivery: suspend (String?) -> FoundationResult<Unit> = { foundationSuccess(Unit) }
    var refresh: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot())
    }
    var purchase: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
    }
    var restore: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot())
    }
    override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> = foundationSuccess(emptyList())
    override suspend fun refreshEntitlements() = refresh()
    override suspend fun purchaseLifetimeUnlock() = purchase()
    override suspend fun restorePurchases() = restore()
    override suspend fun completeEntitlementDelivery(deliveryToken: String?): FoundationResult<Unit> {
        deliveryCalls++
        deliveredTokens += deliveryToken
        return completeDelivery(deliveryToken)
    }
}
