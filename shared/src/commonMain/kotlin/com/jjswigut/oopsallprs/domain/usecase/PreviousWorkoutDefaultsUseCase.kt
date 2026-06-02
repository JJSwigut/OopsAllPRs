package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PreviousWorkoutSnapshot
import com.jjswigut.oopsallprs.domain.model.PreviousWorkoutValue
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository

class PreviousWorkoutDefaultsUseCase(
    private val workouts: WorkoutRepository
) {
    suspend fun snapshotFor(
        exerciseCatalogId: FoundationId,
        isBodyweight: Boolean,
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED
    ): PreviousWorkoutSnapshot? =
        workouts.completedWorkouts()
            .sortedWith(
                compareByDescending<CompletedWorkout> { it.finishedAt.toEpochMilliseconds() }
                    .thenByDescending { it.createdAt.toEpochMilliseconds() }
                    .thenByDescending { it.id.value }
            )
            .firstNotNullOfOrNull { completed ->
                completed.exercises
                    .filter { it.exerciseCatalogId == exerciseCatalogId }
                    .sortedBy { it.position.value }
                    .firstNotNullOfOrNull { exercise ->
                        completed.toSnapshot(exercise, loggingMode).takeIf { it.values.isNotEmpty() }
                    }
            }

    suspend fun valueFor(
        exerciseCatalogId: FoundationId,
        isBodyweight: Boolean,
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
        setIndex: Int
    ): PreviousWorkoutValue? =
        snapshotFor(exerciseCatalogId, isBodyweight, loggingMode)?.valueForSetIndex(setIndex)

    private fun CompletedWorkout.toSnapshot(
        exercise: CompletedExercise,
        loggingMode: ExerciseLoggingMode
    ): PreviousWorkoutSnapshot {
        val values = exercise.loggedSets
            .sortedBy { it.position.value }
            .mapIndexedNotNull { index, set -> set.toPreviousValue(index, id, loggingMode) }
        return PreviousWorkoutSnapshot(
            exerciseCatalogId = exercise.exerciseCatalogId,
            completedWorkoutId = id,
            finishedAt = finishedAt,
            values = values
        )
    }

    private fun ExerciseSet.toPreviousValue(
        index: Int,
        completedWorkoutId: FoundationId,
        loggingMode: ExerciseLoggingMode
    ): PreviousWorkoutValue? {
        val kind = when (loggingMode) {
            ExerciseLoggingMode.BODYWEIGHT -> SetKind.BODYWEIGHT
            ExerciseLoggingMode.TIMED -> SetKind.TIMED
            ExerciseLoggingMode.WEIGHTED -> SetKind.WEIGHTED
        }
        val validReps = reps?.takeIf { it > 0 }
        val validDuration = durationMs?.takeIf { it > 0L }
        val resolvedWeight = when (kind) {
            SetKind.BODYWEIGHT -> {
                if (validReps == null) return null
                null
            }
            SetKind.WEIGHTED -> {
                if (validReps == null) return null
                weight?.takeIf { it.value >= 0.0 } ?: return null
            }
            SetKind.TIMED -> {
                if (validDuration == null) return null
                null
            }
        }
        return PreviousWorkoutValue(
            setIndex = index,
            setKind = kind,
            weight = resolvedWeight,
            reps = validReps.takeIf { kind != SetKind.TIMED },
            durationMs = validDuration.takeIf { kind == SetKind.TIMED },
            sourceCompletedWorkoutId = completedWorkoutId,
            sourceSetId = id
        )
    }
}
