package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveExerciseGroupContext
import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class WorkoutLifecycleUseCases(
    private val workouts: WorkoutRepository,
    private val sessions: SessionRepository,
    private val routines: RoutineRepository,
    private val activeUx: ActiveWorkoutUxRepository? = null,
    private val preferences: PreferencesRepository? = null,
    private val notifications: RestAlertScheduler? = null,
    private val previousDefaults: PreviousWorkoutDefaultsUseCase? = null
) {
    private var lastRestAlertRequest: RestAlertRequest? = null

    suspend fun startEmpty(now: Instant = Clock.System.now()): FoundationResult<ActiveWorkout> {
        currentActiveWorkout()?.let {
            return foundationFailure(FoundationError.Conflict("An active workout is already in progress"))
        }
        val workout = ActiveWorkout(
            id = newFoundationId("workout"),
            startedAt = now,
            createdAt = now,
            updatedAt = now
        )
        return when (val created = workouts.createActiveWorkout(workout)) {
            is FoundationResult.Failure -> created
            is FoundationResult.Success -> {
                sessions.save(
                    ActiveSessionState(
                        workout.id,
                        workout.startedAt,
                        lastOpenedRoute = ACTIVE_WORKOUT_ROUTE,
                        updatedAt = now
                    )
                )
                foundationSuccess(created.value)
            }
        }
    }

    suspend fun startFromRoutine(routineId: FoundationId, now: Instant = Clock.System.now()): FoundationResult<ActiveWorkout> {
        currentActiveWorkout()?.let {
            return foundationFailure(FoundationError.Conflict("An active workout is already in progress"))
        }
        val routine = routines.routine(routineId)
            ?: return foundationFailure(FoundationError.NotFound("Routine not found: $routineId"))
        val workoutId = newFoundationId("workout")
        val activeExercises = routine.exercises.map { exercise ->
            val activeExerciseId = newFoundationId("active-exercise")
            val loggingMode = exercise.plannedSets.loggingMode()
            val isBodyweight = loggingMode == ExerciseLoggingMode.BODYWEIGHT || loggingMode == ExerciseLoggingMode.TIMED
            val previous = previousDefaults?.snapshotFor(
                exerciseCatalogId = exercise.exerciseCatalogId,
                isBodyweight = isBodyweight,
                loggingMode = loggingMode,
                loggingConfiguration = exercise.resolvedLoggingConfiguration.configuration
            )
            val groupContext = routine.exercises.groupContextFor(exercise)
            val plannedSets = exercise.plannedSets.expandedForGroupRounds(groupContext?.rounds)
            ActiveExercise(
                id = activeExerciseId,
                activeWorkoutId = workoutId,
                reference = ExerciseReference(
                    exerciseCatalogId = exercise.exerciseCatalogId,
                    displayNameSnapshot = exercise.displayNameSnapshot,
                    isBodyweight = isBodyweight,
                    loggingMode = loggingMode,
                    definitionRevisionSnapshot = exercise.definitionRevisionSnapshot,
                    seedKeySnapshot = exercise.seedKeySnapshot,
                    resolvedLoggingConfiguration = exercise.resolvedLoggingConfiguration
                ),
                position = exercise.position,
                groupContext = groupContext,
                sets = plannedSets.mapIndexed { setIndex, planned ->
                    val previousValue = previous?.valueForSetIndex(setIndex)
                    ExerciseSet(
                        id = newFoundationId("set"),
                        exerciseInstanceId = activeExerciseId,
                        position = OrderedPosition(setIndex),
                        setKind = planned.setKind,
                        weight = planned.targetWeight ?: previousValue?.weight,
                        reps = planned.targetReps ?: previousValue?.reps,
                        durationMs = planned.targetDurationMs ?: previousValue?.durationMs,
                        captureConfigurationId = exercise.resolvedLoggingConfiguration.configuration.id,
                        distanceMeters = planned.targetDistanceMeters ?: previousValue?.distanceMeters,
                        observedEffort = null,
                        loggedAt = null,
                        createdAt = now,
                        updatedAt = now
                    )
                },
                rest = exercise.rest
            )
        }
        val workout = ActiveWorkout(
            id = workoutId,
            startedAt = now,
            routineId = routine.id,
            routineSnapshotName = routine.name,
            exercises = activeExercises,
            createdAt = now,
            updatedAt = now
        )
        return when (val created = workouts.createActiveWorkout(workout)) {
            is FoundationResult.Failure -> created
            is FoundationResult.Success -> {
                sessions.save(
                    ActiveSessionState(
                        workout.id,
                        workout.startedAt,
                        lastOpenedRoute = ACTIVE_WORKOUT_ROUTE,
                        updatedAt = now
                    )
                )
                foundationSuccess(created.value)
            }
        }
    }

    suspend fun restoreActiveSession(now: Instant = Clock.System.now()): ActiveSessionState? {
        val state = sessions.load() ?: return null
        val restEndsAt = state.restEndsAt
        val restAdjusted = if (restEndsAt != null && restEndsAt <= now) {
            cancelRestAlert()
            state.withoutRest(now)
        } else {
            if (restEndsAt != null) {
                reconcileRestAlert(restEndsAt)
            }
            state
        }
        val effectiveStartedAt = restAdjusted.activeWorkoutId
            ?.let { workouts.activeWorkout(it) }
            ?.effectiveStartedAt(preferences?.startWorkoutTimerWithFirstSet() ?: true)
        val adjusted = if (effectiveStartedAt != null && effectiveStartedAt != restAdjusted.startedAt) {
            restAdjusted.copy(startedAt = effectiveStartedAt, updatedAt = now)
        } else {
            restAdjusted
        }
        if (adjusted != state) sessions.save(adjusted)
        return adjusted
    }

    suspend fun activeWorkout(activeWorkoutId: FoundationId): ActiveWorkout? =
        workouts.activeWorkout(activeWorkoutId)

    suspend fun currentActiveWorkout(): ActiveWorkout? =
        workouts.currentActiveWorkout()

    suspend fun updateRestTimer(
        activeWorkoutId: FoundationId,
        originSetId: FoundationId?,
        restEndsAt: Instant,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val current = sessions.load()
        val state = ActiveSessionState(
            activeWorkoutId = workout.id,
            startedAt = workout.startedAt,
            restEndsAt = restEndsAt,
            restStartedAt = now,
            restOriginSetId = originSetId,
            lastOpenedRoute = current?.lastOpenedRoute,
            updatedAt = now
        )
        return when (val saved = sessions.save(state)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> {
                reconcileRestAlert(restEndsAt)
                saved
            }
        }
    }

    suspend fun startRestTimer(
        activeWorkoutId: FoundationId,
        originSetId: FoundationId,
        durationSeconds: Int,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState> {
        if (durationSeconds <= 0) {
            return clearRestTimer(activeWorkoutId, now)
        }
        val endsAt = Instant.fromEpochMilliseconds(now.toEpochMilliseconds() + durationSeconds * 1_000L)
        return updateRestTimer(activeWorkoutId, originSetId, endsAt, now)
    }

    suspend fun adjustRestTimer(
        activeWorkoutId: FoundationId,
        deltaSeconds: Int,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState> {
        val current = sessions.load()
            ?: return foundationFailure(FoundationError.NotFound("Active session not found"))
        if (current.activeWorkoutId != activeWorkoutId || current.restEndsAt == null) {
            return foundationFailure(FoundationError.NotFound("Active rest not found"))
        }
        val nextEndsAt = Instant.fromEpochMilliseconds(current.restEndsAt.toEpochMilliseconds() + deltaSeconds * 1_000L)
        if (nextEndsAt <= now) {
            return clearRestTimer(activeWorkoutId, now)
        }
        return updateRestTimer(activeWorkoutId, current.restOriginSetId, nextEndsAt, now)
    }

    suspend fun clearRestTimer(
        activeWorkoutId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState> {
        val current = sessions.load()
            ?: return foundationFailure(FoundationError.NotFound("Active session not found"))
        if (current.activeWorkoutId != activeWorkoutId) {
            return foundationFailure(FoundationError.NotFound("Active session not found for workout: $activeWorkoutId"))
        }
        cancelRestAlert()
        return sessions.save(current.withoutRest(now))
    }

    suspend fun clearRestIfOrigin(
        activeWorkoutId: FoundationId,
        originSetId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState?> {
        val current = sessions.load() ?: return foundationSuccess(null)
        if (current.activeWorkoutId != activeWorkoutId || current.restOriginSetId != originSetId) {
            return foundationSuccess(current)
        }
        cancelRestAlert()
        return when (val saved = sessions.save(current.withoutRest(now))) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(saved.value)
        }
    }

    suspend fun refreshRestAlertForPreference(now: Instant = Clock.System.now()) {
        val current = sessions.load()
        val restEndsAt = current?.restEndsAt
        if (restEndsAt == null || restEndsAt <= now) {
            cancelRestAlert()
            return
        }
        reconcileRestAlert(restEndsAt)
    }

    suspend fun saveLastOpenedRoute(
        route: String,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveSessionState> {
        val current = sessions.load()
        val next = current?.copy(lastOpenedRoute = route, updatedAt = now)
            ?: ActiveSessionState(
                activeWorkoutId = null,
                startedAt = null,
                lastOpenedRoute = route,
                updatedAt = now
            )
        return sessions.save(next)
    }

    suspend fun discard(activeWorkoutId: FoundationId, now: Instant = Clock.System.now()): FoundationResult<Unit> {
        cancelRestAlert()
        sessions.clear(now)
        activeUx?.clearWorkoutUx(activeWorkoutId, now)
        return workouts.discardActiveWorkout(activeWorkoutId, now)
    }

    private suspend fun reconcileRestAlert(restEndsAt: Instant) {
        val scheduler = notifications ?: return
        val request = RestAlertRequest(
            restEndsAt = restEndsAt,
            soundEnabled = preferences?.restSoundEnabled() ?: true,
            persistentSurfaceEnabled = preferences?.restTimerSurfaceEnabled() ?: true
        )
        if (request == lastRestAlertRequest) return
        scheduler.schedule(
            restEndsAt = request.restEndsAt,
            soundEnabled = request.soundEnabled,
            persistentSurfaceEnabled = request.persistentSurfaceEnabled
        )
        lastRestAlertRequest = request
    }

    private fun cancelRestAlert() {
        lastRestAlertRequest = null
        notifications?.cancel()
    }

    private data class RestAlertRequest(
        val restEndsAt: Instant,
        val soundEnabled: Boolean,
        val persistentSurfaceEnabled: Boolean
    )

    private companion object {
        const val ACTIVE_WORKOUT_ROUTE = "active-workout"
    }
}

private fun List<com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate>.loggingMode(): ExerciseLoggingMode =
    when {
        any { it.setKind == SetKind.TIMED } -> ExerciseLoggingMode.TIMED
        any { it.setKind == SetKind.BODYWEIGHT } -> ExerciseLoggingMode.BODYWEIGHT
        else -> ExerciseLoggingMode.WEIGHTED
    }

private fun List<com.jjswigut.oopsallprs.domain.model.RoutineExercise>.groupContextFor(
    exercise: com.jjswigut.oopsallprs.domain.model.RoutineExercise
): ActiveExerciseGroupContext? {
    val groupId = exercise.groupId ?: return null
    val groupPosition = exercise.groupPosition ?: return null
    val rounds = (exercise.groupRounds ?: 1).coerceAtLeast(1)
    val groupSize = count { it.groupId == groupId }
    val label = when {
        groupSize >= 2 -> "Circuit"
        else -> return null
    }
    return ActiveExerciseGroupContext(
        groupId = groupId,
        groupPosition = groupPosition,
        label = label,
        rounds = rounds
    )
}

private fun List<com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate>.expandedForGroupRounds(
    rounds: Int?
): List<com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate> {
    val sorted = sortedBy { it.position.value }
    val targetCount = rounds ?: return sorted
    if (sorted.isEmpty() || targetCount <= sorted.size) return sorted
    return List(targetCount) { index -> sorted.getOrElse(index) { sorted.last() } }
}
