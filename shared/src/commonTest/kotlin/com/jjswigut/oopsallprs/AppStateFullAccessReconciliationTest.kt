package com.jjswigut.oopsallprs

import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.platform.FullAccessBillingObserver
import com.jjswigut.oopsallprs.testing.testAppState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppStateFullAccessReconciliationTest {
    @Test
    fun storeSignalUpdatesProfileAndTrainWithoutReplayingBlockedActions() = runTest {
        val store = InMemoryFoundationStore()
        store.updateFullAccess { FullAccessState(completedFreeWorkouts = 10) }
        val billing = SignalingBilling()
        val app = testAppState(store = store, billing = billing)
        val collection = backgroundScope.launch { app.observeFullAccessEntitlements() }
        runCurrent()
        assertNotNull(billing.observer)
        assertIs<FoundationResult.Failure>(app.workoutHome.startEmpty())
        app.profile.startBackupSetup()
        assertTrue(app.workoutHome.state.value.isFullAccessPaywallVisible)
        assertTrue(app.profile.state.value.isUnlockDialogVisible)
        app.profile.restorePurchases()
        assertNotNull(app.profile.state.value.fullAccessStatus.storeMessage)

        billing.unlocked = true
        billing.observer?.onEntitlementsChanged()
        runCurrent()

        assertTrue(app.profile.state.value.fullAccessStatus.hasFullAccess)
        assertTrue(app.workoutHome.state.value.fullAccess.hasFullAccess)
        assertFalse(app.profile.state.value.isUnlockDialogVisible)
        assertFalse(app.workoutHome.state.value.isFullAccessPaywallVisible)
        assertNull(app.profile.state.value.fullAccessStatus.storeMessage)
        assertNull(app.workoutHome.state.value.activeSession)
        assertNull(app.profile.state.value.lastExport)

        billing.unlocked = false
        billing.observer?.onEntitlementsChanged()
        runCurrent()
        assertFalse(app.profile.state.value.fullAccessStatus.hasFullAccess)
        assertFalse(app.workoutHome.state.value.fullAccess.hasFullAccess)
        assertIs<FoundationResult.Failure>(app.workoutHome.startEmpty())
        collection.cancelAndJoin()
        assertNull(billing.observer)
    }

    @Test
    fun backgroundAccessRefreshDoesNotDismissAnOpenSettingsEditor() = runTest {
        val app = testAppState(billing = SignalingBilling())
        app.profile.hydrate()
        app.profile.openDefaultRestPicker()
        app.profile.setDraftDefaultRestSeconds(90)

        app.refreshFullAccessEntitlements()

        assertTrue(app.profile.state.value.isDefaultRestPickerVisible)
        assertEquals(90, app.profile.state.value.draftDefaultRestSeconds)
    }
}

private class SignalingBilling : FullAccessBillingAdapter {
    var observer: FullAccessBillingObserver? = null
    var unlocked = false
    override fun setEntitlementObserver(observer: FullAccessBillingObserver?) {
        this.observer = observer
    }
    override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>> = foundationSuccess(emptyList())
    override suspend fun refreshEntitlements() = foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = unlocked))
    override suspend fun purchaseLifetimeUnlock() = refreshEntitlements()
    override suspend fun restorePurchases() = refreshEntitlements()
}
