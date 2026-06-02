package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class PersonalRecordDerivationUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun rebuildFrom(workouts: List<CompletedWorkout>) {
        val points = workouts.flatMap { workout ->
            workout.exercises.flatMap { exercise ->
                exercise.loggedSets.flatMap { set ->
                    set.toProgressPoints(
                        exerciseCatalogId = exercise.exerciseCatalogId,
                        sourceWorkoutId = workout.id,
                        fallbackRecordedAt = workout.finishedAt
                    )
                }
            }
        }

        val records = points
            .groupBy { point -> point.recordBucket() }
            .mapNotNull { (bucket, values) ->
                values.maxByOrNull { it.value }?.let { point ->
                    PersonalRecord(
                        id = newFoundationId("pr"),
                        exerciseCatalogId = point.exerciseCatalogId,
                        recordKind = bucket.kind,
                        reps = point.reps,
                        weight = point.weight,
                        value = point.value,
                        sourceWorkoutId = point.sourceWorkoutId,
                        sourceSetId = point.sourceSetId ?: return@let null,
                        achievedAt = point.recordedAt,
                        createdAt = Clock.System.now()
                    )
                }
            }

        progressRepository.replaceRecords(records, points)
    }

    private fun ExerciseSet.toProgressPoints(
        exerciseCatalogId: FoundationId,
        sourceWorkoutId: FoundationId,
        fallbackRecordedAt: Instant
    ): List<ProgressPoint> {
        val recordedAt = loggedAt ?: fallbackRecordedAt
        return when (setKind) {
            SetKind.BODYWEIGHT -> listOfNotNull(
                reps?.let { repCount ->
                    progressPoint(
                        exerciseCatalogId = exerciseCatalogId,
                        sourceWorkoutId = sourceWorkoutId,
                        metric = ProgressMetric.BODYWEIGHT_REPS,
                        value = repCount.toDouble(),
                        recordedAt = recordedAt
                    )
                }
            )
            SetKind.TIMED -> listOfNotNull(
                durationMs?.takeIf { it > 0L }?.let { duration ->
                    progressPoint(
                        exerciseCatalogId = exerciseCatalogId,
                        sourceWorkoutId = sourceWorkoutId,
                        metric = ProgressMetric.TIME,
                        value = duration.toDouble(),
                        recordedAt = recordedAt
                    )
                }
            )
            SetKind.WEIGHTED -> {
                val loggedWeight = weight
                val repCount = reps
                if (loggedWeight == null || repCount == null || repCount <= 0) {
                    emptyList()
                } else {
                    listOf(
                        progressPoint(
                            exerciseCatalogId = exerciseCatalogId,
                            sourceWorkoutId = sourceWorkoutId,
                            metric = ProgressMetric.BEST_SET,
                            value = loggedWeight.value,
                            recordedAt = recordedAt
                        ),
                        progressPoint(
                            exerciseCatalogId = exerciseCatalogId,
                            sourceWorkoutId = sourceWorkoutId,
                            metric = ProgressMetric.ESTIMATED_ONE_REP_MAX,
                            value = loggedWeight.value * (1.0 + repCount.toDouble() / 30.0),
                            recordedAt = recordedAt
                        ),
                        progressPoint(
                            exerciseCatalogId = exerciseCatalogId,
                            sourceWorkoutId = sourceWorkoutId,
                            metric = ProgressMetric.VOLUME,
                            value = loggedWeight.value * repCount.toDouble(),
                            recordedAt = recordedAt
                        )
                    )
                }
            }
        }
    }

    private fun ExerciseSet.progressPoint(
        exerciseCatalogId: FoundationId,
        sourceWorkoutId: FoundationId,
        metric: ProgressMetric,
        value: Double,
        recordedAt: Instant
    ): ProgressPoint =
        ProgressPoint(
            id = newFoundationId("progress"),
            exerciseCatalogId = exerciseCatalogId,
            sourceWorkoutId = sourceWorkoutId,
            sourceSetId = id,
            metric = metric,
            value = value,
            weight = weight,
            reps = reps,
            recordedAt = recordedAt
        )

    private fun ProgressPoint.recordBucket(): RecordBucket {
        val kind = when (metric) {
            ProgressMetric.BEST_SET -> PersonalRecordKind.WEIGHT_FOR_REPS
            ProgressMetric.BODYWEIGHT_REPS -> PersonalRecordKind.BODYWEIGHT_REPS
            ProgressMetric.ESTIMATED_ONE_REP_MAX -> PersonalRecordKind.ESTIMATED_ONE_REP_MAX
            ProgressMetric.VOLUME -> PersonalRecordKind.VOLUME
            ProgressMetric.TIME -> PersonalRecordKind.TIME
        }
        return RecordBucket(
            exerciseId = exerciseCatalogId,
            kind = kind,
            reps = when (kind) {
                PersonalRecordKind.WEIGHT_FOR_REPS,
                PersonalRecordKind.BODYWEIGHT_REPS -> reps
                PersonalRecordKind.ESTIMATED_ONE_REP_MAX,
                PersonalRecordKind.VOLUME,
                PersonalRecordKind.TIME -> null
            }
        )
    }

    private data class RecordBucket(
        val exerciseId: FoundationId,
        val kind: PersonalRecordKind,
        val reps: Int?
    )
}
