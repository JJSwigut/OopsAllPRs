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
    fun backupSetupFlowAdvancesBacksUpAndDismisses() {
        val holder = ProfileStateHolder()

        holder.startBackupSetup()
        assertEquals(BackupSetupStep.INTRO, holder.state.value.backupSetupStep)

        holder.advanceBackupSetup()
        assertEquals(BackupSetupStep.LOCATION, holder.state.value.backupSetupStep)

        holder.advanceBackupSetup()
        assertEquals(BackupSetupStep.READY, holder.state.value.backupSetupStep)

        holder.backUpBackupSetup()
        assertEquals(BackupSetupStep.LOCATION, holder.state.value.backupSetupStep)

        holder.dismissBackupSetup()
        assertNull(holder.state.value.backupSetupStep)
    }

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
        assertTrue(holder.state.value.backupStatus.isLinked)
        assertEquals("Backup updated", holder.state.value.backupStatus.lastOutcomeLabel)
        assertNull(holder.state.value.backupError)
    }

    @Test
    fun linkBackupFileClosesSetupFlow() = runTest {
        val pkg = packageWithRevision("local-1")
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(FakeBackupRepository(pkg), FakeSyncRepository(), documents)
        )

        holder.startBackupSetup()
        holder.advanceBackupSetup()
        holder.advanceBackupSetup()
        holder.linkBackupFile().successValue()

        assertNull(holder.state.value.backupSetupStep)
        assertTrue(holder.state.value.backupStatus.isLinked)
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
