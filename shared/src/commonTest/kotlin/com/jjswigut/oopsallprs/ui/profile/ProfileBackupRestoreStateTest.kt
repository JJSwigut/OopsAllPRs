package com.jjswigut.oopsallprs.ui.profile

import com.jjswigut.oopsallprs.data.backup.ActiveWorkoutDto
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
import kotlin.test.assertNotNull

class ProfileBackupRestoreStateTest {
    @Test
    fun restoreFromFileExportsSafetyBackupAndReportsActiveWorkoutReplacement() = runTest {
        val empty = packageWithRevision("local-1")
        val pkg = empty.copy(
            activeWorkout = ActiveWorkoutDto(
                id = "restored-active",
                startedAt = 1_000,
                routineId = null,
                routineSnapshotName = null,
                exercises = emptyList(),
                createdAt = 1_000,
                updatedAt = 1_000,
                status = "ACTIVE"
            ),
            summary = empty.summary.copy(hasActiveWorkout = true)
        )
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(
                backupRepository = FakeBackupRepository(pkg, restoredActiveWorkoutReplaced = true),
                syncRepository = FakeSyncRepository(),
                documents = documents
            )
        )

        holder.restoreFromFile().successValue()

        assertEquals(1, documents.createCount)
        assertEquals("Active workout was replaced by the backup.", holder.state.value.restoreWarning)
        assertEquals("Safety backup created before restore.", holder.state.value.safetyBackupMessage)
        assertNotNull(holder.state.value.lastRestoreMessage)
    }
}
