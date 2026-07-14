package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.di.createExerciseLoggingComposition
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.EffortTarget
import com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision
import com.jjswigut.oopsallprs.domain.model.ExerciseReference
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
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.ActiveWorkoutUxSession
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.repository.ActiveWorkoutUxRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurableLoggingBehaviorTest {
    @Test
    fun directAddUsesUserThenDefinitionDefaultsForSeededAndCustomExercises() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val seeded = harness.exercise("Pull-Up")
        val custom = harness.catalog.createCustomExercise(
            name = "Ring Row",
            isBodyweight = true,
            now = instant(1_000),
            defaultLoggingConfiguration = LegacyLoggingConfigurations.bodyweight
        ).successValue()
        val seededDefault = LoggingConfigurationTransforms.bodyweightAddedLoad(
            LegacyLoggingConfigurations.bodyweight,
            enabled = true
        )
        harness.store.saveLoggingConfiguration(seededDefault).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = seeded.id,
                configuration = seededDefault,
                basedOnDefinitionRevision = seeded.definitionRevision,
                configuredAt = instant(1_100)
            )
        ).successValue()

        val workout = harness.lifecycle.startEmpty(instant(2_000)).successValue()
        val seededActive = harness.setLogging.addExercise(
            workout.id,
            ExerciseReference(seeded.id, seeded.displayName, isBodyweight = true),
            instant(2_100)
        ).successValue()
        val customActive = harness.setLogging.addExercise(workout.id, custom.id, instant(2_200)).successValue()

        assertEquals(LoggingConfigurationSource.USER_DEFAULT, seededActive.resolvedLoggingConfiguration.source)
        assertEquals(seededDefault.id, seededActive.resolvedLoggingConfiguration.configuration.id)
        assertEquals(seeded.definitionRevision, seededActive.reference.definitionRevisionSnapshot)
        assertEquals(seeded.seedKey, seededActive.reference.seedKeySnapshot)
        assertEquals(LoggingConfigurationSource.DEFINITION_DEFAULT, customActive.resolvedLoggingConfiguration.source)
        assertEquals(custom.definitionRevision, customActive.reference.definitionRevisionSnapshot)
        assertNull(customActive.reference.seedKeySnapshot)

        val seededOverride = harness.configurationManagement.setObservedEffort(
            workout.id,
            seededActive.id,
            supportedKinds = listOf(EffortKind.RIR),
            now = instant(2_300)
        ).successValue()
        val customOverride = harness.configurationManagement.setBodyweightAddedLoad(
            workout.id,
            customActive.id,
            enabled = true,
            now = instant(2_400)
        ).successValue()
        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, seededOverride.resolvedLoggingConfiguration.source)
        assertEquals(seededDefault.measures, seededOverride.resolvedLoggingConfiguration.configuration.measures)
        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, customOverride.resolvedLoggingConfiguration.source)
        assertTrue(customOverride.resolvedLoggingConfiguration.configuration.measures.any { it.loadRole == LoadRole.ADDED_TO_BODYWEIGHT })
    }

    @Test
    fun bodyweightOverrideSupportsBlankZeroAndPositiveLoadAndDisablingOnlyClearsDraftLoad() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val pullUp = harness.exercise("Pull-Up")
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val active = harness.setLogging.addExercise(workout.id, pullUp.id, instant(1_100)).successValue()

        assertEquals(listOf(MeasureKind.REPETITIONS), active.resolvedLoggingConfiguration.configuration.measures.map { it.kind })
        val enabled = harness.configurationManagement.setBodyweightAddedLoad(
            workout.id,
            active.id,
            enabled = true,
            now = instant(1_200)
        ).successValue()
        val addedLoad = enabled.resolvedLoggingConfiguration.configuration.measures.single { it.kind == MeasureKind.LOAD }
        assertEquals(MeasureRequirement.OPTIONAL, addedLoad.requirement)
        assertEquals(LoadRole.ADDED_TO_BODYWEIGHT, addedLoad.loadRole)

        harness.setLogging.confirmSet(workout.id, active.id, SetKind.BODYWEIGHT, 8, null, 0, instant(1_300)).successValue()
        harness.setLogging.confirmSet(workout.id, active.id, SetKind.BODYWEIGHT, 8, WeightKg(0.0), 1, instant(1_400)).successValue()
        harness.setLogging.confirmSet(workout.id, active.id, SetKind.BODYWEIGHT, 6, WeightKg(10.0), 2, instant(1_500)).successValue()
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-added-load"),
            activeWorkoutId = workout.id,
            exerciseInstanceId = active.id,
            position = OrderedPosition(3),
            setKind = SetKind.BODYWEIGHT,
            reps = 5,
            weight = WeightKg(5.0),
            updatedAt = instant(1_600)
        )
        harness.setLogging.saveDraft(draft, instant(1_600)).successValue()

        val disabled = harness.configurationManagement.setBodyweightAddedLoad(
            workout.id,
            active.id,
            enabled = false,
            now = instant(1_700)
        ).successValue()
        val stored = harness.store.activeWorkout(workout.id)!!.exercises.single()
        val recoveredDraft = harness.setLogging.recoverDrafts(workout.id).single()

        assertFalse(disabled.resolvedLoggingConfiguration.configuration.measures.any { it.kind == MeasureKind.LOAD })
        assertEquals(listOf(null, WeightKg(0.0), WeightKg(10.0)), stored.sets.map { it.weight })
        assertNull(recoveredDraft.weight)
        assertEquals(disabled.resolvedLoggingConfiguration.configuration.id, recoveredDraft.captureConfigurationId)
    }

    @Test
    fun savingDefaultChangesFutureResolutionWithoutMutatingRoutineOrHistorySnapshots() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val pullUp = harness.exercise("Pull-Up")
        val routineExerciseId = FoundationId("routine-exercise-pull-up")
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Pull",
            exercises = listOf(
                RoutineExercise(
                    id = routineExerciseId,
                    routineId = FoundationId("pending-routine"),
                    exerciseCatalogId = pullUp.id,
                    displayNameSnapshot = pullUp.displayName,
                    position = OrderedPosition(0),
                    plannedSets = listOf(
                        RoutineSetTemplate(
                            id = FoundationId("routine-set-pull-up"),
                            routineExerciseId = routineExerciseId,
                            position = OrderedPosition(0),
                            targetWeight = null,
                            targetReps = 8,
                            setKind = SetKind.BODYWEIGHT
                        )
                    )
                )
            ),
            now = instant(1_000)
        ).successValue()
        val originalRoutineConfigurationId = routine.exercises.single().resolvedLoggingConfiguration.configuration.id

        val historyWorkout = harness.lifecycle.startEmpty(instant(2_000)).successValue()
        val historyExercise = harness.setLogging.addExercise(historyWorkout.id, pullUp.id, instant(2_100)).successValue()
        val historySet = harness.setLogging.confirmSet(
            historyWorkout.id,
            historyExercise.id,
            SetKind.BODYWEIGHT,
            10,
            null,
            0,
            instant(2_200)
        ).successValue()
        val completed = harness.routines.finishWorkout(historyWorkout.id, instant(2_500)).successValue()

        val overrideWorkout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val overrideExercise = harness.setLogging.addExercise(overrideWorkout.id, pullUp.id, instant(3_100)).successValue()
        val override = harness.configurationManagement.setBodyweightAddedLoad(
            overrideWorkout.id,
            overrideExercise.id,
            enabled = true,
            now = instant(3_200)
        ).successValue()
        harness.configurationManagement.saveActiveConfigurationAsDefault(
            overrideWorkout.id,
            overrideExercise.id,
            instant(3_300)
        ).successValue()
        val preferenceState = harness.configurationManagement.activeConfigurationPreferenceState(
            overrideWorkout.id,
            overrideExercise.id
        ).successValue()

        assertTrue(preferenceState.matchesSavedDefault)
        assertEquals(override.resolvedLoggingConfiguration.configuration, preferenceState.savedDefault?.configuration)
        assertEquals(originalRoutineConfigurationId, harness.store.routine(routine.id)!!.exercises.single().resolvedLoggingConfiguration.configuration.id)
        assertEquals(historySet.captureConfigurationId, harness.store.completedWorkout(completed.id)!!.exercises.single().loggedSets.single().captureConfigurationId)
        harness.lifecycle.discard(overrideWorkout.id, instant(3_400)).successValue()

        val futureWorkout = harness.lifecycle.startEmpty(instant(4_000)).successValue()
        val futureExercise = harness.setLogging.addExercise(futureWorkout.id, pullUp.id, instant(4_100)).successValue()
        assertEquals(LoggingConfigurationSource.USER_DEFAULT, futureExercise.resolvedLoggingConfiguration.source)
        assertEquals(override.resolvedLoggingConfiguration.configuration.id, futureExercise.resolvedLoggingConfiguration.configuration.id)
        assertNotEquals(originalRoutineConfigurationId, futureExercise.resolvedLoggingConfiguration.configuration.id)
    }

    @Test
    fun distanceEffortRoutineTargetsAndDraftsRoundTripWithoutTurningTargetsIntoObservations() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val pullUp = harness.exercise("Pull-Up")
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("pull_up_distance_effort_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
            ),
            observedEffort = ObservedEffortSpec(
                listOf(EffortKind.RPE, EffortKind.RIR, EffortKind.FAILURE_OUTCOME)
            )
        )
        harness.store.saveLoggingConfiguration(configuration).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = pullUp.id,
                configuration = configuration,
                basedOnDefinitionRevision = pullUp.definitionRevision,
                configuredAt = instant(1_000)
            )
        ).successValue()
        val routineExerciseId = FoundationId("routine-exercise-distance")
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Distance Pull",
            exercises = listOf(
                RoutineExercise(
                    id = routineExerciseId,
                    routineId = FoundationId("pending-distance-routine"),
                    exerciseCatalogId = pullUp.id,
                    displayNameSnapshot = pullUp.displayName,
                    position = OrderedPosition(0),
                    plannedSets = listOf(
                        RoutineSetTemplate(
                            id = FoundationId("routine-set-distance"),
                            routineExerciseId = routineExerciseId,
                            position = OrderedPosition(0),
                            targetWeight = null,
                            targetReps = 5,
                            setKind = SetKind.BODYWEIGHT,
                            targetDistanceMeters = 12.5,
                            effortTarget = EffortTarget.Rir(2)
                        )
                    ),
                    rest = RestConfiguration.default()
                )
            ),
            now = instant(2_000)
        ).successValue()

        val launched = harness.lifecycle.startFromRoutine(routine.id, instant(3_000)).successValue()
        val planned = launched.exercises.single().sets.single()
        assertEquals(configuration.id, launched.exercises.single().resolvedLoggingConfiguration.configuration.id)
        assertEquals(12.5, planned.distanceMeters)
        assertNull(planned.observedEffort)

        val effort = Effort(rpeTenths = 85, rir = 1, failureOutcome = FailureOutcome.NOT_REACHED)
        val draft = PersistedSetDraft(
            draftId = FoundationId("draft-distance-effort"),
            activeWorkoutId = launched.id,
            exerciseInstanceId = launched.exercises.single().id,
            position = OrderedPosition(0),
            setKind = SetKind.BODYWEIGHT,
            reps = 5,
            weight = null,
            distanceMeters = 13.0,
            observedEffort = effort,
            updatedAt = instant(3_100)
        )
        harness.setLogging.saveDraft(draft, instant(3_100)).successValue()
        val recovered = harness.setLogging.recoverDrafts(launched.id).single()
        assertEquals(configuration.id, recovered.captureConfigurationId)
        assertEquals(13.0, recovered.distanceMeters)
        assertEquals(effort, recovered.observedEffort)

        val logged = harness.setLogging.confirmSet(
            activeWorkoutId = launched.id,
            exerciseInstanceId = launched.exercises.single().id,
            setKind = SetKind.BODYWEIGHT,
            reps = 5,
            weight = null,
            position = 0,
            loggedAt = instant(3_200),
            distanceMeters = 13.0,
            observedEffort = effort
        ).successValue()
        assertEquals(configuration.id, logged.captureConfigurationId)
        assertEquals(13.0, logged.distanceMeters)
        assertEquals(effort, logged.observedEffort)
        assertEquals(EffortTarget.Rir(2), routine.exercises.single().plannedSets.single().effortTarget)
    }

    @Test
    fun compositeConfigurationKeepsRepetitionsWhenLegacyProjectionIsTimed() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val exercise = harness.exercise("Pull-Up")
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("repetitions_and_duration_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.DURATION, MeasureRequirement.REQUIRED)
            )
        )
        harness.store.saveLoggingConfiguration(configuration).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = exercise.id,
                configuration = configuration,
                basedOnDefinitionRevision = exercise.definitionRevision,
                configuredAt = instant(1_000)
            )
        ).successValue()

        val workout = harness.lifecycle.startEmpty(instant(2_000)).successValue()
        val active = harness.setLogging.addExercise(workout.id, exercise.id, instant(2_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = active.id,
            setKind = SetKind.TIMED,
            reps = 3,
            weight = null,
            position = 0,
            loggedAt = instant(2_200),
            durationMs = 30_000
        ).successValue()
        assertEquals(3, logged.reps)

        val edited = harness.setLogging.editLoggedSet(
            activeWorkoutId = workout.id,
            setId = logged.id,
            reps = 4,
            weight = null,
            now = instant(2_300),
            durationMs = 35_000
        ).successValue()
        assertEquals(4, edited.reps)
        assertEquals(35_000, edited.durationMs)
    }

    @Test
    fun effortTransformRetainsMeasureOrderAndUsesNewImmutableIdentity() {
        val base = LegacyLoggingConfigurations.weighted
        val transformed = LoggingConfigurationTransforms.observedEffort(
            base,
            listOf(EffortKind.RPE, EffortKind.RIR, EffortKind.FAILURE_OUTCOME)
        )

        assertEquals(base.measures, transformed.measures)
        assertEquals(listOf(EffortKind.RPE, EffortKind.RIR, EffortKind.FAILURE_OUTCOME), transformed.observedEffort?.kinds)
        assertNotEquals(base.id, transformed.id)
    }

    @Test
    fun distanceOnlySetCanBeConfirmedAndEditedWithNullReps() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val pullUp = harness.exercise("Pull-Up")
        val distanceOnly = LoggingConfiguration(
            id = LoggingConfigurationId("distance_only_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        harness.store.saveLoggingConfiguration(distanceOnly).successValue()
        harness.store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = pullUp.id,
                configuration = distanceOnly,
                basedOnDefinitionRevision = pullUp.definitionRevision,
                configuredAt = instant(1_000)
            )
        ).successValue()
        val workout = harness.lifecycle.startEmpty(instant(2_000)).successValue()
        val active = harness.setLogging.addExercise(workout.id, pullUp.id, instant(2_100)).successValue()
        val logged = harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = active.id,
            setKind = SetKind.BODYWEIGHT,
            reps = null,
            weight = null,
            position = 0,
            loggedAt = instant(2_200),
            distanceMeters = 100.0
        ).successValue()

        val edited = harness.setLogging.editLoggedSet(
            activeWorkoutId = workout.id,
            setId = logged.id,
            reps = null,
            weight = null,
            now = instant(2_300),
            distanceMeters = 125.0
        ).successValue()

        assertNull(edited.reps)
        assertEquals(125.0, edited.distanceMeters)
    }

    @Test
    fun failedDraftUpdateCompensatesBeforeReportingOverrideFailure() = runTest {
        val harness = BehaviorHarness().apply { seed() }
        val pullUp = harness.exercise("Pull-Up")
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val active = harness.setLogging.addExercise(workout.id, pullUp.id, instant(1_100)).successValue()
        val first = draft(workout.id, active.id, "draft-first", 0, instant(1_200))
        val second = draft(workout.id, active.id, "draft-second", 1, instant(1_300))
        harness.setLogging.saveDraft(first, instant(1_200)).successValue()
        harness.setLogging.saveDraft(second, instant(1_300)).successValue()
        val originalWorkout = harness.store.activeWorkout(workout.id)
        val originalDrafts = harness.store.loadSetDrafts(workout.id)
        val failingUx = FailOnDraftSaveRepository(harness.store, failOnAttempt = 2)
        val management = ExerciseLoggingConfigurationUseCases(
            harness.store,
            harness.store,
            harness.store,
            harness.store,
            failingUx
        )

        val result = management.setBodyweightAddedLoad(
            workout.id,
            active.id,
            enabled = true,
            now = instant(1_400)
        )

        assertTrue(result is FoundationResult.Failure)
        assertEquals(originalWorkout, harness.store.activeWorkout(workout.id))
        assertEquals(originalDrafts, harness.store.loadSetDrafts(workout.id))
    }

    @Test
    fun addedLoadTransformRefusesToReinterpretAssistance() {
        val assisted = LoggingConfiguration(
            id = LoggingConfigurationId("assisted_bodyweight_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.ASSISTANCE)
            )
        )

        assertFailsWith<IllegalArgumentException> {
            LoggingConfigurationTransforms.bodyweightAddedLoad(assisted, enabled = true)
        }
    }

    private fun draft(
        workoutId: FoundationId,
        exerciseId: FoundationId,
        id: String,
        position: Int,
        updatedAt: kotlinx.datetime.Instant
    ): PersistedSetDraft = PersistedSetDraft(
        draftId = FoundationId(id),
        activeWorkoutId = workoutId,
        exerciseInstanceId = exerciseId,
        position = OrderedPosition(position),
        setKind = SetKind.BODYWEIGHT,
        reps = 5,
        weight = null,
        updatedAt = updatedAt
    )
}

private class BehaviorHarness {
    val store = InMemoryFoundationStore()
    private val composition = createExerciseLoggingComposition(store, store, store, store)
    val configurationManagement = requireNotNull(composition.management)
    val lifecycle = WorkoutLifecycleUseCases(store, store, store, activeUx = store, preferences = store)
    val setLogging = SetLoggingUseCases(
        workouts = store,
        setLedger = store,
        preferences = store,
        configurationManagement = configurationManagement,
        activeUx = store
    )
    val catalog = ExerciseCatalogUseCases(store, store, store)
    val routines = RoutineUseCases(
        workouts = store,
        routines = store,
        activeUx = store,
        preferences = store,
        configurationManagement = configurationManagement
    )

    suspend fun seed() {
        ExerciseSeedIngestion(store).ingest(SAMPLE_CSV).successValue()
    }

    suspend fun exercise(displayName: String) =
        store.all().single { it.displayName == displayName }
}

private class FailOnDraftSaveRepository(
    private val delegate: ActiveWorkoutUxRepository,
    private val failOnAttempt: Int
) : ActiveWorkoutUxRepository {
    private var saveAttempts = 0

    override suspend fun loadUxSession(activeWorkoutId: FoundationId): ActiveWorkoutUxSession? =
        delegate.loadUxSession(activeWorkoutId)

    override suspend fun saveUxSession(session: ActiveWorkoutUxSession): FoundationResult<ActiveWorkoutUxSession> =
        delegate.saveUxSession(session)

    override suspend fun loadSetDrafts(activeWorkoutId: FoundationId): List<PersistedSetDraft> =
        delegate.loadSetDrafts(activeWorkoutId)

    override suspend fun saveSetDraft(draft: PersistedSetDraft): FoundationResult<PersistedSetDraft> {
        saveAttempts += 1
        return if (saveAttempts == failOnAttempt) {
            foundationFailure(FoundationError.Persistence("Injected draft failure"))
        } else {
            delegate.saveSetDraft(draft)
        }
    }

    override suspend fun clearExerciseDrafts(
        activeWorkoutId: FoundationId,
        exerciseInstanceId: FoundationId
    ): FoundationResult<Unit> = delegate.clearExerciseDrafts(activeWorkoutId, exerciseInstanceId)

    override suspend fun clearWorkoutUx(
        activeWorkoutId: FoundationId,
        now: kotlinx.datetime.Instant
    ): FoundationResult<Unit> = delegate.clearWorkoutUx(activeWorkoutId, now)
}
