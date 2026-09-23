package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.jjswigut.oopsallprs.data.backup.BackupSnapshotIdentity
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import java.nio.file.Files
import java.util.Properties
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlBackupTransactionalSnapshotTest {
    @Test
    fun restoringWithoutIncomingActiveWarnsAndPreservesRemovedWorkoutInSafetyCopy() = runTest {
        val harness = SqlFoundationStoreTestHarness()
        try {
            val repos = harness.repositories()
            seedBackupExercises(repos)
            val backup = SqlBackupRepository(harness.database, repos.store)
            val incoming = backup.createPackage().successValue()
            assertNull(incoming.activeWorkout)
            val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
            val exercise = repos.setLogging.addExercise(
                workout.id, ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
                instant(1_100)
            ).successValue()
            repos.setLogging.confirmSet(
                workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)
            ).successValue()
            val before = backup.createPackage().successValue()

            val plan = backup.restorePlan(incoming).successValue()

            assertTrue(plan.requiresActiveWorkoutWarning)
            assertTrue(plan.warnings.contains("Current active workout will be removed."))
            assertEquals(before.lastLocalRevision, backup.currentRevision().value)

            val result = backup.restore(incoming, expectedLocalRevision = before.lastLocalRevision).successValue()

            assertTrue(result.activeWorkoutReplaced)
            assertEquals(before.lastLocalRevision, result.safetyBackup.lastLocalRevision)
            assertEquals(assertNotNull(before.activeWorkout), result.safetyBackup.activeWorkout)
            assertEquals(before.activeSession, result.safetyBackup.activeSession)
            assertFalse(result.restoredSummary.hasActiveWorkout)
            assertNull(backup.createPackage().successValue().activeWorkout)
        } finally {
            harness.driver.close()
        }
    }

    @Test
    fun emptyLocalWorkoutDoesNotWarnOrReportReplacementRegardlessOfIncomingActive() = runTest {
        for (hasIncomingActive in listOf(false, true)) {
            val source = SqlFoundationStoreTestHarness()
            val destination = SqlFoundationStoreTestHarness()
            try {
                val sourceRepos = source.repositories()
                if (hasIncomingActive) sourceRepos.lifecycle.startEmpty(instant(1_000)).successValue()
                val incoming = SqlBackupRepository(source.database, sourceRepos.store).createPackage().successValue()
                val repos = destination.repositories()
                val backup = SqlBackupRepository(destination.database, repos.store)
                val before = backup.createPackage().successValue()

                val plan = backup.restorePlan(incoming).successValue()

                assertFalse(plan.requiresActiveWorkoutWarning)
                assertFalse(plan.warnings.any { it.startsWith("Current active workout") })

                val result = backup.restore(incoming, expectedLocalRevision = before.lastLocalRevision).successValue()

                assertFalse(result.activeWorkoutReplaced)
                assertNull(result.safetyBackup.activeWorkout)
                assertEquals(before.lastLocalRevision, result.safetyBackup.lastLocalRevision)
                assertEquals(hasIncomingActive, result.restoredSummary.hasActiveWorkout)
            } finally {
                source.driver.close()
                destination.driver.close()
            }
        }
    }

    @Test
    fun createPackageKeepsPreferencesAndExportsInOneWalSnapshot() = runTest {
        assertCoherentPreferencesAndExports { it.createPackage().successValue().lastLocalRevision }
    }

    @Test
    fun currentRevisionIdentifiesOneWalSnapshotRatherThanTornRows() = runTest {
        assertCoherentPreferencesAndExports { it.currentRevision().value }
    }

    private suspend fun assertCoherentPreferencesAndExports(readRevision: suspend (SqlBackupRepository) -> String) {
        WalBackupFixture().use { fixture ->
            val repos = fixture.harness.repositories()
            repos.store.setWeightUnit(WeightUnit.POUNDS).successValue()
            val backup = SqlBackupRepository(fixture.database, repos.store)
            val before = backup.createPackage().successValue()
            var writerCommitted = false
            fixture.driver.afterQuery = { sql ->
                if (!writerCommitted && sql.contains("FROM user_preferences")) {
                    fixture.writerDatabase.transaction {
                        fixture.writer.execute(null,
                            "UPDATE user_preferences SET weight_unit = 'KILOGRAMS', rest_sound_enabled = 0", 0, null).value
                        fixture.writerDatabase.progressQueriesQueries.insertExportSnapshot(
                            "interleaved-export", "WORKOUTS", 42L, "KILOGRAMS", 0L, 1L
                        )
                    }
                    writerCommitted = true
                }
            }

            val observedRevision = readRevision(backup)

            assertTrue(writerCommitted, "The second connection must commit between snapshot queries")
            fixture.driver.afterQuery = null
            val after = backup.createPackage().successValue()
            assertNotEquals(before.lastLocalRevision, after.lastLocalRevision)
            assertEquals("KILOGRAMS", after.preferences.weightUnit)
            assertFalse(after.preferences.restSoundEnabled)
            assertEquals("interleaved-export", after.exportMetadata.single().id)
            assertEquals(before.lastLocalRevision, observedRevision,
                "The snapshot must retain the state established before the interleaved commit")
        }
    }

    @Test
    fun currentSummaryCannotCountTheSameWorkoutAsBothActiveAndCompleted() = runTest {
        WalBackupFixture().use { fixture ->
            val repos = fixture.harness.repositories()
            seedBackupExercises(repos)
            val workout = repos.lifecycle.startEmpty(instant(1_000)).successValue()
            val exercise = repos.setLogging.addExercise(
                workout.id, ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
                instant(1_100)
            ).successValue()
            repos.setLogging.confirmSet(
                workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)
            ).successValue()
            val backup = SqlBackupRepository(fixture.database, repos.store)
            val before = backup.currentSummary()
            assertTrue(before.hasActiveWorkout)
            assertEquals(0, before.workoutCount)
            assertEquals(1, before.setCount)
            var writerCommitted = false
            fixture.driver.afterQuery = { sql ->
                if (!writerCommitted && sql.contains("FROM active_workouts") && sql.contains("'ACTIVE'")) {
                    fixture.writerDatabase.transaction {
                        fixture.writerDatabase.workoutQueriesQueries.updateWorkoutStatus("COMPLETED", 2_000L, workout.id.value)
                        fixture.writerDatabase.routineQueriesQueries.insertCompletedWorkout(
                            "completed-interleaved", workout.id.value, 1_000L, 2_000L, 1_000L, null, 2_000L
                        )
                        fixture.writerDatabase.workoutQueriesQueries.clearSessionState()
                    }
                    writerCommitted = true
                }
            }

            val observed = backup.currentSummary()

            assertTrue(writerCommitted)
            fixture.driver.afterQuery = null
            val after = backup.currentSummary()
            assertFalse(after.hasActiveWorkout)
            assertEquals(1, after.workoutCount)
            assertEquals(1, after.setCount)
            assertEquals(before, observed)
        }
    }

    @Test
    fun staleExpectedRevisionPreservesLocalEditsAndOldSyncLink() = runTest {
        WalBackupFixture().use { fixture ->
            val repos = fixture.harness.repositories()
            val backup = SqlBackupRepository(fixture.database, repos.store)
            val safety = backup.createPackage().successValue()
            repos.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
            repos.lifecycle.startEmpty(instant(1_000)).successValue()
            val sync = SqlBackupSyncRepository(fixture.database)
            val oldLink = linkedState()
            sync.saveSyncState(oldLink).successValue()
            val edited = backup.createPackage().successValue()
            val repository: BackupRepository = backup

            val result = repository.restore(safety, expectedLocalRevision = safety.lastLocalRevision)

            assertIs<FoundationError.Conflict>(assertIs<FoundationResult.Failure>(result).error)
            assertEquals(edited.lastLocalRevision, backup.currentRevision().value)
            assertEquals(edited.activeWorkout, backup.createPackage().successValue().activeWorkout)
            assertEquals(oldLink, sync.loadSyncState())
        }
    }

    @Test
    fun editCommittedDuringGuardSnapshotCannotBeOverwrittenByRestore() = runTest {
        WalBackupFixture().use { fixture ->
            val repos = fixture.harness.repositories()
            repos.store.setWeightUnit(WeightUnit.POUNDS).successValue()
            val backup = SqlBackupRepository(fixture.database, repos.store)
            val safety = backup.createPackage().successValue()
            val sync = SqlBackupSyncRepository(fixture.database)
            val oldLink = linkedState()
            sync.saveSyncState(oldLink).successValue()
            var writerCommitted = false
            fixture.driver.afterQuery = { sql ->
                if (!writerCommitted && sql.contains("FROM user_preferences")) {
                    fixture.writer.execute(null, "UPDATE user_preferences SET weight_unit = 'KILOGRAMS'", 0, null).value
                    writerCommitted = true
                }
            }
            val repository: BackupRepository = backup

            val result = repository.restore(safety, expectedLocalRevision = safety.lastLocalRevision)

            assertTrue(writerCommitted)
            fixture.driver.afterQuery = null
            // A WAL read transaction cannot upgrade over another writer's commit. Either conflict
            // detection or SQLite's write-upgrade failure must leave that edit and the link intact.
            assertIs<FoundationResult.Failure>(result)
            assertEquals(WeightUnit.KILOGRAMS, repos.store.weightUnit())
            assertEquals(oldLink, sync.loadSyncState())
        }
    }

    @Test
    fun restoredRevisionExcludesAnEditCommittedImmediatelyAfterRestoreCommit() = runTest {
        val source = SqlFoundationStoreTestHarness()
        try {
            val sourceRepos = source.repositories()
            seedBackupExercises(sourceRepos)
            createCompletedMixedWorkout(sourceRepos, asCircuit = true)
            sourceRepos.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
            val incoming = SqlBackupRepository(source.database, sourceRepos.store).createPackage().successValue()
            WalBackupFixture().use { fixture ->
                val repos = fixture.harness.repositories()
                repos.store.setWeightUnit(WeightUnit.POUNDS).successValue()
                val sync = SqlBackupSyncRepository(fixture.database)
                sync.saveSyncState(linkedState()).successValue()
                val before = SqlBackupRepository(fixture.database, repos.store).createPackage().successValue()
                var postCommitEditApplied = false
                val backup = SqlBackupRepository(fixture.database, repos.store, restoreFaultInjector = {
                    fixture.database.transaction {
                        afterCommit {
                            fixture.writer.execute(null, "UPDATE user_preferences SET weight_unit = 'POUNDS'", 0, null).value
                            postCommitEditApplied = true
                        }
                    }
                })
                val repository: BackupRepository = backup

                val result = repository.restore(incoming, expectedLocalRevision = before.lastLocalRevision).successValue()

                assertTrue(postCommitEditApplied)
                val actual = backup.createPackage().successValue()
                assertEquals("POUNDS", actual.preferences.weightUnit)
                val restoredRevision = assertNotNull(result.restoredLocalRevision)
                assertEquals(BackupSnapshotIdentity.revision(incoming), restoredRevision.value)
                assertNotEquals(actual.lastLocalRevision, restoredRevision.value,
                    "An edit after commit must remain dirty relative to the exact restored snapshot")
                assertEquals(before.lastLocalRevision, result.safetyBackup.lastLocalRevision)
                assertEquals(incoming.completedWorkouts, actual.completedWorkouts)
                assertNull(result.syncWarning)
                assertNull(fixture.database.backupQueriesQueries.selectSyncState().executeAsOneOrNull())
            }
        } finally {
            source.driver.close()
        }
    }

    private fun linkedState() = BackupSyncState(
        linkedFile = BackupLinkedFile("old.json", "memory://old", "memory"),
        lastLocalRevision = "old-local",
        lastBackupRevision = "old-remote",
        lastOutcome = BackupSyncOutcome.CLEAN,
        updatedAt = instant(1)
    )
}

// Hooks surround real, fully consumed synchronous queries; no repository method is suspended
// inside a transaction or callback. A second driver supplies deterministic WAL interleavings.
internal class BackupQueryHookDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var beforeQuery: ((String) -> Unit)? = null
    var afterQuery: ((String) -> Unit)? = null

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> {
        beforeQuery?.invoke(sql)
        val result = delegate.executeQuery(identifier, sql, mapper, parameters, binders)
        afterQuery?.invoke(sql)
        return result
    }
}

private class WalBackupFixture : AutoCloseable {
    private val directory = Files.createTempDirectory("backup-snapshot-wal-").toFile()
    private val url = "jdbc:sqlite:${directory.resolve("workouts.db").absolutePath}"
    private val properties = Properties().apply { setProperty("busy_timeout", "1000") }
    val driver = BackupQueryHookDriver(JdbcSqliteDriver(url, properties))
    val harness = SqlFoundationStoreTestHarness(driver)
    val database get() = harness.database
    val writer = JdbcSqliteDriver(url, properties)
    val writerDatabase = WorkoutDatabase(writer)

    init {
        driver.executeQuery(null, "PRAGMA journal_mode = WAL", { cursor ->
            check(cursor.next().value)
            check(cursor.getString(0).equals("wal", ignoreCase = true))
            QueryResult.Value(Unit)
        }, 0, null).value
    }

    override fun close() {
        try {
            writer.close()
        } finally {
            try {
                driver.close()
            } finally {
                directory.deleteRecursively()
            }
        }
    }
}
