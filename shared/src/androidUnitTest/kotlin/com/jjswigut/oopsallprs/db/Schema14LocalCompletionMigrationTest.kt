package com.jjswigut.oopsallprs.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Schema14LocalCompletionMigrationTest {
    @Test
    fun missingLegacyStateUsesHistoryCountAndDistinctReceiptsWithoutDeletingDuplicates() = withSchema13 { driver ->
        driver.execute(null, "DELETE FROM full_access_state", 0).value
        driver.execute(null,
            "INSERT INTO completed_workouts VALUES ('duplicate-completion', 'legacy-source', 1, 2, 1, NULL, 2)", 0).value
        driver.execute(null,
            "INSERT INTO completed_workouts VALUES ('other-completion', 'legacy-source', 1, 3, 2, NULL, 3)", 0).value
        val history = driver.rows("SELECT * FROM completed_workouts ORDER BY id", 7)
        val distinctSources = history.map { it[1] }.distinct().sortedBy { it }

        WorkoutDatabase.Schema.migrate(driver, 13, 14)

        assertEquals(history, driver.rows("SELECT * FROM completed_workouts ORDER BY id", 7))
        assertEquals(history.size.toString(), driver.rows("SELECT completed_free_workouts FROM full_access_state", 1).single().single())
        assertEquals(distinctSources, driver.rows("SELECT source_active_workout_id FROM local_workout_completion_receipts ORDER BY source_active_workout_id", 1).map { it.single() })
        driver.execute(null, "DELETE FROM completed_workouts", 0).value
        assertEquals(history.size.toString(), driver.rows("SELECT completed_free_workouts FROM full_access_state", 1).single().single())
        assertEquals(distinctSources.size, driver.rows("SELECT source_active_workout_id FROM local_workout_completion_receipts", 1).size)
    }

    @Test
    fun persistedCounterOwnershipAndErrorsSurviveWithoutRecalculation() = withSchema13 { driver ->
        driver.execute(null,
            "INSERT OR REPLACE INTO full_access_state VALUES (1, 17, 1, 'ERROR', 'cached error', 123)", 0).value
        val before = driver.rows("SELECT * FROM full_access_state", 6)
        val history = driver.rows("SELECT * FROM completed_workouts ORDER BY id", 7)

        WorkoutDatabase.Schema.migrate(driver, 13, 14)

        assertEquals(before, driver.rows("SELECT * FROM full_access_state", 6))
        assertEquals(history, driver.rows("SELECT * FROM completed_workouts ORDER BY id", 7))
        assertEquals(history.map { it[1] }.distinct().size,
            driver.rows("SELECT source_active_workout_id FROM local_workout_completion_receipts", 1).size)
    }

    @Test
    fun emptyLegacyHistoryStartsAtZeroWithoutInventingReceipts() = withSchema13 { driver ->
        driver.execute(null, "DELETE FROM completed_workouts", 0).value
        driver.execute(null, "DELETE FROM full_access_state", 0).value

        WorkoutDatabase.Schema.migrate(driver, 13, 14)

        assertEquals("0", driver.rows("SELECT completed_free_workouts FROM full_access_state", 1).single().single())
        assertTrue(driver.rows("SELECT source_active_workout_id FROM local_workout_completion_receipts", 1).isEmpty())
    }

    private fun withSchema13(block: (JdbcSqliteDriver) -> Unit) {
        assertTrue(WorkoutDatabase.Schema.version >= 14)
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/representative.db"))
        val file = Files.createTempFile("schema-14-completion-", ".db").toFile()
        resource.use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        try {
            WorkoutDatabase.Schema.migrate(driver, 10, 13)
            block(driver)
        } finally {
            driver.close()
            file.delete()
        }
    }

    private fun JdbcSqliteDriver.rows(sql: String, columns: Int): List<List<String?>> = executeQuery(
        null, sql, { cursor ->
            val rows = mutableListOf<List<String?>>()
            while (cursor.next().value) rows += List(columns) { cursor.getString(it) }
            QueryResult.Value(rows)
        }, 0, null
    ).value
}
