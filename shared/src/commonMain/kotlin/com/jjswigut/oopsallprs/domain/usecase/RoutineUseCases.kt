package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.RoutineRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class RoutineUseCases(
    private val workouts: WorkoutRepository,
    private val routines: RoutineRepository,
    private val activeUx: ActiveWorkoutUxRepository? = null,
    private val personalRecords: PersonalRecordDerivationUseCase? = null,
    private val preferences: PreferencesRepository? = null,
    private val restNotifications: RestAlertScheduler? = null,
    private val fullAccess: FullAccessUseCases? = null,
    private val configurationManagement: ExerciseLoggingConfigurationUseCases? = null
) {
    suspend fun finishWorkout(activeWorkoutId: FoundationId, finishedAt: Instant = Clock.System.now()): FoundationResult<CompletedWorkout> {
        val active = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val completedId = newFoundationId("completed")
        val completedExercises = active.exercises.mapNotNull { exercise ->
            val logged = exercise.sets.filter { it.isLogged }
            if (logged.isEmpty()) null else CompletedExercise(
                id = newFoundationId("completed-exercise"),
                completedWorkoutId = completedId,
                exerciseCatalogId = exercise.reference.exerciseCatalogId,
                displayNameSnapshot = exercise.reference.displayNameSnapshot,
                position = exercise.position,
                loggedSets = logged,
                rest = exercise.rest,
                groupContext = exercise.groupContext
            )
        }
        val effectiveStartedAt = active.effectiveStartedAt(
            preferences?.startWorkoutTimerWithFirstSet() ?: true
        )
        val completed = CompletedWorkout(
            id = completedId,
            sourceActiveWorkoutId = active.id,
            startedAt = effectiveStartedAt,
            finishedAt = finishedAt,
            durationMs = finishedAt.toEpochMilliseconds() - effectiveStartedAt.toEpochMilliseconds(),
            routineId = active.routineId,
            exercises = completedExercises,
            createdAt = finishedAt
        )
        val result = workouts.finishWorkout(completed)
        if (result is FoundationResult.Success) {
            restNotifications?.cancel()
            activeUx?.clearWorkoutUx(activeWorkoutId, finishedAt)
            personalRecords?.rebuildFrom(workouts.completedWorkouts())
            fullAccess?.recordCompletedWorkout(finishedAt)
        }
        return result
    }

    suspend fun saveCompletedWorkoutAsRoutine(completedWorkoutId: FoundationId, name: String, now: Instant = Clock.System.now()): FoundationResult<ReusableRoutine> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return foundationFailure(FoundationError.Validation("Template name is required"))
        }
        val completed = workouts.completedWorkout(completedWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: $completedWorkoutId"))
        val routineId = newFoundationId("routine")
        val exercises = completed.exercises.map { completedExercise ->
            val routineExerciseId = newFoundationId("routine-exercise")
            val exercise = RoutineExercise(
                id = routineExerciseId,
                routineId = routineId,
                exerciseCatalogId = completedExercise.exerciseCatalogId,
                displayNameSnapshot = completedExercise.displayNameSnapshot,
                position = completedExercise.position,
                groupId = completedExercise.groupContext?.groupId,
                groupPosition = completedExercise.groupContext?.groupPosition,
                groupRounds = completedExercise.groupContext?.rounds,
                rest = completedExercise.rest.takeIf { it.durationSeconds > 0 }
                    ?: RestConfiguration(
                        durationSeconds = preferences?.defaultRestSeconds() ?: RestConfiguration.DEFAULT_SECONDS
                    ),
                plannedSets = completedExercise.loggedSets.mapIndexed { index, set ->
                    RoutineSetTemplate(
                        id = newFoundationId("routine-set"),
                        routineExerciseId = routineExerciseId,
                        position = OrderedPosition(index),
                        targetWeight = set.weight,
                        targetReps = set.reps,
                        targetDurationMs = set.durationMs,
                        setKind = set.setKind,
                        targetDistanceMeters = set.distanceMeters,
                        effortTarget = null
                    )
                }
            )
            val capturedConfigurationId = completedExercise.loggedSets.lastOrNull()?.captureConfigurationId
            when (
                val snapshot = configurationManagement?.snapshotRoutineExercise(
                    exercise,
                    capturedConfigurationId
                ) ?: foundationSuccess(exercise)
            ) {
                is FoundationResult.Failure -> return snapshot
                is FoundationResult.Success -> snapshot.value
            }
        }
        return routines.saveRoutine(
            ReusableRoutine(
                id = routineId,
                name = trimmedName,
                exercises = exercises,
                createdAt = now,
                updatedAt = now,
                sourceCompletedWorkoutId = completedWorkoutId
            )
        )
    }

    suspend fun saveRoutine(
        routineId: FoundationId?,
        name: String,
        exercises: List<RoutineExercise>,
        sourceCompletedWorkoutId: FoundationId? = null,
        now: Instant = Clock.System.now()
    ): FoundationResult<ReusableRoutine> {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            return foundationFailure(FoundationError.Validation("Routine name is required"))
        }
        if (exercises.isEmpty()) {
            return foundationFailure(FoundationError.Validation("Add at least one exercise"))
        }
        val existing = routineId?.let { routines.routine(it) }
        if (routineId != null && existing == null) {
            return foundationFailure(FoundationError.NotFound("Routine not found: $routineId"))
        }
        val resolvedRoutineId = routineId ?: newFoundationId("routine")
        val normalizedExercises = exercises.mapIndexed { index, exercise ->
            val routineExerciseId = exercise.id
            val normalized = exercise.copy(
                id = routineExerciseId,
                routineId = resolvedRoutineId,
                position = OrderedPosition(index),
                plannedSets = exercise.plannedSets.mapIndexed { setIndex, set ->
                    set.copy(
                        routineExerciseId = routineExerciseId,
                        position = OrderedPosition(setIndex)
                    )
                }
            )
            when (
                val snapshot = configurationManagement?.snapshotRoutineExercise(normalized)
                    ?: foundationSuccess(normalized)
            ) {
                is FoundationResult.Failure -> return snapshot
                is FoundationResult.Success -> snapshot.value
            }
        }
        val routine = ReusableRoutine(
            id = resolvedRoutineId,
            name = trimmedName,
            exercises = normalizedExercises,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
            sourceCompletedWorkoutId = existing?.sourceCompletedWorkoutId ?: sourceCompletedWorkoutId,
            archivedAt = null
        )
        return when (val saved = routines.saveRoutine(routine)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(saved.value)
        }
    }

    suspend fun deleteRoutine(routineId: FoundationId, now: Instant = Clock.System.now()): FoundationResult<Unit> =
        routines.deleteRoutine(routineId, now)

    suspend fun deleteCompletedWorkout(completedWorkoutId: FoundationId, now: Instant = Clock.System.now()): FoundationResult<Unit> {
        workouts.completedWorkout(completedWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: $completedWorkoutId"))
        val result = workouts.deleteCompletedWorkout(completedWorkoutId, now)
        if (result is FoundationResult.Success) {
            personalRecords?.rebuildFrom(workouts.completedWorkouts())
        }
        return result
    }

    suspend fun listRoutines(): List<ReusableRoutine> = routines.routines()

    suspend fun listRoutinesByRecentUse(): List<ReusableRoutine> {
        val latestUseByRoutine = workouts.completedWorkouts()
            .mapNotNull { workout ->
                workout.routineId?.let { routineId -> routineId to workout.finishedAt }
            }
            .groupingBy { it.first }
            .fold(null as Instant?) { latest, (_, finishedAt) ->
                if (latest == null || finishedAt > latest) finishedAt else latest
            }
        return routines.routines()
            .sortedWith(
                compareByDescending<ReusableRoutine> { latestUseByRoutine[it.id] != null }
                    .thenByDescending { latestUseByRoutine[it.id] ?: it.updatedAt }
                    .thenBy { it.name.lowercase() }
            )
    }
}
