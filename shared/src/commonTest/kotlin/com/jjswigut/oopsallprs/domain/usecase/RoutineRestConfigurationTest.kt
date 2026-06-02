package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.RestConfiguration
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutineRestConfigurationTest {
    @Test
    fun savedRoutineCarriesExerciseRestIntoLaunchedWorkout() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.updateExerciseRest(
            workout.id,
            exercise.id,
            RestConfiguration(durationSeconds = 180),
            instant(1_150)
        ).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()
        val routine = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()

        val launched = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        assertEquals(180, routine.exercises.single().rest.durationSeconds)
        assertEquals(180, launched.exercises.single().rest.durationSeconds)
    }

    @Test
    fun savingRoutineUsesDefaultRestWhenCompletedExerciseRestWasDisabled() = runTest {
        val harness = FoundationHarness()
        harness.store.setDefaultRestSeconds(240).successValue()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.updateExerciseRest(
            workout.id,
            exercise.id,
            RestConfiguration.disabled(),
            instant(1_150)
        ).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_200)).successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        val routine = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()

        assertEquals(RestConfiguration(durationSeconds = 240), routine.exercises.single().rest)
    }
}
