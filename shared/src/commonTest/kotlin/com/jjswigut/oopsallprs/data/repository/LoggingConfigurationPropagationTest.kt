package com.jjswigut.oopsallprs.data.repository

import com.jjswigut.oopsallprs.domain.model.ActiveExercise
import com.jjswigut.oopsallprs.domain.model.ActiveWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.ExerciseCatalogItem
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionOrigin
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseLoggingMode
import com.jjswigut.oopsallprs.domain.model.ExerciseSeedImport
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.PersistedSetDraft
import com.jjswigut.oopsallprs.domain.model.ResolvedLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.ReusableRoutine
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.snapshotReference
import com.jjswigut.oopsallprs.domain.model.toLegacyLoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.validateLoadMagnitude
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class LoggingConfigurationPropagationTest {
    @Test
    fun legacyFactoriesUseCanonicalDeterministicIds() {
        assertSame(LegacyLoggingConfigurations.weighted, ExerciseLoggingMode.WEIGHTED.toLegacyLoggingConfiguration())
        assertSame(LegacyLoggingConfigurations.bodyweight, SetKind.BODYWEIGHT.toLegacyLoggingConfiguration())
        assertEquals("legacy_weighted_v1", LegacyLoggingConfigurations.weighted.id.value)
        assertEquals("legacy_bodyweight_v1", LegacyLoggingConfigurations.bodyweight.id.value)
        assertEquals("legacy_timed_v1", LegacyLoggingConfigurations.timed.id.value)
        assertNotEquals(
            LegacyLoggingConfigurations.bodyweight.id,
            SetKind.BODYWEIGHT.toLegacyLoggingConfiguration(hasLegacyLoad = true).id
        )
    }

    @Test
    fun directAddSnapshotsResolvedUserDefault() = runTest {
        val store = InMemoryFoundationStore()
        val definition = seedDefinition()
        store.saveDefinition(definition)
        val preferred = userConfiguration(definition, addedWeightConfiguration("user-added-weight"), configuredAt = instant(2))
        store.saveUserExerciseConfiguration(preferred).successValue()
        val workout = activeWorkout()
        store.createActiveWorkout(workout).successValue()

        val added = SetLoggingUseCases(store, store)
            .addExercise(
                activeWorkoutId = workout.id,
                reference = definition.snapshotReference(store.userExerciseConfiguration(definition.id)),
                now = instant(3)
            )
            .successValue()

        assertEquals(LoggingConfigurationSource.USER_DEFAULT, added.resolvedLoggingConfiguration.source)
        assertEquals(preferred.configuration, added.resolvedLoggingConfiguration.configuration)
        assertEquals(definition.definitionRevision, added.reference.definitionRevisionSnapshot)
        assertEquals(definition.seedKey, added.reference.seedKeySnapshot)
    }

    @Test
    fun existingRoutineRetainsSnapshotUntilExplicitlyUpdated() = runTest {
        val store = InMemoryFoundationStore()
        val definition = seedDefinition()
        store.saveDefinition(definition)
        val firstDefault = userConfiguration(definition, addedWeightConfiguration("user-v1"), instant(2))
        store.saveUserExerciseConfiguration(firstDefault).successValue()
        val originalExercise = routineExercise(
            resolved = definition.snapshotReference(firstDefault).resolvedLoggingConfiguration
        )
        val routine = routine(originalExercise)
        store.saveRoutine(routine).successValue()

        val nextDefault = userConfiguration(definition, distanceConfiguration("user-v2"), instant(3))
        store.saveUserExerciseConfiguration(nextDefault).successValue()

        val retained = store.routine(routine.id)?.exercises?.single()
        assertEquals(firstDefault.configuration, retained?.resolvedLoggingConfiguration?.configuration)

        val explicitlyUpdated = assertNotNull(retained).updateConfigurationSnapshot(definition, nextDefault)
        store.saveRoutine(routine.copy(exercises = listOf(explicitlyUpdated), updatedAt = instant(4))).successValue()

        assertEquals(
            nextDefault.configuration,
            store.routine(routine.id)?.exercises?.single()?.resolvedLoggingConfiguration?.configuration
        )
    }

    @Test
    fun workoutOnlyOverrideChangesOnlyActiveSnapshot() {
        val definition = seedDefinition()
        val userDefault = userConfiguration(definition, addedWeightConfiguration("saved-default"), instant(2))
        val active = activeExercise(definition.snapshotReference(userDefault))
        val override = distanceConfiguration("workout-only")

        val updated = active.withWorkoutOverride(override)

        assertEquals(override, updated.resolvedLoggingConfiguration.configuration)
        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, updated.resolvedLoggingConfiguration.source)
        assertEquals(userDefault.configuration, active.resolvedLoggingConfiguration.configuration)
        assertEquals(LegacyLoggingConfigurations.bodyweight, definition.defaultLoggingConfiguration)
    }

    @Test
    fun saveAsDefaultDoesNotMutateSeedDefinition() = runTest {
        val store = InMemoryFoundationStore()
        val definition = seedDefinition()
        store.saveDefinition(definition)
        val before = store.exercise(definition.id)
        val preferred = userConfiguration(definition, addedWeightConfiguration("saved-default"), instant(2))

        store.saveUserExerciseConfiguration(preferred).successValue()

        assertEquals(before, store.exercise(definition.id))
        assertEquals(ExerciseDefinitionOrigin.SEED, store.exercise(definition.id)?.origin)
        assertEquals(definition.seedKey, store.exercise(definition.id)?.seedKey)
        assertEquals(preferred, store.userExerciseConfiguration(definition.id))
    }

    @Test
    fun activeAndDraftSnapshotsAreProcessReady() = runTest {
        val store = InMemoryFoundationStore()
        val configuration = distanceConfiguration("carry-with-effort", effort = true)
        store.saveLoggingConfiguration(configuration).successValue()
        val definition = seedDefinition(defaultConfiguration = configuration, mode = ExerciseLoggingMode.WEIGHTED)
        val reference = definition.snapshotReference(workoutOverride = configuration)
        val active = activeWorkout(exercises = listOf(activeExercise(reference)))
        store.createActiveWorkout(active).successValue()
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-1"),
            activeWorkoutId = active.id,
            exerciseInstanceId = active.exercises.single().id,
            position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            reps = null,
            weight = WeightKg(24.0),
            updatedAt = instant(2),
            captureConfigurationId = configuration.id,
            distanceMeters = 40.0,
            observedEffort = Effort(rir = 2)
        )
        store.saveSetDraft(draft).successValue()

        val reloadedExercise = store.activeWorkout(active.id)?.exercises?.single()
        val reloadedDraft = store.loadSetDrafts(active.id).single()
        assertEquals(configuration, reloadedExercise?.resolvedLoggingConfiguration?.configuration)
        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, reloadedExercise?.resolvedLoggingConfiguration?.source)
        assertEquals(configuration.id, reloadedDraft.captureConfigurationId)
        assertEquals(40.0, reloadedDraft.distanceMeters)
        assertEquals(Effort(rir = 2), reloadedDraft.observedEffort)
    }

    @Test
    fun mixedLoadedAndUnloadedBodyweightSetsRemainValid() {
        val configuration = addedWeightConfiguration("bodyweight-added")
        val unloaded = exerciseSet(
            id = "unloaded",
            setKind = SetKind.BODYWEIGHT,
            configuration = configuration,
            reps = 12,
            weight = null
        )
        val loaded = exerciseSet(
            id = "loaded",
            setKind = SetKind.BODYWEIGHT,
            configuration = configuration,
            reps = 8,
            weight = WeightKg(10.0)
        )
        val zeroLoad = unloaded.copy(id = FoundationId("zero-load"), weight = WeightKg(0.0))

        assertNull(unloaded.validateForLogging(configuration))
        assertNull(loaded.validateForLogging(configuration))
        assertNull(zeroLoad.validateForLogging(configuration))
        assertEquals(SetKind.BODYWEIGHT, unloaded.setKind)
        assertEquals(configuration.id, loaded.captureConfigurationId)
    }

    @Test
    fun assistanceRemainsDistinctAndRequiresItsOwnLoad() {
        val configuration = configuration(
            id = "assistance",
            measures = listOf(
                required(MeasureKind.REPETITIONS),
                load(MeasureRequirement.REQUIRED, LoadRole.ASSISTANCE)
            )
        )
        val assisted = exerciseSet(
            id = "assisted",
            setKind = SetKind.BODYWEIGHT,
            configuration = configuration,
            reps = 6,
            weight = WeightKg(35.0)
        )
        val missingAssistance = assisted.copy(id = FoundationId("missing-assistance"), weight = null)

        assertNull(assisted.validateForLogging(configuration))
        assertNotNull(missingAssistance.validateForLogging(configuration))
        assertEquals(LoadRole.ASSISTANCE, configuration.measures.single { it.kind == MeasureKind.LOAD }.loadRole)
        assertNotEquals(
            LoadRole.ADDED_TO_BODYWEIGHT,
            configuration.measures.single { it.kind == MeasureKind.LOAD }.loadRole
        )
    }

    @Test
    fun negativeAddedBodyweightLoadIsRejectedWhileZeroRemainsValid() {
        val error = validateLoadMagnitude(
            value = -2.5,
            requirement = MeasureRequirement.OPTIONAL,
            loadRole = LoadRole.ADDED_TO_BODYWEIGHT
        )

        assertEquals("Added bodyweight load cannot be negative", error?.message)
        assertNull(
            validateLoadMagnitude(
                value = 0.0,
                requirement = MeasureRequirement.OPTIONAL,
                loadRole = LoadRole.ADDED_TO_BODYWEIGHT
            )
        )
    }

    @Test
    fun negativeAssistanceLoadIsRejected() {
        val error = validateLoadMagnitude(
            value = -20.0,
            requirement = MeasureRequirement.REQUIRED,
            loadRole = LoadRole.ASSISTANCE
        )

        assertEquals("Assistance load cannot be negative", error?.message)
    }

    @Test
    fun distanceIsValidatedInCanonicalMeters() {
        val configuration = distanceConfiguration("farmer-carry")
        val valid = exerciseSet(
            id = "distance",
            setKind = SetKind.WEIGHTED,
            configuration = configuration,
            reps = null,
            weight = WeightKg(32.0),
            distanceMeters = 50.0
        )

        assertNull(valid.validateForLogging(configuration))
        assertNotNull(valid.copy(distanceMeters = null).validateForLogging(configuration))
        assertNotNull(valid.copy(distanceMeters = 0.0).validateForLogging(configuration))
        assertEquals(50.0, valid.distanceMeters)
    }

    @Test
    fun observedEffortIsCapturedOnlyWhenEnabled() {
        val effortEnabled = configuration(
            id = "effort-enabled",
            measures = listOf(required(MeasureKind.REPETITIONS)),
            effort = ObservedEffortSpec(listOf(EffortKind.RIR, EffortKind.FAILURE_OUTCOME))
        )
        val effort = Effort(rir = 1, failureOutcome = FailureOutcome.NOT_REACHED)
        val set = exerciseSet(
            id = "effort",
            setKind = SetKind.BODYWEIGHT,
            configuration = effortEnabled,
            reps = 10,
            weight = null,
            observedEffort = effort
        )
        val effortDisabled = configuration(
            id = "effort-disabled",
            measures = listOf(required(MeasureKind.REPETITIONS))
        )

        assertNull(set.validateForLogging(effortEnabled))
        assertEquals(effort, set.observedEffort)
        assertNotNull(
            set.copy(captureConfigurationId = effortDisabled.id).validateForLogging(effortDisabled)
        )
    }

    @Test
    fun immutableConfigurationIdsRejectChangedContent() = runTest {
        val store = InMemoryFoundationStore()
        val original = addedWeightConfiguration("immutable")
        store.saveLoggingConfiguration(original).successValue()
        val changed = distanceConfiguration("immutable")

        assertIs<FoundationResult.Failure>(store.saveLoggingConfiguration(changed))
        assertEquals(original, store.loggingConfiguration(original.id))
    }

    private suspend fun InMemoryFoundationStore.saveDefinition(definition: ExerciseCatalogItem) {
        saveSeedItems(
            items = listOf(definition),
            import = ExerciseSeedImport(
                id = FoundationId("seed-import"),
                sourceName = "test",
                sourceHash = "v1",
                importedAt = instant(1),
                rowCount = 1,
                rejectedRowCount = 0,
                warnings = emptyList()
            )
        ).successValue()
    }

    private fun seedDefinition(
        defaultConfiguration: LoggingConfiguration = LegacyLoggingConfigurations.bodyweight,
        mode: ExerciseLoggingMode = ExerciseLoggingMode.BODYWEIGHT
    ): ExerciseCatalogItem =
        ExerciseCatalogItem(
            id = FoundationId("exercise-bulgarian-split-squat"),
            canonicalName = "bulgarian split squat",
            displayName = "Bulgarian Split Squat",
            muscleGroup = "Legs",
            equipment = "Bodyweight",
            movementPattern = "Squat",
            exerciseType = "Bodyweight",
            experienceLevel = "Intermediate",
            bodyRegion = "Lower",
            isBodyweight = true,
            loggingMode = mode,
            isUserCreated = false,
            createdAt = instant(1),
            updatedAt = instant(1),
            defaultLoggingConfiguration = defaultConfiguration
        )

    private fun userConfiguration(
        definition: ExerciseCatalogItem,
        configuration: LoggingConfiguration,
        configuredAt: Instant
    ): UserExerciseConfiguration =
        UserExerciseConfiguration(
            exerciseDefinitionId = definition.id,
            configuration = configuration,
            basedOnDefinitionRevision = definition.definitionRevision,
            configuredAt = configuredAt
        )

    private fun routineExercise(resolved: ResolvedLoggingConfiguration): RoutineExercise =
        RoutineExercise(
            id = FoundationId("routine-exercise"),
            routineId = FoundationId("routine"),
            exerciseCatalogId = FoundationId("exercise-bulgarian-split-squat"),
            displayNameSnapshot = "Bulgarian Split Squat",
            position = OrderedPosition(0),
            plannedSets = listOf(
                RoutineSetTemplate(
                    id = FoundationId("routine-set"),
                    routineExerciseId = FoundationId("routine-exercise"),
                    position = OrderedPosition(0),
                    targetWeight = null,
                    targetReps = 8,
                    setKind = SetKind.BODYWEIGHT
                )
            ),
            resolvedLoggingConfiguration = resolved
        )

    private fun routine(exercise: RoutineExercise): ReusableRoutine =
        ReusableRoutine(
            id = FoundationId("routine"),
            name = "Lower",
            exercises = listOf(exercise),
            createdAt = instant(1),
            updatedAt = instant(1)
        )

    private fun activeWorkout(exercises: List<ActiveExercise> = emptyList()): ActiveWorkout =
        ActiveWorkout(
            id = FoundationId("active-workout"),
            startedAt = instant(1),
            exercises = exercises,
            createdAt = instant(1),
            updatedAt = instant(1)
        )

    private fun activeExercise(reference: com.jjswigut.oopsallprs.domain.model.ExerciseReference): ActiveExercise =
        ActiveExercise(
            id = FoundationId("active-exercise"),
            activeWorkoutId = FoundationId("active-workout"),
            reference = reference,
            position = OrderedPosition(0)
        )

    private fun exerciseSet(
        id: String,
        setKind: SetKind,
        configuration: LoggingConfiguration,
        reps: Int?,
        weight: WeightKg?,
        distanceMeters: Double? = null,
        observedEffort: Effort? = null
    ): ExerciseSet =
        ExerciseSet(
            id = FoundationId(id),
            exerciseInstanceId = FoundationId("active-exercise"),
            position = OrderedPosition(0),
            setKind = setKind,
            weight = weight,
            reps = reps,
            loggedAt = instant(2),
            createdAt = instant(2),
            updatedAt = instant(2),
            captureConfigurationId = configuration.id,
            distanceMeters = distanceMeters,
            observedEffort = observedEffort
        )

    private fun addedWeightConfiguration(id: String): LoggingConfiguration =
        configuration(
            id = id,
            measures = listOf(
                required(MeasureKind.REPETITIONS),
                load(MeasureRequirement.OPTIONAL, LoadRole.ADDED_TO_BODYWEIGHT)
            )
        )

    private fun distanceConfiguration(id: String, effort: Boolean = false): LoggingConfiguration =
        configuration(
            id = id,
            measures = listOf(
                required(MeasureKind.DISTANCE),
                load(MeasureRequirement.REQUIRED, LoadRole.EXTERNAL_RESISTANCE)
            ),
            effort = ObservedEffortSpec(listOf(EffortKind.RIR)).takeIf { effort }
        )

    private fun configuration(
        id: String,
        measures: List<MeasureSpec>,
        effort: ObservedEffortSpec? = null
    ): LoggingConfiguration =
        LoggingConfiguration(
            id = LoggingConfigurationId(id),
            schemaVersion = LoggingSchemaVersion(1),
            measures = measures,
            observedEffort = effort
        )

    private fun required(kind: MeasureKind): MeasureSpec =
        MeasureSpec(kind, MeasureRequirement.REQUIRED)

    private fun load(requirement: MeasureRequirement, role: LoadRole): MeasureSpec =
        MeasureSpec(MeasureKind.LOAD, requirement, role)

    private fun instant(value: Long): Instant = Instant.fromEpochMilliseconds(value)

    private fun <T> FoundationResult<T>.successValue(): T =
        when (this) {
            is FoundationResult.Success -> value
            is FoundationResult.Failure -> error("Expected success, got $error")
        }
}
