package com.jjswigut.oopsallprs.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class Schema15RestTimerSurfaceOptInMigrationTest {
    @Test
    fun schema14RowsRequireAnExplicitOptInForOutsideAppRestTimer() {
        assertTrue(WorkoutDatabase.Schema.version >= 15)
        val resource = checkNotNull(javaClass.getResourceAsStream("/schema-10/representative.db"))
        val file = Files.createTempFile("schema-15-rest-surface-", ".db").toFile()
        resource.use { Files.copy(it, file.toPath(), StandardCopyOption.REPLACE_EXISTING) }
        val driver = JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}")
        try {
            WorkoutDatabase.Schema.migrate(driver, 10, 14)
            driver.execute(null, "UPDATE user_preferences SET rest_timer_surface_enabled = 1 WHERE singleton_id = 1", 0).value

            WorkoutDatabase.Schema.migrate(driver, 14, 15)

            assertEquals("0", driver.executeQuery(
                null,
                "SELECT rest_timer_surface_enabled FROM user_preferences WHERE singleton_id = 1",
                { cursor ->
                    check(cursor.next().value)
                    QueryResult.Value(checkNotNull(cursor.getLong(0)).toString())
                },
                0,
                null
            ).value)
        } finally {
            driver.close()
            file.delete()
        }
    }
}
