package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.FakeSyncRepository
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ProfileBackupConflictStateTest {
    @Test
    fun restoredDataIsReportedAsPartialSuccessWhenRelinkingFails() = runTest {
        for (throws in listOf(false, true)) {
            val delegate = FakeSyncRepository(BackupSyncState(linkedFile = LINK, updatedAt = instant(1)))
            val sync = object : BackupSyncRepository by delegate {
                override suspend fun saveSyncState(state: BackupSyncState): FoundationResult<BackupSyncState> {
                    if (state.linkedFile != null) {
                        if (throws) error("Metadata unavailable")
                        return foundationFailure(FoundationError.Persistence("Metadata unavailable"))
                    }
                    return delegate.saveSyncState(state)
                }
            }
            val repository = FakeBackupRepository(packageWithRevision("local-1"))
            val documents = FakeDocumentAdapter(BackupPackageCodec().encode(packageWithRevision("remote-2")).successValue())
            val holder = ProfileStateHolder(backupSync = BackupSyncCoordinator(repository, sync, documents))

            val result = holder.restoreFromFile().successValue()

            assertEquals(1, repository.restoreCount)
            assertNotNull(result.syncWarning)
            assertTrue(holder.state.value.lastRestoreMessage.orEmpty().startsWith("Restored"))
            assertTrue(holder.state.value.backupError.orEmpty().contains("Relink"))
            assertFalse(holder.state.value.backupStatus.canBackup)
            assertNull(sync.loadSyncState().linkedFile)
            assertEquals(BackupSyncOutcome.RESTORED, sync.loadSyncState().lastOutcome)
        }
    }

    @Test
    fun providerFailureKeepsConflictActionsAndErrorAfterProfileRecreation() = runTest {
        val sync = FakeSyncRepository(BackupSyncState(
            linkedFile = LINK,
            lastBackupRevision = "backup-1",
            lastLocalRevision = "local-1",
            lastOutcome = BackupSyncOutcome.CONFLICT,
            updatedAt = instant(1)
        ))
        val delegate = FakeDocumentAdapter("unavailable")
        val documents = object : BackupDocumentAdapter by delegate {
            override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String> =
                foundationFailure(FoundationError.Platform("Provider unavailable"))
        }
        val coordinator = BackupSyncCoordinator(FakeBackupRepository(packageWithRevision("local-2")), sync, documents)
        val holder = ProfileStateHolder(backupSync = coordinator)

        holder.syncNow()

        assertTrue(holder.state.value.backupStatus.hasConflict)
        assertEquals("Provider unavailable", holder.state.value.backupError)
        val reopened = ProfileStateHolder(backupSync = coordinator)
        reopened.hydrate()
        assertTrue(reopened.state.value.backupStatus.hasConflict)
        assertEquals("Provider unavailable", reopened.state.value.backupError)
    }

    @Test
    fun syncNowExposesConflictActions() = runTest {
        val backupPackage = packageWithRevision("backup-2")
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = FakeBackupRepository(
                    packageWithRevision("local-2"),
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
                documents = FakeDocumentAdapter(BackupPackageCodec().encode(backupPackage).successValue())
            )
        )

        holder.syncNow().successValue()

        assertTrue(holder.state.value.backupStatus.hasConflict)
        assertEquals("Conflict needs review", holder.state.value.backupStatus.lastOutcomeLabel)
    }

    @Test
    fun keepLocalConflictDecisionOverwritesBackup() = runTest {
        val pkg = packageWithRevision("local-2")
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(packageWithRevision("backup-2")).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = FakeBackupRepository(pkg),
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
            )
        )

        holder.keepLocalBackup().successValue()

        assertEquals(1, documents.writeCount)
        assertEquals("Backup updated", holder.state.value.backupStatus.lastOutcomeLabel)
    }

    private companion object {
        val LINK = BackupLinkedFile("backup.json", "mem://backup", "memory")
    }
}
