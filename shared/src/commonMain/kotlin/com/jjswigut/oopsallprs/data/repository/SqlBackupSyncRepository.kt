package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.db.Sync_state
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlBackupSyncRepository(
    private val database: WorkoutDatabase
) : BackupSyncRepository {
    private val backupQueries get() = database.backupQueriesQueries

    override suspend fun loadSyncState(): BackupSyncState =
        backupQueries.selectSyncState().executeAsOneOrNull()?.toDomain()
            ?: BackupSyncState(updatedAt = Clock.System.now())

    override suspend fun saveSyncState(state: BackupSyncState): FoundationResult<BackupSyncState> {
        backupQueries.upsertSyncState(
            linked_backup_display_name = state.linkedFile?.displayName,
            provider_reference = state.linkedFile?.providerReference,
            provider_reference_kind = state.linkedFile?.providerReferenceKind,
            last_backup_revision = state.lastBackupRevision,
            last_backup_timestamp = state.lastBackupTimestamp?.toEpochMilliseconds(),
            last_local_revision = state.lastLocalRevision,
            last_local_timestamp = state.lastLocalTimestamp?.toEpochMilliseconds(),
            last_outcome = state.lastOutcome.name,
            last_error = state.lastError,
            last_conflict_summary = state.lastConflictSummary,
            updated_at = state.updatedAt.toEpochMilliseconds()
        )
        return foundationSuccess(state)
    }

    override suspend fun clearSyncState(): FoundationResult<Unit> {
        backupQueries.clearSyncState()
        return foundationSuccess(Unit)
    }

    override suspend fun applyConflictDecision(decision: BackupConflictDecision): FoundationResult<BackupSyncState> {
        val current = loadSyncState()
        val updated = current.copy(
            lastOutcome = when (decision) {
                BackupConflictDecision.KEEP_LOCAL_OVERWRITE_BACKUP -> BackupSyncOutcome.LOCAL_WRITTEN
                BackupConflictDecision.RESTORE_BACKUP_AFTER_SAFETY_COPY -> BackupSyncOutcome.BACKUP_CHANGED
                BackupConflictDecision.CANCEL -> BackupSyncOutcome.CONFLICT
            },
            updatedAt = Clock.System.now()
        )
        return saveSyncState(updated)
    }

    private fun Sync_state.toDomain(): BackupSyncState =
        BackupSyncState(
            linkedFile = if (linked_backup_display_name != null && provider_reference != null && provider_reference_kind != null) {
                BackupLinkedFile(
                    displayName = linked_backup_display_name,
                    providerReference = provider_reference,
                    providerReferenceKind = provider_reference_kind
                )
            } else {
                null
            },
            lastBackupRevision = last_backup_revision,
            lastBackupTimestamp = last_backup_timestamp?.let(Instant::fromEpochMilliseconds),
            lastLocalRevision = last_local_revision,
            lastLocalTimestamp = last_local_timestamp?.let(Instant::fromEpochMilliseconds),
            lastOutcome = runCatching { BackupSyncOutcome.valueOf(last_outcome) }.getOrDefault(BackupSyncOutcome.FAILED),
            lastError = last_error,
            lastConflictSummary = last_conflict_summary,
            updatedAt = Instant.fromEpochMilliseconds(updated_at)
        )
}
