package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedback
import com.jjswigut.oopsallprs.domain.model.ActivePrFeedbackKind
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository

class ActivePrFeedbackUseCase(
    private val progressRepository: ProgressRepository
) {
    suspend fun feedbackFor(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        set: ExerciseSet
    ): ActivePrFeedback? {
        val newValue = when (set.setKind) {
            SetKind.BODYWEIGHT -> set.reps?.toDouble() ?: return null
            SetKind.WEIGHTED -> set.weight?.value ?: return null
            SetKind.TIMED -> set.durationMs?.toDouble() ?: return null
        }
        val reps = set.reps
        val previous = previousBest(workout, exercise, set, reps)
        if (previous != null && newValue <= previous) {
            return null
        }
        val kind = when (set.setKind) {
            SetKind.BODYWEIGHT -> ActivePrFeedbackKind.BODYWEIGHT_REPS
            SetKind.WEIGHTED -> ActivePrFeedbackKind.WEIGHT_FOR_REPS
            SetKind.TIMED -> ActivePrFeedbackKind.TIME
        }
        return ActivePrFeedback(
            setId = set.id,
            exerciseCatalogId = exercise.reference.exerciseCatalogId,
            kind = kind,
            label = labelFor(kind, newValue, reps, previous),
            previousValue = previous,
            newValue = newValue
        )
    }

    private suspend fun previousBest(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        set: ExerciseSet,
        reps: Int?
    ): Double? {
        val recordBest = progressRepository.personalRecords()
            .filter { record ->
                record.exerciseCatalogId == exercise.reference.exerciseCatalogId &&
                    record.sourceSetId != set.id &&
                    when (set.setKind) {
                        SetKind.BODYWEIGHT -> record.recordKind == PersonalRecordKind.BODYWEIGHT_REPS
                        SetKind.WEIGHTED -> record.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS && record.reps == reps
                        SetKind.TIMED -> record.recordKind == PersonalRecordKind.TIME
                    }
            }
            .maxOfOrNull { it.value }

        val activeBest = workout.exercises
            .filter { it.reference.exerciseCatalogId == exercise.reference.exerciseCatalogId }
            .flatMap { it.sets }
            .filter { candidate ->
                candidate.isLogged &&
                    candidate.id != set.id &&
                    candidate.loggedAt != null &&
                    candidate.setKind == set.setKind &&
                    when (set.setKind) {
                        SetKind.BODYWEIGHT -> true
                        SetKind.WEIGHTED -> candidate.reps == reps
                        SetKind.TIMED -> true
                    }
            }
            .mapNotNull { candidate ->
                when (candidate.setKind) {
                    SetKind.BODYWEIGHT -> candidate.reps?.toDouble()
                    SetKind.WEIGHTED -> candidate.weight?.value
                    SetKind.TIMED -> candidate.durationMs?.toDouble()
                }
            }
            .maxOrNull()

        return listOfNotNull(recordBest, activeBest).maxOrNull()
    }

    private fun labelFor(
        kind: ActivePrFeedbackKind,
        newValue: Double,
        reps: Int?,
        previous: Double?
    ): String {
        val value = when (kind) {
            ActivePrFeedbackKind.BODYWEIGHT_REPS -> "${newValue.toInt()} reps"
            ActivePrFeedbackKind.WEIGHT_FOR_REPS -> "${newValue.trimmed()} kg x ${reps ?: 0}"
            ActivePrFeedbackKind.TIME -> formatDuration(newValue.toLong())
        }
        return if (previous == null) {
            "New PR: $value"
        } else {
            "PR: $value"
        }
    }

    private fun Double.trimmed(): String =
        if (this % 1.0 == 0.0) toInt().toString() else toString()

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "$minutes:${seconds.toString().padStart(2, '0')}"
        }
    }
}
