package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.LoggingConfigurationIdentity
import com.jjswigut.oopsallprs.data.export.EXPORT_FORMAT_VERSION
import com.jjswigut.oopsallprs.data.export.durationExportLabel
import com.jjswigut.oopsallprs.data.export.exportDurationMillis
import com.jjswigut.oopsallprs.data.export.exportValueLabel
import com.jjswigut.oopsallprs.data.export.loadRoleCode
import com.jjswigut.oopsallprs.data.export.rpeExportValue
import com.jjswigut.oopsallprs.db.Active_exercises
import com.jjswigut.oopsallprs.db.Active_session_state
import com.jjswigut.oopsallprs.db.Active_set_drafts
import com.jjswigut.oopsallprs.db.Active_workout_ux_sessions
import com.jjswigut.oopsallprs.db.Active_workouts
import com.jjswigut.oopsallprs.db.Completed_workouts
import com.jjswigut.oopsallprs.db.Exercise_catalog
import com.jjswigut.oopsallprs.db.Exercise_sets
import com.jjswigut.oopsallprs.db.Full_access_state
import com.jjswigut.oopsallprs.db.Logging_configurations
import com.jjswigut.oopsallprs.db.Personal_records
import com.jjswigut.oopsallprs.db.Progress_points
import com.jjswigut.oopsallprs.db.Routine_exercises
import com.jjswigut.oopsallprs.db.Routine_set_templates
import com.jjswigut.oopsallprs.db.Routines
import com.jjswigut.oopsallprs.db.SelectLoggedSets
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveExerciseGroupContext
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.EffortTarget
import com.jjswigut.oopsallprs.domain.model.EffortTargetKind
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionOrigin
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedKey
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportSnapshot
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.WireCode
import com.jjswigut.oopsallprs.domain.model.WorkoutStatus
import com.jjswigut.oopsallprs.domain.model.canonicalExerciseName
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.model.toLegacyLoggingConfiguration
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.CompletedWorkoutCorrectionRepository
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository
import com.jjswigut.oopsallprs.domain.repository.UserExerciseConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlFoundationStore(
    private val database: WorkoutDatabase,
    private val correctionFaultInjector: (() -> Unit)? = null
) : WorkoutRepository,
    CompletedWorkoutCorrectionRepository,
    SessionRepository,
    ActiveWorkoutUxRepository,
    SetLedgerRepository,
    RoutineRepository,
    ExerciseRepository,
    PreferencesRepository,
    FullAccessRepository,
    ProgressRepository,
    ExportRepository,
    LoggingConfigurationRepository,
    UserExerciseConfigurationRepository {

    private val workoutQueries get() = database.workoutQueriesQueries
    private val setQueries get() = database.setQueriesQueries
    private val routineQueries get() = database.routineQueriesQueries
    private val exerciseQueries get() = database.exerciseQueriesQueries
    private val progressQueries get() = database.progressQueriesQueries
    private val loggingConfigurationQueries get() = database.loggingConfigurationQueriesQueries

    init {
        database.transaction {
            LegacyLoggingConfigurations.all.forEach { configuration ->
                if (loggingConfigurationQueries.selectLoggingConfiguration(configuration.id.value).executeAsOneOrNull() == null) {
                    insertLoggingConfiguration(configuration, sourceCode = LEGACY_SOURCE_CODE, createdAt = 0L)
                }
            }
        }
    }

    override suspend fun loggingConfiguration(id: LoggingConfigurationId): LoggingConfiguration? =
        loggingConfigurationQueries.selectLoggingConfiguration(id.value).executeAsOneOrNull()
            ?.toLoggingConfigurationOrNull()

    override suspend fun loggingConfigurations(): List<LoggingConfiguration> =
        loggingConfigurationQueries.selectLoggingConfigurations().executeAsList()
            .mapNotNull { it.toLoggingConfigurationOrNull() }

    override suspend fun saveLoggingConfiguration(
        configuration: LoggingConfiguration
    ): FoundationResult<LoggingConfiguration> {
        val existingById = loggingConfigurationQueries.selectLoggingConfiguration(configuration.id.value)
            .executeAsOneOrNull()
        if (existingById != null) {
            val stored = existingById.toLoggingConfigurationOrNull()
                ?: return foundationFailure(
                    FoundationError.Persistence("Stored logging configuration ${configuration.id} is malformed")
                )
            return if (LoggingConfigurationIdentity.hasSameSemanticContent(stored, configuration)) {
                foundationSuccess(stored)
            } else {
                foundationFailure(
                    FoundationError.Conflict("Logging configuration ID ${configuration.id} already has different content")
                )
            }
        }

        val contentHash = LoggingConfigurationIdentity.contentHash(configuration)
        val duplicate = loggingConfigurationQueries.selectLoggingConfigurationByContentHash(contentHash)
            .executeAsOneOrNull()
            ?.toLoggingConfigurationOrNull()
        if (duplicate != null) return foundationSuccess(duplicate)

        return try {
            database.transaction {
                insertLoggingConfiguration(
                    configuration = configuration,
                    sourceCode = CUSTOM_SOURCE_CODE,
                    createdAt = Clock.System.now().toDbLong()
                )
            }
            foundationSuccess(configuration)
        } catch (error: Exception) {
            foundationFailure(
                FoundationError.Persistence(error.message ?: "Unable to persist logging configuration")
            )
        }
    }

    override suspend fun userExerciseConfiguration(
        exerciseDefinitionId: FoundationId
    ): UserExerciseConfiguration? {
        val row = loggingConfigurationQueries.selectUserExerciseConfiguration(exerciseDefinitionId.value)
            .executeAsOneOrNull() ?: return null
        val configuration = loggingConfiguration(LoggingConfigurationId(row.logging_configuration_id)) ?: return null
        return runCatching {
            UserExerciseConfiguration(
                exerciseDefinitionId = FoundationId(row.exercise_catalog_id),
                configuration = configuration,
                basedOnDefinitionRevision = ExerciseDefinitionRevision(row.based_on_definition_revision),
                configuredAt = row.updated_at.toInstant()
            )
        }.getOrNull()
    }

    override suspend fun saveUserExerciseConfiguration(
        configuration: UserExerciseConfiguration
    ): FoundationResult<UserExerciseConfiguration> {
        val definition = exerciseQueries.selectExerciseById(configuration.exerciseDefinitionId.value)
            .executeAsOneOrNull()
        if (definition == null || definition.archived_at != null) {
            return foundationFailure(
                FoundationError.NotFound("Exercise not found: ${configuration.exerciseDefinitionId}")
            )
        }
        val currentRevision = definition.definition_revision?.let {
            runCatching { ExerciseDefinitionRevision(it) }.getOrNull()
                ?: return foundationFailure(
                    FoundationError.Persistence("Exercise definition has an invalid persisted revision")
                )
        } ?: ExerciseDefinitionRevision(1)
        if (configuration.basedOnDefinitionRevision != currentRevision) {
            return foundationFailure(
                FoundationError.Conflict("Exercise definition changed before its default could be saved")
            )
        }
        return when (val saved = saveLoggingConfiguration(configuration.configuration)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> {
                val existing = loggingConfigurationQueries
                    .selectUserExerciseConfiguration(configuration.exerciseDefinitionId.value)
                    .executeAsOneOrNull()
                loggingConfigurationQueries.upsertUserExerciseConfiguration(
                    exercise_catalog_id = configuration.exerciseDefinitionId.value,
                    logging_configuration_id = saved.value.id.value,
                    based_on_definition_revision = configuration.basedOnDefinitionRevision.value,
                    created_at = existing?.created_at ?: configuration.configuredAt.toDbLong(),
                    updated_at = configuration.configuredAt.toDbLong()
                )
                foundationSuccess(configuration.copy(configuration = saved.value))
            }
        }
    }

    override suspend fun clearUserExerciseConfiguration(
        exerciseDefinitionId: FoundationId
    ): FoundationResult<Unit> {
        loggingConfigurationQueries.deleteUserExerciseConfiguration(exerciseDefinitionId.value)
        return foundationSuccess(Unit)
    }

    override suspend fun createActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
        currentActiveWorkout()?.let {
            return foundationFailure(FoundationError.Conflict("Only one active workout is supported"))
        }
        ensureConfigurationsStored(workout.exercises.map { it.resolvedLoggingConfiguration.configuration })
            ?.let { return foundationFailure(it) }
        firstMissingConfigurationId(workout.exercises.flatMap(ActiveExercise::sets).map(ExerciseSet::captureConfigurationId))
            ?.let { return foundationFailure(FoundationError.Validation("Unknown or malformed logging configuration: $it")) }
        saveActiveWorkoutRows(workout)
        return foundationSuccess(workout)
    }

    override suspend fun activeWorkout(id: FoundationId): ActiveWorkout? =
        workoutQueries.selectActiveWorkout(id.value).executeAsOneOrNull()
            ?.takeIf { it.status == WorkoutStatus.ACTIVE.name }
            ?.toActiveWorkoutOrNull()

    override suspend fun currentActiveWorkout(): ActiveWorkout? =
        workoutQueries.selectCurrentActiveWorkout().executeAsOneOrNull()?.toActiveWorkoutOrNull()

    override suspend fun saveActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
        ensureConfigurationsStored(workout.exercises.map { it.resolvedLoggingConfiguration.configuration })
            ?.let { return foundationFailure(it) }
        firstMissingConfigurationId(workout.exercises.flatMap(ActiveExercise::sets).map(ExerciseSet::captureConfigurationId))
            ?.let { return foundationFailure(FoundationError.Validation("Unknown or malformed logging configuration: $it")) }
        saveActiveWorkoutRows(workout)
        return foundationSuccess(workout)
    }

    override suspend fun discardActiveWorkout(id: FoundationId, now: Instant): FoundationResult<Unit> {
        database.transaction {
            setQueries.deleteSetsForWorkout(id.value)
            workoutQueries.clearActiveWorkoutDrafts(id.value)
            workoutQueries.clearActiveWorkoutUx(id.value)
            workoutQueries.deleteActiveWorkout(id.value)
        }
        return foundationSuccess(Unit)
    }

    override suspend fun finishWorkout(workout: CompletedWorkout): FoundationResult<CompletedWorkout> {
        workout.exercises.flatMap(CompletedExercise::loggedSets)
            .map(ExerciseSet::captureConfigurationId)
            .distinct()
            .firstOrNull { loggingConfiguration(it) == null }
            ?.let {
                return foundationFailure(FoundationError.Validation("Unknown or malformed logging configuration: $it"))
            }
        database.transaction {
            routineQueries.insertCompletedWorkout(
                id = workout.id.value,
                source_active_workout_id = workout.sourceActiveWorkoutId.value,
                started_at = workout.startedAt.toDbLong(),
                finished_at = workout.finishedAt.toDbLong(),
                duration_ms = workout.durationMs,
                routine_id = workout.routineId?.value,
                created_at = workout.createdAt.toDbLong()
            )
            val source = workoutQueries.selectActiveWorkout(workout.sourceActiveWorkoutId.value).executeAsOneOrNull()
            if (source == null) {
                workoutQueries.insertActiveWorkout(
                    id = workout.sourceActiveWorkoutId.value,
                    started_at = workout.startedAt.toDbLong(),
                    routine_id = workout.routineId?.value,
                    routine_snapshot_name = null,
                    status = WorkoutStatus.COMPLETED.name,
                    created_at = workout.startedAt.toDbLong(),
                    updated_at = workout.finishedAt.toDbLong()
                )
            } else {
                workoutQueries.updateWorkoutStatus(
                    status = WorkoutStatus.COMPLETED.name,
                    updated_at = workout.finishedAt.toDbLong(),
                    id = workout.sourceActiveWorkoutId.value
                )
            }
            workout.exercises.forEach { exercise ->
                val sourceExerciseId = exercise.loggedSets.firstOrNull()?.exerciseInstanceId ?: exercise.id
                setQueries.insertActiveExerciseWithLoggingConfiguration(
                    id = sourceExerciseId.value,
                    active_workout_id = workout.sourceActiveWorkoutId.value,
                    exercise_catalog_id = exercise.exerciseCatalogId.value,
                    display_name_snapshot = exercise.displayNameSnapshot,
                    equipment_snapshot = null,
                    is_bodyweight = exercise.loggedSets.any { it.setKind == SetKind.BODYWEIGHT || it.setKind == SetKind.TIMED }.toDbLong(),
                    position = exercise.position.value.toLong(),
                    logging_mode = exercise.loggedSets.loggingMode(exercise.loggedSets.any { it.setKind == SetKind.BODYWEIGHT }).name,
                    group_id = null,
                    group_position = null,
                    group_label = null,
                    group_rounds = null,
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong(),
                    logging_configuration_id = exercise.loggedSets.firstOrNull()?.captureConfigurationId?.value
                        ?: exercise.loggedSets
                            .firstOrNull()
                            ?.setKind
                            ?.toLegacyLoggingConfiguration(
                                hasLegacyLoad = exercise.loggedSets.any {
                                    it.setKind == SetKind.BODYWEIGHT && it.weight != null
                                }
                            )
                            ?.id
                            ?.value
                )
                exercise.loggedSets.forEach { set ->
                    setQueries.upsertSet(workout.sourceActiveWorkoutId, set)
                }
            }
            workoutQueries.clearSessionState()
            workoutQueries.clearActiveWorkoutUx(workout.sourceActiveWorkoutId.value)
            workoutQueries.clearActiveWorkoutDrafts(workout.sourceActiveWorkoutId.value)
        }
        return foundationSuccess(workout)
    }

    override suspend fun deleteCompletedWorkout(id: FoundationId, now: Instant): FoundationResult<Unit> {
        val completed = routineQueries.selectCompletedWorkout(id.value).executeAsOneOrNull()
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: $id"))
        database.transaction {
            setQueries.deleteSetsForWorkout(completed.source_active_workout_id)
            setQueries.deleteActiveExercisesForWorkout(completed.source_active_workout_id)
            routineQueries.deleteCompletedWorkout(id.value)
            workoutQueries.clearActiveWorkoutDrafts(completed.source_active_workout_id)
            workoutQueries.clearActiveWorkoutUx(completed.source_active_workout_id)
            workoutQueries.deleteActiveWorkout(completed.source_active_workout_id)
        }
        return foundationSuccess(Unit)
    }

    override suspend fun completedWorkout(id: FoundationId): CompletedWorkout? =
        routineQueries.selectCompletedWorkout(id.value).executeAsOneOrNull()?.toCompletedWorkoutOrNull()

    override suspend fun completedWorkouts(): List<CompletedWorkout> =
        routineQueries.selectCompletedWorkouts().executeAsList().mapNotNull { it.toCompletedWorkoutOrNull() }

    override suspend fun saveCompletedWorkoutCorrection(
        workout: CompletedWorkout,
        records: List<PersonalRecord>,
        points: List<ProgressPoint>
    ): FoundationResult<CompletedWorkout> {
        val existing = completedWorkout(workout.id)
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: ${workout.id}"))
        if (existing.sourceActiveWorkoutId != workout.sourceActiveWorkoutId) {
            return foundationFailure(FoundationError.Validation("Completed workout source cannot be changed"))
        }
        return try {
            database.transaction {
                setQueries.deleteSetsForWorkout(workout.sourceActiveWorkoutId.value)
                correctionFaultInjector?.invoke()
                workout.exercises.flatMap(CompletedExercise::loggedSets).forEach { set ->
                    setQueries.upsertSet(workout.sourceActiveWorkoutId, set)
                }
                progressQueries.deletePersonalRecords()
                progressQueries.deleteProgressPoints()
                records.forEach { progressQueries.insertPersonalRecord(it) }
                points.forEach { progressQueries.insertProgressPoint(it) }
            }
            foundationSuccess(workout)
        } catch (error: Throwable) {
            foundationFailure(
                FoundationError.Persistence("Workout correction failed: ${error.message ?: "unknown error"}")
            )
        }
    }

    override suspend fun load(): ActiveSessionState? =
        workoutQueries.selectSessionState().executeAsOneOrNull()?.toActiveSessionState()

    override suspend fun save(state: ActiveSessionState): FoundationResult<ActiveSessionState> {
        workoutQueries.upsertSessionState(
            active_workout_id = state.activeWorkoutId?.value,
            started_at = state.startedAt?.toDbLong(),
            rest_ends_at = state.restEndsAt?.toDbLong(),
            rest_started_at = state.restStartedAt?.toDbLong(),
            rest_origin_set_id = state.restOriginSetId?.value,
            last_opened_route = state.lastOpenedRoute,
            updated_at = state.updatedAt.toDbLong()
        )
        return foundationSuccess(state)
    }

    override suspend fun clear(now: Instant): FoundationResult<Unit> {
        workoutQueries.clearSessionState()
        return foundationSuccess(Unit)
    }

    override suspend fun loadUxSession(activeWorkoutId: FoundationId): ActiveWorkoutUxSession? =
        workoutQueries.selectActiveWorkoutUxSession(activeWorkoutId.value).executeAsOneOrNull()?.toActiveWorkoutUxSession()

    override suspend fun saveUxSession(session: ActiveWorkoutUxSession): FoundationResult<ActiveWorkoutUxSession> {
        workoutQueries.upsertActiveWorkoutUxSession(
            active_workout_id = session.activeWorkoutId.value,
            focused_exercise_instance_id = session.focusedExerciseInstanceId?.value,
            focused_draft_id = session.focusedDraftId?.value,
            updated_at = session.updatedAt.toDbLong()
        )
        return foundationSuccess(session)
    }

    override suspend fun loadSetDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft> =
        workoutQueries.selectActiveSetDrafts(activeWorkoutId.value).executeAsList().mapNotNull { it.toPersistedSetDraftOrNull() }

    override suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft> {
        if (loggingConfiguration(draft.captureConfigurationId) == null) {
            return foundationFailure(
                FoundationError.Validation("Unknown or malformed logging configuration: ${draft.captureConfigurationId}")
            )
        }
        workoutQueries.upsertActiveSetDraftWithLoggingConfiguration(
            draft_id = draft.draftId.value,
            active_workout_id = draft.activeWorkoutId.value,
            exercise_instance_id = draft.exerciseInstanceId.value,
            position = draft.position.value.toLong(),
            set_kind = draft.setKind.name,
            reps = draft.reps?.toLong(),
            weight_kg = draft.weight?.value,
            duration_ms = draft.durationMs,
            timer_started_at = draft.timerStartedAt?.toDbLong(),
            updated_at = draft.updatedAt.toDbLong(),
            logging_configuration_id = draft.captureConfigurationId.value,
            distance_m = draft.distanceMeters,
            rpe_tenths = draft.observedEffort?.rpeTenths?.toLong(),
            rir = draft.observedEffort?.rir?.toLong(),
            failure_outcome = draft.observedEffort?.failureOutcome?.wireCode?.value
        )
        return foundationSuccess(draft)
    }

    override suspend fun clearExerciseDrafts(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId
    ): FoundationResult<Unit> {
        workoutQueries.clearActiveExerciseDrafts(activeWorkoutId.value, exerciseInstanceId.value)
        return foundationSuccess(Unit)
    }

    override suspend fun clearWorkoutUx(activeWorkoutId: FoundationId, now: Instant): FoundationResult<Unit> {
        database.transaction {
            workoutQueries.clearActiveWorkoutUx(activeWorkoutId.value)
            workoutQueries.clearActiveWorkoutDrafts(activeWorkoutId.value)
        }
        return foundationSuccess(Unit)
    }

    override suspend fun confirmSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet> {
        val configuration = loggingConfiguration(set.captureConfigurationId)
            ?: return foundationFailure(
                FoundationError.Validation("Unknown or malformed logging configuration: ${set.captureConfigurationId}")
            )
        set.validateForLogging(configuration)?.let { return foundationFailure(it) }
        activeWorkout(workoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $workoutId"))
        setQueries.deleteUnloggedSetAtPosition(
            workout_id = workoutId.value,
            exercise_instance_id = set.exerciseInstanceId.value,
            position = set.position.value.toLong()
        )
        setQueries.upsertSet(workoutId, set)
        workoutQueries.updateWorkoutStatus(WorkoutStatus.ACTIVE.name, set.updatedAt.toDbLong(), workoutId.value)
        return foundationSuccess(set)
    }

    override suspend fun editLoggedSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet> {
        if (set.loggedAt == null) {
            return foundationFailure(FoundationError.Validation("Logged-set edit path requires loggedAt"))
        }
        return confirmSet(workoutId, set)
    }

    override suspend fun deleteLoggedSet(
        workoutId: FoundationId,
        setId: FoundationId,
        now: Instant
    ): FoundationResult<ExerciseSet> {
        activeWorkout(workoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $workoutId"))
        val set = setQueries.selectSetForWorkout(workoutId.value, setId.value).executeAsOneOrNull()?.toExerciseSetOrNull()
            ?: return foundationFailure(FoundationError.NotFound("Logged set not found: $setId"))
        if (!set.isLogged) {
            return foundationFailure(FoundationError.Validation("Only logged sets can be deleted"))
        }
        database.transaction {
            setQueries.deleteSetForWorkout(workoutId.value, setId.value)
            workoutQueries.updateWorkoutStatus(WorkoutStatus.ACTIVE.name, now.toDbLong(), workoutId.value)
        }
        return foundationSuccess(set)
    }

    override suspend fun routine(id: FoundationId): ReusableRoutine? =
        routineQueries.selectRoutine(id.value).executeAsOneOrNull()?.toReusableRoutineOrNull()

    override suspend fun routines(): List<ReusableRoutine> =
        routineQueries.selectRoutines().executeAsList().mapNotNull { it.toReusableRoutineOrNull() }

    override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
        if (routine.name.trim().isEmpty()) {
            return foundationFailure(FoundationError.Validation("Routine name cannot be blank"))
        }
        ensureConfigurationsStored(routine.exercises.map { it.resolvedLoggingConfiguration.configuration })
            ?.let { return foundationFailure(it) }
        database.transaction {
            routineQueries.insertRoutine(
                id = routine.id.value,
                name = routine.name,
                source_completed_workout_id = routine.sourceCompletedWorkoutId?.value,
                created_at = routine.createdAt.toDbLong(),
                updated_at = routine.updatedAt.toDbLong(),
                archived_at = routine.archivedAt?.toDbLong()
            )
            routineQueries.deleteRoutineExercises(routine.id.value)
            routine.exercises.forEach { exercise ->
                routineQueries.insertRoutineExerciseWithLoggingConfiguration(
                    id = exercise.id.value,
                    routine_id = exercise.routineId.value,
                    exercise_catalog_id = exercise.exerciseCatalogId.value,
                    display_name_snapshot = exercise.displayNameSnapshot,
                    position = exercise.position.value.toLong(),
                    group_id = exercise.groupId?.value,
                    group_position = exercise.groupPosition?.value?.toLong(),
                    group_rounds = exercise.groupRounds?.toLong(),
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong(),
                    logging_configuration_id = exercise.resolvedLoggingConfiguration.configuration.id.value
                )
                exercise.plannedSets.forEach { set ->
                    routineQueries.insertRoutineSetTemplateWithLoggingConfiguration(
                        id = set.id.value,
                        routine_exercise_id = set.routineExerciseId.value,
                        position = set.position.value.toLong(),
                        target_weight_kg = set.targetWeight?.value,
                        target_reps = set.targetReps?.toLong(),
                        target_duration_ms = set.targetDurationMs,
                        set_kind = set.setKind.name,
                        logging_configuration_id = exercise.resolvedLoggingConfiguration.configuration.id.value,
                        target_distance_m = set.targetDistanceMeters,
                        target_effort_kind = set.effortTarget?.kind?.wireCode?.value,
                        target_rpe_tenths = (set.effortTarget as? EffortTarget.Rpe)?.rpeTenths?.toLong(),
                        target_rir = (set.effortTarget as? EffortTarget.Rir)?.rir?.toLong()
                    )
                }
            }
        }
        return foundationSuccess(routine)
    }

    override suspend fun deleteRoutine(id: FoundationId, now: Instant): FoundationResult<Unit> {
        routine(id)
            ?: return foundationFailure(FoundationError.NotFound("Routine not found: $id"))
        routineQueries.archiveRoutine(now.toDbLong(), now.toDbLong(), id.value)
        return foundationSuccess(Unit)
    }

    override suspend fun search(query: String): List<ExerciseCatalogItem> {
        val canonical = canonicalExerciseName(query)
        return exerciseQueries.searchExercises("%$canonical%").executeAsList().mapNotNull { it.toExerciseCatalogItemOrNull() }
    }

    override suspend fun all(): List<ExerciseCatalogItem> =
        exerciseQueries.selectExercises().executeAsList().mapNotNull { it.toExerciseCatalogItemOrNull() }

    override suspend fun exercise(id: FoundationId): ExerciseCatalogItem? =
        exerciseQueries.selectExerciseById(id.value).executeAsOneOrNull()?.toExerciseCatalogItemOrNull()

    override suspend fun userCreatedExercises(): List<ExerciseCatalogItem> =
        exerciseQueries.selectUserCreatedExercises().executeAsList().mapNotNull { it.toExerciseCatalogItemOrNull() }

    override suspend fun saveSeedItems(
        items: List<ExerciseCatalogItem>,
        import: ExerciseSeedImport
    ): FoundationResult<ExerciseSeedImport> {
        ensureConfigurationsStored(items.map(ExerciseCatalogItem::defaultLoggingConfiguration))
            ?.let { return foundationFailure(it) }
        database.transaction {
            items.forEach { item ->
                val existing = exerciseQueries.selectExerciseByCanonicalName(item.canonicalName).executeAsOneOrNull()
                when {
                    existing == null -> exerciseQueries.insertExercise(item)
                    !existing.is_user_created.toBooleanFlag() -> {
                        // Unknown future or malformed persisted definitions are preserved for a newer reader.
                        existing.toExerciseCatalogItemOrNull()?.let { existingItem ->
                            exerciseQueries.insertExercise(
                                item.copy(id = existingItem.id, createdAt = existingItem.createdAt)
                            )
                        }
                    }
                }
            }
            exerciseQueries.insertSeedImport(
                id = import.id.value,
                source_name = import.sourceName,
                source_hash = import.sourceHash,
                imported_at = import.importedAt.toDbLong(),
                row_count = import.rowCount.toLong(),
                rejected_row_count = import.rejectedRowCount.toLong(),
                warnings = import.warnings.joinToString("\n")
            )
        }
        return foundationSuccess(import)
    }

    override suspend fun saveUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem> {
        ensureConfigurationsStored(listOf(item.defaultLoggingConfiguration))?.let { return foundationFailure(it) }
        val existing = exerciseQueries.selectExerciseByCanonicalName(item.canonicalName).executeAsOneOrNull()
        if (existing != null && existing.id != item.id.value && existing.is_user_created.toBooleanFlag()) {
            return foundationFailure(FoundationError.Validation("Exercise already exists"))
        }
        val userItem = item.copy(
            isUserCreated = true,
            sourceSeedVersion = null,
            origin = ExerciseDefinitionOrigin.USER,
            seedKey = null
        )
        exerciseQueries.insertExercise(userItem)
        return foundationSuccess(userItem)
    }

    override suspend fun updateUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem> {
        ensureConfigurationsStored(listOf(item.defaultLoggingConfiguration))?.let { return foundationFailure(it) }
        val existing = exerciseQueries.selectExerciseById(item.id.value).executeAsOneOrNull()?.toExerciseCatalogItemOrNull()
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: ${item.id}"))
        if (!existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Seeded exercises cannot be edited"))
        }
        val canonicalOwner = exerciseQueries.selectExerciseByCanonicalName(item.canonicalName).executeAsOneOrNull()
        if (canonicalOwner != null && canonicalOwner.id != item.id.value && canonicalOwner.is_user_created.toBooleanFlag()) {
            return foundationFailure(FoundationError.Validation("Exercise already exists"))
        }
        val updated = item.copy(
            isUserCreated = true,
            createdAt = existing.createdAt,
            sourceSeedVersion = null,
            origin = ExerciseDefinitionOrigin.USER,
            seedKey = null
        )
        exerciseQueries.insertExercise(updated)
        return foundationSuccess(updated)
    }

    override suspend fun archiveUserExercise(id: FoundationId, now: Instant): FoundationResult<Unit> {
        val existing = exerciseQueries.selectExerciseById(id.value).executeAsOneOrNull()?.toExerciseCatalogItemOrNull()
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $id"))
        if (!existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Seeded exercises cannot be archived"))
        }
        exerciseQueries.archiveUserExercise(now.toDbLong(), now.toDbLong(), id.value)
        return foundationSuccess(Unit)
    }

    override suspend fun weightUnit(): WeightUnit =
        workoutQueries.selectUserPreferences().executeAsOneOrNull()?.weight_unit?.let(WeightUnit::valueOf)
            ?: WeightUnit.POUNDS

    override suspend fun setWeightUnit(unit: WeightUnit): FoundationResult<WeightUnit> {
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = unit.name,
            date_format = existing?.date_format,
            default_rest_seconds = existing?.default_rest_seconds ?: DEFAULT_REST_SECONDS,
            rest_sound_enabled = existing?.rest_sound_enabled ?: 1L,
            rest_timer_surface_enabled = existing?.rest_timer_surface_enabled ?: 1L,
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = existing?.start_timer_on_first_set ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(unit)
    }

    override suspend fun weightStep(unit: WeightUnit): Double {
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val stored = when (unit) {
            WeightUnit.POUNDS -> existing?.weight_step_lb
            WeightUnit.KILOGRAMS -> existing?.weight_step_kg
        }
        return stored
            ?.takeIf { WeightStepPreference.validate(unit, it) == null }
            ?: WeightStepPreference.defaultFor(unit)
    }

    override suspend fun setWeightStep(unit: WeightUnit, step: Double): FoundationResult<Double> {
        WeightStepPreference.validate(unit, step)?.let { return foundationFailure(it) }
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        val normalized = WeightStepPreference.normalize(step)
        workoutQueries.upsertUserPreferences(
            weight_unit = existing?.weight_unit ?: WeightUnit.POUNDS.name,
            date_format = existing?.date_format,
            default_rest_seconds = existing?.default_rest_seconds ?: DEFAULT_REST_SECONDS,
            rest_sound_enabled = existing?.rest_sound_enabled ?: 1L,
            rest_timer_surface_enabled = existing?.rest_timer_surface_enabled ?: 1L,
            weight_step_lb = if (unit == WeightUnit.POUNDS) normalized else existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = if (unit == WeightUnit.KILOGRAMS) normalized else existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = existing?.start_timer_on_first_set ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(normalized)
    }

    override suspend fun defaultRestSeconds(): Int =
        workoutQueries.selectUserPreferences().executeAsOneOrNull()?.default_rest_seconds?.toInt()
            ?: RestConfiguration.DEFAULT_SECONDS

    override suspend fun setDefaultRestSeconds(seconds: Int): FoundationResult<Int> {
        if (seconds < 0) {
            return foundationFailure(FoundationError.Validation("Default rest cannot be negative"))
        }
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = existing?.weight_unit ?: WeightUnit.POUNDS.name,
            date_format = existing?.date_format,
            default_rest_seconds = seconds.toLong(),
            rest_sound_enabled = existing?.rest_sound_enabled ?: 1L,
            rest_timer_surface_enabled = existing?.rest_timer_surface_enabled ?: 1L,
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = existing?.start_timer_on_first_set ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(seconds)
    }

    override suspend fun restSoundEnabled(): Boolean =
        workoutQueries.selectUserPreferences().executeAsOneOrNull()?.rest_sound_enabled?.toBooleanFlag() ?: true

    override suspend fun setRestSoundEnabled(enabled: Boolean): FoundationResult<Boolean> {
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = existing?.weight_unit ?: WeightUnit.POUNDS.name,
            date_format = existing?.date_format,
            default_rest_seconds = existing?.default_rest_seconds ?: DEFAULT_REST_SECONDS,
            rest_sound_enabled = enabled.toDbLong(),
            rest_timer_surface_enabled = existing?.rest_timer_surface_enabled ?: 1L,
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = existing?.start_timer_on_first_set ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(enabled)
    }

    override suspend fun restTimerSurfaceEnabled(): Boolean =
        workoutQueries.selectUserPreferences().executeAsOneOrNull()?.rest_timer_surface_enabled?.toBooleanFlag() ?: true

    override suspend fun setRestTimerSurfaceEnabled(enabled: Boolean): FoundationResult<Boolean> {
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = existing?.weight_unit ?: WeightUnit.POUNDS.name,
            date_format = existing?.date_format,
            default_rest_seconds = existing?.default_rest_seconds ?: DEFAULT_REST_SECONDS,
            rest_sound_enabled = existing?.rest_sound_enabled ?: 1L,
            rest_timer_surface_enabled = enabled.toDbLong(),
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = existing?.start_timer_on_first_set ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(enabled)
    }

    override suspend fun startWorkoutTimerWithFirstSet(): Boolean =
        workoutQueries.selectUserPreferences().executeAsOneOrNull()?.start_timer_on_first_set?.toBooleanFlag() ?: true

    override suspend fun setStartWorkoutTimerWithFirstSet(enabled: Boolean): FoundationResult<Boolean> {
        val existing = workoutQueries.selectUserPreferences().executeAsOneOrNull()
        val now = Clock.System.now()
        workoutQueries.upsertUserPreferences(
            weight_unit = existing?.weight_unit ?: WeightUnit.POUNDS.name,
            date_format = existing?.date_format,
            default_rest_seconds = existing?.default_rest_seconds ?: DEFAULT_REST_SECONDS,
            rest_sound_enabled = existing?.rest_sound_enabled ?: 1L,
            rest_timer_surface_enabled = existing?.rest_timer_surface_enabled ?: 1L,
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            start_timer_on_first_set = enabled.toDbLong(),
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(enabled)
    }

    override suspend fun loadFullAccess(): FullAccessState =
        workoutQueries.selectFullAccessState().executeAsOneOrNull()?.toFullAccessState()
            ?: FullAccessState(completedFreeWorkouts = completedWorkouts().size)

    override suspend fun saveFullAccess(state: FullAccessState): FoundationResult<FullAccessState> {
        val now = state.updatedAt ?: Clock.System.now()
        workoutQueries.upsertFullAccessState(
            completed_free_workouts = state.normalizedCompletedFreeWorkouts.toLong(),
            lifetime_active = state.lifetimeUnlocked.toDbLong(),
            store_status = state.storeStatus.name,
            last_error = state.lastError,
            updated_at = now.toDbLong()
        )
        return foundationSuccess(
            state.copy(
                completedFreeWorkouts = state.normalizedCompletedFreeWorkouts,
                updatedAt = now
            )
        )
    }

    override suspend fun replaceRecords(
        records: List<PersonalRecord>,
        points: List<ProgressPoint>
    ): FoundationResult<Unit> {
        database.transaction {
            progressQueries.deletePersonalRecords()
            progressQueries.deleteProgressPoints()
            records.forEach { record -> progressQueries.insertPersonalRecord(record) }
            points.forEach { point -> progressQueries.insertProgressPoint(point) }
        }
        return foundationSuccess(Unit)
    }

    override suspend fun personalRecords(): List<PersonalRecord> =
        progressQueries.selectPersonalRecords().executeAsList().map { it.toPersonalRecord() }

    override suspend fun progressPoints(): List<ProgressPoint> =
        progressQueries.selectProgressPoints().executeAsList().map { it.toProgressPoint() }

    override suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile> {
        val now = Clock.System.now()
        val exportConfigurations = loggingConfigurations().associateBy { it.id }
        val rows = when (type) {
            ExportType.WORKOUTS -> completedWorkouts().flatMap { workout ->
                workout.exercises.flatMap { exercise ->
                    exercise.loggedSets.map { set ->
                        listOf(
                            workout.id.value,
                            exercise.displayNameSnapshot,
                            set.reps?.toString().orEmpty(),
                            set.weight?.displayValue(unit)?.toString().orEmpty(),
                            set.durationMs?.toString().orEmpty(),
                            set.durationMs.durationExportLabel(),
                            set.setKind.name,
                            set.loggedAt?.toString().orEmpty(),
                            set.captureConfigurationId.value,
                            set.distanceMeters?.toString().orEmpty(),
                            set.observedEffort?.rpeTenths.rpeExportValue(),
                            set.observedEffort?.rir?.toString().orEmpty(),
                            set.observedEffort?.failureOutcome?.wireCode?.value.orEmpty(),
                            exportConfigurations[set.captureConfigurationId]?.loadRoleCode().orEmpty()
                        )
                    }
                }
            }
            ExportType.PERSONAL_RECORDS -> personalRecords().map { record ->
                listOf(
                    record.exerciseCatalogId.value,
                    record.recordKind.name,
                    record.reps?.toString().orEmpty(),
                    record.weight?.displayValue(unit)?.toString().orEmpty(),
                    record.exportDurationMillis(),
                    record.value.toString(),
                    record.exportValueLabel(unit),
                    record.sourceWorkoutId.value,
                    record.sourceSetId.value,
                    record.metricCode.value,
                    record.derivationVersion.toString()
                )
            }
            ExportType.EXERCISES -> all().map { item ->
                listOf(
                    item.displayName, item.muscleGroup, item.equipment, item.exerciseType,
                    item.loggingMode.name, item.isUserCreated.toString(),
                    item.defaultLoggingConfiguration.id.value,
                    item.defaultLoggingConfiguration.loadRoleCode()
                )
            }
            ExportType.ROUTINES -> routines().map { routine ->
                listOf(
                    routine.id.value,
                    routine.name,
                    routine.exercises.size.toString(),
                    routine.exercises.map { it.resolvedLoggingConfiguration.configuration.id.value }.distinct().joinToString("|"),
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { it.targetDistanceMeters }.joinToString("|"),
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { (it.effortTarget as? EffortTarget.Rpe)?.rpeTenths }.joinToString("|") { it.rpeExportValue() },
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { (it.effortTarget as? EffortTarget.Rir)?.rir }.joinToString("|"),
                    "",
                    routine.exercises.map { it.resolvedLoggingConfiguration.configuration.loadRoleCode() }.filter { it.isNotEmpty() }.distinct().joinToString("|"),
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { it.effortTarget?.kind?.wireCode?.value }.joinToString("|")
                )
            }
        }
        val header = when (type) {
            ExportType.WORKOUTS -> listOf("workout_id", "exercise", "reps", "weight", "duration_ms", "duration_label", "kind", "logged_at", "config_id", "distance_m", "rpe", "rir", "failure_outcome", "load_role")
            ExportType.PERSONAL_RECORDS -> listOf("exercise_id", "kind", "reps", "weight", "duration_ms", "value", "value_label", "source_workout_id", "source_set_id", "metric_code", "derivation_version")
            ExportType.EXERCISES -> listOf("exercise", "muscle_group", "equipment", "type", "logging_mode", "user_created", "config_id", "load_role")
            ExportType.ROUTINES -> listOf("routine_id", "name", "exercise_count", "config_id", "distance_m", "rpe", "rir", "failure_outcome", "load_role", "effort_target")
        }
        val snapshot = ExportSnapshot(newFoundationId("export"), type, now, unit, rows.size, EXPORT_FORMAT_VERSION)
        progressQueries.insertExportSnapshot(
            id = snapshot.id.value,
            export_type = snapshot.exportType.name,
            created_at = snapshot.createdAt.toDbLong(),
            weight_unit = snapshot.weightUnit.name,
            row_count = snapshot.rowCount.toLong(),
            format_version = snapshot.formatVersion.toLong()
        )
        val csv = (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { it.csvEscaped() } }
        return foundationSuccess(
            ExportFile(
                snapshot = snapshot,
                fileName = "${type.name.lowercase()}-${now.toEpochMilliseconds()}.csv",
                content = csv
            )
        )
    }

    private fun saveActiveWorkoutRows(workout: ActiveWorkout) {
        database.transaction {
            workoutQueries.insertActiveWorkout(
                id = workout.id.value,
                started_at = workout.startedAt.toDbLong(),
                routine_id = workout.routineId?.value,
                routine_snapshot_name = workout.routineSnapshotName,
                status = workout.status.name,
                created_at = workout.createdAt.toDbLong(),
                updated_at = workout.updatedAt.toDbLong()
            )
            workout.exercises.forEach { exercise ->
                setQueries.insertActiveExerciseWithLoggingConfiguration(
                    id = exercise.id.value,
                    active_workout_id = exercise.activeWorkoutId.value,
                    exercise_catalog_id = exercise.reference.exerciseCatalogId.value,
                    display_name_snapshot = exercise.reference.displayNameSnapshot,
                    equipment_snapshot = exercise.reference.equipmentSnapshot,
                    is_bodyweight = exercise.reference.isBodyweight.toDbLong(),
                    position = exercise.position.value.toLong(),
                    logging_mode = exercise.reference.loggingMode.name,
                    group_id = exercise.groupContext?.groupId?.value,
                    group_position = exercise.groupContext?.groupPosition?.value?.toLong(),
                    group_label = exercise.groupContext?.label,
                    group_rounds = exercise.groupContext?.rounds?.toLong(),
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong(),
                    logging_configuration_id = exercise.resolvedLoggingConfiguration.configuration.id.value
                )
                exercise.sets.forEach { set -> setQueries.upsertSet(workout.id, set) }
            }
        }
    }

    private fun Active_workouts.toActiveWorkoutOrNull(): ActiveWorkout? = runCatching {
        ActiveWorkout(
            id = FoundationId(id),
            startedAt = started_at.toInstant(),
            routineId = routine_id?.let(::FoundationId),
            routineSnapshotName = routine_snapshot_name,
            exercises = activeExercisesFor(FoundationId(id)),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            status = WorkoutStatus.valueOf(status)
        )
    }.getOrNull()

    private fun activeExercisesFor(activeWorkoutId: FoundationId): List<ActiveExercise> {
        val setsByExercise = setQueries.selectSetsForWorkout(activeWorkoutId.value)
            .executeAsList()
            .map { requireNotNull(it.toExerciseSetOrNull()) }
            .groupBy { it.exerciseInstanceId }
        return setQueries.selectActiveExercises(activeWorkoutId.value)
            .executeAsList()
            .map { row -> requireNotNull(row.toActiveExerciseOrNull(setsByExercise[FoundationId(row.id)].orEmpty())) }
    }

    private fun Active_exercises.toActiveExerciseOrNull(sets: List<ExerciseSet>): ActiveExercise? = runCatching {
        val legacyMode = enumValueOrNull<ExerciseLoggingMode>(logging_mode)
        val fallbackConfiguration = legacyMode?.let(LegacyLoggingConfigurations::from)
        val configuration = requireNotNull(resolveConfiguration(logging_configuration_id, fallbackConfiguration))
        val projectedMode = legacyMode
            ?: LegacyLoggingConfigurations.modeFor(configuration.id)
            ?: if (is_bodyweight.toBooleanFlag()) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED
        ActiveExercise(
            id = FoundationId(id),
            activeWorkoutId = FoundationId(active_workout_id),
            reference = ExerciseReference(
                exerciseCatalogId = FoundationId(exercise_catalog_id),
                displayNameSnapshot = display_name_snapshot,
                isBodyweight = is_bodyweight.toBooleanFlag(),
                loggingMode = projectedMode,
                equipmentSnapshot = equipment_snapshot ?: exerciseQueries.selectExerciseById(exercise_catalog_id).executeAsOneOrNull()?.equipment,
                resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                    configuration = configuration,
                    source = configurationSource(exercise_catalog_id, configuration.id)
                )
            ),
            position = OrderedPosition(position.toInt()),
            groupContext = groupContext(),
            sets = sets.sortedBy { it.position.value },
            rest = RestConfiguration(
                durationSeconds = rest_seconds.toInt(),
                autoStart = rest_auto_start.toBooleanFlag()
            )
        )
    }.getOrNull()

    private fun Active_exercises.groupContext(): ActiveExerciseGroupContext? {
        val id = group_id ?: return null
        val position = group_position ?: return null
        val label = group_label ?: return null
        val rounds = group_rounds ?: return null
        return ActiveExerciseGroupContext(
            groupId = FoundationId(id),
            groupPosition = OrderedPosition(position.toInt()),
            label = label,
            rounds = rounds.toInt()
        )
    }

    private fun Completed_workouts.toCompletedWorkoutOrNull(): CompletedWorkout? = runCatching {
        val completedId = FoundationId(id)
        val sourceWorkoutId = FoundationId(source_active_workout_id)
        val loggedSetsByExercise = setQueries.selectLoggedSets(source_active_workout_id)
            .executeAsList()
            .map { requireNotNull(it.toExerciseSetOrNull()) }
            .groupBy { it.exerciseInstanceId }
        val exercises = setQueries.selectActiveExercises(source_active_workout_id)
            .executeAsList()
            .mapNotNull { exercise ->
                val sets = loggedSetsByExercise[FoundationId(exercise.id)].orEmpty().sortedBy { it.position.value }
                if (sets.isEmpty()) {
                    null
                } else {
                    CompletedExercise(
                        id = FoundationId(exercise.id),
                        completedWorkoutId = completedId,
                        exerciseCatalogId = FoundationId(exercise.exercise_catalog_id),
                        displayNameSnapshot = exercise.display_name_snapshot,
                        position = OrderedPosition(exercise.position.toInt()),
                        loggedSets = sets,
                        rest = RestConfiguration(
                            durationSeconds = exercise.rest_seconds.toInt(),
                            autoStart = exercise.rest_auto_start.toBooleanFlag()
                        )
                    )
                }
            }
        CompletedWorkout(
            id = completedId,
            sourceActiveWorkoutId = sourceWorkoutId,
            startedAt = started_at.toInstant(),
            finishedAt = finished_at.toInstant(),
            durationMs = duration_ms,
            routineId = routine_id?.let(::FoundationId),
            exercises = exercises,
            createdAt = created_at.toInstant()
        )
    }.getOrNull()

    private fun Exercise_sets.toExerciseSetOrNull(): ExerciseSet? = runCatching {
        val kind = enumValueOrNull<SetKind>(set_kind) ?: return null
        val configuration = resolveConfiguration(
            capture_configuration_id,
            LegacyLoggingConfigurations.from(kind, hasLegacyLoad = kind == SetKind.BODYWEIGHT && weight_kg != null)
        ) ?: return null
        ExerciseSet(
            id = FoundationId(id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = kind,
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            durationMs = duration_ms,
            loggedAt = logged_at?.toInstant(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            editedAt = edited_at?.toInstant(),
            captureConfigurationId = configuration.id,
            distanceMeters = distance_m,
            observedEffort = persistedEffort(rpe_tenths, rir, failure_outcome)
        )
    }.getOrNull()

    private fun SelectLoggedSets.toExerciseSetOrNull(): ExerciseSet? = runCatching {
        val kind = enumValueOrNull<SetKind>(set_kind) ?: return null
        val configuration = resolveConfiguration(
            capture_configuration_id,
            LegacyLoggingConfigurations.from(kind, hasLegacyLoad = kind == SetKind.BODYWEIGHT && weight_kg != null)
        ) ?: return null
        ExerciseSet(
            id = FoundationId(id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = kind,
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            durationMs = duration_ms,
            loggedAt = logged_at.toInstant(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            editedAt = edited_at?.toInstant(),
            captureConfigurationId = configuration.id,
            distanceMeters = distance_m,
            observedEffort = persistedEffort(rpe_tenths, rir, failure_outcome)
        )
    }.getOrNull()

    private fun Active_session_state.toActiveSessionState(): ActiveSessionState =
        ActiveSessionState(
            activeWorkoutId = active_workout_id?.let(::FoundationId),
            startedAt = started_at?.toInstant(),
            restEndsAt = rest_ends_at?.toInstant(),
            restStartedAt = rest_started_at?.toInstant(),
            restOriginSetId = rest_origin_set_id?.let(::FoundationId),
            lastOpenedRoute = last_opened_route,
            updatedAt = updated_at.toInstant()
        )

    private fun Active_workout_ux_sessions.toActiveWorkoutUxSession(): ActiveWorkoutUxSession =
        ActiveWorkoutUxSession(
            activeWorkoutId = FoundationId(active_workout_id),
            focusedExerciseInstanceId = focused_exercise_instance_id?.let(::FoundationId),
            focusedDraftId = focused_draft_id?.let(::FoundationId),
            updatedAt = updated_at.toInstant()
        )

    private fun Active_set_drafts.toPersistedSetDraftOrNull(): PersistedSetDraft? = runCatching {
        val kind = enumValueOrNull<SetKind>(set_kind) ?: return null
        val configuration = resolveConfiguration(
            logging_configuration_id,
            LegacyLoggingConfigurations.from(kind, hasLegacyLoad = kind == SetKind.BODYWEIGHT && weight_kg != null)
        ) ?: return null
        PersistedSetDraft(
            draftId = FoundationId(draft_id),
            activeWorkoutId = FoundationId(active_workout_id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = kind,
            reps = reps?.toInt(),
            weight = weight_kg?.let(::WeightKg),
            durationMs = duration_ms,
            timerStartedAt = timer_started_at?.toInstant(),
            updatedAt = updated_at.toInstant(),
            captureConfigurationId = configuration.id,
            distanceMeters = distance_m,
            observedEffort = persistedEffort(rpe_tenths, rir, failure_outcome)
        )
    }.getOrNull()

    private fun Routines.toReusableRoutineOrNull(): ReusableRoutine? = runCatching {
        val routineId = FoundationId(id)
        ReusableRoutine(
            id = routineId,
            name = name,
            exercises = routineQueries.selectRoutineExercises(id).executeAsList()
                .map { requireNotNull(it.toRoutineExerciseOrNull()) },
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            sourceCompletedWorkoutId = source_completed_workout_id?.let(::FoundationId),
            archivedAt = archived_at?.toInstant()
        )
    }.getOrNull()

    private fun Routine_exercises.toRoutineExerciseOrNull(): RoutineExercise? = runCatching {
        val templateRows = routineQueries.selectRoutineSetTemplates(id).executeAsList()
        val plannedSets = templateRows
            .map { requireNotNull(it.toRoutineSetTemplateOrNull()) }
        val fallbackConfiguration = plannedSets.firstOrNull()?.let { first ->
            LegacyLoggingConfigurations.from(
                first.setKind,
                hasLegacyLoad = first.setKind == SetKind.BODYWEIGHT && plannedSets.any { it.targetWeight != null }
            )
        } ?: LegacyLoggingConfigurations.weighted
        val configuration = requireNotNull(resolveConfiguration(logging_configuration_id, fallbackConfiguration))
        templateRows.forEach { template ->
            template.logging_configuration_id?.let { persistedId ->
                val templateConfiguration = requireNotNull(resolveConfiguration(persistedId, null))
                require(templateConfiguration.id == configuration.id) {
                    "Routine template configuration must match its exercise snapshot"
                }
            }
        }
        RoutineExercise(
            id = FoundationId(id),
            routineId = FoundationId(routine_id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            displayNameSnapshot = display_name_snapshot,
            position = OrderedPosition(position.toInt()),
            groupId = group_id?.let(::FoundationId),
            groupPosition = group_position?.let { OrderedPosition(it.toInt()) },
            groupRounds = group_rounds?.toInt(),
            plannedSets = plannedSets,
            rest = RestConfiguration(
                durationSeconds = rest_seconds.toInt(),
                autoStart = rest_auto_start.toBooleanFlag()
            ),
            resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                configuration = configuration,
                source = configurationSource(exercise_catalog_id, configuration.id)
            )
        )
    }.getOrNull()

    private fun Routine_set_templates.toRoutineSetTemplateOrNull(): RoutineSetTemplate? = runCatching {
        RoutineSetTemplate(
            id = FoundationId(id),
            routineExerciseId = FoundationId(routine_exercise_id),
            position = OrderedPosition(position.toInt()),
            targetWeight = target_weight_kg?.let(::WeightKg),
            targetReps = target_reps?.toInt(),
            targetDurationMs = target_duration_ms,
            setKind = requireNotNull(enumValueOrNull<SetKind>(set_kind)),
            targetDistanceMeters = target_distance_m,
            effortTarget = persistedEffortTarget(target_effort_kind, target_rpe_tenths, target_rir)
        )
    }.getOrNull()

    private fun Exercise_catalog.toExerciseCatalogItemOrNull(): ExerciseCatalogItem? = runCatching {
        val storedMode = enumValueOrNull<ExerciseLoggingMode>(logging_mode)
        val configuration = requireNotNull(resolveConfiguration(
            default_logging_configuration_id,
            storedMode?.let(LegacyLoggingConfigurations::from)
        ))
        val origin = definition_origin?.let {
            requireNotNull(ExerciseDefinitionOrigin.fromWireCode(it))
        } ?: if (is_user_created.toBooleanFlag()) ExerciseDefinitionOrigin.USER else ExerciseDefinitionOrigin.SEED
        ExerciseCatalogItem(
            id = FoundationId(id),
            canonicalName = canonical_name,
            displayName = display_name,
            muscleGroup = muscle_group,
            equipment = equipment,
            movementPattern = movement_pattern,
            exerciseType = exercise_type,
            experienceLevel = experience_level,
            bodyRegion = body_region,
            isBodyweight = is_bodyweight.toBooleanFlag(),
            loggingMode = storedMode
                ?: LegacyLoggingConfigurations.modeFor(configuration.id)
                ?: if (is_bodyweight.toBooleanFlag()) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
            isUserCreated = is_user_created.toBooleanFlag(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            archivedAt = archived_at?.toInstant(),
            sourceSeedVersion = seed_manifest_revision ?: source_seed_version,
            userNotes = user_notes,
            origin = origin,
            definitionRevision = ExerciseDefinitionRevision(definition_revision ?: 1L),
            seedKey = when (origin) {
                ExerciseDefinitionOrigin.SEED -> ExerciseSeedKey(
                    seed_id ?: "oopsallprs_baseline:${canonicalExerciseName(canonical_name)}"
                )
                ExerciseDefinitionOrigin.USER -> null
            },
            defaultLoggingConfiguration = configuration
        )
    }.getOrNull()

    private fun Personal_records.toPersonalRecord(): PersonalRecord {
        val legacyKind = PersonalRecordKind.valueOf(record_kind)
        return PersonalRecord(
            id = FoundationId(id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            recordKind = legacyKind,
            reps = reps?.toInt(),
            weight = weight_kg?.let(::WeightKg),
            value = value_,
            sourceWorkoutId = FoundationId(source_workout_id),
            sourceSetId = FoundationId(source_set_id),
            achievedAt = achieved_at.toInstant(),
            createdAt = created_at.toInstant(),
            metricCode = validatedProgressMetricCode(requireNotNull(metric_code), legacyKind.name),
            derivationVersion = requireNotNull(derivation_version).toInt()
        )
    }

    private fun Progress_points.toProgressPoint(): ProgressPoint {
        val legacyMetric = ProgressMetric.valueOf(metric)
        return ProgressPoint(
            id = FoundationId(id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            sourceWorkoutId = FoundationId(source_workout_id),
            sourceSetId = source_set_id?.let(::FoundationId),
            metric = legacyMetric,
            value = value_,
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            recordedAt = recorded_at.toInstant(),
            metricCode = validatedProgressMetricCode(requireNotNull(metric_code), legacyMetric.name),
            derivationVersion = requireNotNull(derivation_version).toInt()
        )
    }

    private fun Full_access_state.toFullAccessState(): FullAccessState =
        FullAccessState(
            completedFreeWorkouts = completed_free_workouts.toInt().coerceAtLeast(0),
            lifetimeUnlocked = lifetime_active.toBooleanFlag(),
            storeStatus = runCatching { FullAccessStoreStatus.valueOf(store_status) }
                .getOrDefault(FullAccessStoreStatus.NOT_CHECKED),
            lastError = last_error,
            updatedAt = Instant.fromEpochMilliseconds(updated_at)
        )

    private fun SetQueriesAccessor.upsertSet(workoutId: FoundationId, set: ExerciseSet) {
        upsertExerciseSetWithLoggingConfiguration(
            id = set.id.value,
            workout_id = workoutId.value,
            exercise_instance_id = set.exerciseInstanceId.value,
            position = set.position.value.toLong(),
            set_kind = set.setKind.name,
            weight_kg = set.weight?.value,
            reps = set.reps?.toLong(),
            duration_ms = set.durationMs,
            logged_at = set.loggedAt?.toDbLong(),
            created_at = set.createdAt.toDbLong(),
            updated_at = set.updatedAt.toDbLong(),
            edited_at = set.editedAt?.toDbLong(),
            capture_configuration_id = set.captureConfigurationId.value,
            distance_m = set.distanceMeters,
            rpe_tenths = set.observedEffort?.rpeTenths?.toLong(),
            rir = set.observedEffort?.rir?.toLong(),
            failure_outcome = set.observedEffort?.failureOutcome?.wireCode?.value
        )
    }

    private fun ExerciseQueriesAccessor.insertExercise(item: ExerciseCatalogItem) {
        insertExerciseWithLoggingConfiguration(
            id = item.id.value,
            canonical_name = item.canonicalName,
            display_name = item.displayName,
            muscle_group = item.muscleGroup,
            equipment = item.equipment,
            movement_pattern = item.movementPattern,
            exercise_type = item.exerciseType,
            experience_level = item.experienceLevel,
            body_region = item.bodyRegion,
            is_bodyweight = item.isBodyweight.toDbLong(),
            logging_mode = item.loggingMode.name,
            is_user_created = item.isUserCreated.toDbLong(),
            created_at = item.createdAt.toDbLong(),
            updated_at = item.updatedAt.toDbLong(),
            archived_at = item.archivedAt?.toDbLong(),
            source_seed_version = item.sourceSeedVersion,
            user_notes = item.userNotes,
            definition_origin = item.origin.wireCode.value,
            definition_revision = item.definitionRevision.value,
            seed_id = item.seedKey?.value,
            seed_manifest_revision = item.sourceSeedVersion,
            default_logging_configuration_id = item.defaultLoggingConfiguration.id.value
        )
    }

    private fun ProgressQueriesAccessor.insertPersonalRecord(record: PersonalRecord) {
        insertPersonalRecord(
            id = record.id.value,
            exercise_catalog_id = record.exerciseCatalogId.value,
            record_kind = record.recordKind.name,
            reps = record.reps?.toLong(),
            weight_kg = record.weight?.value,
            value_ = record.value,
            source_workout_id = record.sourceWorkoutId.value,
            source_set_id = record.sourceSetId.value,
            achieved_at = record.achievedAt.toDbLong(),
            created_at = record.createdAt.toDbLong(),
            metric_code = record.metricCode.value,
            derivation_version = record.derivationVersion.toLong()
        )
    }

    private fun ProgressQueriesAccessor.insertProgressPoint(point: ProgressPoint) {
        insertProgressPoint(
            id = point.id.value,
            exercise_catalog_id = point.exerciseCatalogId.value,
            source_workout_id = point.sourceWorkoutId.value,
            source_set_id = point.sourceSetId?.value,
            metric = point.metric.name,
            value_ = point.value,
            weight_kg = point.weight?.value,
            reps = point.reps?.toLong(),
            recorded_at = point.recordedAt.toDbLong(),
            metric_code = point.metricCode.value,
            derivation_version = point.derivationVersion.toLong()
        )
    }

    private suspend fun ensureConfigurationsStored(
        configurations: List<LoggingConfiguration>
    ): FoundationError? {
        configurations.distinctBy { it.id }.forEach { configuration ->
            when (val result = saveLoggingConfiguration(configuration)) {
                is FoundationResult.Failure -> return result.error
                is FoundationResult.Success -> if (result.value.id != configuration.id) {
                    return FoundationError.Conflict(
                        "Logging configuration ${configuration.id} duplicates canonical configuration ${result.value.id}"
                    )
                }
            }
        }
        return null
    }

    private suspend fun firstMissingConfigurationId(
        ids: List<LoggingConfigurationId>
    ): LoggingConfigurationId? =
        ids.distinct().firstOrNull { loggingConfiguration(it) == null }

    private fun resolveConfiguration(
        persistedId: String?,
        legacyFallback: LoggingConfiguration?
    ): LoggingConfiguration? =
        if (persistedId == null) {
            legacyFallback
        } else {
            loggingConfigurationQueries.selectLoggingConfiguration(persistedId)
                .executeAsOneOrNull()
                ?.toLoggingConfigurationOrNull()
        }

    private fun configurationSource(
        exerciseCatalogId: String,
        configurationId: LoggingConfigurationId
    ): LoggingConfigurationSource {
        val userDefault = loggingConfigurationQueries.selectUserExerciseConfiguration(exerciseCatalogId)
            .executeAsOneOrNull()
            ?.logging_configuration_id
        if (userDefault == configurationId.value) return LoggingConfigurationSource.USER_DEFAULT
        val catalogDefault = exerciseQueries.selectExerciseById(exerciseCatalogId)
            .executeAsOneOrNull()
            ?.default_logging_configuration_id
        return if (catalogDefault == configurationId.value) {
            LoggingConfigurationSource.DEFINITION_DEFAULT
        } else LoggingConfigurationSource.WORKOUT_OVERRIDE
    }

    private fun persistedEffort(
        rpeTenths: Long?,
        rir: Long?,
        failureOutcome: String?
    ): Effort? {
        if (rpeTenths == null && rir == null && failureOutcome == null) return null
        return Effort(
            rpeTenths = rpeTenths?.toInt(),
            rir = rir?.toInt(),
            failureOutcome = failureOutcome?.let {
                requireNotNull(FailureOutcome.fromWireCode(it)) { "Unknown failure outcome: $it" }
            }
        )
    }

    private fun persistedEffortTarget(
        kindCode: String?,
        rpeTenths: Long?,
        rir: Long?
    ): EffortTarget? {
        if (kindCode == null) {
            require(rpeTenths == null && rir == null) { "Effort target values require a target kind" }
            return null
        }
        return when (requireNotNull(EffortTargetKind.fromWireCode(kindCode))) {
            EffortTargetKind.RPE -> EffortTarget.Rpe(requireNotNull(rpeTenths).toInt())
            EffortTargetKind.RIR -> EffortTarget.Rir(requireNotNull(rir).toInt())
            EffortTargetKind.TO_FAILURE -> {
                require(rpeTenths == null && rir == null)
                EffortTarget.ToFailure
            }
        }
    }

    private fun Logging_configurations.toLoggingConfigurationOrNull(): LoggingConfiguration? = runCatching {
        val measures = loggingConfigurationQueries.selectLoggingConfigurationMeasures(id)
            .executeAsList()
            .map { row ->
                val kind = requireNotNull(MeasureKind.fromWireCode(row.measure_code))
                require(row.canonical_unit_code == kind.canonicalUnit.wireCode.value) {
                    "Measure ${row.measure_code} has an incompatible canonical unit"
                }
                MeasureSpec(
                    kind = kind,
                    requirement = requireNotNull(MeasureRequirement.fromWireCode(row.requirement_code)),
                    loadRole = row.load_role_code?.let { requireNotNull(LoadRole.fromWireCode(it)) }
                )
            }
        val effortKinds = loggingConfigurationQueries.selectLoggingConfigurationEffortKinds(id)
            .executeAsList()
            .map { row -> requireNotNull(EffortKind.fromWireCode(row.effort_kind_code)) }
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId(id),
            schemaVersion = LoggingSchemaVersion(schema_version.toInt()),
            measures = measures,
            observedEffort = effortKinds.takeIf(List<EffortKind>::isNotEmpty)?.let(::ObservedEffortSpec)
        )
        require(content_hash == LoggingConfigurationIdentity.contentHash(configuration)) {
            "Logging configuration $id content hash does not match its semantic content"
        }
        configuration
    }.getOrNull()

    private fun insertLoggingConfiguration(
        configuration: LoggingConfiguration,
        sourceCode: String,
        createdAt: Long
    ) {
        val legacyMode = LegacyLoggingConfigurations.modeFor(configuration.id)
        val legacySetKind = LegacyLoggingConfigurations.setKindFor(configuration.id)
        loggingConfigurationQueries.insertLoggingConfiguration(
            id = configuration.id.value,
            schema_version = configuration.schemaVersion.value.toLong(),
            content_hash = LoggingConfigurationIdentity.contentHash(configuration),
            source_code = sourceCode,
            legacy_logging_mode = legacyMode?.name,
            legacy_set_kind = legacySetKind?.name,
            legacy_had_unexplained_load = (
                configuration.id == LegacyLoggingConfigurations.bodyweightWithUnspecifiedLoad.id
                ).toDbLong(),
            created_at = createdAt
        )
        configuration.measures.forEachIndexed { position, measure ->
            loggingConfigurationQueries.insertLoggingConfigurationMeasure(
                logging_configuration_id = configuration.id.value,
                position = position.toLong(),
                measure_code = measure.kind.wireCode.value,
                requirement_code = measure.requirement.wireCode.value,
                canonical_unit_code = measure.kind.canonicalUnit.wireCode.value,
                load_role_code = measure.loadRole?.wireCode?.value
            )
        }
        configuration.observedEffort?.kinds.orEmpty().forEachIndexed { position, effortKind ->
            loggingConfigurationQueries.insertLoggingConfigurationEffortKind(
                logging_configuration_id = configuration.id.value,
                position = position.toLong(),
                effort_kind_code = effortKind.wireCode.value
            )
        }
    }

    private fun String.csvEscaped(): String =
        if (contains(',') || contains('"') || contains('\n')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    private companion object {
        const val DEFAULT_REST_SECONDS = 120L
        const val LEGACY_SOURCE_CODE = "legacy_runtime_seed"
        const val CUSTOM_SOURCE_CODE = "user_defined"
    }
}

private typealias SetQueriesAccessor = com.jjswigut.oopsallprs.db.SetQueriesQueries
private typealias ExerciseQueriesAccessor = com.jjswigut.oopsallprs.db.ExerciseQueriesQueries
private typealias ProgressQueriesAccessor = com.jjswigut.oopsallprs.db.ProgressQueriesQueries

private fun Instant.toDbLong(): Long = toEpochMilliseconds()

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

private fun Boolean.toDbLong(): Long = if (this) 1L else 0L

private fun Long.toBooleanFlag(): Boolean = this != 0L

private fun List<ExerciseSet>.loggingMode(isBodyweight: Boolean): ExerciseLoggingMode =
    when {
        any { it.setKind == SetKind.TIMED } -> ExerciseLoggingMode.TIMED
        isBodyweight || any { it.setKind == SetKind.BODYWEIGHT } -> ExerciseLoggingMode.BODYWEIGHT
        else -> ExerciseLoggingMode.WEIGHTED
    }

private fun validatedProgressMetricCode(code: String, legacyProjection: String): WireCode {
    val metric = requireNotNull(ProgressEvidenceMetric.fromWireCode(code)) {
        "Unknown progress metric code: $code"
    }
    require(
        metric.legacyRecordKind.name == legacyProjection || metric.legacyProgressMetric.name == legacyProjection
    ) { "Progress metric code $code is incompatible with legacy projection $legacyProjection" }
    return metric.wireCode
}

private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
    enumValues<T>().firstOrNull { it.name == value }
