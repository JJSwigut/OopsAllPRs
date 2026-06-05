package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.FakeSyncRepository
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.profile.ProfileStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AppShellBackupSyncTest {
    @Test
    fun launchResumeCheckSkipsUnlinkedBackupWithoutFileAccess() = runTest {
        val pkg = packageWithRevision("local-1")
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(FakeBackupRepository(pkg), FakeSyncRepository(), documents)
        )

        holder.checkLinkedBackup().successValue()

        assertEquals(0, documents.writeCount)
        assertEquals(0, documents.createCount)
        assertEquals("No backup linked", holder.state.value.backupStatus.lastOutcomeLabel)
    }

    @Test
    fun launchResumeCheckRefreshesLinkedBackupStatus() = runTest {
        val pkg = packageWithRevision("local-1")
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = FakeBackupRepository(pkg),
                syncRepository = FakeSyncRepository(
                    BackupSyncState(
                        linkedFile = BackupLinkedFile("backup.json", "mem://backup", "memory"),
                        lastBackupRevision = "local-1",
                        lastLocalRevision = "local-1",
                        lastOutcome = BackupSyncOutcome.CLEAN,
                        updatedAt = instant(1)
                    )
                ),
                documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
            )
        )

        holder.checkLinkedBackup().successValue()

        assertEquals("Up to date", holder.state.value.backupStatus.lastOutcomeLabel)
    }
}
