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
    val groupId: FoundationId? = null,
    val groupPosition: OrderedPosition? = null,
    val groupRounds: Int? = null,
    val plannedSets: List<RoutineSetTemplate>,
    val rest: RestConfiguration = RestConfiguration.default(),
    val definitionRevisionSnapshot: ExerciseDefinitionRevision = ExerciseDefinitionRevision(1),
    val seedKeySnapshot: ExerciseSeedKey? = null,
    val resolvedLoggingConfiguration: ResolvedLoggingConfiguration = ResolvedLoggingConfiguration(
        configuration = plannedSets.firstOrNull()?.setKind
            ?.toLegacyLoggingConfiguration(hasLegacyLoad = plannedSets.any { it.targetWeight != null })
            ?: LegacyLoggingConfigurations.weighted,
        source = LoggingConfigurationSource.DEFINITION_DEFAULT
    )
) {
    fun updateConfigurationSnapshot(
        definition: ExerciseCatalogItem,
        userDefault: UserExerciseConfiguration? = null
    ): RoutineExercise {
        require(exerciseCatalogId == definition.id) {
            "Routine exercise does not belong to definition ${definition.id}"
        }
        return copy(
            definitionRevisionSnapshot = definition.definitionRevision,
            seedKeySnapshot = definition.seedKey,
            resolvedLoggingConfiguration = definition.resolveLoggingConfiguration(userDefault)
        )
    }
}

data class RoutineSetTemplate(
    val id: FoundationId,
    val routineExerciseId: FoundationId,
    val position: OrderedPosition,
    val targetWeight: WeightKg?,
    val targetReps: Int?,
    val targetDurationMs: Long? = null,
    val setKind: SetKind,
    val targetDistanceMeters: Double? = null,
    val effortTarget: EffortTarget? = null
) {
    init {
        require(targetDistanceMeters == null || (targetDistanceMeters.isFinite() && targetDistanceMeters >= 0.0)) {
            "Routine target distance must be finite and non-negative"
        }
    }
}

data class CompletedExercise(
    val id: FoundationId,
    val completedWorkoutId: FoundationId,
    val exerciseCatalogId: FoundationId,
    val displayNameSnapshot: String,
    val position: OrderedPosition,
    val loggedSets: List<ExerciseSet>,
    val rest: RestConfiguration = RestConfiguration.default(),
    val groupContext: ActiveExerciseGroupContext? = null
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
