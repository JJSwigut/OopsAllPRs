package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class ReusableRoutine(
    val id: FoundationId,
    val name: String,
    val exercises: List<RoutineExercise>,
    val createdAt: Instant,
    val updatedAt: Instant,
    val sourceCompletedWorkoutId: FoundationId? = null,
    val archivedAt: Instant? = null
)

data class RoutineExercise(
    val id: FoundationId,
    val routineId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayNameSnapshot: String,
    val position: OrderedPosition,
    val plannedSets: List<RoutineSetTemplate>,
    val rest: RestConfiguration = RestConfiguration.default()
)

data class RoutineSetTemplate(
    val id: FoundationId,
    val routineExerciseId: FoundationId,
    val position: OrderedPosition,
    val targetWeight: WeightKg?,
    val targetReps: Int?,
    val targetDurationMs: Long? = null,
    val setKind: SetKind
)

data class CompletedExercise(
    val id: FoundationId,
    val completedWorkoutId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayNameSnapshot: String,
    val position: OrderedPosition,
    val loggedSets: List<ExerciseSet>,
    val rest: RestConfiguration = RestConfiguration.default()
)

data class CompletedWorkout(
    val id: FoundationId,
    val sourceActiveWorkoutId: FoundationId,
    val startedAt: Instant,
    val finishedAt: Instant,
    val durationMs: Long,
    val routineId: FoundationId?,
    val exercises: List<CompletedExercise>,
    val createdAt: Instant
)
