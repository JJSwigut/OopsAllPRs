package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.data.export.EXPORT_FORMAT_VERSION
import com.jjswigut.oopsallprs.data.export.durationExportLabel
import com.jjswigut.oopsallprs.data.export.exportDurationMillis
import com.jjswigut.oopsallprs.data.export.exportValueLabel
import com.jjswigut.oopsallprs.data.export.loadRoleCode
import com.jjswigut.oopsallprs.data.export.rpeExportValue
import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionOrigin
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.ExportFile
import com.jjswigut.oopsallprs.domain.model.ExportSnapshot
import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.WorkoutCompletionReceipt
import com.jjswigut.oopsallprs.domain.model.buildCompletedWorkout
import com.jjswigut.oopsallprs.domain.model.recordLocalCompletion
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightStepPreference
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.canonicalExerciseName
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.ExportRepository
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.CompletedWorkoutCorrectionRepository
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.repository.UserExerciseConfigurationRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryFoundationStore :
    WorkoutRepository,
    CompletedWorkoutCorrectionRepository,
    SessionRepository,
    ActiveWorkoutUxRepository,
    SetLedgerRepository,
    RoutineRepository,
    ExerciseRepository,
    LoggingConfigurationRepository,
    UserExerciseConfigurationRepository,
    PreferencesRepository,
    FullAccessRepository,
    ProgressRepository,
    ExportRepository {

    private val activeWorkouts = linkedMapOf<FoundationId, ActiveWorkout>()
    private val completedWorkouts = linkedMapOf<FoundationId, CompletedWorkout>()
    private val routines = linkedMapOf<FoundationId, ReusableRoutine>()
    private val exercises = linkedMapOf<String, ExerciseCatalogItem>()
    private val loggingConfigurationById = linkedMapOf<LoggingConfigurationId, LoggingConfiguration>().apply {
        LegacyLoggingConfigurations.all.forEach { put(it.id, it) }
    }
    private val userExerciseConfigurations = linkedMapOf<FoundationId, UserExerciseConfiguration>()
    private val seedImports = linkedMapOf<FoundationId, ExerciseSeedImport>()
    private val records = mutableListOf<PersonalRecord>()
    private val points = mutableListOf<ProgressPoint>()
    private val activeUxSessions = linkedMapOf<FoundationId, ActiveWorkoutUxSession>()
    private val activeSetDrafts = linkedMapOf<FoundationId, PersistedSetDraft>()
    private var activeSessionState: ActiveSessionState? = null
    private var preferredWeightUnit = WeightUnit.POUNDS
    private var weightStepPreference = WeightStepPreference()
    private var defaultRestSeconds = RestConfiguration.DEFAULT_SECONDS
    private var restSoundEnabled = true
    private var startTimerWithFirstSetPreference = true
    private var restTimerSurfaceEnabled = false
    private var fullAccessState: FullAccessState? = null
    private val accountingMutex = Mutex()
    private val accountedCompletionSources = mutableSetOf<FoundationId>()

    override suspend fun createActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
        if (activeWorkouts.values.any { it.status.name == "ACTIVE" }) {
            return foundationFailure(FoundationError.Conflict("Only one active workout is supported"))
        }
        activeWorkouts[workout.id] = workout
        return foundationSuccess(workout)
    }

    override suspend fun activeWorkout(id: FoundationId): ActiveWorkout? = activeWorkouts[id]

    override suspend fun currentActiveWorkout(): ActiveWorkout? =
        activeWorkouts.values.lastOrNull { it.status.name == "ACTIVE" }

    override suspend fun saveActiveWorkout(workout: ActiveWorkout): FoundationResult<ActiveWorkout> {
        activeWorkouts[workout.id] = workout
        return foundationSuccess(workout)
    }

    override suspend fun discardActiveWorkout(id: FoundationId, now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        activeWorkouts.remove(id)
        activeSessionState = activeSessionState?.takeIf { it.activeWorkoutId != id }
        activeUxSessions.remove(id)
        activeSetDrafts.entries.removeAll { it.value.activeWorkoutId == id }
        return foundationSuccess(Unit)
    }

    override suspend fun finishActiveWorkout(
        id: FoundationId,
        finishedAt: Instant
    ): FoundationResult<WorkoutCompletionReceipt> = accountingMutex.withLock {
        currentCoroutineContext().ensureActive()
        val existing = completedWorkouts.values.filter { it.sourceActiveWorkoutId == id }
        if (existing.size > 1) {
            return@withLock foundationFailure(FoundationError.Conflict("Multiple completed workouts reference $id"))
        }
        existing.singleOrNull()?.let { completed ->
            initializeLocalCompletionAccounting()
            accountedCompletionSources += id
            return@withLock foundationSuccess(WorkoutCompletionReceipt(completed, newlyCompleted = false))
        }
        val active = activeWorkouts[id]
            ?: return@withLock foundationFailure(FoundationError.NotFound("Active workout not found: $id"))
        if (active.status.name != "ACTIVE") {
            return@withLock foundationFailure(FoundationError.Conflict("Workout is not active: $id"))
        }
        if (active.loggedSets().isEmpty()) {
            return@withLock foundationFailure(FoundationError.Validation("Log at least one set before finishing"))
        }
        val completed = buildCompletedWorkout(active, newFoundationId("completed"), finishedAt, startTimerWithFirstSetPreference)
        currentCoroutineContext().ensureActive()
        // Capture the legacy fallback before inserting this completion.
        val access = initializeLocalCompletionAccounting()
        val updated = if (id in accountedCompletionSources) access else access.recordLocalCompletion(finishedAt)
        persistCompletedWorkout(completed)
        accountedCompletionSources += id
        fullAccessState = updated
        foundationSuccess(WorkoutCompletionReceipt(completed, newlyCompleted = true))
    }

    override suspend fun finishWorkout(workout: CompletedWorkout): FoundationResult<CompletedWorkout> = accountingMutex.withLock {
        currentCoroutineContext().ensureActive()
        initializeLocalCompletionAccounting()
        persistCompletedWorkout(workout)
        accountedCompletionSources += workout.sourceActiveWorkoutId
        foundationSuccess(workout)
    }

    private fun persistCompletedWorkout(workout: CompletedWorkout) {
        completedWorkouts[workout.id] = workout
        activeWorkouts.remove(workout.sourceActiveWorkoutId)
        activeSessionState = activeSessionState?.takeIf { it.activeWorkoutId != workout.sourceActiveWorkoutId }
        activeUxSessions.remove(workout.sourceActiveWorkoutId)
        activeSetDrafts.entries.removeAll { it.value.activeWorkoutId == workout.sourceActiveWorkoutId }
    }

    override suspend fun deleteCompletedWorkout(id: FoundationId, now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        completedWorkouts.remove(id)
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: $id"))
        return foundationSuccess(Unit)
    }

    override suspend fun completedWorkout(id: FoundationId): CompletedWorkout? = completedWorkouts[id]

    override suspend fun completedWorkouts(): List<CompletedWorkout> = completedWorkouts.values.toList()

    override suspend fun saveCompletedWorkoutCorrection(
        workout: CompletedWorkout,
        records: List<PersonalRecord>,
        points: List<ProgressPoint>
    ): FoundationResult<CompletedWorkout> {
        if (completedWorkouts[workout.id] == null) {
            return foundationFailure(FoundationError.NotFound("Completed workout not found: ${workout.id}"))
        }
        completedWorkouts[workout.id] = workout
        this.records.clear()
        this.records.addAll(records)
        this.points.clear()
        this.points.addAll(points)
        return foundationSuccess(workout)
    }

    override suspend fun load(): ActiveSessionState? = activeSessionState

    override suspend fun save(state: ActiveSessionState): FoundationResult<ActiveSessionState> {
        activeSessionState = state
        return foundationSuccess(state)
    }

    override suspend fun clear(now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        activeSessionState = null
        return foundationSuccess(Unit)
    }

    override suspend fun loadUxSession(activeWorkoutId: FoundationId): ActiveWorkoutUxSession? =
        activeUxSessions[activeWorkoutId]

    override suspend fun saveUxSession(session: ActiveWorkoutUxSession): FoundationResult<ActiveWorkoutUxSession> {
        activeUxSessions[session.activeWorkoutId] = session
        return foundationSuccess(session)
    }

    override suspend fun loadSetDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft> =
        activeSetDrafts.values
            .filter { it.activeWorkoutId == activeWorkoutId }
            .sortedWith(compareBy<PersistedSetDraft> { it.exerciseInstanceId.value }.thenBy { it.position.value })

    override suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft> {
        activeSetDrafts[draft.draftId] = draft
        return foundationSuccess(draft)
    }

    override suspend fun clearExerciseDrafts(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId
    ): FoundationResult<Unit> {
        activeSetDrafts.entries.removeAll {
            it.value.activeWorkoutId == activeWorkoutId && it.value.exerciseInstanceId == exerciseInstanceId
        }
        return foundationSuccess(Unit)
    }

    override suspend fun clearWorkoutUx(activeWorkoutId: FoundationId, now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        activeUxSessions.remove(activeWorkoutId)
        activeSetDrafts.entries.removeAll { it.value.activeWorkoutId == activeWorkoutId }
        return foundationSuccess(Unit)
    }

    override suspend fun confirmSet(workoutId: FoundationId, set: ExerciseSet): FoundationResult<ExerciseSet> {
        val captureConfiguration = loggingConfigurationById[set.captureConfigurationId]
            ?: return foundationFailure(
                FoundationError.Validation("Unknown logging configuration: ${set.captureConfigurationId}")
            )
        set.validateForLogging(captureConfiguration)?.let { return foundationFailure(it) }
        val workout = activeWorkouts[workoutId]
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $workoutId"))
        val updated = workout.copy(exercises = workout.exercises.replaceSet(set), updatedAt = set.updatedAt)
        activeWorkouts[workoutId] = updated
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
        now: kotlinx.datetime.Instant
    ): FoundationResult<ExerciseSet> {
        val workout = activeWorkouts[workoutId]
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $workoutId"))
        var deleted: ExerciseSet? = null
        val updatedExercises = workout.exercises.map { exercise ->
            val nextSets = exercise.sets.filterNot { set ->
                val shouldDelete = set.id == setId && set.isLogged
                if (shouldDelete) {
                    deleted = set
                }
                shouldDelete
            }
            exercise.copy(sets = nextSets)
        }
        val removed = deleted
            ?: return foundationFailure(FoundationError.NotFound("Logged set not found: $setId"))
        activeWorkouts[workoutId] = workout.copy(exercises = updatedExercises, updatedAt = now)
        return foundationSuccess(removed)
    }

    override suspend fun routine(id: FoundationId): ReusableRoutine? = routines[id]?.takeIf { it.archivedAt == null }

    override suspend fun routines(): List<ReusableRoutine> = routines.values.filter { it.archivedAt == null }

    override suspend fun saveRoutine(routine: ReusableRoutine): FoundationResult<ReusableRoutine> {
        if (routine.name.trim().isEmpty()) {
            return foundationFailure(FoundationError.Validation("Routine name cannot be blank"))
        }
        routines[routine.id] = routine
        return foundationSuccess(routine)
    }

    override suspend fun deleteRoutine(id: FoundationId, now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        val routine = routines[id]
            ?: return foundationFailure(FoundationError.NotFound("Routine not found: $id"))
        routines[id] = routine.copy(updatedAt = now, archivedAt = now)
        return foundationSuccess(Unit)
    }

    override suspend fun search(query: String): List<ExerciseCatalogItem> {
        val canonical = canonicalExerciseName(query)
        return exercises.values
            .filter { it.archivedAt == null && it.canonicalName.contains(canonical) }
            .sortedBy { it.displayName }
    }

    override suspend fun all(): List<ExerciseCatalogItem> =
        exercises.values.filter { it.archivedAt == null }.sortedBy { it.displayName }

    override suspend fun exercise(id: FoundationId): ExerciseCatalogItem? =
        exercises.values.firstOrNull { it.id == id }

    override suspend fun userCreatedExercises(): List<ExerciseCatalogItem> =
        exercises.values
            .filter { it.isUserCreated && it.archivedAt == null }
            .sortedBy { it.displayName }

    override suspend fun saveSeedItems(
        items: List<ExerciseCatalogItem>,
        import: ExerciseSeedImport
    ): FoundationResult<ExerciseSeedImport> {
        items.forEach { item ->
            immutableConfigurationConflict(item.defaultLoggingConfiguration)?.let {
                return foundationFailure(it)
            }
        }
        items.forEach { item ->
            loggingConfigurationById[item.defaultLoggingConfiguration.id] = item.defaultLoggingConfiguration
            val existing = exercises[item.canonicalName]
            if (existing == null || !existing.isUserCreated) {
                exercises[item.canonicalName] = if (existing == null) {
                    item.copy(
                        isUserCreated = false,
                        origin = ExerciseDefinitionOrigin.SEED
                    )
                } else {
                    item.copy(
                        id = existing.id,
                        isUserCreated = false,
                        createdAt = existing.createdAt,
                        origin = ExerciseDefinitionOrigin.SEED,
                        seedKey = existing.seedKey ?: item.seedKey,
                        definitionRevision = if (existing.hasSameDefinitionContent(item)) {
                            existing.definitionRevision
                        } else {
                            ExerciseDefinitionRevision(existing.definitionRevision.value + 1L)
                        }
                    )
                }
            }
        }
        seedImports[import.id] = import
        return foundationSuccess(import)
    }

    override suspend fun saveUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem> {
        val existing = exercises[item.canonicalName]
        if (existing != null && existing.archivedAt == null && existing.id != item.id && existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Exercise already exists"))
        }
        immutableConfigurationConflict(item.defaultLoggingConfiguration)?.let { return foundationFailure(it) }
        loggingConfigurationById[item.defaultLoggingConfiguration.id] = item.defaultLoggingConfiguration
        exercises[item.canonicalName] = item.copy(
            isUserCreated = true,
            sourceSeedVersion = null,
            origin = ExerciseDefinitionOrigin.USER,
            seedKey = null
        )
        return foundationSuccess(exercises.getValue(item.canonicalName))
    }

    override suspend fun updateUserExercise(item: ExerciseCatalogItem): FoundationResult<ExerciseCatalogItem> {
        val existing = exercises.values.firstOrNull { it.id == item.id }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: ${item.id}"))
        if (!existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Seeded exercises cannot be edited"))
        }
        val canonicalOwner = exercises[item.canonicalName]
        if (canonicalOwner != null && canonicalOwner.archivedAt == null && canonicalOwner.id != item.id && canonicalOwner.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Exercise already exists"))
        }
        immutableConfigurationConflict(item.defaultLoggingConfiguration)?.let { return foundationFailure(it) }
        loggingConfigurationById[item.defaultLoggingConfiguration.id] = item.defaultLoggingConfiguration
        exercises.entries.removeAll { it.value.id == item.id }
        exercises[item.canonicalName] = item.copy(
            isUserCreated = true,
            createdAt = existing.createdAt,
            sourceSeedVersion = null,
            origin = ExerciseDefinitionOrigin.USER,
            seedKey = null,
            definitionRevision = if (existing.hasSameDefinitionContent(item)) {
                existing.definitionRevision
            } else {
                ExerciseDefinitionRevision(existing.definitionRevision.value + 1L)
            }
        )
        return foundationSuccess(exercises.getValue(item.canonicalName))
    }

    override suspend fun archiveUserExercise(id: FoundationId, now: kotlinx.datetime.Instant): FoundationResult<Unit> {
        val existing = exercises.values.firstOrNull { it.id == id }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $id"))
        if (!existing.isUserCreated) {
            return foundationFailure(FoundationError.Validation("Seeded exercises cannot be archived"))
        }
        exercises.entries.removeAll { it.value.id == id }
        exercises[existing.canonicalName] = existing.copy(archivedAt = now, updatedAt = now)
        return foundationSuccess(Unit)
    }

    override suspend fun loggingConfiguration(id: LoggingConfigurationId): LoggingConfiguration? =
        loggingConfigurationById[id]

    override suspend fun loggingConfigurations(): List<LoggingConfiguration> =
        loggingConfigurationById.values.toList()

    override suspend fun saveLoggingConfiguration(
        configuration: LoggingConfiguration
    ): FoundationResult<LoggingConfiguration> {
        immutableConfigurationConflict(configuration)?.let { return foundationFailure(it) }
        loggingConfigurationById[configuration.id] = configuration
        return foundationSuccess(configuration)
    }

    override suspend fun userExerciseConfiguration(exerciseDefinitionId: FoundationId): UserExerciseConfiguration? =
        userExerciseConfigurations[exerciseDefinitionId]

    override suspend fun saveUserExerciseConfiguration(
        configuration: UserExerciseConfiguration
    ): FoundationResult<UserExerciseConfiguration> {
        val definition = exercises.values.firstOrNull {
            it.id == configuration.exerciseDefinitionId && it.archivedAt == null
        } ?: return foundationFailure(
            FoundationError.NotFound("Exercise not found: ${configuration.exerciseDefinitionId}")
        )
        if (configuration.basedOnDefinitionRevision != definition.definitionRevision) {
            return foundationFailure(
                FoundationError.Conflict("Exercise definition changed before its default could be saved")
            )
        }
        immutableConfigurationConflict(configuration.configuration)?.let { return foundationFailure(it) }
        loggingConfigurationById[configuration.configuration.id] = configuration.configuration
        userExerciseConfigurations[configuration.exerciseDefinitionId] = configuration
        return foundationSuccess(configuration)
    }

    override suspend fun clearUserExerciseConfiguration(exerciseDefinitionId: FoundationId): FoundationResult<Unit> {
        userExerciseConfigurations.remove(exerciseDefinitionId)
        return foundationSuccess(Unit)
    }

    override suspend fun weightUnit(): WeightUnit = preferredWeightUnit

    override suspend fun setWeightUnit(unit: WeightUnit): FoundationResult<WeightUnit> {
        preferredWeightUnit = unit
        return foundationSuccess(unit)
    }

    override suspend fun weightStep(unit: WeightUnit): Double = weightStepPreference.stepFor(unit)

    override suspend fun setWeightStep(unit: WeightUnit, step: Double): FoundationResult<Double> {
        return when (val updated = weightStepPreference.withStep(unit, step)) {
            is FoundationResult.Failure -> updated
            is FoundationResult.Success -> {
                weightStepPreference = updated.value
                foundationSuccess(weightStepPreference.stepFor(unit))
            }
        }
    }

    override suspend fun defaultRestSeconds(): Int = defaultRestSeconds

    override suspend fun setDefaultRestSeconds(seconds: Int): FoundationResult<Int> {
        if (seconds < 0) {
            return foundationFailure(FoundationError.Validation("Default rest cannot be negative"))
        }
        defaultRestSeconds = seconds
        return foundationSuccess(seconds)
    }

    override suspend fun restSoundEnabled(): Boolean = restSoundEnabled

    override suspend fun setRestSoundEnabled(enabled: Boolean): FoundationResult<Boolean> {
        restSoundEnabled = enabled
        return foundationSuccess(enabled)
    }

    override suspend fun startWorkoutTimerWithFirstSet(): Boolean = startTimerWithFirstSetPreference

    override suspend fun setStartWorkoutTimerWithFirstSet(enabled: Boolean): FoundationResult<Boolean> {
        startTimerWithFirstSetPreference = enabled
        return foundationSuccess(enabled)
    }

    override suspend fun restTimerSurfaceEnabled(): Boolean = restTimerSurfaceEnabled

    override suspend fun setRestTimerSurfaceEnabled(enabled: Boolean): FoundationResult<Boolean> {
        restTimerSurfaceEnabled = enabled
        return foundationSuccess(enabled)
    }

    override suspend fun loadFullAccess(): FullAccessState = accountingMutex.withLock { currentFullAccess() }

    override suspend fun updateFullAccess(transform: (FullAccessState) -> FullAccessState): FoundationResult<FullAccessState> =
        accountingMutex.withLock {
            try {
                currentCoroutineContext().ensureActive()
                val state = transform(currentFullAccess())
                val normalized = state.copy(completedFreeWorkouts = state.normalizedCompletedFreeWorkouts)
                currentCoroutineContext().ensureActive()
                fullAccessState = normalized
                foundationSuccess(normalized)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                foundationFailure(FoundationError.Persistence(error.message ?: "Could not update full access"))
            }
        }

    private fun currentFullAccess(): FullAccessState = fullAccessState ?: FullAccessState(
        completedFreeWorkouts = completedWorkouts.size.coerceAtMost(com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT)
    )

    private fun initializeLocalCompletionAccounting(): FullAccessState = currentFullAccess().also { state ->
        if (fullAccessState == null) {
            accountedCompletionSources += completedWorkouts.values.map { it.sourceActiveWorkoutId }
            fullAccessState = state
        }
    }

    override suspend fun replaceRecords(
        records: List<PersonalRecord>,
        points: List<ProgressPoint>
    ): FoundationResult<Unit> {
        this.records.clear()
        this.records.addAll(records)
        this.points.clear()
        this.points.addAll(points)
        return foundationSuccess(Unit)
    }

    override suspend fun personalRecords(): List<PersonalRecord> = records.toList()

    override suspend fun progressPoints(): List<ProgressPoint> = points.toList()

    override suspend fun export(type: ExportType, unit: WeightUnit): FoundationResult<ExportFile> {
        val now = Clock.System.now()
        val rows = when (type) {
            ExportType.WORKOUTS -> completedWorkouts.values.flatMap { workout ->
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
                            loggingConfigurationById[set.captureConfigurationId]?.loadRoleCode().orEmpty()
                        )
                    }
                }
            }
            ExportType.PERSONAL_RECORDS -> records.map { record ->
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
            ExportType.EXERCISES -> exercises.values.map { item ->
                listOf(
                    item.displayName, item.muscleGroup, item.equipment, item.exerciseType,
                    item.loggingMode.name, item.isUserCreated.toString(),
                    item.defaultLoggingConfiguration.id.value,
                    item.defaultLoggingConfiguration.loadRoleCode()
                )
            }
            ExportType.ROUTINES -> routines.values.map { routine ->
                listOf(
                    routine.id.value,
                    routine.name,
                    routine.exercises.size.toString(),
                    routine.exercises.map { it.resolvedLoggingConfiguration.configuration.id.value }.distinct().joinToString("|"),
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { it.targetDistanceMeters }.joinToString("|"),
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { (it.effortTarget as? com.jjswigut.oopsallprs.domain.model.EffortTarget.Rpe)?.rpeTenths }.joinToString("|") { it.rpeExportValue() },
                    routine.exercises.flatMap { it.plannedSets }.mapNotNull { (it.effortTarget as? com.jjswigut.oopsallprs.domain.model.EffortTarget.Rir)?.rir }.joinToString("|"),
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
        val csv = (listOf(header) + rows).joinToString("\n") { row -> row.joinToString(",") { it.csvEscaped() } }
        return foundationSuccess(
            ExportFile(
                snapshot = ExportSnapshot(newFoundationId("export"), type, now, unit, rows.size, EXPORT_FORMAT_VERSION),
                fileName = "${type.name.lowercase()}-${now.toEpochMilliseconds()}.csv",
                content = csv
            )
        )
    }

    private fun List<ActiveExercise>.replaceSet(set: ExerciseSet): List<ActiveExercise> =
        map { exercise ->
            if (exercise.id != set.exerciseInstanceId) {
                exercise
            } else {
                val nextSets = exercise.sets.filterNot { existing ->
                    existing.id == set.id || (!existing.isLogged && existing.position == set.position)
                } + set
                exercise.copy(sets = nextSets.sortedBy { it.position.value })
            }
        }

    private fun immutableConfigurationConflict(configuration: LoggingConfiguration): FoundationError? {
        val existing = loggingConfigurationById[configuration.id] ?: return null
        return if (existing == configuration) {
            null
        } else {
            FoundationError.Conflict("Logging configuration IDs are immutable: ${configuration.id}")
        }
    }

    private fun ExerciseCatalogItem.hasSameDefinitionContent(other: ExerciseCatalogItem): Boolean =
        canonicalName == other.canonicalName &&
            displayName == other.displayName &&
            muscleGroup == other.muscleGroup &&
            equipment == other.equipment &&
            movementPattern == other.movementPattern &&
            exerciseType == other.exerciseType &&
            experienceLevel == other.experienceLevel &&
            bodyRegion == other.bodyRegion &&
            isBodyweight == other.isBodyweight &&
            loggingMode == other.loggingMode &&
            seedKey == other.seedKey &&
            defaultLoggingConfiguration == other.defaultLoggingConfiguration

    private fun String.csvEscaped(): String =
        if (contains(',') || contains('"') || contains('\n')) {
            "\"" + replace("\"", "\"\"") + "\""
        } else {
            this
        }

}
