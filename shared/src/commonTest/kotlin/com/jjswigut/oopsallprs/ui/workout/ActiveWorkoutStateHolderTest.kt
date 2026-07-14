package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.LoggingConfigurationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ActiveWorkoutStateHolderTest {
    @Test
    fun hydrateCreatesDefaultDraftForExercise() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)

        holder.hydrate(workout.id)

        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(exercise.id, block?.exerciseInstanceId)
        assertEquals(5, block?.draft?.reps)
        assertNotNull(block?.draft?.weight)
    }

    @Test
    fun focusedCloseShowsExerciseOverviewWithoutChangingWorkoutOrDraft() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(
            workout.id,
            harness.weightedReference,
            instant(1_100)
        ).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        holder.hydrate(workout.id, now = instant(1_200))
        holder.updateDraftReps(exercise.id, 8)

        holder.showExerciseOverview()

        assertTrue(holder.state.value.isExerciseOverviewVisible)
        assertEquals(workout.id, holder.state.value.workout?.workoutId)
        assertEquals(8, holder.state.value.workout?.exerciseBlocks?.single()?.draft?.reps)
        assertEquals(workout.id, harness.store.activeWorkout(workout.id)?.id)

        holder.setFocus(exercise.id, instant(1_300))

        assertFalse(holder.state.value.isExerciseOverviewVisible)
        assertEquals(8, holder.state.value.workout?.exerciseBlocks?.single()?.draft?.reps)
    }

    @Test
    fun oneTapConfirmMovesDraftToLoggedHistory() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        holder.updateDraftReps(exercise.id, 8)

        holder.confirmDraft(exercise.id).successValue()

        val block = holder.state.value.workout?.exerciseBlocks?.single()
        assertEquals(1, block?.loggedRows?.size)
        assertEquals(8, block?.loggedRows?.single()?.reps)
        assertEquals(1, block?.draft?.position?.value)
    }

    @Test
    fun confirmingNonFirstExerciseKeepsFocusOnNextDraftForSameExercise() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val first = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val second = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id)
        holder.setFocus(second.id, instant(1_300))

        holder.confirmDraft(second.id).successValue()

        val view = holder.state.value.workout
        assertEquals(listOf(first.id, second.id), view?.exerciseBlocks?.map { it.exerciseInstanceId })
        assertEquals(second.id, view?.focus?.exerciseInstanceId)
        assertEquals(1, view?.exerciseBlocks?.first { it.exerciseInstanceId == second.id }?.draft?.position?.value)
    }

    @Test
    fun confirmingCircuitSetAdvancesThroughExercisesAndRounds() = runTest {
        val harness = FoundationHarness()
        val groupId = FoundationId("routine-group-circuit")
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Bodyweight circuit",
            exercises = listOf(
                circuitExercise("routine-exercise-squat", "Squat", "exercise-squat", groupId, 0),
                circuitExercise("routine-exercise-pushup", "Pushup", "exercise-pushup", groupId, 1),
                circuitExercise("routine-exercise-lunge", "Lunge", "exercise-lunge", groupId, 2)
            ),
            now = instant(1_000)
        ).successValue()
        val active = harness.lifecycle.startFromRoutine(routine.id, instant(2_000)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(active.id, now = instant(2_100))

        val blocks = holder.state.value.workout!!.exerciseBlocks
        val squat = blocks[0].exerciseInstanceId
        val pushup = blocks[1].exerciseInstanceId
        val lunge = blocks[2].exerciseInstanceId

        holder.confirmDraft(squat).successValue()
        assertEquals(pushup, holder.state.value.workout?.focus?.exerciseInstanceId)
        assertEquals(0, holder.state.value.workout?.exerciseBlocks?.first { it.exerciseInstanceId == pushup }?.draft?.position?.value)

        holder.confirmDraft(pushup).successValue()
        assertEquals(lunge, holder.state.value.workout?.focus?.exerciseInstanceId)

        holder.confirmDraft(lunge).successValue()
        assertEquals(squat, holder.state.value.workout?.focus?.exerciseInstanceId)
        assertEquals(1, holder.state.value.workout?.exerciseBlocks?.first { it.exerciseInstanceId == squat }?.draft?.position?.value)
    }

    @Test
    fun canCreateCircuitOnTheFlyFromActiveWorkout() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val first = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val second = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id, now = instant(1_300))

        holder.groupExercisesAsCircuit(listOf(first.id, second.id)).successValue()

        val grouped = holder.state.value.workout!!.exerciseBlocks
        assertEquals(listOf("Circuit", "Circuit"), grouped.map { it.groupLabel })
        assertEquals(listOf(3, 3), grouped.map { it.groupRounds })

        holder.confirmDraft(first.id).successValue()

        assertEquals(second.id, holder.state.value.workout?.focus?.exerciseInstanceId)
    }

    @Test
    fun groupedExerciseBlocksCollapseForDisplay() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val first = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val second = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        val third = harness.setLogging.addExercise(workout.id, harness.timedReference, instant(1_300)).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle)
        holder.hydrate(workout.id, now = instant(1_400))

        holder.groupExercisesAsCircuit(listOf(first.id, second.id)).successValue()

        val displayGroups = holder.state.value.workout!!.exerciseBlockGroups()
        assertEquals(2, displayGroups.size)
        assertEquals(listOf(first.id, second.id), displayGroups[0].blocks.map { it.exerciseInstanceId })
        assertEquals(true, displayGroups[0].isGrouped)
        assertEquals(third.id, displayGroups[1].blocks.single().exerciseInstanceId)
        assertEquals(false, displayGroups[1].isGrouped)
    }

    @Test
    fun missingHistoricalConfigurationIsUnavailableInsteadOfUsingCurrentConfiguration() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val historicalSet = ExerciseSet(
            id = FoundationId("historical-set-missing-config"),
            exerciseInstanceId = exercise.id,
            position = OrderedPosition(0),
            setKind = SetKind.WEIGHTED,
            weight = com.jjswigut.oopsallprs.domain.model.WeightKg(100.0),
            reps = 5,
            loggedAt = instant(1_200),
            createdAt = instant(1_150),
            updatedAt = instant(1_200),
            captureConfigurationId = LoggingConfigurationId("missing-historical-configuration")
        )
        harness.store.saveActiveWorkout(
            harness.store.activeWorkout(workout.id)!!.copy(
                exercises = listOf(exercise.copy(sets = listOf(historicalSet)))
            )
        ).successValue()
        val holder = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)

        holder.hydrate(workout.id, now = instant(1_300))

        val row = holder.state.value.workout!!.exerciseBlocks.single().loggedRows.single()
        assertNull(row.loggingConfiguration)
        assertEquals(LoggingConfigurationId("missing-historical-configuration"), row.captureConfigurationId)

        holder.beginEditSet(row.setId)

        assertNull(holder.state.value.editDraft)
        assertTrue(holder.state.value.errorMessage.orEmpty().contains("unavailable", ignoreCase = true))
    }

    private fun circuitExercise(
        id: String,
        name: String,
        exerciseId: String,
        groupId: FoundationId,
        position: Int
    ): RoutineExercise =
        RoutineExercise(
            id = FoundationId(id),
            routineId = FoundationId("routine-draft"),
            exerciseCatalogId = FoundationId(exerciseId),
            displayNameSnapshot = name,
            position = OrderedPosition(position),
            groupId = groupId,
            groupPosition = OrderedPosition(0),
            groupRounds = 2,
            rest = RestConfiguration(durationSeconds = 30),
            plannedSets = listOf(
                RoutineSetTemplate(
                    id = FoundationId("set-$id"),
                    routineExerciseId = FoundationId(id),
                    position = OrderedPosition(0),
                    targetWeight = null,
                    targetReps = 10,
                    setKind = SetKind.BODYWEIGHT
                )
            )
        )
}
