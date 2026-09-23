package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BackupSnapshotSyncIdentityTest {
    @Test
    fun automaticBackupCapturesPreferenceChangeWithUnchangedWireRevision() = runTest {
        val repo = MutableSnapshotRepository(packageWithRevision("legacy-1"))
        val docs = FakeDocumentAdapter("")
        val sync = FakeSyncRepository()
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        val linked = coordinator.linkNewBackup().successValue()
        repo.pkg = repo.pkg.copy(preferences = repo.pkg.preferences.copy(weightUnit = "KILOGRAMS"))

        val result = coordinator.syncNow().successValue()

        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, result.lastOutcome)
        assertEquals(1, docs.writeCount)
        assertEquals("KILOGRAMS", BackupPackageCodec().decode(docs.content).successValue().preferences.weightUnit)
        assertNotEquals(linked.lastLocalRevision, result.lastLocalRevision)
        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
    }

    @Test
    fun remotePreferenceChangeCannotHideBehindUnchangedWireRevision() = runTest {
        val repo = MutableSnapshotRepository(packageWithRevision("legacy-1"))
        val docs = FakeDocumentAdapter("")
        val sync = FakeSyncRepository()
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        val baseline = coordinator.linkNewBackup().successValue()
        val changed = repo.pkg.copy(preferences = repo.pkg.preferences.copy(weightUnit = "KILOGRAMS"))
        docs.content = BackupPackageCodec().encode(changed).successValue()
        val remote = docs.content

        val result = coordinator.syncNow().successValue()
        coordinator.backupNow().successValue()

        assertEquals(BackupSyncOutcome.BACKUP_CHANGED, result.lastOutcome)
        assertEquals(baseline.lastBackupRevision, result.lastBackupRevision)
        assertEquals(remote, docs.content)
        assertEquals(0, docs.writeCount)
    }

    @Test
    fun envelopeOnlyChangesDoNotCreateConflictOrAutomaticWrite() = runTest {
        val repo = MutableSnapshotRepository(packageWithRevision("legacy-1"))
        val docs = FakeDocumentAdapter("")
        val coordinator = BackupSyncCoordinator(repo, FakeSyncRepository(), docs)
        coordinator.linkNewBackup().successValue()
        docs.content = BackupPackageCodec().encode(repo.pkg.copy(
            createdAt = 100L,
            deviceId = "another-device",
            lastLocalRevision = "other-producer-revision"
        )).successValue()
        repo.pkg = repo.pkg.copy(createdAt = 200L)

        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
        assertEquals(0, docs.writeCount)
    }

    @Test
    fun legacyBaselineRequiresExplicitChoiceEvenWhenRevisionLabelsMatch() = runTest {
        val repo = MutableSnapshotRepository(packageWithRevision("legacy-1"))
        val docs = FakeDocumentAdapter("")
        val sync = FakeSyncRepository()
        val coordinator = BackupSyncCoordinator(repo, sync, docs)
        val state = coordinator.linkNewBackup().successValue()
        sync.saveSyncState(state.copy(lastLocalRevision = "legacy-1", lastBackupRevision = "legacy-1"))

        repeat(2) {
            val pending = BackupSyncCoordinator(repo, sync, docs).syncNow().successValue()
            assertEquals(BackupSyncOutcome.CONFLICT, pending.lastOutcome)
            assertEquals("legacy-1", pending.lastBackupRevision)
            assertTrue(pending.lastConflictSummary.orEmpty().contains("comparison was upgraded"))
            coordinator.resolveConflict(BackupConflictDecision.CANCEL).successValue()
        }
        assertEquals(0, docs.writeCount)
        coordinator.resolveConflict(BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP).successValue()
        assertEquals(1, docs.writeCount)
        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
    }
}

private class MutableSnapshotRepository(var pkg: BackupPackage) : BackupRepository by FakeBackupRepository(pkg) {
    override suspend fun createPackage() = foundationSuccess(pkg)
    override suspend fun currentRevision() = BackupRevision(
        pkg.lastLocalRevision, pkg.createdAt.toBackupInstant(), pkg.summary.toDomain()
    )

    override suspend fun restore(pkg: BackupPackage, expectedLocalRevision: String?): FoundationResult<BackupRestoreResult> {
        val result = FakeBackupRepository(this.pkg).restore(pkg, expectedLocalRevision)
        if (result is FoundationResult.Success) this.pkg = pkg
        return result
    }
}
