package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

enum class WorkoutStatus {
    ACTIVE,
    COMPLETED,
    DISCARDED
}

data class ActiveWorkout(
    val id: FoundationId,
    val startedAt: Instant,
    val routineId: FoundationId? = null,
    val routineSnapshotName: String? = null,
    val exercises: List<ActiveExercise> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val status: WorkoutStatus = WorkoutStatus.ACTIVE
) {
    fun loggedSets(): List<ExerciseSet> = exercises.flatMap { it.sets }.filter { it.isLogged }

    fun effectiveStartedAt(startTimerWithFirstSet: Boolean): Instant =
        if (startTimerWithFirstSet) {
            loggedSets().mapNotNull(ExerciseSet::loggedAt).minOrNull() ?: startedAt
        } else {
            startedAt
        }
}
