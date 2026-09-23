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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class BackupSyncCoordinator(
    private val backupRepository: BackupRepository,
    private val syncRepository: BackupSyncRepository,
    private val documents: BackupDocumentAdapter?
) {
    private val mutationMutex = Mutex()

    suspend fun loadState(): BackupSyncState =
        mutationMutex.withLock { syncRepository.loadSyncState() }

    suspend fun linkNewBackup(): FoundationResult<BackupSyncState> =
        mutationMutex.withLock { linkNewBackupLocked() }

    suspend fun backupNow(): FoundationResult<BackupSyncState> =
        mutationMutex.withLock { syncNowLocked(forceSnapshot = true) }

    suspend fun restoreFromFile(): FoundationResult<BackupRestoreResult> =
        mutationMutex.withLock { restoreFromFileLocked() }

    suspend fun syncLinkedBackupIfAvailable(): FoundationResult<BackupSyncState> =
        mutationMutex.withLock {
            val state = syncRepository.loadSyncState()
            if (state.linkedFile == null) foundationSuccess(state) else syncNowLocked()
        }

    suspend fun syncNow(): FoundationResult<BackupSyncState> =
        mutationMutex.withLock { syncNowLocked() }

    suspend fun resolveConflict(decision: BackupConflictDecision): FoundationResult<BackupSyncState> =
        mutationMutex.withLock { resolveConflictLocked(decision) }

    private suspend fun linkNewBackupLocked(): FoundationResult<BackupSyncState> {
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

    private suspend fun restoreFromFileLocked(): FoundationResult<BackupRestoreResult> {
        val adapter = documents ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        val document = when (val result = adapter.openBackupDocument()) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        return restoreDocument(document)
    }

    private suspend fun syncNowLocked(forceSnapshot: Boolean = false): FoundationResult<BackupSyncState> {
        val adapter = documents ?: return failureState("Backup document picker unavailable")
        val state = syncRepository.loadSyncState()
        val linked = state.linkedFile ?: return failureState("Link a backup file first")
        val backupContent = when (val result = adapter.readBackup(linked)) {
            is FoundationResult.Failure -> return saveState(
                state.copy(
                    lastOutcome = state.errorOutcome(BackupSyncOutcome.UNAVAILABLE),
                    lastError = result.error.message,
                    updatedAt = Clock.System.now()
                )
            )
            is FoundationResult.Success -> result.value
        }
        val backupPackage = when (val result = backupRepository.decodePackage(backupContent)) {
            is FoundationResult.Failure -> return saveState(
                state.copy(
                    lastOutcome = state.errorOutcome(BackupSyncOutcome.FAILED),
                    lastError = result.error.message,
                    updatedAt = Clock.System.now()
                )
            )
            is FoundationResult.Success -> result.value
        }
        val local = when (val result = localRevision()) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        val localChanged = state.lastLocalRevision == null || state.lastLocalRevision != local.value
        val backupChanged = state.lastBackupRevision == null ||
            state.lastBackupRevision != BackupSnapshotIdentity.revision(backupPackage)
        return when {
            // Older versions advanced revisions on conflict. The persisted outcome remains
            // authoritative even when those revisions now compare equal.
            state.hasUnresolvedConflict() || backupChanged -> saveRemoteChange(state, local, backupPackage)
            // A manual request saves a fresh snapshot even when the content is unchanged.
            localChanged || forceSnapshot -> writeLocalTo(linked, backupContent)
            else -> saveState(
                state.copy(
                    lastOutcome = BackupSyncOutcome.CLEAN,
                    lastError = null,
                    updatedAt = Clock.System.now()
                )
            )
        }
    }

    private suspend fun resolveConflictLocked(decision: BackupConflictDecision): FoundationResult<BackupSyncState> {
        return when (decision) {
            BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP -> {
                val linked = syncRepository.loadSyncState().linkedFile
                    ?: return failureState("Link a backup file first")
                writeLocalTo(linked, expectedRemoteContent = null)
            }
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
                val safetySnapshot = when (val safety = exportSafetyBackup(adapter)) {
                    is FoundationResult.Failure -> return failureState(safety.error)
                    is FoundationResult.Success -> safety.value
                }
                when (val restore = backupRepository.restore(
                    pkg,
                    expectedLocalRevision = BackupSnapshotIdentity.revision(safetySnapshot)
                )) {
                    is FoundationResult.Failure -> failureState(restore.error)
                    is FoundationResult.Success -> foundationSuccess(acknowledgeRestore(linked, pkg, restore.value))
                }
            }
            BackupConflictDecision.CANCEL -> {
                val state = syncRepository.loadSyncState()
                foundationSuccess(state)
            }
        }
    }

    private suspend fun restoreDocument(document: BackupDocument): FoundationResult<BackupRestoreResult> {
        val pkg = when (val result = backupRepository.decodePackage(document.content)) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        val adapter = documents ?: return foundationFailure(FoundationError.Platform("Backup document picker unavailable"))
        val safetySnapshot = when (val safety = exportSafetyBackup(adapter)) {
            is FoundationResult.Failure -> return foundationFailure(safety.error)
            is FoundationResult.Success -> safety.value
        }
        val restored = backupRepository.restore(
            pkg,
            expectedLocalRevision = BackupSnapshotIdentity.revision(safetySnapshot)
        )
        return when (restored) {
            is FoundationResult.Failure -> restored
            is FoundationResult.Success -> {
                val state = acknowledgeRestore(document.linkedFile, pkg, restored.value)
                foundationSuccess(restored.value.copy(syncWarning = state.lastError))
            }
        }
    }

    private suspend fun acknowledgeRestore(
        linked: BackupLinkedFile,
        pkg: BackupPackage,
        restored: BackupRestoreResult
    ): BackupSyncState {
        if (restored.syncWarning == null) {
            try {
                when (val saved = saveLinkedState(linked, pkg, BackupSyncOutcome.RESTORED, restored.restoredLocalRevision)) {
                    is FoundationResult.Success -> return saved.value
                    is FoundationResult.Failure -> Unit
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Data restore already committed. Report partial success rather than a false rollback.
            }
        }
        val state = BackupSyncState(
            lastOutcome = BackupSyncOutcome.RESTORED,
            lastError = restored.syncWarning ?: "Workout data was restored, but backup state could not be updated. Relink your backup file before syncing.",
            updatedAt = Clock.System.now()
        )
        try {
            saveState(state)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            // SQL restore unlinks inside its transaction, so the old file remains protected.
        }
        return state
    }

    private suspend fun writeLocalTo(
        linked: BackupLinkedFile,
        expectedRemoteContent: String?
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
        // Null is reserved for explicit keep-local. Recheck after encoding, since
        // another device may change the document while this snapshot is prepared.
        if (expectedRemoteContent != null) {
            val latestContent = when (val result = adapter.readBackup(linked)) {
                is FoundationResult.Failure -> return failureState(result.error.message)
                is FoundationResult.Success -> result.value
            }
            if (latestContent != expectedRemoteContent) {
                val state = syncRepository.loadSyncState()
                val remote = when (val result = backupRepository.decodePackage(latestContent)) {
                    is FoundationResult.Failure -> {
                        // Even unreadable replacement content must require a decision on retry.
                        when (val saved = saveState(state.copy(
                            lastOutcome = BackupSyncOutcome.CONFLICT,
                            lastConflictSummary = "The backup changed while preparing the local backup.",
                            lastError = result.error.message,
                            updatedAt = Clock.System.now()
                        ))) {
                            is FoundationResult.Failure -> return saved
                            is FoundationResult.Success -> return foundationFailure(result.error)
                        }
                    }
                    is FoundationResult.Success -> result.value
                }
                if (BackupSnapshotIdentity.revision(remote) != state.lastBackupRevision) {
                    val local = when (val result = localRevision()) {
                        is FoundationResult.Failure -> return failureState(result.error.message)
                        is FoundationResult.Success -> result.value
                    }
                    return saveRemoteChange(state, local, remote)
                }
            }
        }
        val updatedLink = when (val result = adapter.writeBackup(linked, content)) {
            is FoundationResult.Failure -> return failureState(result.error.message)
            is FoundationResult.Success -> result.value
        }
        return saveLinkedState(updatedLink, pkg, BackupSyncOutcome.LOCAL_WRITTEN)
    }

    private suspend fun exportSafetyBackup(adapter: BackupDocumentAdapter): FoundationResult<BackupPackage> {
        val safetyPackage = when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        val content = when (val result = backupRepository.encodePackage(safetyPackage)) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        when (val result = adapter.createBackupDocument(safetyName(safetyPackage), content)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> Unit
        }
        val currentPackage = when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> return foundationFailure(result.error)
            is FoundationResult.Success -> result.value
        }
        if (BackupSnapshotIdentity.revision(currentPackage) != BackupSnapshotIdentity.revision(safetyPackage)) {
            return foundationFailure(FoundationError.Conflict(
                "Local data changed while creating the safety backup. Review and retry restore."
            ))
        }
        // The repository guard must protect exactly the snapshot successfully exported,
        // not a later capture that could contain edits absent from the safety file.
        return foundationSuccess(safetyPackage)
    }

    private suspend fun saveLinkedState(
        linked: BackupLinkedFile,
        pkg: BackupPackage,
        outcome: BackupSyncOutcome,
        restoredLocalRevision: BackupRevision? = null
    ): FoundationResult<BackupSyncState> {
        // A later local revision may include edits made while the provider was saving.
        // Only the revision actually written (or restored) becomes the baseline.
        return saveState(
            BackupSyncState(
                linkedFile = linked,
                lastBackupRevision = BackupSnapshotIdentity.revision(pkg),
                lastBackupTimestamp = pkg.createdAt.toBackupInstant(),
                lastLocalRevision = restoredLocalRevision?.value ?: BackupSnapshotIdentity.revision(pkg),
                lastLocalTimestamp = restoredLocalRevision?.timestamp ?: pkg.summary.toDomain().latestUpdatedTimestamp
                    ?: pkg.createdAt.toBackupInstant(),
                lastOutcome = outcome,
                lastError = null,
                lastConflictSummary = pkg.summary.toDomain().displayCounts(),
                updatedAt = Clock.System.now()
            )
        )
    }

    private suspend fun saveState(state: BackupSyncState): FoundationResult<BackupSyncState> =
        syncRepository.saveSyncState(state)

    private suspend fun localRevision(): FoundationResult<BackupRevision> =
        when (val result = backupRepository.createPackage()) {
            is FoundationResult.Failure -> result
            is FoundationResult.Success -> foundationSuccess(BackupRevision(
                value = BackupSnapshotIdentity.revision(result.value),
                timestamp = result.value.summary.toDomain().latestUpdatedTimestamp ?: result.value.createdAt.toBackupInstant(),
                summary = result.value.summary.toDomain()
            ))
        }

    private suspend fun saveRemoteChange(
        state: BackupSyncState,
        local: BackupRevision,
        remote: BackupPackage
    ): FoundationResult<BackupSyncState> {
        val bothChanged = state.lastOutcome == BackupSyncOutcome.CONFLICT ||
            state.lastLocalRevision == null || state.lastLocalRevision != local.value
        val summary = remote.summary.toDomain().displayCounts()
        val needsBaselineUpgrade =
            (state.lastBackupRevision != null && !BackupSnapshotIdentity.isContentRevision(state.lastBackupRevision)) ||
                (state.lastLocalRevision != null && !BackupSnapshotIdentity.isContentRevision(state.lastLocalRevision))
        return saveState(state.copy(
            lastOutcome = if (bothChanged) BackupSyncOutcome.CONFLICT else BackupSyncOutcome.BACKUP_CHANGED,
            lastConflictSummary = buildString {
                if (needsBaselineUpgrade) append("Backup comparison was upgraded. Choose which copy to keep before syncing. ")
                append(if (bothChanged) "Local: ${local.summary.displayCounts()} Backup: $summary" else summary)
            },
            lastError = null,
            updatedAt = Clock.System.now()
        ))
    }

    private fun BackupSyncState.hasUnresolvedConflict(): Boolean =
        lastOutcome == BackupSyncOutcome.BACKUP_CHANGED || lastOutcome == BackupSyncOutcome.CONFLICT

    private fun BackupSyncState.errorOutcome(outcome: BackupSyncOutcome): BackupSyncOutcome =
        if (hasUnresolvedConflict()) lastOutcome else outcome

    private suspend fun failureState(message: String): FoundationResult<BackupSyncState> =
        failureState(FoundationError.Platform(message))

    private suspend fun failureState(error: FoundationError): FoundationResult<BackupSyncState> {
        val current = syncRepository.loadSyncState()
        val state = current.copy(
            lastOutcome = current.errorOutcome(
                if (error is FoundationError.Conflict) BackupSyncOutcome.CONFLICT else BackupSyncOutcome.FAILED
            ),
            lastError = error.message,
            updatedAt = Clock.System.now()
        )
        when (val saved = syncRepository.saveSyncState(state)) {
            is FoundationResult.Failure -> return if (error is FoundationError.Conflict) foundationFailure(error) else saved
            is FoundationResult.Success -> Unit
        }
        return foundationFailure(error)
    }

    private fun suggestedName(pkg: BackupPackage): String =
        "oops-all-prs-backup-${pkg.createdAt}.$BACKUP_FILE_EXTENSION"

    private fun safetyName(pkg: BackupPackage): String =
        "oops-all-prs-safety-${pkg.createdAt}.$BACKUP_FILE_EXTENSION"
}
