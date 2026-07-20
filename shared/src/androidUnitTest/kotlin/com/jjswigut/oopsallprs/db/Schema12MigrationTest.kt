package com.jjswigut.oopsallprs.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals

class Schema12MigrationTest {
    @Test
    fun schema10PreferenceRowsGainFirstSetTimerDefaultWithoutLosingValues() {
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/representative.db"))
        val databaseFile = Files.createTempFile("schema-12-", ".db").toFile()
        resource.use { input ->
            Files.copy(input, databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")

        try {
            WorkoutDatabase.Schema.migrate(driver, 10, 12)

            assertEquals(
                listOf("KG", "120", "1"),
                driver.singleRow(
                    "SELECT weight_unit, default_rest_seconds, start_timer_on_first_set FROM user_preferences WHERE singleton_id = 1"
                )
            )
        } finally {
            driver.close()
            databaseFile.delete()
        }
    }

    private fun JdbcSqliteDriver.singleRow(sql: String): List<String> =
        executeQuery(
            identifier = null,
            sql = sql,
            mapper = { cursor ->
                check(cursor.next().value)
                QueryResult.Value(
                    listOf(
                        checkNotNull(cursor.getString(0)),
                        checkNotNull(cursor.getLong(1)).toString(),
                        checkNotNull(cursor.getLong(2)).toString()
                    )
                )
            },
            parameters = 0,
            binders = null
        ).value
}
