package com.jjswigut.oopsallprs.platform

import app.cash.sqldelight.db.SqlDriver
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
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
    fun schedule(
        restEndsAt: Instant,
        soundEnabled: Boolean = true,
        persistentSurfaceEnabled: Boolean = true
    ): RestAlertScheduleResult
    fun cancel()
}

enum class RestAlertScheduleResult {
    SCHEDULED,
    PERMISSION_DENIED,
    UNSUPPORTED
}

expect class RestNotificationScheduler(context: Any? = null) : RestAlertScheduler {
    override fun schedule(
        restEndsAt: Instant,
        soundEnabled: Boolean,
        persistentSurfaceEnabled: Boolean
    ): RestAlertScheduleResult
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

interface FullAccessBillingAdapter {
    suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>>
    suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot>
    suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot>
    suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot>
}

expect class FullAccessBillingHandoff(context: Any? = null) : FullAccessBillingAdapter {
    override suspend fun loadOffers(): FoundationResult<List<FullAccessStoreOffer>>
    override suspend fun refreshEntitlements(): FoundationResult<FullAccessEntitlementSnapshot>
    override suspend fun purchaseLifetimeUnlock(): FoundationResult<FullAccessEntitlementSnapshot>
    override suspend fun restorePurchases(): FoundationResult<FullAccessEntitlementSnapshot>
}

expect class HapticFeedback(context: Any? = null) {
    fun setLogged()
    fun warning()
}

expect class PlatformClock() {
    fun now(): Instant
}
