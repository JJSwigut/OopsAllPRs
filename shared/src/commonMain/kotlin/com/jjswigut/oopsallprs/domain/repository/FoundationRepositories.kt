package com.jjswigut.oopsallprs.domain.repository

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.BackupConflictDecision
import com.jjswigut.oopsallprs.data.backup.BackupPackage
import com.jjswigut.oopsallprs.domain.model.BackupRestorePlan
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.BackupSyncState
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.WorkoutCompletionReceipt
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlinx.datetime.Instant

interface WorkoutRepository {
    suspend fun createActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout>
    suspend fun activeWorkout(id: FoundationId): ActiveWorkout?
    suspend fun currentActiveWorkout(): ActiveWorkout?
    suspend fun saveActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout>
    suspend fun discardActiveWorkout(id: FoundationId, now: Instant): FoundationResult<Unit>
    suspend fun finishWorkout(workout: CompletedWorkout): FoundationResult<CompletedWorkout>
    // Completion, active-state cleanup, durable receipt, and free allowance commit together.
    // Repeating a source id returns the original receipt without another allowance charge.
    suspend fun finishActiveWorkout(id: FoundationId, finishedAt: Instant): FoundationResult<WorkoutCompletionReceipt>
    suspend fun deleteCompletedWorkout(id: FoundationId, now: Instant): FoundationResult<Unit>
    suspend fun completedWorkout(id: FoundationId): CompletedWorkout?
    suspend fun completedWorkouts(): List<CompletedWorkout>
}

/** Atomically replaces one completed workout's set ledger and all derived progress. */
interface CompletedWorkoutCorrectionRepository {
    suspend fun saveCompletedWorkoutCorrection(
        workout: CompletedWorkout,
        records: List<PersonalRecord>,
        points: List<ProgressPoint>
    ): FoundationResult<CompletedWorkout>
}

interface SessionRepository {
    suspend fun load(): ActiveSessionState?
    suspend fun save(state: ActiveSessionState): FoundationResult<ActiveSessionState>
    suspend fun clear(now: Instant): FoundationResult<Unit>
}

interface ActiveWorkoutUxRepository {
    suspend fun loadUxSession(activeWorkoutId: FoundationId): ActiveWorkoutUxSession?
    suspend fun saveUxSession(session: ActiveWorkoutUxSession): FoundationResult<ActiveWorkoutUxSession>
    suspend fun loadSetDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft>
    suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft>
    suspend fun clearExerciseDrafts(activeWorkoutId: FoundationId, exerciseInstanceId: FoundationId): FoundationResult<Unit>
    suspend fun clearWorkoutUx(activeWorkoutId: FoundationId, now: Instant): FoundationResult<Unit>
}

interface SetLedgerRepository {
    suspend fun confirmSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet>
    suspend fun editLoggedSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet>
    suspend fun deleteLoggedSet(workoutId: FoundationId, setId: FoundationId, now: Instant): FoundationResult<ExerciseSet>
}

interface RoutineRepository {
    suspend fun routine(id: FoundationId): ReusableRoutine?
    suspend fun routines(): List<ReusableRoutine>
    suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine>
    suspend fun deleteRoutine(id: FoundationId, now: Instant): FoundationResult<Unit>
}

interface ExerciseRepository {
    suspend fun search(query: String): List<ExerciseCatalogItem>
    suspend fun all(): List<ExerciseCatalogItem>
    suspend fun exercise(id: FoundationId): ExerciseCatalogItem?
    suspend fun userCreatedExercises(): List<ExerciseCatalogItem>
    suspend fun saveSeedItems(items: List<ExerciseCatalogItem>, import: ExerciseSeedImport): FoundationResult<ExerciseSeedImport>
    suspend fun saveUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem>
    suspend fun updateUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem>
    suspend fun archiveUserExercise(id: FoundationId, now: Instant): FoundationResult<Unit>
}

interface LoggingConfigurationRepository {
    suspend fun loggingConfiguration(id: LoggingConfigurationId): LoggingConfiguration?
    suspend fun loggingConfigurations(): List<LoggingConfiguration>
    suspend fun saveLoggingConfiguration(configuration: LoggingConfiguration): FoundationResult<LoggingConfiguration>
}

interface UserExerciseConfigurationRepository {
    suspend fun userExerciseConfiguration(exerciseDefinitionId: FoundationId): UserExerciseConfiguration?
    suspend fun saveUserExerciseConfiguration(configuration: UserExerciseConfiguration): FoundationResult<UserExerciseConfiguration>
    suspend fun clearUserExerciseConfiguration(exerciseDefinitionId: FoundationId): FoundationResult<Unit>
}

interface PreferencesRepository {
    suspend fun weightUnit(): WeightUnit
    suspend fun setWeightUnit(unit: WeightUnit): FoundationResult<WeightUnit>
    suspend fun weightStep(unit: WeightUnit): Double
    suspend fun setWeightStep(unit: WeightUnit, step: Double): FoundationResult<Double>
    suspend fun defaultRestSeconds(): Int
    suspend fun setDefaultRestSeconds(seconds: Int): FoundationResult<Int>
    suspend fun restSoundEnabled(): Boolean
    suspend fun setRestSoundEnabled(enabled: Boolean): FoundationResult<Boolean>
    suspend fun startWorkoutTimerWithFirstSet(): Boolean
    suspend fun setStartWorkoutTimerWithFirstSet(enabled: Boolean): FoundationResult<Boolean>
    suspend fun restTimerSurfaceEnabled(): Boolean
    suspend fun setRestTimerSurfaceEnabled(enabled: Boolean): FoundationResult<Boolean>
}

interface FullAccessRepository {
    suspend fun loadFullAccess(): FullAccessState
    // Read and transform current state atomically with completion accounting; never suspend in transform.
    suspend fun updateFullAccess(transform: (FullAccessState) -> FullAccessState): FoundationResult<FullAccessState>
}

interface ProgressRepository {
    suspend fun replaceRecords(records: List<PersonalRecord>, points: List<ProgressPoint>): FoundationResult<Unit>
    suspend fun personalRecords(): List<PersonalRecord>
    suspend fun progressPoints(): List<ProgressPoint>
}

interface ExportRepository {
    suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile>
}

interface BackupRepository {
    suspend fun createPackage(): FoundationResult<BackupPackage>
    suspend fun decodePackage(content: String): FoundationResult<BackupPackage>
    suspend fun encodePackage(pkg: BackupPackage): FoundationResult<String>
    suspend fun currentRevision(): BackupRevision
    suspend fun currentSummary(): SnapshotSummary
    suspend fun restorePlan(pkg: BackupPackage): FoundationResult<BackupRestorePlan>
    // When supplied, compare the canonical local snapshot identity and replace data atomically.
    // A mismatch must return Conflict without replacing data or acknowledging sync.
    suspend fun restore(pkg: BackupPackage, expectedLocalRevision: String? = null): FoundationResult<BackupRestoreResult>
}

interface BackupSyncRepository {
    suspend fun loadSyncState(): BackupSyncState
    suspend fun saveSyncState(state: BackupSyncState): FoundationResult<BackupSyncState>
    suspend fun clearSyncState(): FoundationResult<Unit>
    suspend fun applyConflictDecision(decision: BackupConflictDecision): FoundationResult<BackupSyncState>
}
