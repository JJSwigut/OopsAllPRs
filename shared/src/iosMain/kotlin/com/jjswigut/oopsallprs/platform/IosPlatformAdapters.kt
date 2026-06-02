package com.jjswigut.oopsallprs.platform

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

actual class PlatformDatabaseDriverFactory actual constructor(context: Any?) {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(WorkoutDatabase.Schema, "oops_all_prs.db")
}

actual class LocalSettingsStore actual constructor(context: Any?) {
    private val values = mutableMapOf<String, String>()
    actual fun getString(key: String): String? = values[key]
    actual fun putString(key: String, value: String) {
        values[key] = value
    }
}

actual class RestNotificationScheduler actual constructor(context: Any?) : RestAlertScheduler {
    actual override fun schedule(restEndsAt: Instant, soundEnabled: Boolean) = Unit
    actual override fun cancel() = Unit
}

actual class FileExportHandoff actual constructor(context: Any?) {
    actual fun share(fileName: String, content: String) = Unit
}

actual class HapticFeedback actual constructor(context: Any?) {
    actual fun setLogged() = Unit
    actual fun warning() = Unit
}

actual class PlatformClock actual constructor() {
    actual fun now(): Instant = Clock.System.now()
}
