package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SqlBackupSyncRepositoryTest {
    @Test
    fun syncStateSurvivesRepositoryRecreation() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repo = SqlBackupSyncRepository(harness.database)
        val state = BackupSyncState(
            linkedFile = BackupLinkedFile("backup.json", "content://backup", "android-uri"),
            lastBackupRevision = "backup-1",
            lastBackupTimestamp = instant(2_000),
            lastLocalRevision = "local-1",
            lastLocalTimestamp = instant(1_500),
            lastOutcome = BackupSyncOutcome.LOCAL_WRITTEN,
            lastError = null,
            lastConflictSummary = "1 workouts, 2 sets, 0 routines, 0 custom exercises",
            updatedAt = instant(2_100)
        )

        repo.saveSyncState(state).successValue()
        val restored = SqlBackupSyncRepository(harness.database).loadSyncState()

        assertEquals("backup.json", restored.linkedFile?.displayName)
        assertEquals("backup-1", restored.lastBackupRevision)
        assertEquals("local-1", restored.lastLocalRevision)
        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, restored.lastOutcome)
        assertEquals("1 workouts, 2 sets, 0 routines, 0 custom exercises", restored.lastConflictSummary)
    }

    @Test
    fun clearSyncStateReturnsUnlinkedDefault() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repo = SqlBackupSyncRepository(harness.database)
        repo.saveSyncState(
            BackupSyncState(
                linkedFile = BackupLinkedFile("backup.json", "content://backup", "android-uri"),
                lastOutcome = BackupSyncOutcome.CLEAN,
                updatedAt = instant(1_000)
            )
        ).successValue()

        repo.clearSyncState().successValue()

        val restored = repo.loadSyncState()
        assertNull(restored.linkedFile)
        assertEquals(BackupSyncOutcome.UNLINKED, restored.lastOutcome)
    }
}
