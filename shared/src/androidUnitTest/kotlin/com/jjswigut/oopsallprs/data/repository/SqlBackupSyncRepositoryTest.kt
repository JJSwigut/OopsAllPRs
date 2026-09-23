package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSnapshotIdentity
import com.jjswigut.oopsallprs.data.backup.FakeBackupRepository
import com.jjswigut.oopsallprs.data.backup.FakeDocumentAdapter
import com.jjswigut.oopsallprs.data.backup.packageWithRevision
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SqlBackupSyncRepositoryTest {
    @Test
    fun legacyV1RestoreEstablishesStableIndependentBaselines() = runTest {
        val content = checkNotNull(javaClass.getResourceAsStream("/backup/v1-released-golden.json"))
            .bufferedReader().use { it.readText() }
        val incoming = BackupPackageCodec().decode(content).successValue()
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val backup = SqlBackupRepository(harness.database, repos.store)
        val linked = BackupLinkedFile("backup.json", "mem://backup", "memory")
        val sync = SqlBackupSyncRepository(harness.database)
        sync.saveSyncState(BackupSyncState(
            linkedFile = linked,
            lastLocalRevision = "legacy-local",
            lastBackupRevision = incoming.lastLocalRevision,
            lastOutcome = BackupSyncOutcome.CLEAN,
            updatedAt = instant(1)
        )).successValue()
        val docs = FakeDocumentAdapter(content)
        var safetyCopies = 0
        val picker = object : BackupDocumentAdapter by docs {
            override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
                safetyCopies++
                return foundationSuccess(linked.copy(providerReference = "mem://safety"))
            }
        }
        val coordinator = BackupSyncCoordinator(backup, sync, picker)
        assertEquals(BackupSyncOutcome.CONFLICT, coordinator.syncNow().successValue().lastOutcome)

        val restored = coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY).successValue()

        assertEquals(1, safetyCopies)
        assertEquals(BackupSnapshotIdentity.revision(incoming), restored.lastBackupRevision)
        assertEquals(backup.currentRevision().value, restored.lastLocalRevision)
        val recreated = BackupSyncCoordinator(backup, SqlBackupSyncRepository(harness.database), picker)
        assertEquals(BackupSyncOutcome.CLEAN, recreated.syncNow().successValue().lastOutcome)
        assertEquals(content, docs.content)
        assertEquals(0, docs.writeCount)
    }

    @Test
    fun automaticBackupCapturesSettingsWithoutWorkoutChanges() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        val backup = SqlBackupRepository(harness.database, repos.store)
        val docs = FakeDocumentAdapter("")
        val coordinator = BackupSyncCoordinator(backup, SqlBackupSyncRepository(harness.database), docs)
        coordinator.linkNewBackup().successValue()
        repos.store.setRestTimerSurfaceEnabled(true).successValue()

        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, coordinator.syncNow().successValue().lastOutcome)
        assertEquals(true, BackupPackageCodec().decode(docs.content).successValue().preferences.restTimerSurfaceEnabled)
        assertEquals(BackupSyncOutcome.CLEAN, coordinator.syncNow().successValue().lastOutcome)
        assertEquals(1, docs.writeCount)
    }

    @Test
    fun preferenceOnlyEditDuringSafetyExportStopsRestoreWithoutChangingWorkoutTimestamps() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val repos = harness.repositories()
        repos.store.setWeightUnit(WeightUnit.POUNDS).successValue()
        val backup = SqlBackupRepository(harness.database, repos.store)
        val initial = backup.createPackage().successValue()
        val linked = BackupLinkedFile("backup.json", "mem://backup", "memory")
        val sync = SqlBackupSyncRepository(harness.database)
        sync.saveSyncState(BackupSyncState(
            linkedFile = linked,
            lastLocalRevision = initial.lastLocalRevision,
            lastBackupRevision = initial.lastLocalRevision,
            lastOutcome = BackupSyncOutcome.CLEAN,
            updatedAt = instant(1)
        )).successValue()
        val remote = BackupPackageCodec().encode(initial.copy(
            preferences = initial.preferences.copy(defaultRestSeconds = initial.preferences.defaultRestSeconds + 1)
        )).successValue()
        val docs = FakeDocumentAdapter(remote)
        var safetyContent: String? = null
        val picker = object : BackupDocumentAdapter by docs {
            override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
                safetyContent = content
                repos.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
                return foundationSuccess(linked.copy(providerReference = "mem://safety"))
            }
        }
        val coordinator = BackupSyncCoordinator(backup, sync, picker)
        coordinator.syncNow().successValue()

        val result = coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(WeightUnit.KILOGRAMS, repos.store.weightUnit())
        assertNotEquals(initial.lastLocalRevision, backup.currentRevision().value)
        assertEquals(initial.summary, backup.createPackage().successValue().summary)
        assertEquals("POUNDS", BackupPackageCodec().decode(requireNotNull(safetyContent)).successValue().preferences.weightUnit)
        assertEquals(BackupSyncOutcome.BACKUP_CHANGED, sync.loadSyncState().lastOutcome)
        assertEquals(initial.lastLocalRevision, sync.loadSyncState().lastBackupRevision)
        assertEquals(remote, docs.content)
    }

    @Test
    fun manualBackupCapturesPreferenceOnlyChangeAndRestoresIt() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        sourceRepos.store.setWeightUnit(WeightUnit.POUNDS).successValue()
        val backup = SqlBackupRepository(source.database, sourceRepos.store)
        val initial = backup.createPackage().successValue()
        val docs = FakeDocumentAdapter(BackupPackageCodec().encode(initial).successValue())
        val sync = SqlBackupSyncRepository(source.database)
        val coordinator = BackupSyncCoordinator(backup, sync, docs)
        coordinator.linkNewBackup().successValue()

        sourceRepos.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
        val result = coordinator.backupNow().successValue()

        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, result.lastOutcome)
        assertEquals(1, docs.writeCount)
        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        SqlBackupRepository(destination.database, destinationRepos.store)
            .restore(BackupPackageCodec().decode(docs.content).successValue()).successValue()
        assertEquals(WeightUnit.KILOGRAMS, destinationRepos.store.weightUnit())
    }

    @Test
    fun unresolvedRemoteChangeSurvivesSqlRepositoryRecreationAndLaterLocalEdit() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        val sync = SqlBackupSyncRepository(harness.database)
        sync.saveSyncState(BackupSyncState(
            linkedFile = BackupLinkedFile("backup.json", "mem://backup", "memory"),
            lastLocalRevision = "local-1",
            lastBackupRevision = "backup-1",
            lastOutcome = BackupSyncOutcome.CLEAN,
            updatedAt = instant(1)
        )).successValue()
        val remote = BackupPackageCodec().encode(packageWithRevision("backup-2")).successValue()
        val docs = FakeDocumentAdapter(remote)
        BackupSyncCoordinator(FakeBackupRepository(packageWithRevision("local-1")), sync, docs).syncNow().successValue()

        val recreated = SqlBackupSyncRepository(harness.database)
        val result = BackupSyncCoordinator(FakeBackupRepository(packageWithRevision("local-2")), recreated, docs)
            .syncNow().successValue()

        assertEquals(BackupSyncOutcome.CONFLICT, result.lastOutcome)
        assertEquals("local-1", recreated.loadSyncState().lastLocalRevision)
        assertEquals("backup-1", recreated.loadSyncState().lastBackupRevision)
        assertEquals(remote, docs.content)
        assertEquals(0, docs.writeCount)
    }

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
