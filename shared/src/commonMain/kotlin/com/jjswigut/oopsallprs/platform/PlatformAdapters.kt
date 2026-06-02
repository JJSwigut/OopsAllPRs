package com.jjswigut.oopsallprs.platform

import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Instant

expect class PlatformDatabaseDriverFactory(context: Any? = null) {
    fun createDriver(): SqlDriver
}

expect class LocalSettingsStore(context: Any? = null) {
    fun getString(key: String): String?
    fun putString(key: String, value: String)
}

interface RestAlertScheduler {
    fun schedule(restEndsAt: Instant, soundEnabled: Boolean = true)
    fun cancel()
}

expect class RestNotificationScheduler(context: Any? = null) : RestAlertScheduler {
    override fun schedule(restEndsAt: Instant, soundEnabled: Boolean)
    override fun cancel()
}

expect class FileExportHandoff(context: Any? = null) {
    fun share(fileName: String, content: String)
}

expect class HapticFeedback(context: Any? = null) {
    fun setLogged()
    fun warning()
}

expect class PlatformClock() {
    fun now(): Instant
}
