package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileStoreOperationsTest {
    @Test
    fun optionalPurchaseIsAvailableAtEveryTrialStageWithVerifiedLocalizedOffer() = runTest {
        for (completed in listOf(0, 1, 9, 10)) {
            val store = InMemoryFoundationStore()
            store.setFullAccessForTest(FullAccessState(completedFreeWorkouts = completed)).successValue()
            val billing = ControlledStoreBilling()
            val holder = holder(store, billing)
            assertIs<FullAccessOfferState.Loading>(holder.state.value.fullAccessStatus.offerState)
            assertFalse(holder.state.value.fullAccessStatus.canPurchase)
            assertEquals("", holder.state.value.fullAccessStatus.offerLabel)

            holder.hydrate()
            holder.refreshStoreOffer()
            val status = holder.state.value.fullAccessStatus
            assertTrue(status.canPurchase, "Trial count $completed must not hide purchase")
            assertEquals(VERIFIED_OFFER.priceLabel, status.offerLabel)
            assertEquals(VERIFIED_OFFER.termsLabel, status.termsLabel)
            holder.purchaseLifetimeUnlock().successValue()
            assertEquals(1, billing.purchases)
            assertTrue(holder.state.value.fullAccessStatus.hasFullAccess)
            assertFalse(holder.state.value.fullAccessStatus.canPurchase)
            assertEquals(completed, store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test
    fun hydrationPublishesLocalPreferencesWithoutFetchingOrWaitingForStoreOffer() = runTest {
        val store = InMemoryFoundationStore()
        store.setDefaultRestSeconds(90).successValue()
        val blocked = CompletableDeferred<Unit>()
        var offerReads = 0
        val billing = ControlledStoreBilling().apply {
            offers = { offerReads++; blocked.await(); foundationSuccess(listOf(VERIFIED_OFFER)) }
        }
        val holder = holder(store, billing)
        holder.hydrate()
        assertEquals(0, offerReads)
        assertTrue(holder.state.value.isHydrated)
        assertEquals(90, holder.state.value.defaultRestSeconds)

        val fetching = launch { holder.refreshStoreOffer() }
        runCurrent()
        store.setDefaultRestSeconds(60).successValue()
        holder.hydrate()
        assertEquals(60, holder.state.value.defaultRestSeconds)
        assertTrue(holder.state.value.fullAccessStatus.isStoreBusy)
        fetching.cancel()
        fetching.join()
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
    }

    @Test
    fun offerTimeoutEndsLoadingAndAllowsExplicitRetry() = runTest {
        val blocked = CompletableDeferred<Unit>()
        val billing = ControlledStoreBilling().apply {
            offers = { blocked.await(); foundationSuccess(listOf(VERIFIED_OFFER)) }
        }
        val holder = holder(InMemoryFoundationStore(), billing)
        val fetching = async { holder.refreshStoreOffer() }
        runCurrent()
        assertIs<FullAccessOfferState.Loading>(holder.state.value.fullAccessStatus.offerState)
        assertTrue(holder.state.value.fullAccessStatus.isStoreBusy)
        advanceTimeBy(15_001)
        runCurrent()
        assertIs<FullAccessOfferState.Unavailable>(fetching.await())
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        assertFalse(holder.state.value.fullAccessStatus.canPurchase)
        billing.offers = { foundationSuccess(listOf(VERIFIED_OFFER)) }
        holder.refreshStoreOffer()
        assertTrue(holder.state.value.fullAccessStatus.canPurchase)
    }

    @Test
    fun callerTimeoutPropagatesAndReleasesOfferLoadingState() = runTest {
        val billing = ControlledStoreBilling().apply { offers = { awaitCancellation() } }
        val holder = holder(InMemoryFoundationStore(), billing)

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { holder.refreshStoreOffer() }
        }
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        assertIs<FullAccessOfferState.Unavailable>(holder.state.value.fullAccessStatus.offerState)
        billing.offers = { foundationSuccess(listOf(VERIFIED_OFFER)) }
        holder.refreshStoreOffer()
        assertTrue(holder.state.value.fullAccessStatus.canPurchase)
    }

    @Test
    fun purchaseWithoutVerifiedOfferDoesNotLaunchAndOfferRetryEnablesIt() = runTest {
        val billing = ControlledStoreBilling()
        val holder = holder(InMemoryFoundationStore(), billing)
        assertIs<FoundationResult.Failure>(holder.purchaseLifetimeUnlock())
        assertEquals(0, billing.purchases)
        billing.offers = { foundationSuccess(emptyList()) }
        holder.refreshStoreOffer()
        assertIs<FullAccessOfferState.Unavailable>(holder.state.value.fullAccessStatus.offerState)
        assertIs<FoundationResult.Failure>(holder.purchaseLifetimeUnlock())
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)

        billing.offers = { foundationSuccess(listOf(VERIFIED_OFFER)) }
        holder.refreshStoreOffer()
        assertTrue(holder.state.value.fullAccessStatus.canPurchase)
        holder.purchaseLifetimeUnlock().successValue()
        assertEquals(1, billing.purchases)
    }

    @Test
    fun freeExportDoesNotLoadAnOfferOrOpenAnUnlockDialog() = runTest {
        val offerReady = CompletableDeferred<Unit>()
        val billing = ControlledStoreBilling().apply { offers = { offerReady.await(); foundationSuccess(listOf(VERIFIED_OFFER)) } }
        val holder = holder(InMemoryFoundationStore(), billing)
        holder.export(ExportType.WORKOUTS).successValue()

        assertFalse(holder.state.value.isUnlockDialogVisible)
        assertNull(holder.state.value.exportError)
        assertTrue(holder.state.value.lastExport != null)
        assertFalse(holder.state.value.isExporting)
    }

    @Test
    fun restoreWithoutOfferReportsNoPurchaseSeparatelyFromFailureAndAllowsRetry() = runTest {
        val billing = ControlledStoreBilling().apply { offers = { error("Must not load offers for restore") } }
        val holder = holder(InMemoryFoundationStore(), billing)
        holder.startBackupSetup()
        val restored = holder.restorePurchases().successValue()
        assertFalse(restored.hasFullAccess)
        assertEquals(1, billing.restores)
        assertEquals("No lifetime purchase was found for this store account.", holder.state.value.fullAccessStatus.storeMessage)
        assertNull(holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        assertTrue(holder.state.value.isUnlockDialogVisible)

        billing.restore = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        holder.restorePurchases().successValue()
        assertEquals("Lifetime purchase restored.", holder.state.value.fullAccessStatus.storeMessage)
        assertTrue(holder.state.value.fullAccessStatus.hasFullAccess)
        assertNull(holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.isUnlockDialogVisible)
        assertNull(holder.state.value.backupError)
        assertNull(holder.state.value.backupSetupStep)
    }

    @Test
    fun returnedFailuresAndThrownStoreErrorsReleaseBusyAndPermitRetry() = runTest {
        val billing = ControlledStoreBilling()
        val holder = holder(InMemoryFoundationStore(), billing)
        holder.refreshStoreOffer()
        billing.purchase = { foundationFailure(FoundationError.Platform("Purchase canceled")) }
        assertIs<FoundationResult.Failure>(holder.purchaseLifetimeUnlock())
        assertEquals("Purchase canceled", holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        assertTrue(holder.state.value.fullAccessStatus.canPurchase)

        billing.purchase = { error("Disconnected") }
        assertIs<FoundationResult.Failure>(holder.purchaseLifetimeUnlock())
        assertEquals("Disconnected", holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)

        billing.purchase = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        holder.purchaseLifetimeUnlock().successValue()
        assertTrue(holder.state.value.fullAccessStatus.hasFullAccess)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
    }

    @Test
    fun restoreFailuresDoNotDependOnOfferAndReleaseBusyForRetry() = runTest {
        val billing = ControlledStoreBilling()
        val holder = holder(InMemoryFoundationStore(), billing)
        billing.restore = { foundationFailure(FoundationError.Platform("Restore unavailable")) }
        assertIs<FoundationResult.Failure>(holder.restorePurchases())
        assertEquals("Restore unavailable", holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        assertNull(holder.state.value.fullAccessStatus.storeMessage)
        billing.restore = { error("Store disconnected") }
        assertIs<FoundationResult.Failure>(holder.restorePurchases())
        assertEquals("Store disconnected", holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        billing.restore = { foundationSuccess(FullAccessEntitlementSnapshot()) }
        holder.restorePurchases().successValue()
        assertNull(holder.state.value.fullAccessStatus.error)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
    }

    @Test
    fun purchaseAndRestoreRunSeriallyWithoutReloadingOfferAfterSuccess() = runTest {
        val entered = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        val billing = ControlledStoreBilling().apply {
            purchase = { entered.complete(Unit); complete.await(); foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
            restore = { foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        }
        val holder = holder(InMemoryFoundationStore(), billing)
        holder.refreshStoreOffer()
        billing.offers = { error("Must not reload offer after committed store operation") }
        val purchase = async { holder.purchaseLifetimeUnlock() }
        entered.await()
        val restore = async { holder.restorePurchases() }
        runCurrent()
        assertEquals(0, billing.restores)
        assertFalse(restore.isCompleted)
        complete.complete(Unit)
        purchase.await().successValue()
        restore.await().successValue()
        assertEquals(1, billing.restores)
        assertTrue(holder.state.value.fullAccessStatus.hasFullAccess)
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
    }

    @Test
    fun canceledOfferPurchaseAndRestoreReleaseBusyAndCanBeRetried() = runTest {
        for (operation in listOf("offer", "purchase", "restore")) {
            val billing = ControlledStoreBilling()
            val holder = holder(InMemoryFoundationStore(), billing)
            holder.refreshStoreOffer()
            val entered = CompletableDeferred<Unit>()
            val wait = CompletableDeferred<Unit>()
            when (operation) {
                "offer" -> billing.offers = { entered.complete(Unit); wait.await(); foundationSuccess(listOf(VERIFIED_OFFER)) }
                "purchase" -> billing.purchase = { entered.complete(Unit); wait.await(); foundationSuccess(FullAccessEntitlementSnapshot()) }
                else -> billing.restore = { entered.complete(Unit); wait.await(); foundationSuccess(FullAccessEntitlementSnapshot()) }
            }
            var cancellationObserved = false
            val job = launch {
                try {
                    when (operation) {
                        "offer" -> holder.refreshStoreOffer()
                        "purchase" -> holder.purchaseLifetimeUnlock()
                        else -> holder.restorePurchases()
                    }
                } catch (canceled: CancellationException) {
                    cancellationObserved = true
                    throw canceled
                }
            }
            entered.await()
            assertTrue(holder.state.value.fullAccessStatus.isStoreBusy)
            job.cancel()
            job.join()
            assertTrue(cancellationObserved)
            assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)

            billing.offers = { foundationSuccess(listOf(VERIFIED_OFFER)) }
            billing.restore = { foundationSuccess(FullAccessEntitlementSnapshot()) }
            holder.refreshStoreOffer()
            holder.restorePurchases().successValue()
            assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
        }
    }

    @Test
    fun queuedOperationCancellationDoesNotClearRunningOperationBusyState() = runTest {
        val entered = CompletableDeferred<Unit>()
        val complete = CompletableDeferred<Unit>()
        val billing = ControlledStoreBilling().apply {
            purchase = { entered.complete(Unit); complete.await(); foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true)) }
        }
        val holder = holder(InMemoryFoundationStore(), billing)
        holder.refreshStoreOffer()
        val purchase = async { holder.purchaseLifetimeUnlock() }
        entered.await()
        val queued = launch { holder.restorePurchases() }
        runCurrent()
        assertEquals(0, billing.restores)
        queued.cancel()
        queued.join()
        assertTrue(holder.state.value.fullAccessStatus.isStoreBusy)
        complete.complete(Unit)
        purchase.await().successValue()
        assertFalse(holder.state.value.fullAccessStatus.isStoreBusy)
    }

    private fun holder(store: InMemoryFoundationStore, billing: ControlledStoreBilling) = ProfileStateHolder(
        preferences = store, exports = store, fullAccess = FullAccessUseCases(store, billing)
    )
}

private val VERIFIED_OFFER = FullAccessStoreOffer("Lifetime", "EUR 17.49", "One-time store purchase.")

private class ControlledStoreBilling : FullAccessBillingAdapter {
    var purchases = 0
    var restores = 0
    var offers: suspend () -> FoundationResult<List<FullAccessStoreOffer>> = { foundationSuccess(listOf(VERIFIED_OFFER)) }
    var purchase: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
    }
    var restore: suspend () -> FoundationResult<FullAccessEntitlementSnapshot> = {
        foundationSuccess(FullAccessEntitlementSnapshot())
    }
    override suspend fun loadOffers() = offers()
    override suspend fun refreshEntitlements() = foundationSuccess(FullAccessEntitlementSnapshot())
    override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot> {
        purchases++
        return purchase()
    }
    override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot> {
        restores++
        return restore()
    }
}
