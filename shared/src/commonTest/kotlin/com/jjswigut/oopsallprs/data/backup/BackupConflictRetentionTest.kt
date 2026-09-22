package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BackupConflictRetentionTest {
    @Test
    fun repeatedChecksAndLaterLocalEditCannotOverwriteUnresolvedRemoteHistory() = runTest {
        val remote = BackupPackageCodec().encode(packageWithRevision("remote-2")).successValue()
        val documents = FakeDocumentAdapter(remote)
        val sync = linkedState()
        val originalLocal = FakeBackupRepository(packageWithRevision("local-1"))

        // Recreate the coordinator between checks, as on relaunch; only persisted state survives.
        BackupSyncCoordinator(originalLocal, sync, documents).syncNow().successValue()
        BackupSyncCoordinator(originalLocal, sync, documents).syncNow().successValue()
        val editedLocal = FakeBackupRepository(packageWithRevision("local-2"))
        val result = BackupSyncCoordinator(editedLocal, sync, documents).syncNow().successValue()

        assertEquals(0, documents.writeCount, "Unresolved remote history must not be overwritten")
        assertEquals(remote, documents.content)
        assertEquals(BackupSyncOutcome.CONFLICT, result.lastOutcome)
    }

    @Test
    fun backupNowCannotBypassUnresolvedRemoteHistory() = runTest {
        val remote = BackupPackageCodec().encode(packageWithRevision("remote-2")).successValue()
        val documents = FakeDocumentAdapter(remote)
        val coordinator = BackupSyncCoordinator(FakeBackupRepository(packageWithRevision("local-2")), linkedState(), documents)

        val result = coordinator.backupNow().successValue()

        assertEquals(0, documents.writeCount)
        assertEquals(remote, documents.content)
        assertEquals(BackupSyncOutcome.CONFLICT, result.lastOutcome)
    }

    @Test
    fun cancelAndRecreationKeepBothChangedConflictAndAgreedBaselines() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-2")
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        coordinator.syncNow().successValue()
        coordinator.resolveConflict(BackupConflictDecision.CANCEL).successValue()

        val result = BackupSyncCoordinator(repo, sync, docs).syncNow().successValue()

        assertEquals(BackupSyncOutcome.CONFLICT, result.lastOutcome)
        assertEquals(contentRevision("local-1"), result.lastLocalRevision)
        assertEquals(contentRevision("remote-1"), result.lastBackupRevision)
        assertEquals(0, docs.writes)
    }

    @Test
    fun unreadableAndInvalidFilesDoNotClearAnUnresolvedConflict() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-2")
        val originalRemote = docs.content
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        coordinator.syncNow().successValue()

        docs.failRead = true
        coordinator.syncNow()
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)
        assertNotNull(sync.loadSyncState().lastError)
        docs.failRead = false
        docs.content = "not a backup"
        coordinator.syncNow()
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)

        docs.content = originalRemote
        repo.revision = "local-3"
        BackupSyncCoordinator(repo, sync, docs).backupNow().successValue()
        assertEquals(originalRemote, docs.content)
        assertEquals(0, docs.writes)
    }

    @Test
    fun legacyConflictRowsWithAlreadyAdvancedBaselinesRemainUnresolved() = runTest {
        for (outcome in listOf(BackupSyncOutcome.CONFLICT, BackupSyncOutcome.BACKUP_CHANGED)) {
            val sync = FakeSyncRepository(BackupSyncState(
                linkedFile = TEST_LINK,
                lastLocalRevision = "local-1",
                lastBackupRevision = "remote-2",
                lastOutcome = outcome,
                updatedAt = instant(2)
            ))
            val docs = ControlledDocuments("remote-2")
            val coordinator = BackupSyncCoordinator(MutableBackupRepository("local-1"), sync, docs)

            coordinator.syncNow().successValue()
            coordinator.backupNow().successValue()

            assertTrue(sync.loadSyncState().lastOutcome in listOf(BackupSyncOutcome.CONFLICT, BackupSyncOutcome.BACKUP_CHANGED))
            assertEquals(0, docs.writes)
        }
    }

    @Test
    fun explicitKeepLocalResolvesConflictOnlyAfterSuccessfulWrite() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-2")
        val coordinator = BackupSyncCoordinator(MutableBackupRepository("local-2"), sync, docs)
        coordinator.syncNow().successValue()

        val result = coordinator.resolveConflict(BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP).successValue()

        assertEquals(1, docs.writes)
        assertEquals("local-2", BackupPackageCodec().decode(docs.content).successValue().lastLocalRevision)
        assertEquals(contentRevision("local-2"), result.lastLocalRevision)
        assertEquals(contentRevision("local-2"), result.lastBackupRevision)
        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, result.lastOutcome)
        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
    }

    @Test
    fun failedExplicitOverwriteKeepsConflictForRetry() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-2")
        val remote = docs.content
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        coordinator.syncNow().successValue()
        docs.failWrite = true

        assertTrue(coordinator.resolveConflict(BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP) is FoundationResult.Failure)
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)
        assertEquals(contentRevision("local-1"), sync.loadSyncState().lastLocalRevision)
        assertEquals(contentRevision("remote-1"), sync.loadSyncState().lastBackupRevision)

        docs.failWrite = false
        repo.revision = "local-3"
        coordinator.syncNow().successValue()
        assertEquals(remote, docs.content)
        assertEquals(0, docs.writes)
    }

    @Test
    fun remoteChangeWhilePreparingSnapshotStopsAutomaticOverwrite() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-1")
        val repo = MutableBackupRepository("local-2")
        val changedRemote = BackupPackageCodec().encode(packageWithRevision("remote-2")).successValue()
        repo.onEncode = { docs.content = changedRemote }

        BackupSyncCoordinator(repo, sync, docs).syncNow().successValue()

        assertEquals(0, docs.writes)
        assertEquals(changedRemote, docs.content)
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)
    }

    @Test
    fun localEditDuringWriteRemainsDirtyAndIsIncludedInNextBackup() = runTest {
        val sync = linkedState()
        val docs = ControlledDocuments("remote-1")
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        docs.onWrite = { repo.revision = "local-3" }

        val first = coordinator.syncNow().successValue()
        assertEquals(contentRevision("local-2"), first.lastLocalRevision)
        assertEquals(contentRevision("local-2"), first.lastBackupRevision)

        docs.onWrite = {}
        coordinator.syncNow().successValue()
        assertEquals(2, docs.writes)
        assertEquals("local-3", BackupPackageCodec().decode(docs.content).successValue().lastLocalRevision)
    }

    @Test
    fun concurrentSyncAndManualBackupDoNotRacePastComparison() = runTest {
        val enteredWrite = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val docs = ControlledDocuments("remote-1")
        docs.onWrite = {
            enteredWrite.complete(Unit)
            finishWrite.await()
        }
        val coordinator = BackupSyncCoordinator(MutableBackupRepository("local-2"), linkedState(), docs)
        val first = async { coordinator.syncNow() }
        enteredWrite.await()
        val second = async { coordinator.backupNow() }
        runCurrent()
        assertFalse(second.isCompleted)
        assertEquals(2, docs.reads)
        finishWrite.complete(Unit)

        first.await().successValue()
        second.await().successValue()
        assertEquals(2, docs.writes)
    }

    @Test
    fun localEditDuringSafetyExportDoesNotGetReplacedByRestore() = runTest {
        val docs = ControlledDocuments("remote-2")
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, linkedState(), docs)
        coordinator.syncNow().successValue()
        docs.onCreate = { repo.revision = "local-3" }

        assertTrue(coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY) is FoundationResult.Failure)
        assertEquals(0, repo.restores)
        assertEquals("local-3", repo.revision)
        assertEquals(1, docs.safetyCopies.size)
    }

    @Test
    fun restoreResolvesConflictAfterExportingLocalSafetyCopy() = runTest {
        val docs = ControlledDocuments("remote-2")
        val originalRemote = docs.content
        val repo = MutableBackupRepository("local-2")
        val coordinator = BackupSyncCoordinator(repo, linkedState(), docs)
        coordinator.syncNow().successValue()

        val result = coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY).successValue()

        assertEquals(1, repo.restores)
        assertEquals("local-2", BackupPackageCodec().decode(docs.safetyCopies.single()).successValue().lastLocalRevision)
        assertEquals("remote-2", repo.revision)
        assertEquals(originalRemote, docs.content)
        assertEquals(BackupSyncOutcome.RESTORED, result.lastOutcome)
        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
    }

    @Test
    fun canceledSafetyCopyDoesNotRestoreOrDismissConflict() = runTest {
        val docs = ControlledDocuments("remote-2")
        docs.failCreate = true
        val repo = MutableBackupRepository("local-2")
        val sync = linkedState()
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        coordinator.syncNow().successValue()

        assertTrue(coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY) is FoundationResult.Failure)

        assertEquals(0, repo.restores)
        assertEquals("local-2", repo.revision)
        assertTrue(docs.safetyCopies.isEmpty())
        assertEquals(BackupSyncOutcome.CONFLICT, sync.loadSyncState().lastOutcome)
    }

    private fun linkedState() = FakeSyncRepository(BackupSyncState(
        linkedFile = BackupLinkedFile("backup.json", "mem://backup", "memory"),
        lastLocalRevision = contentRevision("local-1"),
        lastBackupRevision = contentRevision("remote-1"),
        lastOutcome = BackupSyncOutcome.CLEAN,
        updatedAt = instant(1)
    ))
}

private val TEST_LINK = BackupLinkedFile("backup.json", "mem://backup", "memory")

private class MutableBackupRepository(var revision: String) : BackupRepository by FakeBackupRepository(packageWithRevision(revision)) {
    var onEncode: suspend () -> Unit = {}
    var restores = 0
    override suspend fun createPackage() = foundationSuccess(packageWithRevision(revision))
    override suspend fun currentRevision(): BackupRevision {
        val pkg = packageWithRevision(revision)
        return BackupRevision(revision, pkg.createdAt.toBackupInstant(), pkg.summary.toDomain())
    }
    override suspend fun encodePackage(pkg: BackupPackage): FoundationResult<String> {
        onEncode()
        return BackupPackageCodec().encode(pkg)
    }
    override suspend fun restore(pkg: BackupPackage, expectedLocalRevision: String?): FoundationResult<com.jjswigut.oopsallprs.domain.model.BackupRestoreResult> {
        val safety = packageWithRevision(revision)
        if (expectedLocalRevision != null && expectedLocalRevision != BackupSnapshotIdentity.revision(safety)) {
            return foundationFailure(FoundationError.Conflict("Local data changed after the safety backup."))
        }
        restores++
        revision = pkg.lastLocalRevision
        return foundationSuccess(com.jjswigut.oopsallprs.domain.model.BackupRestoreResult(pkg.summary.toDomain(), safety, false))
    }
}

private class ControlledDocuments(revision: String) : BackupDocumentAdapter {
    var content = BackupPackageCodec().encode(packageWithRevision(revision)).successValue()
    var failRead = false
    var failWrite = false
    var failCreate = false
    var writes = 0
    var reads = 0
    val safetyCopies = mutableListOf<String>()
    var onWrite: suspend () -> Unit = {}
    var onCreate: suspend () -> Unit = {}
    override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
        if (failCreate) return foundationFailure(FoundationError.Platform("Safety backup canceled"))
        onCreate()
        safetyCopies += content
        return foundationSuccess(TEST_LINK.copy(providerReference = "mem://safety-${safetyCopies.size}"))
    }
    override suspend fun openBackupDocument() = foundationSuccess(BackupDocument(TEST_LINK, content))
    override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String> {
        reads++
        return if (failRead) foundationFailure(FoundationError.Platform("Provider unavailable")) else foundationSuccess(content)
    }
    override suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile> {
        if (failWrite) return foundationFailure(FoundationError.Platform("Write failed"))
        onWrite()
        writes++
        this.content = content
        return foundationSuccess(linkedFile)
    }
}
