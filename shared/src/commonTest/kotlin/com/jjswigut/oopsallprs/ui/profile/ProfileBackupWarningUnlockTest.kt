package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.FakeSyncRepository
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.platform.FullAccessBillingObserver
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileBackupWarningUnlockTest {
    @Test
    fun purchaseAfterDeniedConflictRestoreRecoversPersistedWarningWithoutReplay() = runTest {
        val fixture = WarningFixture()
        fixture.denyConflictRestore()

        fixture.holder.purchaseLifetimeUnlock().successValue()

        fixture.assertUnlockedWithWarningAndNoBackupIo()
    }

    @Test
    fun restorePurchaseAfterDeniedConflictRestoreRecoversPersistedWarningWithoutReplay() = runTest {
        val fixture = WarningFixture()
        fixture.denyConflictRestore()

        fixture.holder.restorePurchases().successValue()

        fixture.assertUnlockedWithWarningAndNoBackupIo()
        assertEquals("Lifetime purchase restored.", fixture.holder.state.value.fullAccessStatus.storeMessage)
    }

    @Test
    fun observerUnlockAfterDeniedConflictRestoreRecoversPersistedWarningWithoutReplay() = runTest {
        val fixture = WarningFixture()
        fixture.denyConflictRestore()
        val initialRefresh = CompletableDeferred<Unit>()
        val unlockedRefresh = CompletableDeferred<Unit>()
        val collector = launch {
            fixture.access.observeEntitlementChanges().collect {
                fixture.access.refreshEntitlements().successValue()
                fixture.holder.refreshFullAccess()
                if (fixture.holder.state.value.fullAccessStatus.hasFullAccess) unlockedRefresh.complete(Unit)
                else initialRefresh.complete(Unit)
            }
        }
        try {
            initialRefresh.await()
            fixture.billing.unlocked = true
            assertNotNull(fixture.billing.observer).onEntitlementsChanged()
            unlockedRefresh.await()

            fixture.assertUnlockedWithWarningAndNoBackupIo()
        } finally {
            collector.cancelAndJoin()
        }
    }

    @Test
    fun hydrationWaitingForBackupStatePreservesCompletedPurchaseStatusAndWarning() = runTest {
        val delegate = FakeSyncRepository(conflictState())
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var pauseRead = false
        val sync = object : BackupSyncRepository by delegate {
            override suspend fun loadSyncState(): BackupSyncState {
                val snapshot = delegate.loadSyncState()
                if (pauseRead) {
                    pauseRead = false
                    entered.complete(Unit)
                    finish.await()
                }
                return snapshot
            }
        }
        val fixture = WarningFixture(sync)
        fixture.denyConflictRestore()
        fixture.billing.purchaseMessage = "Store delivery needs another check."
        pauseRead = true
        val hydration = async { fixture.holder.hydrate() }
        entered.await()
        fixture.holder.purchaseLifetimeUnlock().successValue()
        val purchasedStatus = fixture.holder.state.value.fullAccessStatus
        finish.complete(Unit)
        hydration.await()

        assertEquals(purchasedStatus, fixture.holder.state.value.fullAccessStatus)
        assertEquals("Store delivery needs another check.", purchasedStatus.storeMessage)
        assertFalse(purchasedStatus.isStoreBusy)
        fixture.assertUnlockedWithWarningAndNoBackupIo()
    }

    @Test
    fun purchaseDoesNotClearAnUnrelatedBackupError() = runTest {
        val store = InMemoryFoundationStore()
        store.setFullAccessForTest(FullAccessState(lifetimeUnlocked = true)).successValue()
        val holder = ProfileStateHolder(fullAccess = FullAccessUseCases(store, WarningBilling()))
        holder.hydrate()
        assertIs<FoundationResult.Failure>(holder.backupNow())
        assertEquals("Backup unavailable", holder.state.value.backupError)
        store.setFullAccessForTest(FullAccessState()).successValue()
        holder.refreshFullAccess()
        holder.refreshStoreOffer()

        holder.purchaseLifetimeUnlock().successValue()

        assertEquals("Backup unavailable", holder.state.value.backupError)
    }

    @Test
    fun newerBackupStateClearsRetainedWarningBeforeLaterUnlock() = runTest {
        val fixture = WarningFixture()
        fixture.denyConflictRestore()
        fixture.sync.saveSyncState(conflictState().copy(
            lastOutcome = BackupSyncOutcome.CLEAN,
            lastError = null
        )).successValue()
        fixture.holder.checkLinkedBackup().successValue()
        assertNull(fixture.holder.state.value.backupStatus.warning)
        assertNull(fixture.holder.state.value.backupError)

        assertIs<FoundationResult.Failure>(fixture.holder.backupNow())
        fixture.holder.purchaseLifetimeUnlock().successValue()

        assertNull(fixture.holder.state.value.backupError)
        assertFalse(fixture.holder.state.value.backupStatus.hasConflict)
        assertEquals(0, fixture.documents.writeCount)
    }
}

private const val BACKUP_WARNING = "Provider unavailable; review the backup conflict."

private fun conflictState() = BackupSyncState(
    linkedFile = BackupLinkedFile("backup.json", "mem://backup", "memory"),
    lastOutcome = BackupSyncOutcome.CONFLICT,
    lastError = BACKUP_WARNING,
    updatedAt = instant(1)
)

private class WarningFixture(val sync: BackupSyncRepository = FakeSyncRepository(conflictState())) {
    val billing = WarningBilling()
    val access = FullAccessUseCases(InMemoryFoundationStore(), billing)
    val repository = FakeBackupRepository(packageWithRevision("local-1"))
    val documents = FakeDocumentAdapter("unavailable")
    val holder = ProfileStateHolder(
        backupSync = BackupSyncCoordinator(repository, sync, documents),
        fullAccess = access
    )

    suspend fun denyConflictRestore() {
        holder.hydrate()
        assertEquals(BACKUP_WARNING, holder.state.value.backupError)
        assertIs<FoundationResult.Failure>(holder.restoreBackupConflict())
        assertTrue(holder.state.value.isUnlockDialogVisible)
        assertEquals("Unlock forever to restore from backup.", holder.state.value.backupError)
        assertEquals(BACKUP_WARNING, holder.state.value.backupStatus.warning)
    }

    suspend fun assertUnlockedWithWarningAndNoBackupIo() {
        assertTrue(holder.state.value.fullAccessStatus.hasFullAccess)
        assertFalse(holder.state.value.isUnlockDialogVisible)
        assertNull(holder.state.value.fullAccessStatus.error)
        assertTrue(holder.state.value.backupStatus.hasConflict)
        assertEquals(BACKUP_WARNING, holder.state.value.backupStatus.warning)
        assertEquals(BACKUP_WARNING, sync.loadSyncState().lastError)
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)
        assertEquals(0, repository.restoreCount)
        assertEquals(0, repository.createPackageCount)
        assertEquals(0, documents.createCount)
        assertEquals(0, documents.openCount)
        assertEquals(0, documents.readCount)
        assertEquals(0, documents.writeCount)
        assertEquals(BACKUP_WARNING, holder.state.value.backupError)
    }
}

private class WarningBilling : FullAccessBillingAdapter {
    var observer: FullAccessBillingObserver? = null
    var unlocked = false
    var purchaseMessage: String? = null
    override fun setEntitlementObserver(observer: FullAccessBillingObserver?) {
        this.observer = observer
    }
    override suspend fun loadOffers() = foundationSuccess(listOf(
        FullAccessStoreOffer("Lifetime", "EUR 17.49", "One-time store purchase.")
    ))
    override suspend fun refreshEntitlements() = foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = unlocked))
    override suspend fun purchaseLifetimeUnlock() = foundationSuccess(
        FullAccessEntitlementSnapshot(lifetimeUnlocked = true, message = purchaseMessage)
    )
    override suspend fun restorePurchases() = foundationSuccess(FullAccessEntitlementSnapshot(lifetimeUnlocked = true))
}
