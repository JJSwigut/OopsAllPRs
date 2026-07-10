package com.jjswigut.oopsallprs.domain.model

data class ExerciseReference(
    val exerciseCatalogId: FoundationId,
    val displayNameSnapshot: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
    val equipmentSnapshot: String? = null
)

data class ActiveExercise(
    val id: FoundationId,
    val activeWorkoutId: FoundationId,
    val reference: ExerciseReference,
    val position: OrderedPosition,
    val groupContext: ActiveExerciseGroupContext? = null,
    val sets: List<ExerciseSet> = emptyList(),
    val rest: RestConfiguration = RestConfiguration.default()
)

data class ActiveExerciseGroupContext(
    val groupId: FoundationId,
    val groupPosition: OrderedPosition,
    val label: String,
    val rounds: Int
)
