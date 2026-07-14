package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.data.exercise.ExerciseSeedIngestion
import com.jjswigut.oopsallprs.data.repository.InMemoryFoundationStore
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.FailureOutcome
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationSource
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.UserExerciseConfiguration
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.ExerciseLoggingConfigurationUseCases
import com.jjswigut.oopsallprs.domain.usecase.PreviousWorkoutDefaultsUseCase
import com.jjswigut.oopsallprs.domain.usecase.SetLoggingUseCases
import com.jjswigut.oopsallprs.domain.usecase.WorkoutLifecycleUseCases
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConfigurableLoggingStateHolderTest {
    @Test
    fun bodyweightDefaultsToRepsOnlyAndAddedLoadOverrideCanBeDisabled() = runTest {
        val harness = ConfigUiHarness()
        val pullUp = harness.seededPullUp()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, pullUp.id, instant(1_100)).successValue()
        val holder = harness.holder()
        holder.hydrate(workout.id, now = instant(1_200))

        val initial = holder.block()
        assertEquals(listOf(MeasureKind.REPETITIONS), initial.loggingConfiguration.measures.map { it.kind })
        assertFalse(initial.tracksAddedWeight)
        assertFalse(initial.canSaveConfigurationAsDefault)

        holder.setBodyweightAddedLoad(exercise.id, enabled = true).successValue()
        val enabled = holder.block()
        assertTrue(enabled.tracksAddedWeight)
        assertEquals(
            listOf(MeasureKind.REPETITIONS, MeasureKind.LOAD),
            enabled.loggingConfiguration.measures.map { it.kind }
        )
        assertEquals(LoadRole.ADDED_TO_BODYWEIGHT, enabled.loggingConfiguration.measures.last().loadRole)
        assertTrue(enabled.canSaveConfigurationAsDefault)

        holder.setBodyweightAddedLoad(exercise.id, enabled = false).successValue()
        val disabled = holder.block()
        assertEquals(listOf(MeasureKind.REPETITIONS), disabled.loggingConfiguration.measures.map { it.kind })
        assertNull(disabled.draft.weight)
        assertTrue(disabled.canSaveConfigurationAsDefault)
    }

    @Test
    fun saveDefaultActionDisappearsAfterPersistingTheActiveOverride() = runTest {
        val harness = ConfigUiHarness()
        val pullUp = harness.seededPullUp()
        val workout = harness.lifecycle.startEmpty(instant(2_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, pullUp.id, instant(2_100)).successValue()
        val holder = harness.holder()
        holder.hydrate(workout.id, now = instant(2_200))

        holder.setBodyweightAddedLoad(exercise.id, enabled = true).successValue()
        assertTrue(holder.block().canSaveConfigurationAsDefault)

        holder.saveActiveConfigurationAsDefault(exercise.id).successValue()

        assertFalse(holder.block().canSaveConfigurationAsDefault)
    }

    @Test
    fun eachEffortKindLogsOnlyItsSelectedObservation() = runTest {
        val harness = ConfigUiHarness()
        val pullUp = harness.seededPullUp()
        val workout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, pullUp.id, instant(3_100)).successValue()
        val holder = harness.holder()
        holder.hydrate(workout.id, now = instant(3_200))

        holder.setEffortKind(exercise.id, EffortKind.RIR).successValue()
        holder.updateDraftEffort(exercise.id, parseEffortInput("2", EffortKind.RIR))
        holder.confirmDraft(exercise.id).successValue()

        holder.setEffortKind(exercise.id, EffortKind.RPE).successValue()
        holder.updateDraftEffort(exercise.id, parseEffortInput("8.5", EffortKind.RPE))
        holder.confirmDraft(exercise.id).successValue()

        holder.setEffortKind(exercise.id, EffortKind.FAILURE_OUTCOME).successValue()
        holder.updateDraftEffort(
            exercise.id,
            MeasureInputUpdate("reached", Effort(failureOutcome = FailureOutcome.REACHED))
        )
        holder.confirmDraft(exercise.id).successValue()

        assertEquals(
            listOf(
                Effort(rir = 2),
                Effort(rpeTenths = 85),
                Effort(failureOutcome = FailureOutcome.REACHED)
            ),
            holder.block().loggedRows.map { it.observedEffort }
        )
    }

    @Test
    fun distanceOnlyDraftKeepsNullRepsThroughConfirmAndEdit() = runTest {
        val harness = ConfigUiHarness()
        val pullUp = harness.seededPullUp()
        val distanceOnly = LoggingConfiguration(
            id = LoggingConfigurationId("ui_distance_only_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED))
        )
        harness.setUserDefault(pullUp.id, pullUp.definitionRevision, distanceOnly)
        val workout = harness.lifecycle.startEmpty(instant(4_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, pullUp.id, instant(4_100)).successValue()
        val holder = harness.holder()
        holder.hydrate(workout.id, now = instant(4_200))

        assertNull(holder.block().draft.reps)
        holder.updateDraftDistanceInput(exercise.id, parseDistanceInput("100"))
        val logged = holder.confirmDraft(exercise.id).successValue()
        assertNull(logged.reps)
        assertEquals(100.0, logged.distanceMeters)

        holder.beginEditSet(logged.id)
        holder.updateEditReps(null)
        holder.updateEditDistanceInput(parseDistanceInput("125"))
        holder.confirmEditSet(instant(4_300)).successValue()

        assertNull(holder.block().loggedRows.single().reps)
        assertEquals(125.0, holder.block().loggedRows.single().distanceMeters)
    }

    @Test
    fun recreationPreservesOverrideFocusConfigurationAndAdvancedDraftValues() = runTest {
        val harness = ConfigUiHarness()
        val pullUp = harness.seededPullUp()
        val base = LoggingConfiguration(
            id = LoggingConfigurationId("ui_reps_distance_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.OPTIONAL)
            )
        )
        harness.setUserDefault(pullUp.id, pullUp.definitionRevision, base)
        val workout = harness.lifecycle.startEmpty(instant(5_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, pullUp.id, instant(5_100)).successValue()
        val first = harness.holder()
        first.hydrate(workout.id, now = instant(5_200))
        first.setBodyweightAddedLoad(exercise.id, enabled = true).successValue()
        first.setEffortKind(exercise.id, EffortKind.RPE).successValue()
        first.updateDraftReps(exercise.id, 8)
        first.updateDraftWeightInput(
            exercise.id,
            parseWeightInput("10", WeightUnit.KILOGRAMS, LoadRole.ADDED_TO_BODYWEIGHT)
        )
        first.updateDraftDistanceInput(exercise.id, parseDistanceInput("250"))
        first.updateDraftEffort(exercise.id, parseEffortInput("8.5", EffortKind.RPE))
        first.setFocus(exercise.id, instant(5_300))
        val before = first.block()

        val recovered = harness.holder()
        recovered.hydrate(workout.id, now = instant(5_400))
        val after = recovered.block()

        assertEquals(LoggingConfigurationSource.WORKOUT_OVERRIDE, after.loggingConfigurationSource)
        assertEquals(before.loggingConfiguration.id, after.loggingConfiguration.id)
        assertEquals(before.draft.captureConfigurationId, after.draft.captureConfigurationId)
        assertEquals(exercise.id, recovered.state.value.focusSnapshot?.exerciseInstanceId)
        assertEquals(8, after.draft.reps)
        assertEquals(10.0, after.draft.weight?.value)
        assertEquals(250.0, after.draft.distanceMeters)
        assertEquals(Effort(rpeTenths = 85), after.draft.observedEffort)
        assertTrue(after.canSaveConfigurationAsDefault)
    }
}

private class ConfigUiHarness {
    val store = InMemoryFoundationStore()
    val management = ExerciseLoggingConfigurationUseCases(store, store, store, store, store)
    val previousDefaults = PreviousWorkoutDefaultsUseCase(store, store)
    val lifecycle = WorkoutLifecycleUseCases(
        workouts = store,
        sessions = store,
        routines = store,
        activeUx = store,
        preferences = store,
        previousDefaults = previousDefaults
    )
    val setLogging = SetLoggingUseCases(
        workouts = store,
        setLedger = store,
        preferences = store,
        configurationManagement = management,
        activeUx = store
    )

    fun holder() = ActiveWorkoutStateHolder(
        setLogging = setLogging,
        lifecycle = lifecycle,
        activeUx = store,
        previousDefaults = previousDefaults,
        configurationManagement = management
    )

    suspend fun seededPullUp() = run {
        if (store.all().isEmpty()) ExerciseSeedIngestion(store).ingest(SAMPLE_CSV).successValue()
        store.all().single { it.displayName == "Pull-Up" }
    }

    suspend fun setUserDefault(
        exerciseId: com.jjswigut.oopsallprs.domain.model.FoundationId,
        revision: com.jjswigut.oopsallprs.domain.model.ExerciseDefinitionRevision,
        configuration: LoggingConfiguration
    ) {
        store.saveLoggingConfiguration(configuration).successValue()
        store.saveUserExerciseConfiguration(
            UserExerciseConfiguration(
                exerciseDefinitionId = exerciseId,
                configuration = configuration,
                basedOnDefinitionRevision = revision,
                configuredAt = instant(900)
            )
        ).successValue()
    }

}

private fun ActiveWorkoutStateHolder.block(): ExerciseBlockState =
    state.value.workout!!.exerciseBlocks.single()
