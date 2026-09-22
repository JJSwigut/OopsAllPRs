package com.jjswigut.oopsallprs.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Schema13RestTimerSurfaceMigrationTest {
    @Test
    fun schema12PreferenceRowsPreserveFirstSetChoiceAndGainRestSurfaceDefault() {
        assertTrue(WorkoutDatabase.Schema.version >= 13)
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/representative.db"))
        val databaseFile = Files.createTempFile("schema-13-rest-surface-", ".db").toFile()
        resource.use { input ->
            Files.copy(input, databaseFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")

        try {
            WorkoutDatabase.Schema.migrate(driver, 10, 12)
            driver.execute(
                identifier = null,
                sql = "UPDATE user_preferences SET start_timer_on_first_set = 0 WHERE singleton_id = 1",
                parameters = 0,
                binders = null
            )

            WorkoutDatabase.Schema.migrate(driver, 12, 13)

            assertEquals(
                listOf("0", "1"),
                driver.executeQuery(
                    identifier = null,
                    sql = "SELECT start_timer_on_first_set, rest_timer_surface_enabled FROM user_preferences WHERE singleton_id = 1",
                    mapper = { cursor ->
                        check(cursor.next().value)
                        QueryResult.Value(
                            listOf(
                                checkNotNull(cursor.getLong(0)).toString(),
                                checkNotNull(cursor.getLong(1)).toString()
                            )
                        )
                    },
                    parameters = 0,
                    binders = null
                ).value
            )
        } finally {
            driver.close()
            databaseFile.delete()
        }
    }
}
