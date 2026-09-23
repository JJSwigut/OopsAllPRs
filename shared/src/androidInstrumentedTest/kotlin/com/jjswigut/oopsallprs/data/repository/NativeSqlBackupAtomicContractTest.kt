package com.jjswigut.oopsallprs.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.jjswigut.oopsallprs.data.backup.BackupPackage
import com.jjswigut.oopsallprs.data.backup.BackupSnapshotIdentity
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.RoutineUseCases
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class NativeSqlBackupAtomicContractTest {
    private val fixtures = mutableListOf<NativeBackupDatabaseFixture>()

    @After
    fun closeOwnedDatabases() {
        var failure: Throwable? = null
        fixtures.asReversed().forEach { fixture ->
            try {
                fixture.close()
            } catch (error: Throwable) {
                val first = failure
                if (first == null) failure = error else first.addSuppressed(error)
            }
        }
        failure?.let { throw it }
    }

    @Test(timeout = 30_000)
    fun snapshotRoundTripPreservesRecoverableContentOnAndroidSqliteDriver() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val original = source.backup.createPackage().nativeSuccess()
        assertPopulatedSnapshot(original)
        assertEquals(BackupSnapshotIdentity.revision(original), original.lastLocalRevision)
        assertEquals(original.lastLocalRevision, source.backup.currentRevision().value)

        val destination = newDatabase().primary
        val decoded = destination.backup.decodePackage(source.backup.encodePackage(original).nativeSuccess()).nativeSuccess()
        val restored = destination.backup.restore(decoded).nativeSuccess()
        val actual = destination.backup.createPackage().nativeSuccess()

        assertRecoverableContentEquals(original, actual)
        assertEquals(actual.lastLocalRevision, assertNotNull(restored.restoredLocalRevision).value)
        assertEquals(destination.backup.currentRevision(), restored.restoredLocalRevision)
        assertNull(restored.syncWarning)
    }

    @Test(timeout = 30_000)
    fun archivedCustomExerciseRoundTripsWithHistoryAndUserLoggingOverride() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val exerciseId = FoundationId("bench")
        val exercise = assertNotNull(source.store.exercise(exerciseId))
        val loggingOverride = source.store.saveUserExerciseConfiguration(UserExerciseConfiguration(
            exerciseDefinitionId = exerciseId,
            configuration = extraConfiguration(),
            basedOnDefinitionRevision = exercise.definitionRevision,
            configuredAt = nativeInstant(4_000)
        )).nativeSuccess()
        val before = source.backup.createPackage().nativeSuccess()
        val archivedAt = nativeInstant(5_000)

        source.store.archiveUserExercise(exerciseId, archivedAt).nativeSuccess()

        assertFalse(source.store.all().any { it.id == exerciseId })
        assertEquals(archivedAt.toEpochMilliseconds(), source.database.exerciseQueriesQueries
            .selectExerciseById(exerciseId.value).executeAsOne().archived_at)
        val archived = source.backup.createPackage().nativeSuccess()
        val expectedExercise = before.exercises.single { it.id == exerciseId.value }.copy(
            archivedAt = archivedAt.toEpochMilliseconds(), updatedAt = archivedAt.toEpochMilliseconds()
        )
        assertEquals(expectedExercise, archived.exercises.single { it.id == exerciseId.value })
        assertEquals(before.completedWorkouts, archived.completedWorkouts)
        assertEquals(before.userExerciseConfigurations, archived.userExerciseConfigurations)
        assertTrue(archived.userExerciseConfigurations.any { it.exerciseDefinitionId == exerciseId.value })
        assertNotEquals(before.lastLocalRevision, archived.lastLocalRevision)

        val destination = newDatabase().primary
        val decoded = destination.backup.decodePackage(source.backup.encodePackage(archived).nativeSuccess()).nativeSuccess()
        val restored = destination.backup.restore(decoded).nativeSuccess()
        val actual = destination.backup.createPackage().nativeSuccess()

        assertRecoverableContentEquals(archived, actual)
        assertEquals(expectedExercise, actual.exercises.single { it.id == exerciseId.value })
        assertEquals(before.completedWorkouts, actual.completedWorkouts)
        assertEquals(loggingOverride, destination.store.userExerciseConfiguration(exerciseId))
        assertFalse(destination.store.all().any { it.id == exerciseId })
        assertEquals(archivedAt.toEpochMilliseconds(), destination.database.exerciseQueriesQueries
            .selectExerciseById(exerciseId.value).executeAsOne().archived_at)
        assertEquals(destination.backup.currentRevision(), assertNotNull(restored.restoredLocalRevision))
    }

    @Test(timeout = 30_000)
    fun archivedRoutineRoundTripsWithTemplatesAndReferencingCompletedHistory() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val routine = source.store.routines().single()
        val lifecycle = WorkoutLifecycleUseCases(source.store, source.store, source.store, source.store, source.store)
        lifecycle.discard(assertNotNull(source.store.currentActiveWorkout()).id, nativeInstant(4_000)).nativeSuccess()
        val workout = lifecycle.startFromRoutine(routine.id, nativeInstant(4_100)).nativeSuccess()
        val weighted = workout.exercises.single { it.reference.exerciseCatalogId == FoundationId("bench") }
        SetLoggingUseCases(source.store, source.store, source.store).confirmSet(
            workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(90.0), 0, nativeInstant(4_200)
        ).nativeSuccess()
        val completed = RoutineUseCases(source.store, source.store, source.store, preferences = source.store)
            .finishWorkout(workout.id, nativeInstant(4_500)).nativeSuccess().workout
        assertEquals(routine.id, completed.routineId)
        val before = source.backup.createPackage().nativeSuccess()
        val archivedAt = nativeInstant(5_000)

        // The public delete operation archives a routine rather than deleting its history/templates.
        source.store.deleteRoutine(routine.id, archivedAt).nativeSuccess()

        assertNull(source.store.routine(routine.id))
        assertFalse(source.store.routines().any { it.id == routine.id })
        val archived = source.backup.createPackage().nativeSuccess()
        val expectedRoutine = before.routines.single { it.id == routine.id.value }.copy(
            archivedAt = archivedAt.toEpochMilliseconds(), updatedAt = archivedAt.toEpochMilliseconds()
        )
        assertEquals(expectedRoutine, archived.routines.single { it.id == routine.id.value })
        assertTrue(expectedRoutine.exercises.all { it.plannedSets.isNotEmpty() })
        assertEquals(before.completedWorkouts, archived.completedWorkouts)
        assertNotEquals(before.lastLocalRevision, archived.lastLocalRevision)

        val destination = newDatabase().primary
        val decoded = destination.backup.decodePackage(source.backup.encodePackage(archived).nativeSuccess()).nativeSuccess()
        val restored = destination.backup.restore(decoded).nativeSuccess()
        val actual = destination.backup.createPackage().nativeSuccess()

        assertRecoverableContentEquals(archived, actual)
        assertEquals(expectedRoutine, actual.routines.single { it.id == routine.id.value })
        assertEquals(before.completedWorkouts, actual.completedWorkouts)
        assertEquals(routine.id, assertNotNull(destination.store.completedWorkout(completed.id)).routineId)
        assertNull(destination.store.routine(routine.id))
        assertFalse(destination.store.routines().any { it.id == routine.id })
        assertEquals(destination.backup.currentRevision(), assertNotNull(restored.restoredLocalRevision))
    }

    @Test(timeout = 30_000)
    fun corruptCompletedSetRejectsSnapshotAndGuardedRestoreWithoutChangingRows() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().context
        check(context.packageName == "com.jjswigut.oopsallprs.backupcontract.test")
        val name = "backup-contract-integrity-${UUID.randomUUID()}.db"
        check(!context.getDatabasePath(name).exists())
        val driver = AndroidSqliteDriver(WorkoutDatabase.Schema, context, name)
        try {
            val local = NativeBackupConnection(WorkoutDatabase(driver))
            local.seedRecoverableContent()
            val valid = local.backup.createPackage().nativeSuccess()
            assertPopulatedSnapshot(valid)
            val linked = oldSyncState(valid.lastLocalRevision)
            local.sync.saveSyncState(linked).nativeSuccess()

            // Bypass domain mapping, keeping SQLite constraints and triggers enabled.
            driver.execute(null, """
                UPDATE exercise_sets SET set_kind = 'INVALID_KIND'
                WHERE id = (SELECT MIN(id) FROM exercise_sets WHERE logged_at IS NOT NULL)
            """.trimIndent(), 0)
            val completedId = valid.completedWorkouts.single().sourceActiveWorkoutId
            assertEquals(1, local.database.setQueriesQueries.selectLoggedSets(completedId)
                .executeAsList().count { it.set_kind == "INVALID_KIND" })
            val before = rawNativeDatabaseRows(driver)

            val snapshot = local.backup.createPackage()

            assertEquals(before, rawNativeDatabaseRows(driver), "Snapshot must not repair or discard persisted rows")
            assertIs<FoundationResult.Failure>(snapshot, "Corrupt logged sets must not produce a smaller successful backup")

            val restore = local.backup.restore(valid, expectedLocalRevision = valid.lastLocalRevision)

            assertEquals(before, rawNativeDatabaseRows(driver), "An unreadable local safety snapshot must prevent replacement/unlink")
            assertIs<FoundationResult.Failure>(restore)
            assertEquals(linked, local.sync.loadSyncState())
        } finally {
            // This connection belongs only to this test; delete only after close succeeds.
            driver.close()
            check(context.deleteDatabase(name))
        }
    }

    @Test(timeout = 30_000)
    fun guardedRestoreRejectsStaleRevisionWithoutMutationOrSyncLinkLoss() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val incoming = source.backup.createPackage().nativeSuccess()
        val local = newDatabase().primary
        local.seedRecoverableContent()
        val expected = local.backup.currentRevision().value
        val linked = oldSyncState(expected)
        local.sync.saveSyncState(linked).nativeSuccess()
        local.store.setWeightUnit(WeightUnit.POUNDS).nativeSuccess()
        val before = local.backup.createPackage().nativeSuccess()
        val persistedPreferences = local.database.workoutQueriesQueries.selectUserPreferences().executeAsOne()
        assertNotEquals(expected, before.lastLocalRevision)
        var reachedMutation = false
        val guarded = SqlBackupRepository(local.database, local.store, restoreFaultInjector = { reachedMutation = true })

        val result = guarded.restore(incoming, expectedLocalRevision = expected)

        assertIs<FoundationError.Conflict>(assertIs<FoundationResult.Failure>(result).error)
        assertFalse(reachedMutation)
        assertRecoverableContentEquals(before, local.backup.createPackage().nativeSuccess())
        assertEquals(persistedPreferences, local.database.workoutQueriesQueries.selectUserPreferences().executeAsOne())
        assertEquals(linked, local.sync.loadSyncState())
    }

    @Test(timeout = 30_000)
    fun successfulGuardedRestoreReturnsCommittedBaselineAndExactSafetySnapshot() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val incoming = source.backup.createPackage().nativeSuccess()
        val local = newDatabase().primary
        local.seedRecoverableContent()
        local.store.setWeightUnit(WeightUnit.POUNDS).nativeSuccess()
        local.store.setDefaultRestSeconds(120).nativeSuccess()
        val before = local.backup.createPackage().nativeSuccess()
        local.sync.saveSyncState(oldSyncState(before.lastLocalRevision)).nativeSuccess()
        assertNotEquals(incoming.lastLocalRevision, before.lastLocalRevision)

        val result = local.backup.restore(incoming, expectedLocalRevision = before.lastLocalRevision).nativeSuccess()
        val actual = local.backup.createPackage().nativeSuccess()

        assertRecoverableContentEquals(before, result.safetyBackup)
        assertRecoverableContentEquals(incoming, actual)
        assertEquals(local.backup.currentRevision(), assertNotNull(result.restoredLocalRevision))
        assertEquals(actual.lastLocalRevision, result.restoredLocalRevision?.value)
        assertTrue(result.activeWorkoutReplaced)
        assertNull(result.syncWarning)
        assertNull(local.database.backupQueriesQueries.selectSyncState().executeAsOneOrNull())
    }

    @Test(timeout = 30_000)
    fun retainedImmutableConfigurationIsIncludedInReturnedLocalBaseline() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val incoming = source.backup.createPackage().nativeSuccess()
        val local = newDatabase().primary
        val extra = extraConfiguration()
        local.store.saveLoggingConfiguration(extra).nativeSuccess()
        val expected = local.backup.currentRevision().value

        val result = local.backup.restore(incoming, expectedLocalRevision = expected).nativeSuccess()
        val actual = local.backup.createPackage().nativeSuccess()

        assertTrue(actual.loggingConfigurations.any { it.id == extra.id.value })
        assertFalse(incoming.loggingConfigurations.any { it.id == extra.id.value })
        assertEquals(extra, local.store.loggingConfiguration(extra.id))
        assertNotEquals(incoming.lastLocalRevision, actual.lastLocalRevision)
        assertEquals(BackupSnapshotIdentity.revision(actual), assertNotNull(result.restoredLocalRevision).value)
        assertEquals(local.backup.currentRevision(), result.restoredLocalRevision)
    }

    @Test(timeout = 30_000)
    fun injectedFaultRollsBackDeletedRowsInsertedConfigurationsAndUnlink() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val extra = extraConfiguration()
        source.store.saveLoggingConfiguration(extra).nativeSuccess()
        val incoming = source.backup.createPackage().nativeSuccess()
        val local = newDatabase().primary
        local.seedRecoverableContent()
        local.store.setWeightUnit(WeightUnit.POUNDS).nativeSuccess()
        val before = local.backup.createPackage().nativeSuccess()
        val linked = oldSyncState(before.lastLocalRevision)
        local.sync.saveSyncState(linked).nativeSuccess()
        var injected = false
        var sawUnlink = false
        var sawLedgerCleared = false
        var sawExportsCleared = false
        var sawInsertedConfiguration = false
        val failing = SqlBackupRepository(local.database, local.store, restoreFaultInjector = {
            injected = true
            sawUnlink = local.database.backupQueriesQueries.selectSyncState().executeAsOneOrNull() == null
            sawLedgerCleared = local.database.routineQueriesQueries.selectCompletedWorkouts().executeAsList().isEmpty()
            sawExportsCleared = local.database.progressQueriesQueries.selectExportSnapshots().executeAsList().isEmpty()
            sawInsertedConfiguration = local.database.loggingConfigurationQueriesQueries
                .selectLoggingConfiguration(extra.id.value).executeAsOneOrNull() != null
            error("native restore fault after destructive writes")
        })

        val result = failing.restore(incoming, expectedLocalRevision = before.lastLocalRevision)

        assertIs<FoundationResult.Failure>(result)
        assertTrue(injected)
        assertTrue(sawUnlink)
        assertTrue(sawLedgerCleared)
        assertTrue(sawExportsCleared)
        assertTrue(sawInsertedConfiguration)
        assertRecoverableContentEquals(before, local.backup.createPackage().nativeSuccess())
        assertNull(local.store.loggingConfiguration(extra.id))
        assertEquals(linked, local.sync.loadSyncState())
    }

    @Test(timeout = 30_000)
    fun guardObservesCommittedEditFromAnotherNativeConnectionAndThread() = runBlocking {
        val source = newDatabase().primary
        source.seedRecoverableContent()
        val incoming = source.backup.createPackage().nativeSuccess()
        val fixture = newDatabase()
        val local = fixture.primary
        local.seedRecoverableContent()
        val expected = local.backup.currentRevision().value
        val linked = oldSyncState(expected)
        local.sync.saveSyncState(linked).nativeSuccess()
        val peer = fixture.openConnection()

        // A joined commit provides deterministic ordering; this is not a stress/scheduling test.
        fixture.onWorker { peer.store.setWeightUnit(WeightUnit.POUNDS).nativeSuccess() }
        val afterEdit = local.backup.createPackage().nativeSuccess()
        assertNotEquals(expected, afterEdit.lastLocalRevision)
        val result = local.backup.restore(incoming, expectedLocalRevision = expected)

        assertIs<FoundationError.Conflict>(assertIs<FoundationResult.Failure>(result).error)
        assertRecoverableContentEquals(afterEdit, local.backup.createPackage().nativeSuccess())
        assertRecoverableContentEquals(afterEdit, peer.backup.createPackage().nativeSuccess())
        assertEquals(linked, peer.sync.loadSyncState())
    }

    private fun newDatabase(): NativeBackupDatabaseFixture = NativeBackupDatabaseFixture().also(fixtures::add)

    private fun rawNativeDatabaseRows(driver: SqlDriver): Map<String, List<List<String?>>> {
        fun rows(sql: String, columns: Int): List<List<String?>> = driver.executeQuery(
            null, sql, { cursor ->
                QueryResult.Value(buildList {
                    while (cursor.next().value) add(List(columns) { cursor.getString(it) })
                })
            }, 0
        ).value
        val tables = rows(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' ORDER BY name", 1
        ).map { requireNotNull(it.single()) }
        return tables.associateWith { table ->
            val identifier = table.replace("\"", "\"\"")
            val columns = rows("PRAGMA table_info(\"$identifier\")", 6).size
            rows("SELECT * FROM \"$identifier\" ORDER BY rowid", columns)
        }
    }

    private fun assertPopulatedSnapshot(pkg: BackupPackage) {
        assertEquals(1, pkg.completedWorkouts.size)
        assertEquals(2, pkg.completedWorkouts.single().exercises.size)
        pkg.completedWorkouts.single().exercises.forEach { exercise ->
            assertNotNull(exercise.groupId)
            assertNotNull(exercise.groupPosition)
            assertEquals("Circuit", exercise.groupLabel)
            assertEquals(3, exercise.groupRounds)
            assertTrue(exercise.loggedSets.isNotEmpty())
        }
        assertTrue(pkg.loggingConfigurations.isNotEmpty())
        assertTrue(pkg.routines.isNotEmpty())
        assertTrue(pkg.personalRecords.isNotEmpty())
        assertTrue(pkg.progressPoints.isNotEmpty())
        assertTrue(pkg.exportMetadata.isNotEmpty())
        assertNotNull(pkg.activeWorkout)
        assertNotNull(pkg.activeSession)
        assertNotNull(pkg.activeUxSession)
        assertTrue(pkg.activeSetDrafts.isNotEmpty())
    }

    private fun assertRecoverableContentEquals(expected: BackupPackage, actual: BackupPackage) {
        assertEquals(BackupSnapshotIdentity.revision(expected), BackupSnapshotIdentity.revision(actual))
        assertEquals(actual.lastLocalRevision, BackupSnapshotIdentity.revision(actual))
        // Envelope creation time/device and derived summary are not recoverable content.
        assertEquals(expected.copy(createdAt = actual.createdAt, deviceId = actual.deviceId, summary = actual.summary), actual)
    }

    private fun oldSyncState(revision: String) = BackupSyncState(
        linkedFile = BackupLinkedFile("owned-test.json", "test-only://backup", "test-only"),
        lastLocalRevision = revision,
        lastBackupRevision = "previous-remote",
        lastLocalTimestamp = nativeInstant(42),
        lastBackupTimestamp = nativeInstant(41),
        lastOutcome = BackupSyncOutcome.CLEAN,
        updatedAt = nativeInstant(43)
    )

    private fun extraConfiguration() = LoggingConfiguration(
        id = LoggingConfigurationId("native-distance-duration"),
        schemaVersion = LoggingSchemaVersion(1),
        measures = listOf(
            MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED),
            MeasureSpec(MeasureKind.DURATION, MeasureRequirement.OPTIONAL)
        )
    )
}
