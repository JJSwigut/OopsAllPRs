package com.jjswigut.oopsallprs.data.backup

import com.jjswigut.oopsallprs.domain.model.BackupDocument
import com.jjswigut.oopsallprs.domain.model.BackupLinkedFile
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncOutcome
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.repository.BackupSyncRepository
import com.jjswigut.oopsallprs.platform.BackupDocumentAdapter
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BackupSyncConflictPolicyTest {
    @Test
    fun localOnlyChangeWritesBackup() = runTest {
        val pkg = packageWithRevision("local-1")
        val repo = FakeBackupRepository(pkg, BackupRevision("local-2", instant(2), SnapshotSummary(workoutCount = 2)))
        val sync = FakeSyncRepository(
            BackupSyncState(
                linkedFile = LINK,
                lastBackupRevision = "local-1",
                lastLocalRevision = "local-1",
                lastOutcome = BackupSyncOutcome.CLEAN,
                updatedAt = instant(1)
            )
        )
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(pkg).successValue())

        val state = BackupSyncCoordinator(repo, sync, documents).syncNow().successValue()

        assertEquals(BackupSyncOutcome.LOCAL_WRITTEN, state.lastOutcome)
        assertEquals(1, documents.writeCount)
    }

    @Test
    fun bothChangedReportsConflictWithoutWriting() = runTest {
        val backupPackage = packageWithRevision("backup-2")
        val repo = FakeBackupRepository(packageWithRevision("local-2"), BackupRevision("local-2", instant(2), SnapshotSummary(workoutCount = 2)))
        val sync = FakeSyncRepository(
            BackupSyncState(
                linkedFile = LINK,
                lastBackupRevision = "backup-1",
                lastLocalRevision = "local-1",
                lastOutcome = BackupSyncOutcome.CLEAN,
                updatedAt = instant(1)
            )
        )
        val documents = FakeDocumentAdapter(BackupPackageCodec().encode(backupPackage).successValue())

        val state = BackupSyncCoordinator(repo, sync, documents).syncNow().successValue()

        assertEquals(BackupSyncOutcome.CONFLICT, state.lastOutcome)
        assertEquals(0, documents.writeCount)
    }

    private companion object {
        val LINK = BackupLinkedFile("backup.json", "mem://backup", "memory")
    }
}

internal class FakeBackupRepository(
    private var pkg: BackupPackage,
    private var revision: BackupRevision = BackupRevision(pkg.lastLocalRevision, pkg.createdAt.toBackupInstant(), pkg.summary.toDomain()),
    private val restoredActiveWorkoutReplaced: Boolean = false
) : BackupRepository {
    private val codec = BackupPackageCodec()
    var createPackageCount: Int = 0
    var restoreCount: Int = 0

    override suspend fun createPackage(): FoundationResult<BackupPackage> {
        createPackageCount += 1
        return foundationSuccess(pkg)
    }

    override suspend fun decodePackage(content: String): FoundationResult<BackupPackage> = codec.decode(content)
    override suspend fun encodePackage(pkg: BackupPackage): FoundationResult<String> = codec.encode(pkg)
    override suspend fun currentRevision(): BackupRevision = revision
    override suspend fun currentSummary(): SnapshotSummary = revision.summary
    override suspend fun restorePlan(pkg: BackupPackage) = foundationSuccess(
        com.jjswigut.oopsallprs.domain.model.BackupRestorePlan(pkg.summary.toDomain(), revision.summary, false)
    )
    override suspend fun restore(pkg: BackupPackage): FoundationResult<com.jjswigut.oopsallprs.domain.model.BackupRestoreResult> {
        restoreCount += 1
        return foundationSuccess(
            com.jjswigut.oopsallprs.domain.model.BackupRestoreResult(
                restoredSummary = pkg.summary.toDomain(),
                safetyBackup = this.pkg,
                activeWorkoutReplaced = restoredActiveWorkoutReplaced
            )
        )
    }
}

internal class FakeSyncRepository(
    private var state: BackupSyncState = BackupSyncState(updatedAt = instant(0))
) : BackupSyncRepository {
    override suspend fun loadSyncState(): BackupSyncState = state
    override suspend fun saveSyncState(state: BackupSyncState): FoundationResult<BackupSyncState> {
        this.state = state
        return foundationSuccess(state)
    }
    override suspend fun clearSyncState(): FoundationResult<Unit> {
        state = BackupSyncState(updatedAt = instant(0))
        return foundationSuccess(Unit)
    }
    override suspend fun applyConflictDecision(decision: com.jjswigut.oopsallprs.domain.model.BackupConflictDecision): FoundationResult<BackupSyncState> =
        foundationSuccess(state)
}

internal class FakeDocumentAdapter(
    initialContent: String,
    private val linkedFile: BackupLinkedFile = BackupLinkedFile("backup.json", "mem://backup", "memory")
) : BackupDocumentAdapter {
    var content: String = initialContent
    var createCount: Int = 0
    var openCount: Int = 0
    var readCount: Int = 0
    var writeCount: Int = 0

    override suspend fun createBackupDocument(suggestedName: String, content: String): FoundationResult<BackupLinkedFile> {
        this.content = content
        createCount += 1
        return foundationSuccess(linkedFile.copy(displayName = suggestedName))
    }

    override suspend fun openBackupDocument(): FoundationResult<BackupDocument> {
        openCount += 1
        return foundationSuccess(BackupDocument(linkedFile, content))
    }

    override suspend fun readBackup(linkedFile: BackupLinkedFile): FoundationResult<String> {
        readCount += 1
        return foundationSuccess(content)
    }

    override suspend fun writeBackup(linkedFile: BackupLinkedFile, content: String): FoundationResult<BackupLinkedFile> {
        this.content = content
        writeCount += 1
        return foundationSuccess(linkedFile)
    }
}

internal fun packageWithRevision(revision: String): BackupPackage =
    BackupPackage(
        formatVersion = BACKUP_FORMAT_VERSION,
        createdAt = instant(revision.last().digitToIntOrNull()?.toLong() ?: 1L).toBackupMillis(),
        deviceId = "test",
        lastLocalRevision = revision,
        appSchemaVersion = 8,
        summary = SnapshotSummary().toDto(),
        preferences = PreferencesSnapshotDto("POUNDS", 5.0, 2.5, 120, true),
        loggingConfigurations = emptyList(),
        userExerciseConfigurations = emptyList(),
        exercises = emptyList(),
        routines = emptyList(),
        activeWorkout = null,
        activeSession = null,
        activeUxSession = null,
        activeSetDrafts = emptyList(),
        completedWorkouts = emptyList(),
        personalRecords = emptyList(),
        progressPoints = emptyList(),
        exportMetadata = emptyList()
    )
