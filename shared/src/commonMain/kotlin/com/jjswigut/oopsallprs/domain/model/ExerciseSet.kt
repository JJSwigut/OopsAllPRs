package com.jjswigut.oopsallprs.domain.model

import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Instant

enum class SetKind {
    WEIGHTED,
    BODYWEIGHT,
    TIMED
}

data class ExerciseSet(
    val id: FoundationId,
    val exerciseInstanceId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val weight: WeightKg?,
    val reps: Int?,
    val loggedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val editedAt: Instant? = null,
    val durationMs: Long? = null
) {
    val isLogged: Boolean = loggedAt != null

    fun validateForLogging(): FoundationError? =
        when {
            setKind == SetKind.TIMED && (durationMs == null || durationMs <= 0L) -> FoundationError.Validation("Timed sets require a positive duration")
            setKind != SetKind.TIMED && (reps == null || reps <= 0) -> FoundationError.Validation("Logged sets require positive reps")
            setKind == SetKind.WEIGHTED && weight == null -> FoundationError.Validation("Weighted sets require a weight")
            setKind == SetKind.WEIGHTED && weight != null && weight.value < 0.0 -> FoundationError.Validation("Weighted sets cannot have negative weight")
            setKind == SetKind.BODYWEIGHT && weight != null && weight.value < 0.0 -> FoundationError.Validation("Bodyweight added load cannot be negative")
            else -> null
        }
}
