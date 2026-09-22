package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class WorkoutCompletionReceipt(
    val workout: CompletedWorkout,
    val newlyCompleted: Boolean
)

internal fun buildCompletedWorkout(
    active: ActiveWorkout,
    completedWorkoutId: FoundationId,
    finishedAt: Instant,
    startWorkoutTimerWithFirstSet: Boolean
): CompletedWorkout {
    val startedAt = active.effectiveStartedAt(startWorkoutTimerWithFirstSet)
    return CompletedWorkout(
        id = completedWorkoutId,
        sourceActiveWorkoutId = active.id,
        startedAt = startedAt,
        finishedAt = finishedAt,
        durationMs = finishedAt.toEpochMilliseconds() - startedAt.toEpochMilliseconds(),
        routineId = active.routineId,
        exercises = active.exercises.mapNotNull { exercise ->
            val logged = exercise.sets.filter { it.isLogged }
            if (logged.isEmpty()) null else CompletedExercise(
                id = exercise.id,
                completedWorkoutId = completedWorkoutId,
                exerciseCatalogId = exercise.reference.exerciseCatalogId,
                displayNameSnapshot = exercise.reference.displayNameSnapshot,
                position = exercise.position,
                loggedSets = logged,
                rest = exercise.rest,
                groupContext = exercise.groupContext
            )
        },
        createdAt = finishedAt
    )
}

internal fun FullAccessState.recordLocalCompletion(now: Instant): FullAccessState =
    if (hasFullAccess || normalizedCompletedFreeWorkouts >= FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT) {
        this
    } else {
        copy(completedFreeWorkouts = normalizedCompletedFreeWorkouts + 1, updatedAt = now)
    }
