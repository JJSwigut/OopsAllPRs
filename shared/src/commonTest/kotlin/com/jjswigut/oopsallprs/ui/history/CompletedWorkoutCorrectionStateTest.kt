package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.Effort
import com.jjswigut.oopsallprs.domain.model.EffortKind
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoadRole
import com.jjswigut.oopsallprs.domain.model.LegacyLoggingConfigurations
import com.jjswigut.oopsallprs.domain.model.LoggingConfiguration
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.LoggingSchemaVersion
import com.jjswigut.oopsallprs.domain.model.MeasureKind
import com.jjswigut.oopsallprs.domain.model.MeasureRequirement
import com.jjswigut.oopsallprs.domain.model.MeasureSpec
import com.jjswigut.oopsallprs.domain.model.ObservedEffortSpec
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompletedWorkoutCorrectionStateTest {
    @Test
    fun editAddDeleteRemainDraftUntilTransactionalSave() = runTest {
        val harness = FoundationHarness()
        val completed = completedWorkout(harness)
        val holder = holder(harness)
        holder.presentCompletedWorkout(completed.id)
        holder.beginEditing()

        val exercise = completed.exercises.single()
        holder.editSet(exercise.id, exercise.loggedSets.first().id)
        holder.updateReps(3)
        holder.updateWeight(WeightKg(80.0))
        holder.applySetEdit(instant(5_000))
        holder.addSet(exercise.id)
        holder.updateReps(8)
        holder.updateWeight(WeightKg(70.0))
        holder.applySetEdit(instant(5_100))
        holder.requestDeleteSet(exercise.loggedSets.last().id)
        holder.confirmDeleteSet()

        assertEquals(listOf(5, 6), harness.store.completedWorkout(completed.id)!!.exercises.single().loggedSets.map { it.reps })
        holder.saveEditing().successValue()

        val corrected = harness.store.completedWorkout(completed.id)!!.exercises.single().loggedSets
        assertEquals(listOf(3, 8), corrected.map { it.reps })
        assertEquals(listOf(WeightKg(80.0), WeightKg(70.0)), corrected.map { it.weight })
        assertEquals(listOf(0, 1), corrected.map { it.position.value })
        assertEquals(completed.sourceActiveWorkoutId, harness.store.completedWorkout(completed.id)!!.sourceActiveWorkoutId)
    }

    @Test
    fun cancelLeavesCompletedWorkoutUnchanged() = runTest {
        val harness = FoundationHarness()
        val completed = completedWorkout(harness)
        val holder = holder(harness)
        holder.presentCompletedWorkout(completed.id)
        holder.beginEditing()
        val set = completed.exercises.single().loggedSets.first()
        holder.editSet(completed.exercises.single().id, set.id)
        holder.updateWeight(WeightKg(1.0))
        holder.applySetEdit(instant(5_000))
        holder.cancelEditing()

        assertEquals(WeightKg(100.0), harness.store.completedWorkout(completed.id)!!.exercises.single().loggedSets.first().weight)
        assertNull(holder.state.value.editDraft)
    }

    @Test
    fun validationRetainsEditorAndCapturedConfigurationControlsMeasures() = runTest {
        val harness = FoundationHarness()
        val configuration = LoggingConfiguration(
            id = LoggingConfigurationId("history_all_measures_v1"),
            schemaVersion = LoggingSchemaVersion(1),
            measures = listOf(
                MeasureSpec(MeasureKind.REPETITIONS, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.LOAD, MeasureRequirement.OPTIONAL, LoadRole.EXTERNAL_RESISTANCE),
                MeasureSpec(MeasureKind.DURATION, MeasureRequirement.REQUIRED),
                MeasureSpec(MeasureKind.DISTANCE, MeasureRequirement.REQUIRED)
            ),
            observedEffort = ObservedEffortSpec(listOf(EffortKind.RIR))
        )
        harness.store.saveLoggingConfiguration(configuration).successValue()
        val completed = configuredWorkout(harness, configuration)
        val holder = holder(harness)
        holder.presentCompletedWorkout(completed.id)
        holder.beginEditing()
        holder.editSet(completed.exercises.single().id, completed.exercises.single().loggedSets.single().id)

        val editor = assertNotNull(holder.state.value.editDraft?.setEditor)
        assertEquals(configuration, editor.draft.loggingConfiguration)
        holder.updateDistance(null)
        holder.applySetEdit(instant(5_000))

        assertNotNull(holder.state.value.editDraft?.setEditor)
        assertTrue(holder.state.value.editDraft!!.setEditor!!.draft.inlineError!!.contains("Distance"))
    }

    @Test
    fun bodyweightAndTimedCorrectionsPreserveTheirCapturedShapes() = runTest {
        val harness = FoundationHarness()
        val completedId = FoundationId("mixed-shapes-completed")
        val bodyExerciseId = FoundationId("body-exercise")
        val timedExerciseId = FoundationId("timed-exercise")
        val finished = instant(2_000)
        val workout = CompletedWorkout(
            id = completedId,
            sourceActiveWorkoutId = FoundationId("mixed-shapes-source"),
            startedAt = instant(1_000),
            finishedAt = finished,
            durationMs = 1_000,
            routineId = null,
            exercises = listOf(
                CompletedExercise(
                    bodyExerciseId, completedId, FoundationId("body-catalog"), "Pull-Up", OrderedPosition(0),
                    listOf(ExerciseSet(
                        FoundationId("body-set"), bodyExerciseId, OrderedPosition(0), SetKind.BODYWEIGHT,
                        null, 10, instant(1_500), instant(1_500), instant(1_500),
                        captureConfigurationId = LegacyLoggingConfigurations.bodyweight.id
                    ))
                ),
                CompletedExercise(
                    timedExerciseId, completedId, FoundationId("timed-catalog"), "Plank", OrderedPosition(1),
                    listOf(ExerciseSet(
                        FoundationId("timed-set"), timedExerciseId, OrderedPosition(0), SetKind.TIMED,
                        null, null, instant(1_600), instant(1_600), instant(1_600),
                        durationMs = 60_000,
                        captureConfigurationId = LegacyLoggingConfigurations.timed.id
                    ))
                )
            ),
            createdAt = finished
        )
        harness.store.finishWorkout(workout).successValue()
        val corrected = workout.copy(exercises = listOf(
            workout.exercises[0].copy(loggedSets = listOf(workout.exercises[0].loggedSets.single().copy(
                reps = 12, updatedAt = instant(3_000), editedAt = instant(3_000)
            ))),
            workout.exercises[1].copy(loggedSets = listOf(workout.exercises[1].loggedSets.single().copy(
                durationMs = 90_000, updatedAt = instant(3_000), editedAt = instant(3_000)
            )))
        ))

        harness.corrections.save(corrected).successValue()

        val saved = harness.store.completedWorkout(completedId)!!
        assertEquals(12, saved.exercises[0].loggedSets.single().reps)
        assertNull(saved.exercises[0].loggedSets.single().weight)
        assertEquals(90_000, saved.exercises[1].loggedSets.single().durationMs)
        assertNull(saved.exercises[1].loggedSets.single().reps)
    }

    private fun holder(harness: FoundationHarness) = HistoryStateHolder(
        harness.store,
        harness.store,
        harness.routines,
        harness.corrections,
        harness.store
    )

    private suspend fun completedWorkout(harness: FoundationHarness): CompletedWorkout {
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 6, WeightKg(90.0), 1, instant(1_300)).successValue()
        return harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
    }

    private suspend fun configuredWorkout(
        harness: FoundationHarness,
        configuration: LoggingConfiguration
    ): CompletedWorkout {
        val finished = instant(2_000)
        val exerciseId = FoundationId("configured-exercise-instance")
        val set = ExerciseSet(
            id = FoundationId("configured-set"),
            exerciseInstanceId = exerciseId,
            position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            weight = WeightKg(20.0),
            reps = 5,
            durationMs = 60_000,
            distanceMeters = 100.0,
            observedEffort = Effort(rir = 2),
            captureConfigurationId = configuration.id,
            loggedAt = instant(1_500),
            createdAt = instant(1_500),
            updatedAt = instant(1_500)
        )
        val completed = CompletedWorkout(
            id = FoundationId("configured-completed"),
            sourceActiveWorkoutId = FoundationId("configured-source"),
            startedAt = instant(1_000),
            finishedAt = finished,
            durationMs = 1_000,
            routineId = null,
            exercises = listOf(
                CompletedExercise(
                    id = exerciseId,
                    completedWorkoutId = FoundationId("configured-completed"),
                    exerciseCatalogId = FoundationId("configured-exercise"),
                    displayNameSnapshot = "Configured",
                    position = OrderedPosition(0),
                    loggedSets = listOf(set)
                )
            ),
            createdAt = finished
        )
        harness.store.finishWorkout(completed).successValue()
        return completed
    }
}
