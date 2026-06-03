package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.db.Active_exercises
import com.jjswigut.oopsallprs.db.Active_session_state
import com.jjswigut.oopsallprs.db.Active_set_drafts
import com.jjswigut.oopsallprs.db.Active_workout_ux_sessions
import com.jjswigut.oopsallprs.db.Active_workouts
import com.jjswigut.oopsallprs.db.Completed_workouts
import com.jjswigut.oopsallprs.db.Exercise_catalog
import com.jjswigut.oopsallprs.db.Exercise_sets
import com.jjswigut.oopsallprs.db.Personal_records
import com.jjswigut.oopsallprs.db.Progress_points
import com.jjswigut.oopsallprs.db.Routine_exercises
import com.jjswigut.oopsallprs.db.Routine_set_templates
import com.jjswigut.oopsallprs.db.Routines
import com.jjswigut.oopsallprs.db.SelectLoggedSets
import com.jjswigut.oopsallprs.db.WorkoutDatabase
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportSnapshot
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.WorkoutStatus
import com.jjswigut.oopsallprs.domain.model.canonicalExerciseName
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SqlFoundationStore(
    private val database: WorkoutDatabase
) : WorkoutRepository,
    SessionRepository,
    ActiveWorkoutUxRepository,
    SetLedgerRepository,
    RoutineRepository,
    ExerciseRepository,
    PreferencesRepository,
    ProgressRepository,
    ExportRepository {

    private val workoutQueries get() = database.workoutQueriesQueries
    private val setQueries get() = database.setQueriesQueries
    private val routineQueries get() = database.routineQueriesQueries
    private val exerciseQueries get() = database.exerciseQueriesQueries
    private val progressQueries get() = database.progressQueriesQueries

    override suspend fun createActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
        currentActiveWorkout()?.let {
            return foundationFailure(FoundationError.Conflict("Only one active workout is supported"))
        }
        saveActiveWorkoutRows(workout)
        return foundationSuccess(workout)
    }

    override suspend fun activeWorkout(id: FoundationId): ActiveWorkout? =
        workoutQueries.selectActiveWorkout(id.value).executeAsOneOrNull()
            ?.takeIf { it.status == WorkoutStatus.ACTIVE.name }
            ?.toActiveWorkout()

    override suspend fun currentActiveWorkout(): ActiveWorkout? =
        workoutQueries.selectCurrentActiveWorkout().executeAsOneOrNull()?.toActiveWorkout()

    override suspend fun saveActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
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
                setQueries.insertActiveExercise(
                    id = sourceExerciseId.value,
                    active_workout_id = workout.sourceActiveWorkoutId.value,
                    exercise_catalog_id = exercise.exerciseCatalogId.value,
                    display_name_snapshot = exercise.displayNameSnapshot,
                    equipment_snapshot = null,
                    is_bodyweight = exercise.loggedSets.any { it.setKind == SetKind.BODYWEIGHT || it.setKind == SetKind.TIMED }.toDbLong(),
                    position = exercise.position.value.toLong(),
                    logging_mode = exercise.loggedSets.loggingMode(exercise.loggedSets.any { it.setKind == SetKind.BODYWEIGHT }).name,
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong()
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
        routineQueries.selectCompletedWorkout(id.value).executeAsOneOrNull()?.toCompletedWorkout()

    override suspend fun completedWorkouts(): List<CompletedWorkout> =
        routineQueries.selectCompletedWorkouts().executeAsList().map { it.toCompletedWorkout() }

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
        workoutQueries.selectActiveSetDrafts(activeWorkoutId.value).executeAsList().map { it.toPersistedSetDraft() }

    override suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft> {
        workoutQueries.upsertActiveSetDraft(
            draft_id = draft.draftId.value,
            active_workout_id = draft.activeWorkoutId.value,
            exercise_instance_id = draft.exerciseInstanceId.value,
            position = draft.position.value.toLong(),
            set_kind = draft.setKind.name,
            reps = draft.reps?.toLong(),
            weight_kg = draft.weight?.value,
            duration_ms = draft.durationMs,
            timer_started_at = draft.timerStartedAt?.toDbLong(),
            updated_at = draft.updatedAt.toDbLong()
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
        set.validateForLogging()?.let { return foundationFailure(it) }
        activeWorkout(workoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $workoutId"))
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
        val set = setQueries.selectSetForWorkout(workoutId.value, setId.value).executeAsOneOrNull()?.toExerciseSet()
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
        routineQueries.selectRoutine(id.value).executeAsOneOrNull()?.toReusableRoutine()

    override suspend fun routines(): List<ReusableRoutine> =
        routineQueries.selectRoutines().executeAsList().map { it.toReusableRoutine() }

    override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
        if (routine.name.trim().isEmpty()) {
            return foundationFailure(FoundationError.Validation("Routine name cannot be blank"))
        }
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
                routineQueries.insertRoutineExercise(
                    id = exercise.id.value,
                    routine_id = exercise.routineId.value,
                    exercise_catalog_id = exercise.exerciseCatalogId.value,
                    display_name_snapshot = exercise.displayNameSnapshot,
                    position = exercise.position.value.toLong(),
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong()
                )
                exercise.plannedSets.forEach { set ->
                    routineQueries.insertRoutineSetTemplate(
                        id = set.id.value,
                        routine_exercise_id = set.routineExerciseId.value,
                        position = set.position.value.toLong(),
                        target_weight_kg = set.targetWeight?.value,
                        target_reps = set.targetReps?.toLong(),
                        target_duration_ms = set.targetDurationMs,
                        set_kind = set.setKind.name
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
        return exerciseQueries.searchExercises("%$canonical%").executeAsList().map { it.toExerciseCatalogItem() }
    }

    override suspend fun all(): List<ExerciseCatalogItem> =
        exerciseQueries.selectExercises().executeAsList().map { it.toExerciseCatalogItem() }

    override suspend fun exercise(id: FoundationId): ExerciseCatalogItem? =
        exerciseQueries.selectExerciseById(id.value).executeAsOneOrNull()?.toExerciseCatalogItem()

    override suspend fun userCreatedExercises(): List<ExerciseCatalogItem> =
        exerciseQueries.selectUserCreatedExercises().executeAsList().map { it.toExerciseCatalogItem() }

    override suspend fun saveSeedItems(
        items: List<ExerciseCatalogItem>,
        import: ExerciseSeedImport
    ): FoundationResult<ExerciseSeedImport> {
        database.transaction {
            items.forEach { item ->
                val existing = exerciseQueries.selectExerciseByCanonicalName(item.canonicalName).executeAsOneOrNull()
                if (existing == null || !existing.is_user_created.toBooleanFlag()) {
                    val seedItem = existing?.toExerciseCatalogItem()?.let { existingItem ->
                        item.copy(id = existingItem.id, createdAt = existingItem.createdAt)
                    } ?: item
                    exerciseQueries.insertExercise(seedItem)
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
        val existing = exerciseQueries.selectExerciseByCanonicalName(item.canonicalName).executeAsOneOrNull()
        if (existing != null && existing.id != item.id.value && existing.is_user_created.toBooleanFlag()) {
            return foundationFailure(FoundationError.Validation("Exercise already exists"))
        }
        val userItem = item.copy(isUserCreated = true, sourceSeedVersion = null)
        exerciseQueries.insertExercise(userItem)
        return foundationSuccess(userItem)
    }

    override suspend fun updateUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem> {
        val existing = exerciseQueries.selectExerciseById(item.id.value).executeAsOneOrNull()?.toExerciseCatalogItem()
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
            sourceSeedVersion = null
        )
        exerciseQueries.insertExercise(updated)
        return foundationSuccess(updated)
    }

    override suspend fun archiveUserExercise(id: FoundationId, now: Instant): FoundationResult<Unit> {
        val existing = exerciseQueries.selectExerciseById(id.value).executeAsOneOrNull()?.toExerciseCatalogItem()
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
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
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
            weight_step_lb = if (unit == WeightUnit.POUNDS) normalized else existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = if (unit == WeightUnit.KILOGRAMS) normalized else existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
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
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
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
            weight_step_lb = existing?.weight_step_lb ?: WeightStepPreference.DEFAULT_POUNDS_STEP,
            weight_step_kg = existing?.weight_step_kg ?: WeightStepPreference.DEFAULT_KILOGRAMS_STEP,
            android_auto_backup_allowed = existing?.android_auto_backup_allowed ?: 1L,
            created_at = existing?.created_at ?: now.toDbLong(),
            updated_at = now.toDbLong()
        )
        return foundationSuccess(enabled)
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
                            set.loggedAt?.toString().orEmpty()
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
                    record.value.takeIf { record.recordKind == PersonalRecordKind.TIME }?.toLong()?.toString().orEmpty(),
                    record.value.toString(),
                    record.exportValueLabel(unit),
                    record.sourceWorkoutId.value,
                    record.sourceSetId.value
                )
            }
            ExportType.EXERCISES -> all().map { item ->
                listOf(item.displayName, item.muscleGroup, item.equipment, item.exerciseType, item.loggingMode.name, item.isUserCreated.toString())
            }
            ExportType.ROUTINES -> routines().map { routine ->
                listOf(routine.id.value, routine.name, routine.exercises.size.toString())
            }
        }
        val header = when (type) {
            ExportType.WORKOUTS -> listOf("workout_id", "exercise", "reps", "weight", "duration_ms", "duration_label", "kind", "logged_at")
            ExportType.PERSONAL_RECORDS -> listOf("exercise_id", "kind", "reps", "weight", "duration_ms", "value", "value_label", "source_workout_id", "source_set_id")
            ExportType.EXERCISES -> listOf("exercise", "muscle_group", "equipment", "type", "logging_mode", "user_created")
            ExportType.ROUTINES -> listOf("routine_id", "name", "exercise_count")
        }
        val snapshot = ExportSnapshot(newFoundationId("export"), type, now, unit, rows.size)
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
                setQueries.insertActiveExercise(
                    id = exercise.id.value,
                    active_workout_id = exercise.activeWorkoutId.value,
                    exercise_catalog_id = exercise.reference.exerciseCatalogId.value,
                    display_name_snapshot = exercise.reference.displayNameSnapshot,
                    equipment_snapshot = exercise.reference.equipmentSnapshot,
                    is_bodyweight = exercise.reference.isBodyweight.toDbLong(),
                    position = exercise.position.value.toLong(),
                    logging_mode = exercise.reference.loggingMode.name,
                    rest_seconds = exercise.rest.durationSeconds.toLong(),
                    rest_auto_start = exercise.rest.autoStart.toDbLong()
                )
                exercise.sets.forEach { set -> setQueries.upsertSet(workout.id, set) }
            }
        }
    }

    private fun Active_workouts.toActiveWorkout(): ActiveWorkout =
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

    private fun activeExercisesFor(activeWorkoutId: FoundationId): List<ActiveExercise> {
        val setsByExercise = setQueries.selectSetsForWorkout(activeWorkoutId.value)
            .executeAsList()
            .map { it.toExerciseSet() }
            .groupBy { it.exerciseInstanceId }
        return setQueries.selectActiveExercises(activeWorkoutId.value)
            .executeAsList()
            .map { row -> row.toActiveExercise(setsByExercise[FoundationId(row.id)].orEmpty()) }
    }

    private fun Active_exercises.toActiveExercise(sets: List<ExerciseSet>): ActiveExercise =
        ActiveExercise(
            id = FoundationId(id),
            activeWorkoutId = FoundationId(active_workout_id),
            reference = ExerciseReference(
                exerciseCatalogId = FoundationId(exercise_catalog_id),
                displayNameSnapshot = display_name_snapshot,
                isBodyweight = is_bodyweight.toBooleanFlag(),
                loggingMode = ExerciseLoggingMode.valueOf(logging_mode),
                equipmentSnapshot = equipment_snapshot ?: exerciseQueries.selectExerciseById(exercise_catalog_id).executeAsOneOrNull()?.equipment
            ),
            position = OrderedPosition(position.toInt()),
            sets = sets.sortedBy { it.position.value },
            rest = RestConfiguration(
                durationSeconds = rest_seconds.toInt(),
                autoStart = rest_auto_start.toBooleanFlag()
            )
        )

    private fun Completed_workouts.toCompletedWorkout(): CompletedWorkout {
        val completedId = FoundationId(id)
        val sourceWorkoutId = FoundationId(source_active_workout_id)
        val loggedSetsByExercise = setQueries.selectLoggedSets(source_active_workout_id)
            .executeAsList()
            .map { it.toExerciseSet() }
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
        return CompletedWorkout(
            id = completedId,
            sourceActiveWorkoutId = sourceWorkoutId,
            startedAt = started_at.toInstant(),
            finishedAt = finished_at.toInstant(),
            durationMs = duration_ms,
            routineId = routine_id?.let(::FoundationId),
            exercises = exercises,
            createdAt = created_at.toInstant()
        )
    }

    private fun Exercise_sets.toExerciseSet(): ExerciseSet =
        ExerciseSet(
            id = FoundationId(id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = SetKind.valueOf(set_kind),
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            durationMs = duration_ms,
            loggedAt = logged_at?.toInstant(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            editedAt = edited_at?.toInstant()
        )

    private fun SelectLoggedSets.toExerciseSet(): ExerciseSet =
        ExerciseSet(
            id = FoundationId(id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = SetKind.valueOf(set_kind),
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            durationMs = duration_ms,
            loggedAt = logged_at.toInstant(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            editedAt = edited_at?.toInstant()
        )

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

    private fun Active_set_drafts.toPersistedSetDraft(): PersistedSetDraft =
        PersistedSetDraft(
            draftId = FoundationId(draft_id),
            activeWorkoutId = FoundationId(active_workout_id),
            exerciseInstanceId = FoundationId(exercise_instance_id),
            position = OrderedPosition(position.toInt()),
            setKind = SetKind.valueOf(set_kind),
            reps = reps?.toInt(),
            weight = weight_kg?.let(::WeightKg),
            durationMs = duration_ms,
            timerStartedAt = timer_started_at?.toInstant(),
            updatedAt = updated_at.toInstant()
        )

    private fun Routines.toReusableRoutine(): ReusableRoutine {
        val routineId = FoundationId(id)
        return ReusableRoutine(
            id = routineId,
            name = name,
            exercises = routineQueries.selectRoutineExercises(id).executeAsList().map { it.toRoutineExercise() },
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            sourceCompletedWorkoutId = source_completed_workout_id?.let(::FoundationId),
            archivedAt = archived_at?.toInstant()
        )
    }

    private fun Routine_exercises.toRoutineExercise(): RoutineExercise =
        RoutineExercise(
            id = FoundationId(id),
            routineId = FoundationId(routine_id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            displayNameSnapshot = display_name_snapshot,
            position = OrderedPosition(position.toInt()),
            plannedSets = routineQueries.selectRoutineSetTemplates(id).executeAsList().map { it.toRoutineSetTemplate() },
            rest = RestConfiguration(
                durationSeconds = rest_seconds.toInt(),
                autoStart = rest_auto_start.toBooleanFlag()
            )
        )

    private fun Routine_set_templates.toRoutineSetTemplate(): RoutineSetTemplate =
        RoutineSetTemplate(
            id = FoundationId(id),
            routineExerciseId = FoundationId(routine_exercise_id),
            position = OrderedPosition(position.toInt()),
            targetWeight = target_weight_kg?.let(::WeightKg),
            targetReps = target_reps?.toInt(),
            targetDurationMs = target_duration_ms,
            setKind = SetKind.valueOf(set_kind)
        )

    private fun Exercise_catalog.toExerciseCatalogItem(): ExerciseCatalogItem =
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
            loggingMode = ExerciseLoggingMode.valueOf(logging_mode),
            isUserCreated = is_user_created.toBooleanFlag(),
            createdAt = created_at.toInstant(),
            updatedAt = updated_at.toInstant(),
            archivedAt = archived_at?.toInstant(),
            sourceSeedVersion = source_seed_version,
            userNotes = user_notes
        )

    private fun Personal_records.toPersonalRecord(): PersonalRecord =
        PersonalRecord(
            id = FoundationId(id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            recordKind = PersonalRecordKind.valueOf(record_kind),
            reps = reps?.toInt(),
            weight = weight_kg?.let(::WeightKg),
            value = value_,
            sourceWorkoutId = FoundationId(source_workout_id),
            sourceSetId = FoundationId(source_set_id),
            achievedAt = achieved_at.toInstant(),
            createdAt = created_at.toInstant()
        )

    private fun Progress_points.toProgressPoint(): ProgressPoint =
        ProgressPoint(
            id = FoundationId(id),
            exerciseCatalogId = FoundationId(exercise_catalog_id),
            sourceWorkoutId = FoundationId(source_workout_id),
            sourceSetId = source_set_id?.let(::FoundationId),
            metric = ProgressMetric.valueOf(metric),
            value = value_,
            weight = weight_kg?.let(::WeightKg),
            reps = reps?.toInt(),
            recordedAt = recorded_at.toInstant()
        )

    private fun SetQueriesAccessor.upsertSet(workoutId: FoundationId, set: ExerciseSet) {
        upsertExerciseSet(
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
            edited_at = set.editedAt?.toDbLong()
        )
    }

    private fun ExerciseQueriesAccessor.insertExercise(item: ExerciseCatalogItem) {
        insertExercise(
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
            user_notes = item.userNotes
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
            created_at = record.createdAt.toDbLong()
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
            recorded_at = point.recordedAt.toDbLong()
        )
    }

    private fun String.csvEscaped(): String =
        if (contains(',') || contains('"') || contains('\n')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

    private fun Long?.durationExportLabel(): String {
        if (this == null) return ""
        val totalSeconds = (coerceAtLeast(0L) / 1_000L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }

    private fun PersonalRecord.exportValueLabel(unit: WeightUnit): String =
        when (recordKind) {
            PersonalRecordKind.WEIGHT_FOR_REPS -> {
                val weightLabel = weight?.displayValue(unit)?.toString() ?: value.toString()
                val repsLabel = reps?.let { " x $it" }.orEmpty()
                "$weightLabel ${unit.name.lowercase()}$repsLabel"
            }
            PersonalRecordKind.BODYWEIGHT_REPS -> "${reps ?: value.toInt()} reps"
            PersonalRecordKind.ESTIMATED_ONE_REP_MAX -> "${WeightKg(value).displayValue(unit)} ${unit.name.lowercase()}"
            PersonalRecordKind.VOLUME -> "${WeightKg(value).displayValue(unit)} ${unit.name.lowercase()} volume"
            PersonalRecordKind.TIME -> value.toLong().durationExportLabel()
        }

    private companion object {
        const val DEFAULT_REST_SECONDS = 120L
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
