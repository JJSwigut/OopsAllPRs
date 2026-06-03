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
    val sets: List<ExerciseSet> = emptyList(),
    val rest: RestConfiguration = RestConfiguration.default()
)
