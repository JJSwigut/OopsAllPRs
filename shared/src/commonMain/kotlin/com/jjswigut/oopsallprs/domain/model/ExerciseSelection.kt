package com.jjswigut.oopsallprs.domain.model

data class ExerciseReference(
    val exerciseCatalogId: FoundationId,
    val displayNameSnapshot: String,
    val isBodyweight: Boolean,
    val loggingMode: ExerciseLoggingMode = if (isBodyweight) ExerciseLoggingMode.BODYWEIGHT else ExerciseLoggingMode.WEIGHTED,
    val equipmentSnapshot: String? = null,
    val definitionOriginSnapshot: ExerciseDefinitionOrigin? = null,
    val definitionRevisionSnapshot: ExerciseDefinitionRevision = ExerciseDefinitionRevision(1),
    val seedKeySnapshot: ExerciseSeedKey? = null,
    val resolvedLoggingConfiguration: ResolvedLoggingConfiguration = ResolvedLoggingConfiguration(
        configuration = loggingMode.toLegacyLoggingConfiguration(),
        source = LoggingConfigurationSource.DEFINITION_DEFAULT
    )
)

data class ActiveExercise(
    val id: FoundationId,
    val activeWorkoutId: FoundationId,
    val reference: ExerciseReference,
    val position: OrderedPosition,
    val groupContext: ActiveExerciseGroupContext? = null,
    val sets: List<ExerciseSet> = emptyList(),
    val rest: RestConfiguration = RestConfiguration.default()
) {
    val resolvedLoggingConfiguration: ResolvedLoggingConfiguration
        get() = reference.resolvedLoggingConfiguration

    fun withWorkoutOverride(configuration: LoggingConfiguration): ActiveExercise =
        copy(
            reference = reference.copy(
                resolvedLoggingConfiguration = ResolvedLoggingConfiguration(
                    configuration = configuration,
                    source = LoggingConfigurationSource.WORKOUT_OVERRIDE
                )
            )
        )
}

data class ActiveExerciseGroupContext(
    val groupId: FoundationId,
    val groupPosition: OrderedPosition,
    val label: String,
    val rounds: Int
)
