package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.backup.BackupPackage
import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSnapshotReader
import com.jjswigut.oopsallprs.data.backup.toBackupMillis
import com.jjswigut.oopsallprs.data.backup.toDomain
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupRestorePlan
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SnapshotSummary
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.BackupRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock

class SqlBackupRepository(
    private val database: WorkoutDatabase,
    private val store: SqlFoundationStore,
    private val snapshotReader: BackupSnapshotReader = BackupSnapshotReader(
        workouts = store,
        sessions = store,
        activeUx = store,
        routines = store,
        exercises = store,
        preferences = store,
        progress = store
    ),
    private val codec: BackupPackageCodec = BackupPackageCodec(),
    private val restoreFaultInjector: (() -> Unit)? = null
) : BackupRepository {
    private val backupQueries get() = database.backupQueriesQueries
    private val workoutQueries get() = database.workoutQueriesQueries
    private val setQueries get() = database.setQueriesQueries
    private val routineQueries get() = database.routineQueriesQueries
    private val exerciseQueries get() = database.exerciseQueriesQueries
    private val progressQueries get() = database.progressQueriesQueries

    override suspend fun createPackage(): FoundationResult<BackupPackage> =
        snapshotReader.createPackage()

    override suspend fun decodePackage(content: String): FoundationResult<BackupPackage> =
        codec.decode(content)

    override suspend fun encodePackage(pkg: BackupPackage): FoundationResult<String> =
        codec.encode(pkg)

    override suspend fun currentRevision(): BackupRevision =
        snapshotReader.revision()

    override suspend fun currentSummary(): SnapshotSummary =
        snapshotReader.currentSummary()

    override suspend fun restorePlan(pkg: BackupPackage): FoundationResult<BackupRestorePlan> {
        val validated = when (val result = codec.validate(pkg)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val localSummary = currentSummary()
        val backupSummary = validated.summary.toDomain()
        return foundationSuccess(
            BackupRestorePlan(
                backupSummary = backupSummary,
                localSummary = localSummary,
                requiresActiveWorkoutWarning = localSummary.hasActiveWorkout && validated.activeWorkout != null,
                warnings = buildList {
                    if (localSummary.hasActiveWorkout && validated.activeWorkout != null) {
                        add("Current active workout will be replaced.")
                    }
                    add("A safety backup is created before local data is replaced.")
                }
            )
        )
    }

    override suspend fun restore(pkg: BackupPackage): FoundationResult<BackupRestoreResult> {
        val validated = when (val result = codec.validate(pkg)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val localPackage = when (val result = createPackage()) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val localHadActiveWorkout = currentSummary().hasActiveWorkout
        return try {
            database.transaction {
                clearRestoreTables()
                restoreFaultInjector?.invoke()
                restorePreferences(validated)
                restoreExercises(validated)
                restoreRoutines(validated)
                restoreActiveWorkout(validated)
                restoreCompletedWorkouts(validated)
                restoreSession(validated)
                restoreUx(validated)
                restoreProgress(validated)
                restoreExports(validated)
            }
            foundationSuccess(
                BackupRestoreResult(
                    restoredSummary = validated.summary.toDomain(),
                    safetyBackup = localPackage,
                    activeWorkoutReplaced = localHadActiveWorkout && validated.activeWorkout != null
                )
            )
        } catch (throwable: Throwable) {
            foundationFailure(FoundationError.Persistence("Restore failed: ${throwable.message ?: "unknown error"}"))
        }
    }

    private fun clearRestoreTables() {
        backupQueries.deleteAllActiveSetDrafts()
        backupQueries.deleteAllActiveWorkoutUx()
        backupQueries.deleteAllSessionState()
        backupQueries.deleteAllExerciseSets()
        backupQueries.deleteAllActiveExercises()
        backupQueries.deleteAllCompletedWorkouts()
        backupQueries.deleteAllActiveWorkouts()
        backupQueries.deleteAllRoutineSetTemplates()
        backupQueries.deleteAllRoutineExercises()
        backupQueries.deleteAllRoutines()
        backupQueries.deleteAllPersonalRecords()
        backupQueries.deleteAllProgressPoints()
        backupQueries.deleteAllExportSnapshots()
        backupQueries.deleteAllExerciseCatalog()
    }

    private fun restorePreferences(pkg: BackupPackage) {
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = pkg.preferences.weightUnit,
            date_format = null,
            default_rest_seconds = pkg.preferences.defaultRestSeconds.toLong(),
            rest_sound_enabled = if (pkg.preferences.restSoundEnabled) 1L else 0L,
            weight_step_lb = pkg.preferences.weightStepPounds,
            weight_step_kg = pkg.preferences.weightStepKilograms,
            android_auto_backup_allowed = 1L,
            created_at = now.toBackupMillis(),
            updated_at = now.toBackupMillis()
        )
    }

    private fun restoreExercises(pkg: BackupPackage) {
        pkg.exercises.forEach { item ->
            exerciseQueries.insertExercise(
                id = item.id,
                canonical_name = item.canonicalName,
                display_name = item.displayName,
                muscle_group = item.muscleGroup,
                equipment = item.equipment,
                movement_pattern = item.movementPattern,
                exercise_type = item.exerciseType,
                experience_level = item.experienceLevel,
                body_region = item.bodyRegion,
                is_bodyweight = if (item.isBodyweight) 1L else 0L,
                logging_mode = item.loggingMode,
                is_user_created = if (item.isUserCreated) 1L else 0L,
                created_at = item.createdAt,
                updated_at = item.updatedAt,
                archived_at = item.archivedAt,
                source_seed_version = item.sourceSeedVersion,
                user_notes = item.userNotes
            )
        }
    }

    private fun restoreRoutines(pkg: BackupPackage) {
        pkg.routines.forEach { routine ->
            routineQueries.insertRoutine(
                id = routine.id,
                name = routine.name,
                source_completed_workout_id = routine.sourceCompletedWorkoutId,
                created_at = routine.createdAt,
                updated_at = routine.updatedAt,
                archived_at = routine.archivedAt
            )
            routine.exercises.forEach { exercise ->
                routineQueries.insertRoutineExercise(
                    id = exercise.id,
                    routine_id = exercise.routineId,
                    exercise_catalog_id = exercise.exerciseCatalogId,
                    display_name_snapshot = exercise.displayNameSnapshot,
                    position = exercise.position.toLong(),
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = if (exercise.rest.autoStart) 1L else 0L
                )
                exercise.plannedSets.forEach { set ->
                    routineQueries.insertRoutineSetTemplate(
                        id = set.id,
                        routine_exercise_id = set.routineExerciseId,
                        position = set.position.toLong(),
                        target_weight_kg = set.targetWeightKg,
                        target_reps = set.targetReps?.toLong(),
                        target_duration_ms = set.targetDurationMs,
                        set_kind = set.setKind
                    )
                }
            }
        }
    }

    private fun restoreActiveWorkout(pkg: BackupPackage) {
        val active = pkg.activeWorkout ?: return
        insertActiveWorkoutRow(active.id, active.startedAt, active.routineId, active.routineSnapshotName, active.status, active.createdAt, active.updatedAt)
        active.exercises.forEach { exercise ->
            insertActiveExerciseRow(active.id, exercise.id, exercise.exerciseCatalogId, exercise.displayNameSnapshot, exercise.equipmentSnapshot, exercise.isBodyweight, exercise.loggingMode, exercise.position, exercise.rest)
            exercise.sets.forEach { set -> insertSetRow(active.id, set) }
        }
    }

    private fun restoreCompletedWorkouts(pkg: BackupPackage) {
        pkg.completedWorkouts.forEach { workout ->
            insertActiveWorkoutRow(
                id = workout.sourceActiveWorkoutId,
                startedAt = workout.startedAt,
                routineId = workout.routineId,
                routineSnapshotName = null,
                status = "COMPLETED",
                createdAt = workout.startedAt,
                updatedAt = workout.finishedAt
            )
            routineQueries.insertCompletedWorkout(
                id = workout.id,
                source_active_workout_id = workout.sourceActiveWorkoutId,
                started_at = workout.startedAt,
                finished_at = workout.finishedAt,
                duration_ms = workout.durationMs,
                routine_id = workout.routineId,
                created_at = workout.createdAt
            )
            workout.exercises.forEach { exercise ->
                insertActiveExerciseRow(
                    activeWorkoutId = workout.sourceActiveWorkoutId,
                    id = exercise.id,
                    exerciseCatalogId = exercise.exerciseCatalogId,
                    displayNameSnapshot = exercise.displayNameSnapshot,
                    equipmentSnapshot = null,
                    isBodyweight = exercise.loggedSets.any { it.setKind != "WEIGHTED" },
                    loggingMode = exercise.loggedSets.firstOrNull()?.setKind ?: "WEIGHTED",
                    position = exercise.position,
                    rest = exercise.rest
                )
                exercise.loggedSets.forEach { set -> insertSetRow(workout.sourceActiveWorkoutId, set) }
            }
        }
    }

    private fun restoreSession(pkg: BackupPackage) {
        pkg.activeSession?.let { session ->
            workoutQueries.upsertSessionState(
                active_workout_id = session.activeWorkoutId,
                started_at = session.startedAt,
                rest_ends_at = session.restEndsAt,
                rest_started_at = session.restStartedAt,
                rest_origin_set_id = session.restOriginSetId,
                last_opened_route = session.lastOpenedRoute,
                updated_at = session.updatedAt
            )
        }
    }

    private fun restoreUx(pkg: BackupPackage) {
        pkg.activeUxSession?.let { ux ->
            workoutQueries.upsertActiveWorkoutUxSession(
                active_workout_id = ux.activeWorkoutId,
                focused_exercise_instance_id = ux.focusedExerciseInstanceId,
                focused_draft_id = ux.focusedDraftId,
                updated_at = ux.updatedAt
            )
        }
        pkg.activeSetDrafts.forEach { draft ->
            workoutQueries.upsertActiveSetDraft(
                draft_id = draft.draftId,
                active_workout_id = draft.activeWorkoutId,
                exercise_instance_id = draft.exerciseInstanceId,
                position = draft.position.toLong(),
                set_kind = draft.setKind,
                reps = draft.reps?.toLong(),
                weight_kg = draft.weightKg,
                duration_ms = draft.durationMs,
                timer_started_at = draft.timerStartedAt,
                updated_at = draft.updatedAt
            )
        }
    }

    private fun restoreProgress(pkg: BackupPackage) {
        pkg.personalRecords.forEach { record ->
            progressQueries.insertPersonalRecord(
                id = record.id,
                exercise_catalog_id = record.exerciseCatalogId,
                record_kind = record.recordKind,
                reps = record.reps?.toLong(),
                weight_kg = record.weightKg,
                value_ = record.value,
                source_workout_id = record.sourceWorkoutId,
                source_set_id = record.sourceSetId,
                achieved_at = record.achievedAt,
                created_at = record.createdAt
            )
        }
        pkg.progressPoints.forEach { point ->
            progressQueries.insertProgressPoint(
                id = point.id,
                exercise_catalog_id = point.exerciseCatalogId,
                source_workout_id = point.sourceWorkoutId,
                source_set_id = point.sourceSetId,
                metric = point.metric,
                value_ = point.value,
                weight_kg = point.weightKg,
                reps = point.reps?.toLong(),
                recorded_at = point.recordedAt
            )
        }
    }

    private fun restoreExports(pkg: BackupPackage) {
        pkg.exportMetadata.forEach { snapshot ->
            progressQueries.insertExportSnapshot(
                id = snapshot.id,
                export_type = snapshot.exportType,
                created_at = snapshot.createdAt,
                weight_unit = runCatching { WeightUnit.valueOf(snapshot.weightUnit).name }.getOrDefault(snapshot.weightUnit),
                row_count = snapshot.rowCount.toLong(),
                format_version = snapshot.formatVersion.toLong()
            )
        }
    }

    private fun insertActiveWorkoutRow(
        id: String,
        startedAt: Long,
        routineId: String?,
        routineSnapshotName: String?,
        status: String,
        createdAt: Long,
        updatedAt: Long
    ) {
        workoutQueries.insertActiveWorkout(
            id = id,
            started_at = startedAt,
            routine_id = routineId,
            routine_snapshot_name = routineSnapshotName,
            status = status,
            created_at = createdAt,
            updated_at = updatedAt
        )
    }

    private fun insertActiveExerciseRow(
        activeWorkoutId: String,
        id: String,
        exerciseCatalogId: String,
        displayNameSnapshot: String,
        equipmentSnapshot: String?,
        isBodyweight: Boolean,
        loggingMode: String,
        position: Int,
        rest: com.jjswigut.oopsallprs.data.backup.RestConfigurationDto
    ) {
        setQueries.insertActiveExercise(
            id = id,
            active_workout_id = activeWorkoutId,
            exercise_catalog_id = exerciseCatalogId,
            display_name_snapshot = displayNameSnapshot,
            equipment_snapshot = equipmentSnapshot,
            is_bodyweight = if (isBodyweight) 1L else 0L,
            position = position.toLong(),
            logging_mode = if (loggingMode == "TIMED") "TIMED" else if (isBodyweight) "BODYWEIGHT" else "WEIGHTED",
            rest_seconds = rest.durationSeconds.toLong(),
            rest_auto_start = if (rest.autoStart) 1L else 0L
        )
    }

    private fun insertSetRow(workoutId: String, set: com.jjswigut.oopsallprs.data.backup.ExerciseSetDto) {
        setQueries.upsertExerciseSet(
            id = set.id,
            workout_id = workoutId,
            exercise_instance_id = set.exerciseInstanceId,
            position = set.position.toLong(),
            set_kind = set.setKind,
            weight_kg = set.weightKg,
            reps = set.reps?.toLong(),
            duration_ms = set.durationMs,
            logged_at = set.loggedAt,
            created_at = set.createdAt,
            updated_at = set.updatedAt,
            edited_at = set.editedAt
        )
    }
}
