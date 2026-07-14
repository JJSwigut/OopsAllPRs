package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.PreviousWorkoutSnapshot
import com.jjswigut.oopsallprs.domain.model.PreviousWorkoutValue
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.toLegacyLoggingConfiguration
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository

class PreviousWorkoutDefaultsUseCase(
    private val workouts: WorkoutRepository,
    private val configurations: LoggingConfigurationRepository? = workouts as? LoggingConfigurationRepository
) {
    suspend fun snapshotFor(
        exerciseCatalogId: FoundationId,
        isBodyweight: Boolean,
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
        loggingConfiguration: LoggingConfiguration = loggingMode.toLegacyLoggingConfiguration()
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
                        completed.toSnapshot(exercise, loggingConfiguration, isBodyweight).takeIf { it.values.isNotEmpty() }
                    }
            }

    suspend fun valueFor(
        exerciseCatalogId: FoundationId,
        isBodyweight: Boolean,
        loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
        setIndex: Int,
        loggingConfiguration: LoggingConfiguration = loggingMode.toLegacyLoggingConfiguration()
    ): PreviousWorkoutValue? =
        snapshotFor(exerciseCatalogId, isBodyweight, loggingMode, loggingConfiguration)?.valueForSetIndex(setIndex)

    private suspend fun CompletedWorkout.toSnapshot(
        exercise: CompletedExercise,
        loggingConfiguration: LoggingConfiguration,
        isBodyweight: Boolean
    ): PreviousWorkoutSnapshot {
        val values = buildList {
            exercise.loggedSets.sortedBy { it.position.value }.forEachIndexed { index, set ->
                val captured = configurations?.loggingConfiguration(set.captureConfigurationId)
                    ?: LegacyLoggingConfigurations.all.firstOrNull { it.id == set.captureConfigurationId }
                    ?: return@forEachIndexed
                set.toPreviousValue(
                    index = index,
                    completedWorkoutId = id,
                    loggingConfiguration = loggingConfiguration,
                    capturedConfiguration = captured,
                    isBodyweight = isBodyweight
                )?.let(::add)
            }
        }
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
        loggingConfiguration: LoggingConfiguration,
        capturedConfiguration: LoggingConfiguration,
        isBodyweight: Boolean
    ): PreviousWorkoutValue? {
        val enabled = loggingConfiguration.measures.map { it.kind }.toSet()
        val compatibleKinds = loggingConfiguration.measures.mapNotNull { target ->
            val source = capturedConfiguration.measures.firstOrNull { it.kind == target.kind }
                ?: return@mapNotNull null
            target.kind.takeIf {
                target.kind != MeasureKind.LOAD || target.loadRole == source.loadRole
            }
        }.toSet()
        val kind = when {
            enabled == setOf(MeasureKind.DURATION) -> SetKind.TIMED
            isBodyweight -> SetKind.BODYWEIGHT
            else -> SetKind.WEIGHTED
        }
        val validReps = reps?.takeIf { it > 0 }
        val validDuration = durationMs?.takeIf { it > 0L }
        val validDistance = distanceMeters?.takeIf { it.isFinite() && it > 0.0 }
        val validWeight = weight?.takeIf { it.value >= 0.0 }
        loggingConfiguration.measures.forEach { measure ->
            if (measure.requirement == MeasureRequirement.REQUIRED) {
                val present = when (measure.kind) {
                    MeasureKind.REPETITIONS -> measure.kind in compatibleKinds && validReps != null
                    MeasureKind.LOAD -> measure.kind in compatibleKinds && validWeight != null
                    MeasureKind.DURATION -> measure.kind in compatibleKinds && validDuration != null
                    MeasureKind.DISTANCE -> measure.kind in compatibleKinds && validDistance != null
                }
                if (!present) return null
            }
        }
        return PreviousWorkoutValue(
            setIndex = index,
            setKind = kind,
            weight = validWeight?.takeIf { MeasureKind.LOAD in compatibleKinds && it.value > 0.0 },
            reps = validReps.takeIf { MeasureKind.REPETITIONS in compatibleKinds },
            durationMs = validDuration.takeIf { MeasureKind.DURATION in compatibleKinds },
            distanceMeters = validDistance.takeIf { MeasureKind.DISTANCE in compatibleKinds },
            sourceCompletedWorkoutId = completedWorkoutId,
            sourceSetId = id
        )
    }
}
