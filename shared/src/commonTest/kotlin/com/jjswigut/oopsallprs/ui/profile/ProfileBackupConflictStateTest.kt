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
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProfileBackupConflictStateTest {
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
