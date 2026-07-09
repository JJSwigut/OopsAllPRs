package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.FakeSyncRepository
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileFullAccessGateTest {
    @Test
    fun unpaidTrialCannotExportOrStartBackupSetup() = runTest {
        val store = InMemoryFoundationStore()
        val holder = ProfileStateHolder(
            preferences = store,
            exports = store,
            fullAccess = FullAccessUseCases(store)
        )
        holder.hydrate()

        val exportResult = holder.export(ExportType.WORKOUTS)
        holder.startBackupSetup()

        assertTrue(exportResult is FoundationResult.Failure)
        assertNull(holder.state.value.lastExport)
        assertEquals("Unlock forever to export your data.", holder.state.value.exportError)
        assertNull(holder.state.value.backupSetupStep)
        assertEquals("Unlock forever to set up backup.", holder.state.value.backupError)
    }

    @Test
    fun freeStatusUsesDefaultTenWorkoutLimit() = runTest {
        val store = InMemoryFoundationStore()
        store.saveFullAccess(FullAccessState(completedFreeWorkouts = 7)).successValue()
        val holder = ProfileStateHolder(
            preferences = store,
            fullAccess = FullAccessUseCases(store)
        )

        holder.hydrate()

        val access = holder.state.value.fullAccessStatus
        assertEquals("Free", access.statusLabel)
        assertEquals("7 of 10 free workouts used", access.detailLabel)
        assertEquals(7, access.completedFreeWorkouts)
        assertEquals(DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT, access.freeWorkoutLimit)
        assertFalse(access.isFreeLimitReached)
    }

    @Test
    fun limitReachedStatusUsesLifetimeOnlyUnlockCopy() = runTest {
        val store = InMemoryFoundationStore()
        store.saveFullAccess(
            FullAccessState(completedFreeWorkouts = DEFAULT_FREE_COMPLETED_WORKOUT_LIMIT)
        ).successValue()
        val holder = ProfileStateHolder(
            preferences = store,
            fullAccess = FullAccessUseCases(store)
        )

        holder.hydrate()

        val access = holder.state.value.fullAccessStatus
        assertEquals("Unlock required", access.statusLabel)
        assertEquals("You've used your free workouts.", access.detailLabel)
        assertEquals("${'$'}14.99", access.offerLabel)
        assertEquals("One-time purchase. No subscription. No account.", access.termsLabel)
        assertTrue(access.isFreeLimitReached)
    }

    @Test
    fun paidUserCanExport() = runTest {
        val store = InMemoryFoundationStore()
        store.saveFullAccess(FullAccessState(lifetimeUnlocked = true)).successValue()
        val holder = ProfileStateHolder(
            preferences = store,
            exports = store,
            fullAccess = FullAccessUseCases(store)
        )
        holder.hydrate()

        holder.export(ExportType.WORKOUTS).successValue()

        assertEquals(ExportType.WORKOUTS, holder.state.value.lastExport?.type)
        assertNull(holder.state.value.exportError)
    }

    @Test
    fun unpaidTrialSkipsAutomaticLinkedBackupSync() = runTest {
        val store = InMemoryFoundationStore()
        val localPackage = packageWithRevision("local-2")
        val backupPackage = packageWithRevision("backup-1")
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(backupPackage).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = FakeBackupRepository(
                    localPackage,
                    BackupRevision("local-2", instant(2), SnapshotSummary(workoutCount = 2))
                ),
                syncRepository = FakeSyncRepository(
                    BackupSyncState(
                        linkedFile = LINK,
                        lastBackupRevision = "backup-1",
                        lastLocalRevision = "local-1",
                        lastOutcome = BackupSyncOutcome.CLEAN,
                        updatedAt = instant(1)
                    )
                ),
                documents = documents
            ),
            fullAccess = FullAccessUseCases(store)
        )
        holder.hydrate()

        holder.checkLinkedBackup().successValue()

        assertEquals(0, documents.writeCount)
        assertNull(holder.state.value.backupError)
        assertEquals("Up to date", holder.state.value.backupStatus.lastOutcomeLabel)
    }

    @Test
    fun unpaidTrialCannotRunBackupOrRestoreOperations() = runTest {
        val store = InMemoryFoundationStore()
        val repository = FakeBackupRepository(packageWithRevision("local-1"))
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(packageWithRevision("backup-1")).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = repository,
                syncRepository = FakeSyncRepository(
                    BackupSyncState(
                        linkedFile = LINK,
                        lastBackupRevision = "backup-1",
                        lastLocalRevision = "local-1",
                        lastOutcome = BackupSyncOutcome.CLEAN,
                        updatedAt = instant(1)
                    )
                ),
                documents = documents
            ),
            fullAccess = FullAccessUseCases(store)
        )
        holder.hydrate()

        assertTrue(holder.linkBackupFile() is FoundationResult.Failure)
        assertTrue(holder.backupNow() is FoundationResult.Failure)
        assertTrue(holder.syncNow() is FoundationResult.Failure)
        assertTrue(holder.restoreFromFile() is FoundationResult.Failure)

        assertEquals(0, repository.createPackageCount)
        assertEquals(0, repository.restoreCount)
        assertEquals(0, documents.createCount)
        assertEquals(0, documents.openCount)
        assertEquals(0, documents.readCount)
        assertEquals(0, documents.writeCount)
        assertEquals("Unlock forever to restore from backup.", holder.state.value.backupError)
    }

    @Test
    fun unpaidTrialCannotRestoreBackupConflict() = runTest {
        val store = InMemoryFoundationStore()
        val repository = FakeBackupRepository(packageWithRevision("local-1"))
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(packageWithRevision("backup-2")).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = repository,
                syncRepository = FakeSyncRepository(
                    BackupSyncState(
                        linkedFile = LINK,
                        lastBackupRevision = "backup-2",
                        lastLocalRevision = "local-1",
                        lastOutcome = BackupSyncOutcome.CONFLICT,
                        updatedAt = instant(1)
                    )
                ),
                documents = documents
            ),
            fullAccess = FullAccessUseCases(store)
        )
        holder.hydrate()

        val result = holder.restoreBackupConflict()

        assertTrue(result is FoundationResult.Failure)
        assertEquals(0, repository.restoreCount)
        assertEquals(0, documents.createCount)
        assertEquals("Unlock forever to restore from backup.", holder.state.value.backupError)
    }

    private companion object {
        val LINK = BackupLinkedFile("backup.json", "mem://backup", "memory")
    }
}
