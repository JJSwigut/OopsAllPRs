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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileDestructiveRestoreWarningTest {
    @Test
    fun removingLocalActiveWorkoutShowsWarningEvenWhenRestoredSnapshotHasNoActiveWorkout() = runTest {
        assertRestoreWarning(localWasActive = true)
    }

    @Test
    fun restoringWithoutAnyLocalActiveWorkoutDoesNotShowDestructiveWarning() = runTest {
        assertRestoreWarning(localWasActive = false)
    }

    private suspend fun assertRestoreWarning(localWasActive: Boolean) {
        val empty = packageWithRevision("local-1")
        val local = if (localWasActive) empty.copy(
            activeWorkout = ActiveWorkoutDto(
                id = "local-active",
                startedAt = 1_000,
                routineId = null,
                routineSnapshotName = null,
                exercises = emptyList(),
                createdAt = 1_000,
                updatedAt = 1_000,
                status = "ACTIVE"
            ),
            summary = empty.summary.copy(hasActiveWorkout = true)
        ) else empty
        val incoming = packageWithRevision("remote-2")
        assertNull(incoming.activeWorkout)
        val repository = FakeBackupRepository(local, restoredActiveWorkoutReplaced = localWasActive)
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(incoming).successValue())
        val holder = ProfileStateHolder(
            backupSync = BackupSyncCoordinator(repository, FakeSyncRepository(), documents)
        )
        holder.hydrate()

        val restored = holder.restoreFromFile().successValue()

        assertEquals(localWasActive, restored.activeWorkoutReplaced)
        assertEquals(local.activeWorkout, restored.safetyBackup.activeWorkout)
        assertFalse(restored.restoredSummary.hasActiveWorkout)
        assertNull(repository.createPackage().successValue().activeWorkout)
        assertEquals(1, repository.restoreCount)
        assertEquals(1, documents.createCount)
        assertEquals(0, documents.writeCount)
        assertFalse(holder.state.value.isBackupBusy)
        assertTrue(assertNotNull(holder.state.value.lastRestoreMessage).startsWith("Restored"))
        assertEquals("Safety backup created before restore.", holder.state.value.safetyBackupMessage)
        val expectedWarning = if (localWasActive) {
            "Active workout was removed. It is preserved in your safety backup."
        } else null
        assertEquals(expectedWarning, holder.state.value.restoreWarning)

        holder.hydrate()

        assertEquals(expectedWarning, holder.state.value.restoreWarning)
        assertNotNull(holder.state.value.lastRestoreMessage)
        assertEquals(1, repository.restoreCount)
    }
}
