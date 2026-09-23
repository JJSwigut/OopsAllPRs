package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupRestoreRevisionGuardTest {
    @Test
    fun fileRestoreRejectsLocalEditAfterSafetyCheckBeforeReplacement() = runTest {
        assertLateEditRejected(fromConflict = false)
    }

    @Test
    fun conflictRestoreRejectsLocalEditAfterSafetyCheckWithoutAcknowledgingOrReplaying() = runTest {
        assertLateEditRejected(fromConflict = true)
    }

    @Test
    fun conflictTypeSurvivesFailureToSaveConflictStatus() = runTest {
        assertLateEditRejected(fromConflict = true, failStatusSave = true)
    }

    @Test
    fun bothRestorePathsPassCanonicalIdentityOfExportedSnapshotOnSuccess() = runTest {
        for (fromConflict in listOf(false, true)) {
            val local = packageWithRevision("local-2")
            val remote = packageWithRevision("remote-2")
            val repository = RestoreBoundaryRepository(local)
            repository.allowReplacement.complete(Unit)
            val documents = RestoreGuardDocuments(remote)
            val sync = RestoreGuardSyncRepository(BackupSyncState(
                linkedFile = RESTORE_GUARD_LINK,
                lastOutcome = BackupSyncOutcome.CONFLICT,
                updatedAt = instant(1)
            ))
            val coordinator = BackupSyncCoordinator(repository, sync, documents)

            if (fromConflict) {
                coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY).successValue()
            } else {
                coordinator.restoreFromFile().successValue()
            }

            val exported = BackupPackageCodec().decode(documents.safetyCopies.single()).successValue()
            assertEquals(local, exported)
            assertEquals<List<String?>>(listOf(BackupSnapshotIdentity.revision(exported)), repository.expectedRevisions)
            assertNotEquals<List<String?>>(listOf(local.lastLocalRevision), repository.expectedRevisions)
            assertEquals(remote, repository.local)
            assertEquals(1, repository.restoreAttempts)
            assertEquals(1, repository.localReplacements)
            assertEquals(BackupSyncOutcome.RESTORED, sync.loadSyncState().lastOutcome)
        }
    }

    @Test
    fun fakeRepositoryRejectsStaleCanonicalIdentityWithoutReplacingData() = runTest {
        val original = packageWithRevision("local-2")
        val changed = original.copy(preferences = original.preferences.copy(weightUnit = "KILOGRAMS"))
        val repository = FakeBackupRepository(changed)

        val result = repository.restore(packageWithRevision("remote-2"), BackupSnapshotIdentity.revision(original))

        assertIs<FoundationError.Conflict>(assertIs<FoundationResult.Failure>(result).error)
        assertEquals(0, repository.restoreCount)
        assertEquals(changed, repository.createPackage().successValue())
    }

    @Test
    fun fakeRepositoryAcceptsMatchingCanonicalIdentityAndLegacyOmittedGuard() = runTest {
        val original = packageWithRevision("local-2")
        val remote = packageWithRevision("remote-2")
        val guarded = FakeBackupRepository(original)
        guarded.restore(remote, BackupSnapshotIdentity.revision(original)).successValue()
        assertEquals(remote, guarded.createPackage().successValue())

        val legacy = FakeBackupRepository(original)
        legacy.restore(remote).successValue()
        assertEquals(remote, legacy.createPackage().successValue())
    }

    private suspend fun assertLateEditRejected(fromConflict: Boolean, failStatusSave: Boolean = false): Unit = coroutineScope {
        val safetySnapshot = packageWithRevision("local-2")
        val remote = packageWithRevision("remote-2")
        val repository = RestoreBoundaryRepository(safetySnapshot)
        val documents = RestoreGuardDocuments(remote)
        val initialSync = BackupSyncState(
            linkedFile = RESTORE_GUARD_LINK,
            lastLocalRevision = contentRevision("local-1"),
            lastBackupRevision = contentRevision("remote-1"),
            lastOutcome = if (fromConflict) BackupSyncOutcome.CONFLICT else BackupSyncOutcome.CLEAN,
            updatedAt = instant(1)
        )
        val sync = RestoreGuardSyncRepository(initialSync)
        sync.failSaves = failStatusSave
        val coordinator = BackupSyncCoordinator(repository, sync, documents)
        val restoring = async {
            if (fromConflict) {
                coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY)
            } else {
                coordinator.restoreFromFile()
            }
        }

        repository.restoreEntered.await()
        val lateEdit = safetySnapshot.copy(
            preferences = safetySnapshot.preferences.copy(weightUnit = "KILOGRAMS")
        )
        try {
            // Both snapshots have been captured; the edit occurs after the post-export check.
            assertEquals(listOf(safetySnapshot, safetySnapshot), repository.capturedSnapshots)
            val exported = BackupPackageCodec().decode(documents.safetyCopies.single()).successValue()
            assertEquals(safetySnapshot, exported)
            assertEquals<List<String?>>(listOf(BackupSnapshotIdentity.revision(exported)), repository.expectedRevisions)
            assertEquals(safetySnapshot.lastLocalRevision, lateEdit.lastLocalRevision)
            assertNotEquals(BackupSnapshotIdentity.revision(exported), BackupSnapshotIdentity.revision(lateEdit))
            repository.local = lateEdit
        } finally {
            repository.allowReplacement.complete(Unit)
        }

        val failure = assertIs<FoundationResult.Failure>(restoring.await())
        assertIs<FoundationError.Conflict>(failure.error)
        assertEquals(lateEdit, repository.local, "The edit not present in the safety copy must survive")
        assertEquals(0, repository.localReplacements)
        assertEquals(1, repository.restoreAttempts, "A conflict must not be retried with a newer revision")
        assertEquals(1, documents.safetyCopies.size)
        assertEquals(0, documents.remoteWrites)
        assertEquals(remote, BackupPackageCodec().decode(documents.remoteContent).successValue())
        val state = sync.loadSyncState()
        assertEquals(initialSync.linkedFile, state.linkedFile)
        assertEquals(initialSync.lastLocalRevision, state.lastLocalRevision)
        assertEquals(initialSync.lastBackupRevision, state.lastBackupRevision)
        assertTrue(sync.savedStates.none { it.lastOutcome == BackupSyncOutcome.RESTORED })
        if (fromConflict) assertEquals(BackupSyncOutcome.CONFLICT, state.lastOutcome)
    }
}

private val RESTORE_GUARD_LINK = BackupLinkedFile("backup.json", "mem://restore-guard", "memory")

private class RestoreBoundaryRepository(var local: BackupPackage) : BackupRepository by FakeBackupRepository(local) {
    val restoreEntered = CompletableDeferred<Unit>()
    val allowReplacement = CompletableDeferred<Unit>()
    val capturedSnapshots = mutableListOf<BackupPackage>()
    val expectedRevisions = mutableListOf<String?>()
    var restoreAttempts = 0
    var localReplacements = 0

    override suspend fun createPackage(): FoundationResult<BackupPackage> {
        capturedSnapshots += local
        return foundationSuccess(local)
    }

    override suspend fun restore(pkg: BackupPackage, expectedLocalRevision: String?): FoundationResult<BackupRestoreResult> {
        restoreAttempts += 1
        expectedRevisions += expectedLocalRevision
        restoreEntered.complete(Unit)
        allowReplacement.await()
        if (expectedLocalRevision != null && expectedLocalRevision != BackupSnapshotIdentity.revision(local)) {
            return foundationFailure(FoundationError.Conflict("Local data changed after the safety backup."))
        }
        val replaced = local
        local = pkg
        localReplacements += 1
        return foundationSuccess(BackupRestoreResult(pkg.summary.toDomain(), replaced, false))
    }
}

private class RestoreGuardDocuments(remote: BackupPackage) : BackupDocumentAdapter {
    var remoteContent = BackupPackageCodec().encode(remote).successValue()
    val safetyCopies = mutableListOf<String>()
    var remoteWrites = 0

    override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
        safetyCopies += content
        return foundationSuccess(RESTORE_GUARD_LINK.copy(providerReference = "mem://restore-safety"))
    }

    override suspend fun openBackupDocument() = foundationSuccess(BackupDocument(RESTORE_GUARD_LINK, remoteContent))

    override suspend fun readBackup(linkedFile: BackupLinkedFile) = foundationSuccess(remoteContent)

    override suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile> {
        remoteWrites += 1
        remoteContent = content
        return foundationSuccess(linkedFile)
    }
}

private class RestoreGuardSyncRepository(
    initial: BackupSyncState,
    private val delegate: FakeSyncRepository = FakeSyncRepository(initial)
) : BackupSyncRepository by delegate {
    val savedStates = mutableListOf<BackupSyncState>()
    var failSaves = false

    override suspend fun saveSyncState(state: BackupSyncState): FoundationResult<BackupSyncState> {
        savedStates += state
        if (failSaves) return foundationFailure(FoundationError.Persistence("Sync status save failed"))
        return delegate.saveSyncState(state)
    }
}
