package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.PreferencesRepository
import com.jjswigut.oopsallprs.domain.repository.SetLedgerRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class SetLoggingUseCases(
    private val workouts: WorkoutRepository,
    private val setLedger: SetLedgerRepository,
    private val preferences: PreferencesRepository? = null
) {
    suspend fun addExercise(activeWorkoutId: FoundationId, reference: ExerciseReference, now: Instant = Clock.System.now()): FoundationResult<ActiveExercise> {
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

    suspend fun confirmSet(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        setKind: SetKind,
        reps: Int,
        weight: WeightKg?,
        position: Int,
        loggedAt: Instant = Clock.System.now(),
        durationMs: Long? = null
    ): FoundationResult<ExerciseSet> {
        val set = ExerciseSet(
            id = newFoundationId("set"),
            exerciseInstanceId = exerciseInstanceId,
            position = OrderedPosition(position),
            setKind = setKind,
            weight = weight,
            reps = reps.takeIf { setKind != SetKind.TIMED },
            durationMs = durationMs,
            loggedAt = loggedAt,
            createdAt = loggedAt,
            updatedAt = loggedAt
        )
        return setLedger.confirmSet(activeWorkoutId, set)
    }

    suspend fun editLoggedSet(activeWorkoutId: FoundationId, set: ExerciseSet, now: Instant = Clock.System.now()): FoundationResult<ExerciseSet> =
        setLedger.editLoggedSet(activeWorkoutId, set.copy(updatedAt = now, editedAt = now))

    suspend fun editLoggedSet(
        activeWorkoutId: FoundationId,
        setId: FoundationId,
        reps: Int,
        weight: WeightKg?,
        now: Instant = Clock.System.now(),
        durationMs: Long? = null
    ): FoundationResult<ExerciseSet> {
        val existing = loggedSet(activeWorkoutId, setId)
            ?: return foundationFailure(FoundationError.NotFound("Logged set not found: $setId"))
        return editLoggedSet(
            activeWorkoutId = activeWorkoutId,
            set = existing.copy(
                reps = reps.takeIf { existing.setKind != SetKind.TIMED },
                weight = weight,
                durationMs = durationMs,
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

    private suspend fun loggedSet(activeWorkoutId: FoundationId, setId: FoundationId): ExerciseSet? =
        workouts.activeWorkout(activeWorkoutId)
            ?.exercises
            .orEmpty()
            .flatMap { it.sets }
            .firstOrNull { it.id == setId && it.isLogged }
}
