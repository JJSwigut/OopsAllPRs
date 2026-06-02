package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class ActiveWorkoutUxSession(
    val activeWorkoutId: FoundationId,
    val focusedExerciseInstanceId: FoundationId?,
    val focusedDraftId: FoundationId?,
    val updatedAt: Instant
)

data class PersistedSetDraft(
    val draftId: FoundationId,
    val activeWorkoutId: FoundationId,
    val exerciseInstanceId: FoundationId,
    val position: OrderedPosition,
    val setKind: SetKind,
    val reps: Int?,
    val weight: WeightKg?,
    val durationMs: Long? = null,
    val timerStartedAt: Instant? = null,
    val updatedAt: Instant
)

enum class ActivePrFeedbackKind {
    WEIGHT_FOR_REPS,
    BODYWEIGHT_REPS,
    TIME
}

data class ActivePrFeedback(
    val setId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val kind: ActivePrFeedbackKind,
    val label: String,
    val previousValue: Double?,
    val newValue: Double
)
