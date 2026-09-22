package com.jjswigut.oopsallprs.data.repository

import app.cash.sqldelight.db.QueryResult
import com.jjswigut.oopsallprs.data.backup.BackupPackage
import com.jjswigut.oopsallprs.data.backup.BackupSyncCoordinator
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SqlBackupSnapshotIntegrityTest {
    @Test
    fun malformedCompletedSetKindFailsSnapshotInsteadOfDroppingWorkout() = runTest {
        assertCorruptionRejected(Corruption.COMPLETED_SET_KIND)
    }

    @Test
    fun malformedCompletedCaptureConfigurationFailsSnapshot() = runTest {
        assertCorruptionRejected(Corruption.COMPLETED_CONFIGURATION)
    }

    @Test
    fun malformedActiveChildFailsSnapshotInsteadOfHidingActiveWorkout() = runTest {
        assertCorruptionRejected(Corruption.ACTIVE_CHILD)
    }

    @Test
    fun malformedDraftFailsSnapshotInsteadOfDroppingDraft() = runTest {
        assertCorruptionRejected(Corruption.DRAFT)
    }

    @Test
    fun partialCircuitFailsSnapshotInsteadOfErasingGroupMetadata() = runTest {
        assertCorruptionRejected(Corruption.PARTIAL_CIRCUIT)
    }

    @Test
    fun malformedUnreferencedConfigurationFailsSnapshotInsteadOfBeingOmitted() = runTest {
        assertCorruptionRejected(Corruption.CONFIGURATION)
    }

    @Test
    fun userOverrideWithMalformedConfigurationFailsSnapshotInsteadOfBeingOmitted() = runTest {
        assertCorruptionRejected(Corruption.USER_OVERRIDE)
    }

    @Test
    fun invalidInitialSnapshotStopsFileRestoreBeforeSafetyExportOrReplacement() = runTest {
        assertRestoreRejected(conflict = false)
    }

    @Test
    fun invalidInitialSnapshotStopsConflictRestoreBeforeSafetyExportOrReplacement() = runTest {
        assertRestoreRejected(conflict = true)
    }

    @Test
    fun emptyDatabaseRemainsAValidSnapshot() = runTest {
        val fixture = IntegrityFixture()
        try {
            val pkg = fixture.backup.createPackage().successValue()
            assertTrue(pkg.completedWorkouts.isEmpty())
            assertNull(pkg.activeWorkout)
            assertTrue(pkg.activeSetDrafts.isEmpty())
            assertTrue(pkg.lastLocalRevision.startsWith("snapshot-v1:"))
        } finally {
            fixture.harness.driver.close()
        }
    }

    @Test
    fun validLegacyNullConfigurationReferencesRemainReadable() = runTest {
        val fixture = IntegrityFixture()
        try {
            val before = fixture.seed()
            fixture.sql("UPDATE exercise_catalog SET default_logging_configuration_id = NULL")
            fixture.sql("UPDATE active_exercises SET logging_configuration_id = NULL")
            fixture.sql("UPDATE exercise_sets SET capture_configuration_id = NULL")
            fixture.sql("UPDATE active_set_drafts SET logging_configuration_id = NULL")

            val after = fixture.backup.createPackage().successValue()

            assertEquals(before.completedWorkouts, after.completedWorkouts)
            val beforeActive = assertNotNull(before.activeWorkout)
            val afterActive = assertNotNull(after.activeWorkout)
            assertEquals(beforeActive.id, afterActive.id)
            assertEquals(beforeActive.exercises.map { it.id }, afterActive.exercises.map { it.id })
            assertEquals(
                beforeActive.exercises.map { it.loggingConfigurationId },
                afterActive.exercises.map { it.loggingConfigurationId }
            )
            assertEquals(before.activeSetDrafts, after.activeSetDrafts)
            fixture.assertForeignKeysValid()
        } finally {
            fixture.harness.driver.close()
        }
    }

    private suspend fun assertCorruptionRejected(corruption: Corruption) {
        val fixture = IntegrityFixture()
        try {
            fixture.seed()
            fixture.corrupt(corruption)
            fixture.assertForeignKeysValid()
            val before = fixture.rawData()

            // An exception also fails this test: callers need a recoverable result.
            assertIs<FoundationResult.Failure>(fixture.backup.createPackage(), corruption.name)

            assertEquals(before, fixture.rawData(), "Snapshot reads must not repair or remove raw data")
        } finally {
            fixture.harness.driver.close()
        }
    }

    private suspend fun assertRestoreRejected(conflict: Boolean) {
        for (corruption in Corruption.entries) {
            val fixture = IntegrityFixture()
            try {
                val valid = fixture.seed()
                val incoming = valid.copy(preferences = valid.preferences.copy(defaultRestSeconds = 321))
                val documents = IntegrityDocuments(fixture.backup.encodePackage(incoming).successValue())
                val sync = SqlBackupSyncRepository(fixture.harness.database)
                val linked = BackupSyncState(
                    linkedFile = INTEGRITY_LINK,
                    lastLocalRevision = valid.lastLocalRevision,
                    lastBackupRevision = valid.lastLocalRevision,
                    lastOutcome = BackupSyncOutcome.CONFLICT,
                    lastConflictSummary = "Both copies changed",
                    updatedAt = instant(5_000)
                )
                sync.saveSyncState(linked).successValue()
                fixture.corrupt(corruption)
                fixture.assertForeignKeysValid()
                val before = fixture.rawData()
                val coordinator = BackupSyncCoordinator(fixture.backup, sync, documents)

                val result = if (conflict) {
                    coordinator.resolveConflict(BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY)
                } else {
                    coordinator.restoreFromFile()
                }

                assertIs<FoundationResult.Failure>(result, corruption.name)
                assertEquals(0, documents.creates, "$corruption must fail before exporting an incomplete safety copy")
                assertEquals(0, documents.writes)
                assertEquals(before, fixture.rawData(), "$corruption must leave every raw data row unchanged")
                val after = sync.loadSyncState()
                assertEquals(linked.linkedFile, after.linkedFile)
                assertEquals(linked.lastLocalRevision, after.lastLocalRevision)
                assertEquals(linked.lastBackupRevision, after.lastBackupRevision)
                assertEquals(BackupSyncOutcome.CONFLICT, after.lastOutcome)
                assertEquals(linked.lastConflictSummary, after.lastConflictSummary)
            } finally {
                fixture.harness.driver.close()
            }
        }
    }
}

private enum class Corruption {
    COMPLETED_SET_KIND, COMPLETED_CONFIGURATION, ACTIVE_CHILD, DRAFT,
    PARTIAL_CIRCUIT, CONFIGURATION, USER_OVERRIDE
}

private class IntegrityFixture {
    val harness = SqlFoundationStoreTestHarness()
    val repos = harness.repositories()
    val backup = SqlBackupRepository(harness.database, repos.store)

    init {
        sql("PRAGMA foreign_keys = ON")
        assertEquals("1", rows("PRAGMA foreign_keys", 1).single().single())
    }

    suspend fun seed(): BackupPackage {
        seedBackupExercises(repos)
        createCompletedMixedWorkout(repos, asCircuit = true)
        val active = repos.lifecycle.startEmpty(instant(3_000)).successValue()
        val exercise = repos.setLogging.addExercise(
            active.id,
            ExerciseReference(FoundationId("exercise-bench"), "Bench Press", isBodyweight = false),
            instant(3_100)
        ).successValue()
        repos.workouts.saveSetDraft(PersistedSetDraft(
            draftId = FoundationId("integrity-draft"),
            activeWorkoutId = active.id,
            exerciseInstanceId = exercise.id,
            position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(50.0),
            updatedAt = instant(3_200)
        )).successValue()
        val pkg = backup.createPackage().successValue()
        assertEquals(1, pkg.completedWorkouts.size)
        assertEquals(2, pkg.completedWorkouts.single().exercises.sumOf { it.loggedSets.size })
        assertEquals(1, assertNotNull(pkg.activeWorkout).exercises.size)
        assertEquals(1, pkg.activeSetDrafts.size)
        return pkg
    }

    fun corrupt(kind: Corruption) {
        when (kind) {
            Corruption.COMPLETED_SET_KIND -> sql(
                "UPDATE exercise_sets SET set_kind = 'INVALID_KIND' WHERE id = (SELECT MIN(id) FROM exercise_sets WHERE logged_at IS NOT NULL)"
            )
            Corruption.COMPLETED_CONFIGURATION -> {
                insertMalformedConfiguration()
                sql("UPDATE exercise_sets SET capture_configuration_id = 'integrity-bad-config' WHERE id = (SELECT MIN(id) FROM exercise_sets WHERE logged_at IS NOT NULL)")
            }
            Corruption.ACTIVE_CHILD -> sql(
                "UPDATE active_exercises SET position = -1 WHERE active_workout_id IN (SELECT id FROM active_workouts WHERE status = 'ACTIVE')"
            )
            Corruption.DRAFT -> sql("UPDATE active_set_drafts SET set_kind = 'INVALID_KIND' WHERE draft_id = 'integrity-draft'")
            Corruption.PARTIAL_CIRCUIT -> sql("UPDATE active_exercises SET group_rounds = NULL WHERE group_id IS NOT NULL")
            Corruption.CONFIGURATION -> insertMalformedConfiguration()
            Corruption.USER_OVERRIDE -> {
                insertMalformedConfiguration()
                sql("INSERT INTO user_exercise_configurations(exercise_catalog_id, logging_configuration_id, based_on_definition_revision, created_at, updated_at) VALUES ('exercise-bench', 'integrity-bad-config', 1, 100, 100)")
            }
        }
    }

    private fun insertMalformedConfiguration() {
        // INSERT is allowed; immutable-row triggers and all schema constraints stay enabled.
        sql("INSERT INTO logging_configurations(id, schema_version, content_hash, source_code, created_at) VALUES ('integrity-bad-config', 1, 'integrity-invalid-hash', 'custom', 100)")
        sql("INSERT INTO logging_configuration_measures(logging_configuration_id, position, measure_code, requirement_code, canonical_unit_code) VALUES ('integrity-bad-config', 0, 'reps', 'INVALID_REQUIREMENT', 'count')")
    }

    fun sql(statement: String) {
        harness.driver.execute(null, statement, 0)
    }

    fun assertForeignKeysValid() {
        assertTrue(rows("PRAGMA foreign_key_check", 4).isEmpty())
    }

    fun rawData(): Map<String, List<List<String?>>> {
        // Sync failure diagnostics may change; linkage and agreed baselines are asserted separately.
        val tables = rows(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%' AND name != 'sync_state' ORDER BY name",
            1
        ).map { requireNotNull(it.single()) }
        return tables.associateWith { table ->
            val columns = rows("PRAGMA table_info(\"$table\")", 6).size
            rows("SELECT * FROM \"$table\" ORDER BY rowid", columns)
        }
    }

    private fun rows(sql: String, columnCount: Int): List<List<String?>> = harness.driver.executeQuery(
        null, sql, { cursor ->
            QueryResult.Value(buildList {
                while (cursor.next().value) add(List(columnCount) { cursor.getString(it) })
            })
        }, 0
    ).value
}

private val INTEGRITY_LINK = BackupLinkedFile("backup.json", "memory://integrity-backup", "memory")

private class IntegrityDocuments(private val incoming: String) : BackupDocumentAdapter {
    var creates = 0
    var writes = 0
    override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
        creates++
        return foundationSuccess(INTEGRITY_LINK.copy(providerReference = "memory://safety"))
    }
    override suspend fun openBackupDocument() = foundationSuccess(BackupDocument(INTEGRITY_LINK, incoming))
    override suspend fun readBackup(linkedFile: BackupLinkedFile) = foundationSuccess(incoming)
    override suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile> {
        writes++
        return foundationSuccess(linkedFile)
    }
}
