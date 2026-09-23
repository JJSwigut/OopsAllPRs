package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.data.backup.BackupPackage
import kotlinx.datetime.Instant

data class BackupLinkedFile(
    val displayName: String,
    val providerReference: String,
    val providerReferenceKind: String
)

data class BackupDocument(
    val linkedFile: BackupLinkedFile,
    val content: String
)

data class BackupRevision(
    val value: String,
    val timestamp: Instant,
    val summary: SnapshotSummary
)

data class SnapshotSummary(
    val workoutCount: Int = 0,
    val setCount: Int = 0,
    val routineCount: Int = 0,
    val customExerciseCount: Int = 0,
    val progressRecordCount: Int = 0,
    val hasActiveWorkout: Boolean = false,
    val latestWorkoutTimestamp: Instant? = null,
    val latestUpdatedTimestamp: Instant? = null
) {
    fun displayCounts(): String =
        "$workoutCount workouts, $setCount sets, $routineCount routines, $customExerciseCount custom exercises"
}

enum class BackupSyncOutcome {
    UNLINKED,
    LINKED,
    CLEAN,
    LOCAL_WRITTEN,
    BACKUP_CHANGED,
    CONFLICT,
    RESTORED,
    UNAVAILABLE,
    FAILED
}

data class BackupSyncState(
    val linkedFile: BackupLinkedFile? = null,
    // Last successfully synchronized baselines, never merely observed conflict revisions.
    val lastBackupRevision: String? = null,
    val lastBackupTimestamp: Instant? = null,
    val lastLocalRevision: String? = null,
    val lastLocalTimestamp: Instant? = null,
    val lastOutcome: BackupSyncOutcome = BackupSyncOutcome.UNLINKED,
    val lastError: String? = null,
    val lastConflictSummary: String? = null,
    val updatedAt: Instant
)

data class BackupRestorePlan(
    val backupSummary: SnapshotSummary,
    val localSummary: SnapshotSummary,
    val requiresActiveWorkoutWarning: Boolean,
    val warnings: List<String> = emptyList()
)

data class BackupRestoreResult(
    val restoredSummary: SnapshotSummary,
    val safetyBackup: BackupPackage,
    val activeWorkoutReplaced: Boolean,
    // Captured by restore, before returning to the coordinator; may include retained immutable configurations.
    val restoredLocalRevision: BackupRevision? = null,
    val syncWarning: String? = null
)

enum class BackupConflictDecision {
    KEEP_LOCAL_OVERWRITE_BACKUP,
    RESTORE_BACKUP_AFTER_SAFETY_COPY,
    CANCEL
}

data class BackupSyncComparison(
    val localRevision: BackupRevision,
    val backupRevision: BackupRevision?,
    val state: BackupSyncState
)
