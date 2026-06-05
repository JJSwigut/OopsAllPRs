package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import kotlinx.datetime.Clock

class BackupSyncCoordinator(
    private val backupRepository: BackupRepository,
    private val syncRepository: BackupSyncRepository,
    private val documents: BackupDocumentAdapter?
) {
    suspend fun loadState(): BackupSyncState =
        syncRepository.loadSyncState()

    suspend fun linkNewBackup(): FoundationResult<BackupSyncState> {
        val adapter = documents ?: return failureState("Backup document picker unavailable")
        val pkg = when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        val content = when (val result = backupRepository.encodePackage(pkg)) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        val linked = when (val result = adapter.createBackupDocument(suggestedName(pkg), content)) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        return saveLinkedState(linked, pkg, BackupSyncOutcome.LOCAL_WRITTEN)
    }

    suspend fun backupNow(): FoundationResult<BackupSyncState> {
        val state = syncRepository.loadSyncState()
        val linked = state.linkedFile ?: return failureState("Link a backup file first")
        return writeLocalTo(linked, BackupSyncOutcome.LOCAL_WRITTEN)
    }

    suspend fun restoreFromFile(): FoundationResult<BackupRestoreResult> {
        val adapter = documents ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        val document = when (val result = adapter.openBackupDocument()) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        return restoreDocument(document)
    }

    suspend fun syncLinkedBackupIfAvailable(): FoundationResult<BackupSyncState> {
        val state = syncRepository.loadSyncState()
        return if (state.linkedFile == null) {
            foundationSuccess(state)
        } else {
            syncNow()
        }
    }

    suspend fun syncNow(): FoundationResult<BackupSyncState> {
        val adapter = documents ?: return failureState("Backup document picker unavailable")
        val state = syncRepository.loadSyncState()
        val linked = state.linkedFile ?: return failureState("Link a backup file first")
        val local = backupRepository.currentRevision()
        val backupContent = when (val result = adapter.readBackup(linked)) {
            is FoundationResult.Failure -> return saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.UNAVAILABLE,
                    lastError = result.error.message,
                    updatedAt = Clock.System.now()
                )
            )
            is FoundationResult.Success -> result.value
        }
        val backupPackage = when (val result = backupRepository.decodePackage(backupContent)) {
            is FoundationResult.Failure -> return saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.FAILED,
                    lastError = result.error.message,
                    updatedAt = Clock.System.now()
                )
            )
            is FoundationResult.Success -> result.value
        }
        val backup = BackupRevision(
            value = backupPackage.lastLocalRevision,
            timestamp = backupPackage.createdAt.toBackupInstant(),
            summary = backupPackage.summary.toDomain()
        )
        val localChanged = state.lastLocalRevision == null || state.lastLocalRevision != local.value
        val backupChanged = state.lastBackupRevision == null || state.lastBackupRevision != backup.value
        return when {
            !localChanged && !backupChanged -> saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.CLEAN,
                    lastError = null,
                    updatedAt = Clock.System.now()
                )
            )
            localChanged && !backupChanged -> writeLocalTo(linked, BackupSyncOutcome.LOCAL_WRITTEN)
            !localChanged && backupChanged -> saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.BACKUP_CHANGED,
                    lastBackupRevision = backup.value,
                    lastBackupTimestamp = backup.timestamp,
                    lastConflictSummary = backup.summary.displayCounts(),
                    lastError = null,
                    updatedAt = Clock.System.now()
                )
            )
            else -> saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.CONFLICT,
                    lastBackupRevision = backup.value,
                    lastBackupTimestamp = backup.timestamp,
                    lastLocalRevision = local.value,
                    lastLocalTimestamp = local.timestamp,
                    lastConflictSummary = "Local: ${local.summary.displayCounts()} Backup: ${backup.summary.displayCounts()}",
                    lastError = null,
                    updatedAt = Clock.System.now()
                )
            )
        }
    }

    suspend fun resolveConflict(decision: BackupConflictDecision): FoundationResult<BackupSyncState> {
        return when (decision) {
            BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP -> backupNow()
            BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY -> {
                val state = syncRepository.loadSyncState()
                val linked = state.linkedFile ?: return failureState("Link a backup file first")
                val adapter = documents ?: return failureState("Backup document picker unavailable")
                val content = when (val result = adapter.readBackup(linked)) {
                    is FoundationResult.Failure -> return failureState(result.error.message)
                    is FoundationResult.Success -> result.value
                }
                val pkg = when (val result = backupRepository.decodePackage(content)) {
                    is FoundationResult.Failure -> return failureState(result.error.message)
                    is FoundationResult.Success -> result.value
                }
                when (val safety = exportSafetyBackup(adapter)) {
                    is FoundationResult.Failure -> return failureState(safety.error.message)
                    is FoundationResult.Success -> Unit
                }
                when (val restore = backupRepository.restore(pkg)) {
                    is FoundationResult.Failure -> failureState(restore.error.message)
                    is FoundationResult.Success -> saveLinkedState(linked, pkg, BackupSyncOutcome.RESTORED)
                }
            }
            BackupConflictDecision.CANCEL -> {
                val state = syncRepository.loadSyncState()
                saveState(state.copy(lastOutcome = BackupSyncOutcome.CONFLICT, updatedAt = Clock.System.now()))
            }
        }
    }

    private suspend fun restoreDocument(document: BackupDocument): FoundationResult<BackupRestoreResult> {
        val pkg = when (val result = backupRepository.decodePackage(document.content)) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        val adapter = documents ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        when (val safety = exportSafetyBackup(adapter)) {
            is FoundationResult.Failure -> return foundationFailure(safety.error)
            is FoundationResult.Success -> Unit
        }
        val restored = backupRepository.restore(pkg)
        if (restored is FoundationResult.Success) {
            saveLinkedState(document.linkedFile, pkg, BackupSyncOutcome.RESTORED)
        }
        return restored
    }

    private suspend fun writeLocalTo(
        linked: BackupLinkedFile,
        outcome: BackupSyncOutcome
    ): FoundationResult<BackupSyncState> {
        val adapter = documents ?: return failureState("Backup document picker unavailable")
        val pkg = when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        val content = when (val result = backupRepository.encodePackage(pkg)) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        val updatedLink = when (val result = adapter.writeBackup(linked, content)) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        return saveLinkedState(updatedLink, pkg, outcome)
    }

    private suspend fun exportSafetyBackup(adapter: BackupDocumentAdapter): FoundationResult<BackupLinkedFile> {
        val safetyPackage = when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        val content = when (val result = backupRepository.encodePackage(safetyPackage)) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        return adapter.createBackupDocument(safetyName(safetyPackage), content)
    }

    private suspend fun saveLinkedState(
        linked: BackupLinkedFile,
        pkg: BackupPackage,
        outcome: BackupSyncOutcome
    ): FoundationResult<BackupSyncState> {
        val local = backupRepository.currentRevision()
        return saveState(
            BackupSyncState(
                linkedFile = linked,
                lastBackupRevision = pkg.lastLocalRevision,
                lastBackupTimestamp = pkg.createdAt.toBackupInstant(),
                lastLocalRevision = local.value,
                lastLocalTimestamp = local.timestamp,
                lastOutcome = outcome,
                lastError = null,
                lastConflictSummary = pkg.summary.toDomain().displayCounts(),
                updatedAt = Clock.System.now()
            )
        )
    }

    private suspend fun saveState(state: BackupSyncState): FoundationResult<BackupSyncState> =
        syncRepository.saveSyncState(state)

    private suspend fun failureState(message: String): FoundationResult<BackupSyncState> {
        val state = syncRepository.loadSyncState().copy(
            lastOutcome = BackupSyncOutcome.FAILED,
            lastError = message,
            updatedAt = Clock.System.now()
        )
        syncRepository.saveSyncState(state)
        return foundationFailure(FoundationError.Platform(message))
    }

    private fun suggestedName(pkg: BackupPackage): String =
        "oops-all-prs-backup-${pkg.createdAt}.$BACKUP_FILE_EXTENSION"

    private fun safetyName(pkg: BackupPackage): String =
        "oops-all-prs-safety-${pkg.createdAt}.$BACKUP_FILE_EXTENSION"
}
