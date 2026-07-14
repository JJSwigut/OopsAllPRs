package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveExerciseGroupContext
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SetLoggingUseCases(
    private val workouts: WorkoutRepository,
    private val setLedger: SetLedgerRepository,
    private val preferences: PreferencesRepository? = null,
    private val configurationManagement: ExerciseLoggingConfigurationUseCases? = null,
    private val activeUx: ActiveWorkoutUxRepository? = null
) {
    suspend fun addExercise(
        activeWorkoutId: FoundationId,
        exerciseCatalogId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveExercise> {
        val management = configurationManagement
            ?: return foundationFailure(FoundationError.Persistence("Exercise logging configuration is unavailable"))
        return when (val reference = management.snapshotReference(exerciseCatalogId)) {
            is FoundationResult.Failure -> reference
            is FoundationResult.Success -> appendExercise(activeWorkoutId, reference.value, now)
        }
    }

    suspend fun addExercise(
        activeWorkoutId: FoundationId,
        reference: ExerciseReference,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveExercise> {
        val management = configurationManagement
        if (management != null) {
            return when (val resolved = management.snapshotReference(reference.exerciseCatalogId)) {
                is FoundationResult.Failure -> resolved
                is FoundationResult.Success -> appendExercise(activeWorkoutId, resolved.value, now)
            }
        }
        return appendExercise(activeWorkoutId, reference, now)
    }

    private suspend fun appendExercise(
        activeWorkoutId: FoundationId,
        reference: ExerciseReference,
        now: Instant
    ): FoundationResult<ActiveExercise> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val exercise = ActiveExercise(
            id = newFoundationId("active-exercise"),
            activeWorkoutId = activeWorkoutId,
            reference = reference,
            position = OrderedPosition(workout.exercises.size),
            rest = RestConfiguration(durationSeconds = preferences?.defaultRestSeconds() ?: RestConfiguration.DEFAULT_SECONDS)
        )
        val updated = workout.copy(exercises = workout.exercises + exercise, updatedAt = now)
        return when (val saved = workouts.saveActiveWorkout(updated)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(exercise)
        }
    }

    suspend fun updateExerciseRest(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        rest: RestConfiguration,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveExercise> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        var updatedExercise: ActiveExercise? = null
        val updated = workout.copy(
            exercises = workout.exercises.map { exercise ->
                if (exercise.id == exerciseInstanceId) {
                    exercise.copy(rest = rest).also { updatedExercise = it }
                } else {
                    exercise
                }
            },
            updatedAt = now
        )
        val exercise = updatedExercise
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        return when (val saved = workouts.saveActiveWorkout(updated)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(exercise)
        }
    }

    suspend fun groupExercisesAsCircuit(
        activeWorkoutId: FoundationId,
        exerciseInstanceIds: List<FoundationId>,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveWorkout> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        if (exerciseInstanceIds.size < 2) {
            return foundationFailure(FoundationError.Validation("Select at least two adjacent exercises"))
        }
        val positionsById = workout.exercises.mapIndexed { index, exercise -> exercise.id to index }.toMap()
        val positions = exerciseInstanceIds.mapNotNull { positionsById[it] }.sorted()
        if (positions.size != exerciseInstanceIds.size || positions != (positions.first()..positions.last()).toList()) {
            return foundationFailure(FoundationError.Validation("Only adjacent exercises can be grouped"))
        }
        val groupId = newFoundationId("active-circuit")
        val groupPosition = OrderedPosition(positions.first())
        val groupRounds = positions
            .mapNotNull { workout.exercises[it].groupContext?.rounds }
            .firstOrNull() ?: DEFAULT_CIRCUIT_ROUNDS
        val selected = exerciseInstanceIds.toSet()
        val updated = workout.copy(
            exercises = workout.exercises.map { exercise ->
                if (exercise.id in selected) {
                    exercise.copy(
                        groupContext = ActiveExerciseGroupContext(
                            groupId = groupId,
                            groupPosition = groupPosition,
                            label = CIRCUIT_LABEL,
                            rounds = groupRounds
                        )
                    )
                } else {
                    exercise
                }
            },
            updatedAt = now
        )
        return workouts.saveActiveWorkout(updated)
    }

    suspend fun ungroupCircuit(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveWorkout> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val groupId = workout.exercises.firstOrNull { it.id == exerciseInstanceId }?.groupContext?.groupId
            ?: return foundationFailure(FoundationError.NotFound("Circuit not found for exercise: $exerciseInstanceId"))
        val updated = workout.copy(
            exercises = workout.exercises.map { exercise ->
                if (exercise.groupContext?.groupId == groupId) exercise.copy(groupContext = null) else exercise
            },
            updatedAt = now
        )
        return workouts.saveActiveWorkout(updated)
    }

    suspend fun adjustCircuitRounds(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        deltaRounds: Int,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveWorkout> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val group = workout.exercises.firstOrNull { it.id == exerciseInstanceId }?.groupContext
            ?: return foundationFailure(FoundationError.NotFound("Circuit not found for exercise: $exerciseInstanceId"))
        val nextRounds = (group.rounds + deltaRounds).coerceIn(1, 12)
        val updated = workout.copy(
            exercises = workout.exercises.map { exercise ->
                if (exercise.groupContext?.groupId == group.groupId) {
                    exercise.copy(groupContext = exercise.groupContext?.copy(rounds = nextRounds))
                } else {
                    exercise
                }
            },
            updatedAt = now
        )
        return workouts.saveActiveWorkout(updated)
    }

    suspend fun confirmSet(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        setKind: SetKind,
        reps: Int?,
        weight: WeightKg?,
        position: Int,
        loggedAt: Instant = Clock.System.now(),
        durationMs: Long? = null,
        distanceMeters: Double? = null,
        observedEffort: Effort? = null
    ): FoundationResult<ExerciseSet> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val exercise = workout.exercises.firstOrNull { it.id == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        val configuration = exercise.resolvedLoggingConfiguration.configuration
        val set = ExerciseSet(
            id = newFoundationId("set"),
            exerciseInstanceId = exerciseInstanceId,
            position = OrderedPosition(position),
            setKind = setKind,
            weight = weight,
            reps = reps?.takeIf {
                configuration.measures.any { measure -> measure.kind == MeasureKind.REPETITIONS }
            },
            durationMs = durationMs,
            captureConfigurationId = configuration.id,
            distanceMeters = distanceMeters,
            observedEffort = observedEffort,
            loggedAt = loggedAt,
            createdAt = loggedAt,
            updatedAt = loggedAt
        )
        set.validateForLogging(configuration)?.let { return foundationFailure(it) }
        return setLedger.confirmSet(activeWorkoutId, set)
    }

    suspend fun editLoggedSet(
        activeWorkoutId: FoundationId,
        set: ExerciseSet,
        now: Instant = Clock.System.now()
    ): FoundationResult<ExerciseSet> {
        val edited = set.copy(updatedAt = now, editedAt = now)
        val configuration = configurationManagement?.loggingConfiguration(edited.captureConfigurationId)
        edited.validateForLogging(configuration)?.let { return foundationFailure(it) }
        return setLedger.editLoggedSet(activeWorkoutId, edited)
    }

    suspend fun editLoggedSet(
        activeWorkoutId: FoundationId,
        setId: FoundationId,
        reps: Int?,
        weight: WeightKg?,
        now: Instant = Clock.System.now(),
        durationMs: Long? = null,
        distanceMeters: Double? = null,
        observedEffort: Effort? = null
    ): FoundationResult<ExerciseSet> {
        val existing = loggedSet(activeWorkoutId, setId)
            ?: return foundationFailure(FoundationError.NotFound("Logged set not found: $setId"))
        val configuration = configurationManagement?.loggingConfiguration(existing.captureConfigurationId)
        val repetitionsEnabled = configuration?.measures?.any { it.kind == MeasureKind.REPETITIONS }
            ?: (existing.setKind != SetKind.TIMED)
        return editLoggedSet(
            activeWorkoutId = activeWorkoutId,
            set = existing.copy(
                reps = reps?.takeIf { repetitionsEnabled },
                weight = weight,
                durationMs = durationMs,
                distanceMeters = distanceMeters,
                observedEffort = observedEffort,
                updatedAt = now,
                editedAt = now
            ),
            now = now
        )
    }

    suspend fun deleteLoggedSet(
        activeWorkoutId: FoundationId,
        setId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ExerciseSet> =
        setLedger.deleteLoggedSet(activeWorkoutId, setId, now)

    suspend fun undoLastLoggedSet(
        activeWorkoutId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ExerciseSet> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val last = workout.exercises
            .flatMap { it.sets }
            .filter { it.isLogged }
            .maxWithOrNull(
                compareBy<ExerciseSet> { it.loggedAt?.toEpochMilliseconds() ?: Long.MIN_VALUE }
                    .thenBy { it.updatedAt.toEpochMilliseconds() }
                    .thenBy { it.id.value }
            )
            ?: return foundationFailure(FoundationError.Validation("No logged sets to undo"))
        return deleteLoggedSet(activeWorkoutId, last.id, now)
    }

    suspend fun saveDraft(
        draft: PersistedSetDraft,
        now: Instant = Clock.System.now()
    ): FoundationResult<PersistedSetDraft> {
        val repository = activeUx
            ?: return foundationFailure(FoundationError.Persistence("Active workout draft persistence is unavailable"))
        val workout = workouts.activeWorkout(draft.activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: ${draft.activeWorkoutId}"))
        val exercise = workout.exercises.firstOrNull { it.id == draft.exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: ${draft.exerciseInstanceId}"))
        validateDraftValues(draft, exercise.resolvedLoggingConfiguration.configuration)?.let {
            return foundationFailure(it)
        }
        return repository.saveSetDraft(
            draft.copy(
                captureConfigurationId = exercise.resolvedLoggingConfiguration.configuration.id,
                updatedAt = now
            )
        )
    }

    suspend fun recoverDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft> =
        activeUx?.loadSetDrafts(activeWorkoutId).orEmpty()

    private fun validateDraftValues(
        draft: PersistedSetDraft,
        configuration: LoggingConfiguration
    ): FoundationError? {
        val enabledMeasures = configuration.measures.map { it.kind }.toSet()
        if (draft.reps != null && MeasureKind.REPETITIONS !in enabledMeasures) {
            return FoundationError.Validation("Repetitions are disabled for this logging configuration")
        }
        if (draft.weight != null && MeasureKind.LOAD !in enabledMeasures) {
            return FoundationError.Validation("Load is disabled for this logging configuration")
        }
        if (draft.durationMs != null && MeasureKind.DURATION !in enabledMeasures) {
            return FoundationError.Validation("Duration is disabled for this logging configuration")
        }
        if (draft.distanceMeters != null) {
            if (MeasureKind.DISTANCE !in enabledMeasures) {
                return FoundationError.Validation("Distance is disabled for this logging configuration")
            }
            if (!draft.distanceMeters.isFinite() || draft.distanceMeters < 0.0) {
                return FoundationError.Validation("Distance must be finite and non-negative")
            }
        }
        return try {
            configuration.requireCanCapture(draft.observedEffort)
            null
        } catch (error: IllegalArgumentException) {
            FoundationError.Validation(error.message ?: "Observed effort is invalid")
        }
    }

    private suspend fun loggedSet(activeWorkoutId: FoundationId, setId: FoundationId): ExerciseSet? =
        workouts.activeWorkout(activeWorkoutId)
            ?.exercises
            .orEmpty()
            .flatMap { it.sets }
            .firstOrNull { it.id == setId && it.isLogged }

    private companion object {
        const val CIRCUIT_LABEL = "Circuit"
        const val DEFAULT_CIRCUIT_ROUNDS = 3
    }
}
