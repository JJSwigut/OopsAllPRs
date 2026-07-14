package com.jjswigut.oopsallprs.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull

class Schema11MigrationTest {
    @Test
    fun emptySchema10MigratesWithImmutableLegacyConfigurations() = withMigratedFixture("empty.db") { driver ->
        assertEquals(4L, driver.count("logging_configurations"))
        assertEquals(6L, driver.count("logging_configuration_measures"))
        assertEquals(0L, driver.count("logging_configuration_effort_kinds"))
        val canonicalHashInputs = mapOf(
            "legacy_weighted_v1" to
                "schema_version=1|measures=0:reps:required:count;1:load:required:kilograms:external_resistance|effort=",
            "legacy_bodyweight_v1" to
                "schema_version=1|measures=0:reps:required:count|effort=",
            "legacy_timed_v1" to
                "schema_version=1|measures=0:duration:required:milliseconds|effort=",
            "legacy_bodyweight_unspecified_load_v1" to
                "schema_version=1|measures=0:reps:required:count;1:load:optional:kilograms:legacy_unspecified|effort="
        )
        assertEquals(
            canonicalHashInputs.mapValues { (_, canonicalInput) -> sha256(canonicalInput) },
            driver.nullableStringMap("SELECT id, content_hash FROM logging_configurations ORDER BY id")
        )
        assertEquals(
            listOf("reps", "load"),
            driver.stringList(
                """
                SELECT measure_code
                FROM logging_configuration_measures
                WHERE logging_configuration_id = 'legacy_weighted_v1'
                ORDER BY position
                """.trimIndent()
            )
        )
        assertEquals(
            "legacy_unspecified",
            driver.nullableString(
                """
                SELECT load_role_code
                FROM logging_configuration_measures
                WHERE logging_configuration_id = 'legacy_bodyweight_unspecified_load_v1'
                  AND measure_code = 'load'
                """.trimIndent()
            )
        )
        driver.executeStatement(
            """
            INSERT INTO logging_configurations(
                id, schema_version, content_hash, source_code, created_at
            ) VALUES ('effort-enabled', 1, 'effort-enabled-hash', 'test', 1)
            """.trimIndent()
        )
        listOf("rpe", "rir", "failure_outcome").forEachIndexed { position, effortKind ->
            driver.executeStatement(
                """
                INSERT INTO logging_configuration_effort_kinds(
                    logging_configuration_id, position, effort_kind_code
                ) VALUES ('effort-enabled', $position, '$effortKind')
                """.trimIndent()
            )
        }
        assertEquals(
            listOf("rpe", "rir", "failure_outcome"),
            driver.stringList(
                """
                SELECT effort_kind_code
                FROM logging_configuration_effort_kinds
                WHERE logging_configuration_id = 'effort-enabled'
                ORDER BY position
                """.trimIndent()
            )
        )
        assertFails {
            driver.executeStatement(
                """
                INSERT INTO logging_configuration_effort_kinds(
                    logging_configuration_id, position, effort_kind_code
                ) VALUES ('effort-enabled', 3, 'rir')
                """.trimIndent()
            )
        }
        assertFails {
            driver.executeStatement(
                "UPDATE logging_configurations SET source_code = 'changed' WHERE id = 'legacy_weighted_v1'"
            )
        }
        assertForeignKeysClean(driver)
    }

    @Test
    fun representativeSchema10RowsArePreservedAndBackfilledDeterministically() =
        withMigratedFixture("representative.db", verifyLegacyRowCounts = true) { driver ->
            assertEquals(
                mapOf(
                    "cat-body" to "legacy_bodyweight_v1",
                    "cat-timed" to "legacy_timed_v1",
                    "cat-unknown" to null,
                    "cat-weighted" to "legacy_weighted_v1"
                ),
                driver.nullableStringMap(
                    "SELECT id, default_logging_configuration_id FROM exercise_catalog ORDER BY id"
                )
            )
            assertEquals(
                mapOf(
                    "set-body" to "legacy_bodyweight_v1",
                    "set-timed" to "legacy_timed_v1",
                    "set-unknown" to null,
                    "set-weighted" to "legacy_weighted_v1"
                ),
                driver.nullableStringMap(
                    "SELECT id, capture_configuration_id FROM exercise_sets ORDER BY id"
                )
            )
            assertEquals(
                mapOf(
                    "re-mixed" to null,
                    "re-timed" to "legacy_timed_v1",
                    "re-weighted" to "legacy_weighted_v1"
                ),
                driver.nullableStringMap(
                    "SELECT id, logging_configuration_id FROM routine_exercises ORDER BY id"
                )
            )
            assertEquals("seed", driver.string("SELECT definition_origin FROM exercise_catalog WHERE id = 'cat-weighted'"))
            assertEquals(
                "oopsallprs_baseline:bench press",
                driver.string("SELECT seed_id FROM exercise_catalog WHERE id = 'cat-weighted'")
            )
            assertEquals("manifest-a", driver.string("SELECT seed_manifest_revision FROM exercise_catalog WHERE id = 'cat-weighted'"))
            assertEquals("user", driver.string("SELECT definition_origin FROM exercise_catalog WHERE id = 'cat-unknown'"))
            assertNull(driver.nullableString("SELECT seed_id FROM exercise_catalog WHERE id = 'cat-unknown'"))
            assertEquals(1L, driver.long("SELECT definition_revision FROM exercise_catalog WHERE id = 'cat-unknown'"))

            assertEquals("weight_for_reps", driver.string("SELECT metric_code FROM personal_records WHERE id = 'pr-1'"))
            assertEquals(1L, driver.long("SELECT derivation_version FROM personal_records WHERE id = 'pr-1'"))
            assertNull(driver.nullableString("SELECT metric_code FROM personal_records WHERE id = 'pr-unknown'"))
            assertNull(driver.nullableLong("SELECT derivation_version FROM personal_records WHERE id = 'pr-unknown'"))
            assertEquals("weight_for_reps", driver.string("SELECT metric_code FROM progress_points WHERE id = 'pp-1'"))
            assertNull(driver.nullableString("SELECT metric_code FROM progress_points WHERE id = 'pp-unknown'"))

            assertEquals(80.0, driver.double("SELECT weight_kg FROM exercise_sets WHERE id = 'set-weighted'"))
            assertEquals("FUTURE_MODE", driver.string("SELECT logging_mode FROM exercise_catalog WHERE id = 'cat-unknown'"))
            assertEquals("FUTURE_KIND", driver.string("SELECT set_kind FROM exercise_sets WHERE id = 'set-unknown'"))

            driver.executeStatement(
                """
                UPDATE routine_set_templates
                SET target_effort_kind = 'rpe', target_rpe_tenths = 80, target_rir = NULL
                WHERE id = 'rt-weighted'
                """.trimIndent()
            )
            assertFails {
                driver.executeStatement(
                    "UPDATE routine_set_templates SET target_rir = 2 WHERE id = 'rt-weighted'"
                )
            }
            driver.executeStatement(
                """
                UPDATE routine_set_templates
                SET target_effort_kind = 'rir', target_rpe_tenths = NULL, target_rir = 2
                WHERE id = 'rt-weighted'
                """.trimIndent()
            )
            driver.executeStatement(
                """
                UPDATE routine_set_templates
                SET target_effort_kind = 'to_failure', target_rpe_tenths = NULL, target_rir = NULL
                WHERE id = 'rt-weighted'
                """.trimIndent()
            )
            assertFails {
                driver.executeStatement(
                    "UPDATE routine_set_templates SET target_effort_kind = 'future_target' WHERE id = 'rt-weighted'"
                )
            }
            assertForeignKeysClean(driver)
        }

    @Test
    fun bodyweightLoadsUseUnspecifiedLoadWithoutChangingCatalogDefault() =
        withMigratedFixture("bodyweight-with-load.db", verifyLegacyRowCounts = true) { driver ->
            assertEquals(
                mapOf(
                    "set-body-load" to "legacy_bodyweight_unspecified_load_v1",
                    "set-body-unloaded" to "legacy_bodyweight_v1",
                    "set-unknown" to null
                ),
                driver.nullableStringMap(
                    "SELECT id, capture_configuration_id FROM exercise_sets ORDER BY id"
                )
            )
            assertEquals(
                "legacy_bodyweight_unspecified_load_v1",
                driver.string("SELECT logging_configuration_id FROM active_exercises WHERE id = 'ae-body-load'")
            )
            assertEquals(
                "legacy_bodyweight_unspecified_load_v1",
                driver.string("SELECT logging_configuration_id FROM active_set_drafts WHERE draft_id = 'draft-body-load'")
            )
            assertEquals(
                "legacy_bodyweight_unspecified_load_v1",
                driver.string("SELECT logging_configuration_id FROM routine_set_templates WHERE id = 'rt-body-load'")
            )
            assertEquals(
                "legacy_bodyweight_unspecified_load_v1",
                driver.string("SELECT logging_configuration_id FROM routine_exercises WHERE id = 're-body'")
            )
            assertEquals(
                "legacy_bodyweight_v1",
                driver.string("SELECT default_logging_configuration_id FROM exercise_catalog WHERE id = 'cat-body-load'")
            )
            assertEquals(12.5, driver.double("SELECT weight_kg FROM exercise_sets WHERE id = 'set-body-load'"))
            assertEquals(7.5, driver.double("SELECT weight_kg FROM active_set_drafts WHERE draft_id = 'draft-body-load'"))
            assertEquals(10.0, driver.double("SELECT target_weight_kg FROM routine_set_templates WHERE id = 'rt-body-load'"))
            assertNull(driver.nullableString("SELECT logging_configuration_id FROM active_exercises WHERE id = 'ae-unknown'"))
            assertNull(driver.nullableString("SELECT capture_configuration_id FROM exercise_sets WHERE id = 'set-unknown'"))
            assertForeignKeysClean(driver)
        }

    private fun withMigratedFixture(
        fixtureName: String,
        verifyLegacyRowCounts: Boolean = false,
        block: (SqlDriver) -> Unit
    ) {
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/$fixtureName")) {
            "Missing schema fixture $fixtureName"
        }
        val databaseFile = Files.createTempFile("schema-11-", ".db").toFile()
        resource.use { input ->
            Files.copy(input, databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        try {
            driver.executeStatement("PRAGMA foreign_keys = ON")
            val before = if (verifyLegacyRowCounts) {
                LEGACY_TABLES.associateWith { table -> driver.count(table) }
            } else {
                emptyMap()
            }

            WorkoutDatabase.Schema.migrate(driver, 10, 11)

            before.forEach { (table, count) -> assertEquals(count, driver.count(table), table) }
            block(driver)
        } finally {
            driver.close()
            databaseFile.delete()
        }
    }

    private fun assertForeignKeysClean(driver: SqlDriver) {
        assertEquals(0L, driver.long("SELECT COUNT(*) FROM pragma_foreign_key_check"))
    }

    private fun SqlDriver.executeStatement(sql: String) {
        execute(null, sql, 0, null).value
    }

    private fun SqlDriver.count(table: String): Long = long("SELECT COUNT(*) FROM $table")

    private fun SqlDriver.string(sql: String): String = checkNotNull(nullableString(sql))

    private fun SqlDriver.nullableString(sql: String): String? = one(sql) { it.getString(0) }

    private fun SqlDriver.long(sql: String): Long = checkNotNull(nullableLong(sql))

    private fun SqlDriver.nullableLong(sql: String): Long? = one(sql) { it.getLong(0) }

    private fun SqlDriver.double(sql: String): Double = checkNotNull(one(sql) { it.getDouble(0) })

    private fun SqlDriver.stringList(sql: String): List<String> = many(sql) { checkNotNull(it.getString(0)) }

    private fun SqlDriver.nullableStringMap(sql: String): Map<String, String?> =
        many(sql) { cursor -> checkNotNull(cursor.getString(0)) to cursor.getString(1) }.toMap()

    private fun <T> SqlDriver.one(sql: String, mapper: (SqlCursor) -> T): T =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                check(cursor.next().value) { "Expected one row for: $sql" }
                QueryResult.Value(mapper(cursor))
            },
            parameters = 0,
            binders = null
        ).value

    private fun <T> SqlDriver.many(sql: String, mapper: (SqlCursor) -> T): List<T> =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                val values = mutableListOf<T>()
                while (cursor.next().value) values += mapper(cursor)
                QueryResult.Value(values)
            },
            parameters = 0,
            binders = null
        ).value

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private companion object {
        val LEGACY_TABLES = listOf(
            "active_workouts",
            "active_exercises",
            "exercise_sets",
            "active_session_state",
            "routines",
            "routine_exercises",
            "routine_set_templates",
            "completed_workouts",
            "exercise_catalog",
            "exercise_seed_imports",
            "user_preferences",
            "personal_records",
            "progress_points",
            "export_snapshots",
            "active_workout_ux_sessions",
            "active_set_drafts",
            "sync_state",
            "full_access_state"
        )
    }
}
