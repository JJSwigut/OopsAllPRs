package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class PreviousWorkoutValue(
    val setIndex: Int,
    val setKind: SetKind,
    val weight: WeightKg?,
    val reps: Int?,
    val durationMs: Long? = null,
    val sourceCompletedWorkoutId: FoundationId,
    val sourceSetId: FoundationId
)

data class PreviousWorkoutSnapshot(
    val exerciseCatalogId: FoundationId,
    val completedWorkoutId: FoundationId,
    val finishedAt: Instant,
    val values: List<PreviousWorkoutValue>
) {
    fun valueForSetIndex(index: Int): PreviousWorkoutValue? =
        values.getOrNull(index) ?: values.lastOrNull()
}
