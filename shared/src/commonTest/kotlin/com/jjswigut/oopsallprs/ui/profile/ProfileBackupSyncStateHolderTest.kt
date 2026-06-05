package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.FakeSyncRepository
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileBackupSyncStateHolderTest {
    @Test
    fun linkBackupFileUpdatesProfileBackupStatus() = runTest {
        val pkg = packageWithRevision("local-1")
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(FakeBackupRepository(pkg), FakeSyncRepository(), documents)
        )

        holder.linkBackupFile().successValue()

        assertFalse(holder.state.value.isBackupBusy)
        assertTrue(holder.state.value.backupStatus.canBackup)
        assertEquals("Backup updated", holder.state.value.backupStatus.lastOutcomeLabel)
        assertNull(holder.state.value.backupError)
    }

    @Test
    fun backupNowRequiresLinkedFile() = runTest {
        val pkg = packageWithRevision("local-1")
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                FakeBackupRepository(pkg),
                FakeSyncRepository(),
                FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
            )
        )

        holder.backupNow()

        assertEquals("Link a backup file first", holder.state.value.backupError)
        assertFalse(holder.state.value.isBackupBusy)
    }
}
