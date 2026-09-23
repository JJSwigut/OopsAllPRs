package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.backup.BackupSnapshotIdentity
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SqlBackupSnapshotIdentityRoundTripTest {
    @Test
    fun completedCircuitRoundTripPreservesMetadataAndSnapshotIdentity() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        seedBackupExercises(sourceRepos)
        createCompletedMixedWorkout(sourceRepos, asCircuit = true)
        val sourceBackup = SqlBackupRepository(source.database, sourceRepos.store)
        val pkg = sourceBackup.createPackage().successValue()
        val exercises = pkg.completedWorkouts.single().exercises
        assertEquals(2, exercises.size)
        exercises.forEach { exercise ->
            assertNotNull(exercise.groupId)
            assertNotNull(exercise.groupPosition)
            assertEquals("Circuit", exercise.groupLabel)
            assertEquals(3, exercise.groupRounds)
        }

        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        val destinationBackup = SqlBackupRepository(destination.database, destinationRepos.store)
        SqlBackupSyncRepository(destination.database).saveSyncState(oldSyncState()).successValue()
        val beforeRestore = destinationBackup.createPackage().successValue()
        val decoded = destinationBackup.decodePackage(sourceBackup.encodePackage(pkg).successValue()).successValue()

        val result = destinationBackup.restore(decoded).successValue()
        val restored = destinationBackup.createPackage().successValue()

        assertEquals(pkg.completedWorkouts, restored.completedWorkouts)
        assertEquals(BackupSnapshotIdentity.revision(pkg), BackupSnapshotIdentity.revision(restored))
        assertEquals(destinationBackup.currentRevision(), assertNotNull(result.restoredLocalRevision))
        assertEquals(BackupSnapshotIdentity.revision(beforeRestore), BackupSnapshotIdentity.revision(result.safetyBackup))
        assertNull(result.syncWarning)
        assertNull(destination.database.backupQueriesQueries.selectSyncState().executeAsOneOrNull())
    }

    @Test
    fun restoreReportsActualLocalIdentityIncludingRetainedImmutableConfiguration() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        seedBackupExercises(sourceRepos)
        createCompletedMixedWorkout(sourceRepos)
        val sourceBackup = SqlBackupRepository(source.database, sourceRepos.store)
        val pkg = sourceBackup.createPackage().successValue()
        val remoteIdentity = BackupSnapshotIdentity.revision(pkg)

        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        val extra = extraConfiguration()
        assertEquals(extra, destinationRepos.store.saveLoggingConfiguration(extra).successValue())
        val destinationBackup = SqlBackupRepository(destination.database, destinationRepos.store)

        val result = destinationBackup.restore(pkg).successValue()
        val restored = destinationBackup.createPackage().successValue()
        val restoredRevision = assertNotNull(result.restoredLocalRevision)

        assertFalse(pkg.loggingConfigurations.any { it.id == extra.id.value })
        assertTrue(restored.loggingConfigurations.any { it.id == extra.id.value })
        assertEquals(extra, destinationRepos.store.loggingConfiguration(extra.id))
        assertEquals(BackupSnapshotIdentity.revision(restored), restoredRevision.value)
        assertEquals(destinationBackup.currentRevision(), restoredRevision)
        assertNotEquals(remoteIdentity, restoredRevision.value)
        assertEquals(remoteIdentity, BackupSnapshotIdentity.revision(restored.copy(
            loggingConfigurations = restored.loggingConfigurations.filterNot { it.id == extra.id.value }
        )))
        assertTrue(result.safetyBackup.loggingConfigurations.any { it.id == extra.id.value })
    }

    @Test
    fun exportMetadataRoundTripsAndParticipatesInPackageAndCurrentRevision() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        seedBackupExercises(sourceRepos)
        createCompletedMixedWorkout(sourceRepos)
        val sourceBackup = SqlBackupRepository(source.database, sourceRepos.store)
        val beforeExport = sourceBackup.currentRevision()
        val export = sourceRepos.store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()

        val pkg = sourceBackup.createPackage().successValue()
        assertEquals(export.snapshot.id.value, pkg.exportMetadata.single().id)
        assertEquals(export.snapshot.rowCount, pkg.exportMetadata.single().rowCount)
        assertNotEquals(beforeExport.value, pkg.lastLocalRevision)
        assertEquals(BackupSnapshotIdentity.revision(pkg), pkg.lastLocalRevision)
        assertEquals(pkg.lastLocalRevision, sourceBackup.currentRevision().value)
        assertEquals(beforeExport.value, BackupSnapshotIdentity.revision(pkg.copy(exportMetadata = emptyList())))

        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        val destinationBackup = SqlBackupRepository(destination.database, destinationRepos.store)
        val decoded = destinationBackup.decodePackage(sourceBackup.encodePackage(pkg).successValue()).successValue()

        val result = destinationBackup.restore(decoded).successValue()
        val restored = destinationBackup.createPackage().successValue()

        assertEquals(pkg.exportMetadata, restored.exportMetadata)
        assertEquals(pkg.lastLocalRevision, restored.lastLocalRevision)
        assertEquals(restored.lastLocalRevision, destinationBackup.currentRevision().value)
        assertEquals(restored.lastLocalRevision, assertNotNull(result.restoredLocalRevision).value)
        assertEquals(1, destination.database.progressQueriesQueries.selectExportSnapshots().executeAsList().size)
    }

    @Test
    fun failedRestoreRollsBackLedgerExportsAndNewImmutableConfiguration() = runTest {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        seedBackupExercises(sourceRepos)
        val extra = extraConfiguration()
        sourceRepos.store.saveLoggingConfiguration(extra).successValue()
        val incoming = SqlBackupRepository(source.database, sourceRepos.store).createPackage().successValue()

        val destination = SqlFoundationStoreTestHarness()
        val destinationRepos = destination.repositories()
        seedBackupExercises(destinationRepos)
        createCompletedMixedWorkout(destinationRepos, asCircuit = true)
        destinationRepos.store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        val sync = SqlBackupSyncRepository(destination.database)
        val linkedState = oldSyncState()
        sync.saveSyncState(linkedState).successValue()
        val destinationBackup = SqlBackupRepository(
            destination.database,
            destinationRepos.store,
            restoreFaultInjector = { error("Injected restore failure") }
        )
        val beforeRestore = destinationBackup.createPackage().successValue()

        assertIs<FoundationResult.Failure>(destinationBackup.restore(incoming))

        val afterFailure = destinationBackup.createPackage().successValue()
        assertEquals(beforeRestore.lastLocalRevision, afterFailure.lastLocalRevision)
        assertEquals(beforeRestore.completedWorkouts, afterFailure.completedWorkouts)
        assertEquals(beforeRestore.exportMetadata, afterFailure.exportMetadata)
        assertEquals(beforeRestore.loggingConfigurations, afterFailure.loggingConfigurations)
        assertFalse(afterFailure.loggingConfigurations.any { it.id == extra.id.value })
        assertEquals(linkedState, sync.loadSyncState())
    }

    @Test
    fun restoredRevisionCaptureFailureRollsBackDataAndOldSyncLink() = runTest {
        assertRestoredRevisionCaptureFailure(IllegalStateException("Injected restored-revision read failure"))
    }

    @Test
    fun restoredRevisionCaptureCancellationRollsBackDataAndOldSyncLink() = runTest {
        assertRestoredRevisionCaptureFailure(CancellationException("Injected restored-revision cancellation"))
    }

    private suspend fun assertRestoredRevisionCaptureFailure(cause: Throwable) {
        val source = SqlFoundationStoreTestHarness()
        val sourceRepos = source.repositories()
        seedBackupExercises(sourceRepos)
        createCompletedMixedWorkout(sourceRepos, asCircuit = true)
        val incoming = SqlBackupRepository(source.database, sourceRepos.store).createPackage().successValue()

        val driver = BackupQueryHookDriver(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY))
        val destination = SqlFoundationStoreTestHarness(driver)
        val repos = destination.repositories()
        val store = repos.store
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos)
        store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        val sync = SqlBackupSyncRepository(destination.database)
        val oldLink = oldSyncState()
        sync.saveSyncState(oldLink).successValue()
        var restoreStarted = false
        var captureFailed = false
        driver.beforeQuery = { sql ->
            if (restoreStarted && sql.contains("FROM export_snapshots")) {
                captureFailed = true
                throw cause
            }
        }
        val backup = SqlBackupRepository(
            destination.database,
            store,
            restoreFaultInjector = { restoreStarted = true }
        )
        val before = backup.createPackage().successValue()

        if (cause is CancellationException) {
            assertSame(cause, assertFailsWith<CancellationException> { backup.restore(incoming) })
        } else {
            assertIs<FoundationResult.Failure>(backup.restore(incoming))
        }

        assertTrue(restoreStarted)
        assertTrue(captureFailed)
        driver.beforeQuery = null
        assertEquals(oldLink, sync.loadSyncState())
        val actual = SqlBackupRepository(destination.database, store).createPackage().successValue()
        assertEquals(before.completedWorkouts, actual.completedWorkouts)
        assertEquals(before.exportMetadata, actual.exportMetadata)
        assertEquals(before.loggingConfigurations, actual.loggingConfigurations)
        assertEquals(before.lastLocalRevision, actual.lastLocalRevision)
    }

    private fun oldSyncState() = BackupSyncState(
        linkedFile = BackupLinkedFile("old-backup.json", "memory://old-backup", "memory"),
        lastLocalRevision = "old-local",
        lastBackupRevision = "old-remote",
        lastOutcome = BackupSyncOutcome.CLEAN,
        updatedAt = instant(1)
    )

    private fun extraConfiguration() = LoggingConfiguration(
        id = LoggingConfigurationId("destination-distance-duration"),
        schemaVersion = LoggingSchemaVersion(1),
        measures = listOf(
            MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED),
            MeasureSpec(MeasureKind.DURATION, MeasureRequirement.OPTIONAL)
        )
    )
}
