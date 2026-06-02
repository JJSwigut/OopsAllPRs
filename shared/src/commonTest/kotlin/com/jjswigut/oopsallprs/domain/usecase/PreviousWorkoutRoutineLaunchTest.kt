package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PreviousWorkoutRoutineLaunchTest {
    @Test
    fun explicit_routine_targets_are_preserved_when_previous_values_exist() = runTest {
        val harness = FoundationHarness()
        harness.completeWeightedRoutineSource(
            listOf(CompletedSetInput(reps = 6, weight = WeightKg(110.0), position = 0))
        )
        val routine = harness.saveRoutineWithSets(
            listOf(routineSet(position = 0, reps = 5, weight = WeightKg(90.0)))
        )

        val active = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        val launched = active.exercises.single().sets.single()
        assertEquals(5, launched.reps)
        assertEquals(WeightKg(90.0), launched.weight)
    }

    @Test
    fun missing_and_partial_routine_targets_are_filled_from_previous_values() = runTest {
        val harness = FoundationHarness()
        harness.completeWeightedRoutineSource(
            listOf(
                CompletedSetInput(reps = 8, weight = WeightKg(100.0), position = 0),
                CompletedSetInput(reps = 7, weight = WeightKg(105.0), position = 1)
            )
        )
        val routine = harness.saveRoutineWithSets(
            listOf(
                routineSet(position = 0, reps = 10, weight = null),
                routineSet(position = 1, reps = null, weight = null)
            )
        )

        val active = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        val first = active.exercises.single().sets[0]
        val second = active.exercises.single().sets[1]
        assertEquals(10, first.reps)
        assertEquals(WeightKg(100.0), first.weight)
        assertEquals(7, second.reps)
        assertEquals(WeightKg(105.0), second.weight)
    }

    @Test
    fun routine_launch_previous_values_do_not_mutate_saved_routine() = runTest {
        val harness = FoundationHarness()
        harness.completeWeightedRoutineSource(
            listOf(CompletedSetInput(reps = 8, weight = WeightKg(100.0), position = 0))
        )
        val routine = harness.saveRoutineWithSets(
            listOf(routineSet(position = 0, reps = null, weight = null))
        )

        harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        val saved = harness.routines.listRoutines().single { it.id == routine.id }
        val savedSet = saved.exercises.single().plannedSets.single()
        assertNull(savedSet.targetReps)
        assertNull(savedSet.targetWeight)
    }
}

private data class CompletedSetInput(
    val reps: Int,
    val weight: WeightKg,
    val position: Int
)

private suspend fun FoundationHarness.completeWeightedRoutineSource(sets: List<CompletedSetInput>) {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(1_100)).successValue()
    sets.forEach { set ->
        setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.WEIGHTED,
            reps = set.reps,
            weight = set.weight,
            position = set.position,
            loggedAt = instant(1_200L + set.position)
        ).successValue()
    }
    routines.finishWorkout(workout.id, instant(2_000)).successValue()
}

private suspend fun FoundationHarness.saveRoutineWithSets(sets: List<RoutineSetTemplate>) =
    routines.saveRoutine(
        routineId = null,
        name = "Under-specified push",
        exercises = listOf(
            RoutineExercise(
                id = FoundationId("routine-exercise-under-specified"),
                routineId = FoundationId("pending"),
                exerciseCatalogId = weightedReference.exerciseCatalogId,
                displayNameSnapshot = weightedReference.displayNameSnapshot,
                position = OrderedPosition(0),
                plannedSets = sets
            )
        ),
        now = instant(3_000)
    ).successValue()

private fun routineSet(position: Int, reps: Int?, weight: WeightKg?): RoutineSetTemplate =
    RoutineSetTemplate(
        id = FoundationId("routine-set-$position"),
        routineExerciseId = FoundationId("routine-exercise-under-specified"),
        position = OrderedPosition(position),
        targetWeight = weight,
        targetReps = reps,
        setKind = SetKind.WEIGHTED
    )
