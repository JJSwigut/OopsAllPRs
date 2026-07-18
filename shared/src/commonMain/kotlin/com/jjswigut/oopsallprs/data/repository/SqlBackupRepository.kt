package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.LoggingConfigurationIdentity
import com.jjswigut.oopsallprs.data.backup.BackupPackage
import com.jjswigut.oopsallprs.data.backup.BackupPackageCodec
import com.jjswigut.oopsallprs.data.backup.BackupSnapshotReader
import com.jjswigut.oopsallprs.data.backup.ExerciseSetDto
import com.jjswigut.oopsallprs.data.backup.LoggingConfigurationDto
import com.jjswigut.oopsallprs.data.backup.RestConfigurationDto
import com.jjswigut.oopsallprs.data.backup.toBackupMillis
import com.jjswigut.oopsallprs.data.backup.toDomain
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.BackupRestorePlan
import com.jjswigut.oopsallprs.domain.model.BackupRestoreResult
import com.jjswigut.oopsallprs.domain.model.BackupRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.SetKind
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
    private val loggingQueries get() = database.loggingConfigurationQueriesQueries

    override suspend fun createPackage(): FoundationResult<BackupPackage> = snapshotReader.createPackage()
    override suspend fun decodePackage(content: String): FoundationResult<BackupPackage> = codec.decode(content)
    override suspend fun encodePackage(pkg: BackupPackage): FoundationResult<String> = codec.encode(pkg)
    override suspend fun currentRevision(): BackupRevision = snapshotReader.revision()
    override suspend fun currentSummary(): SnapshotSummary = snapshotReader.currentSummary()

    override suspend fun restorePlan(pkg: BackupPackage): FoundationResult<BackupRestorePlan> {
        val validated = when (val result = codec.validate(pkg)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        when (val result = validateConfigurationCompatibility(validated)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> Unit
        }
        val localSummary = currentSummary()
        return foundationSuccess(
            BackupRestorePlan(
                backupSummary = validated.summary.toDomain(),
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
        when (val result = validateConfigurationCompatibility(validated)) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> Unit
        }
        val localPackage = when (val result = createPackage()) {
            is FoundationResult.Failure -> return result
            is FoundationResult.Success -> result.value
        }
        val localHadActiveWorkout = currentSummary().hasActiveWorkout
        return try {
            database.transaction {
                restoreLoggingConfigurations(validated)
                clearRestoreTables()
                restoreFaultInjector?.invoke()
                restorePreferences(validated)
                restoreExercises(validated)
                restoreUserExerciseConfigurations(validated)
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
        } catch (error: Throwable) {
            foundationFailure(FoundationError.Persistence("Restore failed: ${error.message ?: "unknown error"}"))
        }
    }

    private suspend fun validateConfigurationCompatibility(pkg: BackupPackage): FoundationResult<Unit> {
        pkg.loggingConfigurations.forEach { dto ->
            val incoming = try {
                dto.toDomain()
            } catch (error: IllegalArgumentException) {
                return foundationFailure(FoundationError.Validation(error.message ?: "Malformed logging configuration"))
            }
            val row = loggingQueries.selectLoggingConfiguration(dto.id).executeAsOneOrNull()
            if (row != null) {
                val stored = store.loggingConfiguration(LoggingConfigurationId(dto.id))
                    ?: return foundationFailure(
                        FoundationError.Validation("Stored logging configuration ${dto.id} is malformed")
                    )
                if (!LoggingConfigurationIdentity.hasSameSemanticContent(stored, incoming)) {
                    return foundationFailure(
                        FoundationError.Conflict("Logging configuration ID ${dto.id} has different content")
                    )
                }
            }
            val sameHash = loggingQueries.selectLoggingConfigurationByContentHash(dto.contentHash).executeAsOneOrNull()
            if (sameHash != null && sameHash.id != dto.id) {
                return foundationFailure(
                    FoundationError.Conflict("Logging configuration content already has identity ${sameHash.id}")
                )
            }
        }
        return foundationSuccess(Unit)
    }

    private fun restoreLoggingConfigurations(pkg: BackupPackage) {
        pkg.loggingConfigurations.forEach { dto ->
            if (loggingQueries.selectLoggingConfiguration(dto.id).executeAsOneOrNull() == null) {
                insertLoggingConfiguration(dto, dto.toDomain())
            }
        }
    }

    private fun insertLoggingConfiguration(dto: LoggingConfigurationDto, configuration: LoggingConfiguration) {
        val legacyMode = LegacyLoggingConfigurations.modeFor(configuration.id)
        val legacyKind = LegacyLoggingConfigurations.setKindFor(configuration.id)
        loggingQueries.insertLoggingConfiguration(
            id = dto.id,
            schema_version = dto.schemaVersion.toLong(),
            content_hash = dto.contentHash,
            source_code = "backup_restore",
            legacy_logging_mode = legacyMode?.name,
            legacy_set_kind = legacyKind?.name,
            legacy_had_unexplained_load = if (
                configuration.id == LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id
            ) 1L else 0L,
            created_at = 0L
        )
        dto.measures.forEachIndexed { position, measure ->
            loggingQueries.insertLoggingConfigurationMeasure(
                dto.id, position.toLong(), measure.kind, measure.requirement, measure.canonicalUnit, measure.loadRole
            )
        }
        dto.observedEffortKinds.forEachIndexed { position, effortKind ->
            loggingQueries.insertLoggingConfigurationEffortKind(dto.id, position.toLong(), effortKind)
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
        val now = Clock.System.now().toBackupMillis()
        workoutQueries.upsertUserPreferences(
            weight_unit = pkg.preferences.weightUnit,
            date_format = null,
            default_rest_seconds = pkg.preferences.defaultRestSeconds.toLong(),
            rest_sound_enabled = pkg.preferences.restSoundEnabled.flag(),
            rest_timer_surface_enabled = pkg.preferences.restTimerSurfaceEnabled.flag(),
            weight_step_lb = pkg.preferences.weightStepPounds,
            weight_step_kg = pkg.preferences.weightStepKilograms,
            android_auto_backup_allowed = 1L,
            start_timer_on_first_set = pkg.preferences.startWorkoutTimerWithFirstSet.flag(),
            created_at = now,
            updated_at = now
        )
    }

    private fun restoreExercises(pkg: BackupPackage) {
        pkg.exercises.forEach { item ->
            exerciseQueries.insertExerciseWithLoggingConfiguration(
                id = item.id,
                canonical_name = item.canonicalName,
                display_name = item.displayName,
                muscle_group = item.muscleGroup,
                equipment = item.equipment,
                movement_pattern = item.movementPattern,
                exercise_type = item.exerciseType,
                experience_level = item.experienceLevel,
                body_region = item.bodyRegion,
                is_bodyweight = item.isBodyweight.flag(),
                logging_mode = item.loggingMode,
                is_user_created = item.isUserCreated.flag(),
                created_at = item.createdAt,
                updated_at = item.updatedAt,
                archived_at = item.archivedAt,
                source_seed_version = item.sourceSeedVersion,
                user_notes = item.userNotes,
                definition_origin = item.origin,
                definition_revision = item.definitionRevision,
                seed_id = item.seedKey,
                seed_manifest_revision = item.seedManifestRevision,
                default_logging_configuration_id = item.defaultLoggingConfigurationId
            )
        }
    }

    private fun restoreUserExerciseConfigurations(pkg: BackupPackage) {
        pkg.userExerciseConfigurations.forEach { configuration ->
            loggingQueries.upsertUserExerciseConfiguration(
                exercise_catalog_id = configuration.exerciseDefinitionId,
                logging_configuration_id = configuration.loggingConfigurationId,
                based_on_definition_revision = configuration.basedOnDefinitionRevision,
                created_at = configuration.configuredAt,
                updated_at = configuration.configuredAt
            )
        }
    }

    private fun restoreRoutines(pkg: BackupPackage) {
        pkg.routines.forEach { routine ->
            routineQueries.insertRoutine(
                routine.id, routine.name, routine.sourceCompletedWorkoutId,
                routine.createdAt, routine.updatedAt, routine.archivedAt
            )
            routine.exercises.forEach { exercise ->
                routineQueries.insertRoutineExerciseWithLoggingConfiguration(
                    exercise.id, exercise.routineId, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
                    exercise.position.toLong(), exercise.groupId, exercise.groupPosition?.toLong(),
                    exercise.groupRounds?.toLong(), exercise.rest.durationSeconds.toLong(),
                    exercise.rest.autoStart.flag(), exercise.loggingConfigurationId
                )
                exercise.plannedSets.forEach { set ->
                    routineQueries.insertRoutineSetTemplateWithLoggingConfiguration(
                        set.id, set.routineExerciseId, set.position.toLong(), set.targetWeightKg,
                        set.targetReps?.toLong(), set.targetDurationMs, set.setKind,
                        set.loggingConfigurationId, set.targetDistanceMeters, set.effortTargetKind,
                        set.targetRpeTenths?.toLong(), set.targetRir?.toLong()
                    )
                }
            }
        }
    }

    private fun restoreActiveWorkout(pkg: BackupPackage) {
        val active = pkg.activeWorkout ?: return
        insertActiveWorkoutRow(
            active.id, active.startedAt, active.routineId, active.routineSnapshotName,
            active.status, active.createdAt, active.updatedAt
        )
        active.exercises.forEach { exercise ->
            insertActiveExerciseRow(
                active.id, exercise.id, exercise.exerciseCatalogId, exercise.displayNameSnapshot,
                exercise.equipmentSnapshot, exercise.isBodyweight, exercise.loggingMode, exercise.position,
                exercise.groupId, exercise.groupPosition, exercise.groupLabel, exercise.groupRounds,
                exercise.rest, exercise.loggingConfigurationId
            )
            exercise.sets.forEach { set -> insertSetRow(active.id, set) }
        }
    }

    private fun restoreCompletedWorkouts(pkg: BackupPackage) {
        pkg.completedWorkouts.forEach { workout ->
            insertActiveWorkoutRow(
                workout.sourceActiveWorkoutId, workout.startedAt, workout.routineId, null,
                "COMPLETED", workout.startedAt, workout.finishedAt
            )
            routineQueries.insertCompletedWorkout(
                workout.id, workout.sourceActiveWorkoutId, workout.startedAt, workout.finishedAt,
                workout.durationMs, workout.routineId, workout.createdAt
            )
            workout.exercises.forEach { exercise ->
                val firstSet = exercise.loggedSets.first()
                val kind = SetKind.valueOf(firstSet.setKind)
                insertActiveExerciseRow(
                    workout.sourceActiveWorkoutId, exercise.id, exercise.exerciseCatalogId,
                    exercise.displayNameSnapshot, null, kind == SetKind.BODYWEIGHT,
                    legacyMode(kind).name, exercise.position, null, null, null, null,
                    exercise.rest, firstSet.captureConfigurationId
                )
                exercise.loggedSets.forEach { set -> insertSetRow(workout.sourceActiveWorkoutId, set) }
            }
        }
    }

    private fun restoreSession(pkg: BackupPackage) {
        pkg.activeSession?.let { session ->
            workoutQueries.upsertSessionState(
                session.activeWorkoutId, session.startedAt, session.restEndsAt, session.restStartedAt,
                session.restOriginSetId, session.lastOpenedRoute, session.updatedAt
            )
        }
    }

    private fun restoreUx(pkg: BackupPackage) {
        pkg.activeUxSession?.let { ux ->
            workoutQueries.upsertActiveWorkoutUxSession(
                ux.activeWorkoutId, ux.focusedExerciseInstanceId, ux.focusedDraftId, ux.updatedAt
            )
        }
        pkg.activeSetDrafts.forEach { draft ->
            workoutQueries.upsertActiveSetDraftWithLoggingConfiguration(
                draft.draftId, draft.activeWorkoutId, draft.exerciseInstanceId, draft.position.toLong(),
                draft.setKind, draft.reps?.toLong(), draft.weightKg, draft.durationMs, draft.timerStartedAt,
                draft.updatedAt, draft.captureConfigurationId, draft.distanceMeters,
                draft.rpeTenths?.toLong(), draft.rir?.toLong(), draft.failureOutcome
            )
        }
    }

    private fun restoreProgress(pkg: BackupPackage) {
        pkg.personalRecords.forEach { record ->
            progressQueries.insertPersonalRecord(
                record.id, record.exerciseCatalogId, record.recordKind, record.reps?.toLong(), record.weightKg,
                record.value, record.sourceWorkoutId, record.sourceSetId, record.achievedAt, record.createdAt,
                record.metricCode, record.derivationVersion.toLong()
            )
        }
        pkg.progressPoints.forEach { point ->
            progressQueries.insertProgressPoint(
                point.id, point.exerciseCatalogId, point.sourceWorkoutId, point.sourceSetId, point.metric,
                point.value, point.weightKg, point.reps?.toLong(), point.recordedAt,
                point.metricCode, point.derivationVersion.toLong()
            )
        }
    }

    private fun restoreExports(pkg: BackupPackage) {
        pkg.exportMetadata.forEach { snapshot ->
            progressQueries.insertExportSnapshot(
                snapshot.id, snapshot.exportType, snapshot.createdAt,
                WeightUnit.valueOf(snapshot.weightUnit).name, snapshot.rowCount.toLong(), snapshot.formatVersion.toLong()
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
        workoutQueries.insertActiveWorkout(id, startedAt, routineId, routineSnapshotName, status, createdAt, updatedAt)
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
        groupId: String?,
        groupPosition: Int?,
        groupLabel: String?,
        groupRounds: Int?,
        rest: RestConfigurationDto,
        configurationId: String
    ) {
        setQueries.insertActiveExerciseWithLoggingConfiguration(
            id, activeWorkoutId, exerciseCatalogId, displayNameSnapshot, equipmentSnapshot,
            isBodyweight.flag(), position.toLong(), loggingMode, groupId, groupPosition?.toLong(),
            groupLabel, groupRounds?.toLong(), rest.durationSeconds.toLong(), rest.autoStart.flag(), configurationId
        )
    }

    private fun insertSetRow(workoutId: String, set: ExerciseSetDto) {
        setQueries.upsertExerciseSetWithLoggingConfiguration(
            set.id, workoutId, set.exerciseInstanceId, set.position.toLong(), set.setKind, set.weightKg,
            set.reps?.toLong(), set.durationMs, set.loggedAt, set.createdAt, set.updatedAt, set.editedAt,
            set.captureConfigurationId, set.distanceMeters, set.rpeTenths?.toLong(), set.rir?.toLong(),
            set.failureOutcome
        )
    }

    private fun legacyMode(kind: SetKind): ExerciseLoggingMode = when (kind) {
        SetKind.WEIGHTED -> ExerciseLoggingMode.WEIGHTED
        SetKind.BODYWEIGHT -> ExerciseLoggingMode.BODYWEIGHT
        SetKind.TIMED -> ExerciseLoggingMode.TIMED
    }

    private fun Boolean.flag(): Long = if (this) 1L else 0L
}
