package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.newFoundationId
import com.jjswigut.oopsallprs.domain.model.resolveLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.snapshotReference
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.repository.ExerciseRepository
import com.jjswigut.oopsallprs.domain.repository.LoggingConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.UserExerciseConfigurationRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

object LoggingConfigurationTransforms {
    fun bodyweightAddedLoad(
        base: LoggingConfiguration,
        enabled: Boolean
    ): LoggingConfiguration {
        val incompatibleLoad = base.measures.firstOrNull {
            it.kind == MeasureKind.LOAD &&
                it.loadRole != LoadRole.ADDED_TO_BODYWEIGHT &&
                it.loadRole != LoadRole.LEGACY_UNSPECIFIED
        }
        require(incompatibleLoad == null) {
            "Added bodyweight load cannot replace ${incompatibleLoad?.loadRole}"
        }
        val withoutAddedLoad = base.measures.filterNot {
            it.kind == MeasureKind.LOAD && (enabled || it.loadRole == LoadRole.ADDED_TO_BODYWEIGHT)
        }
        val withRequiredReps = withoutAddedLoad
            .map { measure ->
                if (measure.kind == MeasureKind.REPETITIONS) {
                    measure.copy(requirement = MeasureRequirement.REQUIRED)
                } else {
                    measure
                }
            }
            .let { measures ->
                if (measures.none { it.kind == MeasureKind.REPETITIONS }) {
                    listOf(MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED)) + measures
                } else {
                    measures
                }
            }
        val transformed = if (enabled) {
            val load = MeasureSpec(
                kind = MeasureKind.LOAD,
                requirement = MeasureRequirement.OPTIONAL,
                loadRole = LoadRole.ADDED_TO_BODYWEIGHT
            )
            val repsIndex = withRequiredReps.indexOfFirst { it.kind == MeasureKind.REPETITIONS }
            withRequiredReps.toMutableList().apply { add(repsIndex + 1, load) }
        } else {
            withRequiredReps
        }
        return base.withNewIdentity(measures = transformed)
    }

    fun observedEffort(
        base: LoggingConfiguration,
        supportedKinds: List<EffortKind>
    ): LoggingConfiguration =
        base.withNewIdentity(
            observedEffort = supportedKinds.takeIf { it.isNotEmpty() }?.let(::ObservedEffortSpec)
        )

    private fun LoggingConfiguration.withNewIdentity(
        measures: List<MeasureSpec> = this.measures,
        observedEffort: ObservedEffortSpec? = this.observedEffort
    ): LoggingConfiguration {
        if (measures == this.measures && observedEffort == this.observedEffort) return this
        return copy(
            id = LoggingConfigurationId(newFoundationId("logging-config").value),
            measures = measures,
            observedEffort = observedEffort
        )
    }
}

data class ResolvedExerciseLogging(
    val definition: ExerciseCatalogItem,
    val resolved: ResolvedLoggingConfiguration
)

data class ActiveConfigurationPreferenceState(
    val activeConfiguration: LoggingConfiguration,
    val savedDefault: UserExerciseConfiguration?,
    val matchesSavedDefault: Boolean
)

class ExerciseLoggingConfigurationUseCases(
    private val exercises: ExerciseRepository,
    private val configurations: LoggingConfigurationRepository,
    private val userConfigurations: UserExerciseConfigurationRepository,
    private val workouts: WorkoutRepository,
    private val activeUx: ActiveWorkoutUxRepository? = null
) {
    suspend fun loggingConfiguration(id: LoggingConfigurationId): LoggingConfiguration? =
        configurations.loggingConfiguration(id)

    suspend fun resolveDefinition(exerciseDefinitionId: FoundationId): FoundationResult<ResolvedExerciseLogging> {
        val definition = exercises.exercise(exerciseDefinitionId)
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseDefinitionId"))
        val userDefault = userConfigurations.userExerciseConfiguration(exerciseDefinitionId)
        val resolved = definition.resolveLoggingConfiguration(userDefault)
        return when (val saved = configurations.saveLoggingConfiguration(resolved.configuration)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(
                ResolvedExerciseLogging(
                    definition = definition,
                    resolved = resolved.copy(configuration = saved.value)
                )
            )
        }
    }

    suspend fun snapshotReference(exerciseDefinitionId: FoundationId): FoundationResult<ExerciseReference> =
        when (val resolved = resolveDefinition(exerciseDefinitionId)) {
            is FoundationResult.Failure -> resolved
            is FoundationResult.Success -> foundationSuccess(
                resolved.value.definition.snapshotReference().copy(
                    resolvedLoggingConfiguration = resolved.value.resolved
                )
            )
        }

    suspend fun snapshotRoutineExercise(
        exercise: RoutineExercise,
        capturedConfigurationId: LoggingConfigurationId? = null
    ): FoundationResult<RoutineExercise> {
        val definition = exercises.exercise(exercise.exerciseCatalogId)
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: ${exercise.exerciseCatalogId}"))
        val userDefault = userConfigurations.userExerciseConfiguration(definition.id)
        val resolved = if (capturedConfigurationId == null) {
            definition.resolveLoggingConfiguration(userDefault)
        } else {
            val captured = configurations.loggingConfiguration(capturedConfigurationId)
                ?: return foundationFailure(
                    FoundationError.NotFound("Logging configuration not found: $capturedConfigurationId")
                )
            ResolvedLoggingConfiguration(
                configuration = captured,
                source = sourceFor(captured, definition, userDefault)
            )
        }
        return when (val saved = configurations.saveLoggingConfiguration(resolved.configuration)) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationSuccess(
                exercise.copy(
                    definitionRevisionSnapshot = definition.definitionRevision,
                    seedKeySnapshot = definition.seedKey,
                    resolvedLoggingConfiguration = resolved.copy(configuration = saved.value)
                )
            )
        }
    }

    suspend fun setBodyweightAddedLoad(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        enabled: Boolean,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveExercise> {
        val located = locateActiveExercise(activeWorkoutId, exerciseInstanceId)
        if (located is FoundationResult.Failure) return located
        val (workout, exercise) = (located as FoundationResult.Success).value
        if (!exercise.reference.isBodyweight) {
            return foundationFailure(FoundationError.Validation("Added bodyweight load is only available for bodyweight exercises"))
        }
        val transformed = try {
            LoggingConfigurationTransforms.bodyweightAddedLoad(
                base = exercise.resolvedLoggingConfiguration.configuration,
                enabled = enabled
            )
        } catch (error: IllegalArgumentException) {
            return foundationFailure(FoundationError.Validation(error.message ?: "Added load is unavailable"))
        }
        return saveWorkoutOverride(workout, exercise, transformed, now) { draft ->
            draft.copy(weight = draft.weight.takeIf { enabled })
        }
    }

    suspend fun setObservedEffort(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        supportedKinds: List<EffortKind>,
        now: Instant = Clock.System.now()
    ): FoundationResult<ActiveExercise> {
        val located = locateActiveExercise(activeWorkoutId, exerciseInstanceId)
        if (located is FoundationResult.Failure) return located
        val (workout, exercise) = (located as FoundationResult.Success).value
        val transformed = try {
            LoggingConfigurationTransforms.observedEffort(
                base = exercise.resolvedLoggingConfiguration.configuration,
                supportedKinds = supportedKinds
            )
        } catch (error: IllegalArgumentException) {
            return foundationFailure(FoundationError.Validation(error.message ?: "Observed effort is invalid"))
        }
        return saveWorkoutOverride(workout, exercise, transformed, now) { draft ->
            draft.copy(observedEffort = draft.observedEffort.takeIf { transformed.observedEffort != null })
        }
    }

    suspend fun saveActiveConfigurationAsDefault(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId,
        now: Instant = Clock.System.now()
    ): FoundationResult<UserExerciseConfiguration> {
        val located = locateActiveExercise(activeWorkoutId, exerciseInstanceId)
        if (located is FoundationResult.Failure) return located
        val exercise = (located as FoundationResult.Success).value.second
        val definition = exercises.exercise(exercise.reference.exerciseCatalogId)
            ?: return foundationFailure(
                FoundationError.NotFound("Exercise not found: ${exercise.reference.exerciseCatalogId}")
            )
        if (definition.definitionRevision != exercise.reference.definitionRevisionSnapshot) {
            return foundationFailure(
                FoundationError.Conflict("Exercise definition changed before its default could be saved")
            )
        }
        return when (
            val saved = configurations.saveLoggingConfiguration(
                exercise.resolvedLoggingConfiguration.configuration
            )
        ) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> userConfigurations.saveUserExerciseConfiguration(
                UserExerciseConfiguration(
                    exerciseDefinitionId = definition.id,
                    configuration = saved.value,
                    basedOnDefinitionRevision = definition.definitionRevision,
                    configuredAt = now
                )
            )
        }
    }

    suspend fun activeConfigurationPreferenceState(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId
    ): FoundationResult<ActiveConfigurationPreferenceState> {
        val located = locateActiveExercise(activeWorkoutId, exerciseInstanceId)
        if (located is FoundationResult.Failure) return located
        val exercise = (located as FoundationResult.Success).value.second
        val savedDefault = userConfigurations.userExerciseConfiguration(exercise.reference.exerciseCatalogId)
        val activeConfiguration = exercise.resolvedLoggingConfiguration.configuration
        return foundationSuccess(
            ActiveConfigurationPreferenceState(
                activeConfiguration = activeConfiguration,
                savedDefault = savedDefault,
                matchesSavedDefault = savedDefault?.configuration == activeConfiguration
            )
        )
    }

    private suspend fun saveWorkoutOverride(
        workout: ActiveWorkout,
        exercise: ActiveExercise,
        configuration: LoggingConfiguration,
        now: Instant,
        transformDraft: (PersistedSetDraft) -> PersistedSetDraft
    ): FoundationResult<ActiveExercise> {
        val canonical = when (val saved = configurations.saveLoggingConfiguration(configuration)) {
            is FoundationResult.Failure -> return saved
            is FoundationResult.Success -> saved.value
        }
        val updatedExercise = exercise.withWorkoutOverride(canonical)
        val ux = activeUx
        val originals = ux?.loadSetDrafts(workout.id)
            .orEmpty()
            .filter { it.exerciseInstanceId == exercise.id }
        val persistedOriginals = mutableListOf<PersistedSetDraft>()
        if (ux != null) {
            originals.forEach { draft ->
                val updatedDraft = transformDraft(draft).copy(
                    captureConfigurationId = canonical.id,
                    updatedAt = now
                )
                when (val saved = ux.saveSetDraft(updatedDraft)) {
                    is FoundationResult.Failure -> {
                        val rollbackError = rollbackDrafts(ux, persistedOriginals)
                        return rollbackError?.let(::foundationFailure) ?: saved
                    }
                    is FoundationResult.Success -> persistedOriginals += draft
                }
            }
        }
        val updatedWorkout = workout.copy(
            exercises = workout.exercises.map { current ->
                if (current.id == exercise.id) updatedExercise else current
            },
            updatedAt = now
        )
        when (val saved = workouts.saveActiveWorkout(updatedWorkout)) {
            is FoundationResult.Failure -> {
                val rollbackError = if (ux == null) null else rollbackDrafts(ux, persistedOriginals)
                return rollbackError?.let(::foundationFailure) ?: saved
            }
            is FoundationResult.Success -> Unit
        }
        return foundationSuccess(updatedExercise)
    }

    private suspend fun rollbackDrafts(
        ux: ActiveWorkoutUxRepository,
        originals: List<PersistedSetDraft>
    ): FoundationError? {
        originals.asReversed().forEach { original ->
            if (ux.saveSetDraft(original) is FoundationResult.Failure) {
                return FoundationError.Persistence(
                    "Logging configuration update failed and the draft rollback could not be completed; reload the workout"
                )
            }
        }
        return null
    }

    private suspend fun locateActiveExercise(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId
    ): FoundationResult<Pair<ActiveWorkout, ActiveExercise>> {
        val workout = workouts.activeWorkout(activeWorkoutId)
            ?: return foundationFailure(FoundationError.NotFound("Active workout not found: $activeWorkoutId"))
        val exercise = workout.exercises.firstOrNull { it.id == exerciseInstanceId }
            ?: return foundationFailure(FoundationError.NotFound("Exercise not found: $exerciseInstanceId"))
        return foundationSuccess(workout to exercise)
    }

    private fun sourceFor(
        configuration: LoggingConfiguration,
        definition: ExerciseCatalogItem,
        userDefault: UserExerciseConfiguration?
    ): LoggingConfigurationSource = when {
        userDefault?.configuration?.id == configuration.id -> LoggingConfigurationSource.USER_DEFAULT
        definition.defaultLoggingConfiguration.id == configuration.id -> LoggingConfigurationSource.DEFINITION_DEFAULT
        else -> LoggingConfigurationSource.WORKOUT_OVERRIDE
    }
}
