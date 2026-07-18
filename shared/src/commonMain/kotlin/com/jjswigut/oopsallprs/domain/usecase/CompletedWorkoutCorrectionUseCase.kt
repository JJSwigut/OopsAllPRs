package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.repository.CompletedWorkoutCorrectionRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError

class CompletedWorkoutCorrectionUseCase(
    private val workouts: WorkoutRepository,
    private val corrections: CompletedWorkoutCorrectionRepository,
    private val configurations: LoggingConfigurationRepository,
    private val personalRecords: PersonalRecordDerivationUseCase
) {
    suspend fun save(candidate: CompletedWorkout): FoundationResult<CompletedWorkout> {
        val original = workouts.completedWorkout(candidate.id)
            ?: return foundationFailure(FoundationError.NotFound("Completed workout not found: ${candidate.id}"))
        validateIdentity(original, candidate)?.let { return foundationFailure(it) }
        candidate.exercises.forEach { exercise ->
            if (exercise.loggedSets.isEmpty()) {
                return foundationFailure(FoundationError.Validation("A completed exercise must keep at least one set"))
            }
            exercise.loggedSets.forEachIndexed { index, set ->
                if (set.exerciseInstanceId != exercise.id) {
                    return foundationFailure(FoundationError.Validation("Set does not belong to this exercise"))
                }
                if (set.position.value != index) {
                    return foundationFailure(FoundationError.Validation("Completed set positions must be contiguous"))
                }
                if (set.loggedAt == null) {
                    return foundationFailure(FoundationError.Validation("Completed sets require a logged timestamp"))
                }
                val configuration = configurations.loggingConfiguration(set.captureConfigurationId)
                    ?: return foundationFailure(
                        FoundationError.Validation("Unknown or malformed logging configuration: ${set.captureConfigurationId}")
                    )
                set.validateForLogging(configuration)?.let { return foundationFailure(it) }
            }
        }
        val allSetIds = candidate.exercises.flatMap(CompletedExercise::loggedSets).map { it.id }
        if (allSetIds.distinct().size != allSetIds.size) {
            return foundationFailure(FoundationError.Validation("Completed set IDs must be unique"))
        }
        val correctedLedger = workouts.completedWorkouts().map { if (it.id == candidate.id) candidate else it }
        val derived = personalRecords.snapshotFrom(correctedLedger)
        return corrections.saveCompletedWorkoutCorrection(candidate, derived.records, derived.points)
    }

    private fun validateIdentity(
        original: CompletedWorkout,
        candidate: CompletedWorkout
    ): FoundationError.Validation? {
        if (
            candidate.sourceActiveWorkoutId != original.sourceActiveWorkoutId ||
            candidate.startedAt != original.startedAt ||
            candidate.finishedAt != original.finishedAt ||
            candidate.durationMs != original.durationMs ||
            candidate.routineId != original.routineId ||
            candidate.createdAt != original.createdAt
        ) return FoundationError.Validation("Workout identity and timestamps cannot be changed")
        if (candidate.exercises.size != original.exercises.size) {
            return FoundationError.Validation("Exercises cannot be added or removed while correcting sets")
        }
        original.exercises.zip(candidate.exercises).forEach { (before, after) ->
            if (
                after.id != before.id ||
                after.completedWorkoutId != before.completedWorkoutId ||
                after.exerciseCatalogId != before.exerciseCatalogId ||
                after.displayNameSnapshot != before.displayNameSnapshot ||
                after.position != before.position ||
                after.rest != before.rest
            ) return FoundationError.Validation("Exercise identity, order, and snapshots cannot be changed")
        }
        return null
    }
}
