package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkoutPreviousValuesTest {
    @Test
    fun add_exercise_prefills_weighted_draft_from_previous_completed_workout() = runTest {
        val harness = FoundationHarness()
        harness.completeWeightedWorkout(reps = 8, weight = WeightKg(100.0))
        val workout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val holder = previousValueHolder(harness)
        holder.hydrate(workout.id, now = instant(3_100))

        val exerciseId = holder.addExercise(workout.id, harness.weightedReference).successValue()

        val draft = holder.state.value.workout?.exerciseBlocks?.single { it.exerciseInstanceId == exerciseId }?.draft
        assertEquals(SetKind.WEIGHTED, draft?.setKind)
        assertEquals(8, draft?.reps)
        assertEquals(WeightKg(100.0), draft?.weight)
    }

    @Test
    fun add_bodyweight_exercise_prefills_reps_only_from_previous_completed_workout() = runTest {
        val harness = FoundationHarness()
        harness.completeBodyweightWorkout(reps = 11)
        val workout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val holder = previousValueHolder(harness)
        holder.hydrate(workout.id, now = instant(3_100))

        val exerciseId = holder.addExercise(workout.id, harness.bodyweightReference).successValue()

        val draft = holder.state.value.workout?.exerciseBlocks?.single { it.exerciseInstanceId == exerciseId }?.draft
        assertEquals(SetKind.BODYWEIGHT, draft?.setKind)
        assertEquals(11, draft?.reps)
        assertNull(draft?.weight)
    }

    @Test
    fun add_exercise_without_history_keeps_existing_safe_defaults() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val holder = previousValueHolder(harness)
        holder.hydrate(workout.id, now = instant(1_100))

        val exerciseId = holder.addExercise(workout.id, harness.weightedReference).successValue()

        val draft = holder.state.value.workout?.exerciseBlocks?.single { it.exerciseInstanceId == exerciseId }?.draft
        assertEquals(SetKind.WEIGHTED, draft?.setKind)
        assertEquals(5, draft?.reps)
        assertEquals(WeightKg(0.0), draft?.weight)
    }

    @Test
    fun previous_value_draft_recovers_through_active_ux_draft_persistence() = runTest {
        val harness = FoundationHarness()
        harness.completeWeightedWorkout(reps = 6, weight = WeightKg(125.0))
        val workout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val firstHolder = previousValueHolder(harness)
        firstHolder.hydrate(workout.id, now = instant(3_100))
        val exerciseId = firstHolder.addExercise(workout.id, harness.weightedReference).successValue()

        val recoveredHolder = previousValueHolder(harness)
        recoveredHolder.hydrate(workout.id, now = instant(3_200))

        val recovered = recoveredHolder.state.value.workout?.exerciseBlocks?.single { it.exerciseInstanceId == exerciseId }?.draft
        assertEquals(6, recovered?.reps)
        assertEquals(WeightKg(125.0), recovered?.weight)
    }
}

private fun previousValueHolder(harness: FoundationHarness): ActiveWorkoutStateHolder =
    ActiveWorkoutStateHolder(
        setLogging = harness.setLogging,
        lifecycle = harness.lifecycle,
        activeUx = harness.store,
        previousDefaults = harness.previousDefaults
    )

private suspend fun FoundationHarness.completeWeightedWorkout(
    reps: Int,
    weight: WeightKg
) {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(1_100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.WEIGHTED,
        reps = reps,
        weight = weight,
        position = 0,
        loggedAt = instant(1_200)
    ).successValue()
    routines.finishWorkout(workout.id, instant(2_000)).successValue()
}

private suspend fun FoundationHarness.completeBodyweightWorkout(reps: Int) {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, bodyweightReference, instant(1_100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.BODYWEIGHT,
        reps = reps,
        weight = null,
        position = 0,
        loggedAt = instant(1_200)
    ).successValue()
    routines.finishWorkout(workout.id, instant(2_000)).successValue()
}
