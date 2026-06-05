package com.jjswigut.oopsallprs.platform

import app.cash.sqldelight.db.SqlDriver
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.FoundationResult
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

interface BackupDocumentAdapter {
    suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile>
    suspend fun openBackupDocument(): FoundationResult<BackupDocument>
    suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String>
    suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile>
}

expect class BackupDocumentHandoff(context: Any? = null) : BackupDocumentAdapter {
    override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile>
    override suspend fun openBackupDocument(): FoundationResult<BackupDocument>
    override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String>
    override suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile>
}

expect class HapticFeedback(context: Any? = null) {
    fun setLogged()
    fun warning()
}

expect class PlatformClock() {
    fun now(): Instant
}
