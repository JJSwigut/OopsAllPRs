package com.jjswigut.oopsallprs.ui.exercise

import com.jjswigut.oopsallprs.domain.model.ExerciseReference
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.seedExerciseCatalog
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExercisePickerUxStateTest {
    @Test
    fun selectingResultClosesPickerAndFocusesNewExercise() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        activeWorkout.hydrate(workout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)
        picker.search("bench")
        val addedId = picker.select(picker.state.value.results.single()).successValue()

        assertFalse(picker.state.value.isOpen)
        assertEquals(addedId, activeWorkout.state.value.workout?.focus?.exerciseInstanceId)
    }

    @Test
    fun defaultPickerShowsFullScrollableCatalogInsteadOfFirstPageOnly() = runTest {
        val harness = FoundationHarness()
        harness.exerciseCatalog.ensureSeeded(largeExerciseCsv()).successValue()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        activeWorkout.hydrate(workout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(workout.id)

        assertEquals(harness.store.all().size, picker.state.value.results.size)
        assertTrue(picker.state.value.results.size > 25)
    }

    @Test
    fun recentlyUsedExercisesAppearAboveFullCatalog() = runTest {
        val harness = FoundationHarness()
        harness.seedExerciseCatalog()
        val bench = harness.store.search("bench").single()
        val loggedWorkout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(
            loggedWorkout.id,
            ExerciseReference(bench.id, bench.displayName, bench.isBodyweight),
            instant(1_100)
        ).successValue()
        harness.setLogging.confirmSet(
            loggedWorkout.id,
            exercise.id,
            SetKind.WEIGHTED,
            reps = 5,
            weight = WeightKg(100.0),
            position = 0,
            loggedAt = instant(1_200)
        ).successValue()
        harness.routines.finishWorkout(loggedWorkout.id, instant(2_000)).successValue().workout
        val activeWorkout = ActiveWorkoutStateHolder(harness.setLogging, harness.lifecycle, harness.store)
        val currentWorkout = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        activeWorkout.hydrate(currentWorkout.id)
        val picker = ExercisePickerStateHolder(harness.exerciseCatalog, activeWorkout)

        picker.open(currentWorkout.id)

        assertEquals("Bench Press", picker.state.value.recentResults.first().displayName)
        assertFalse(picker.state.value.results.any { it.exerciseCatalogId == bench.id && it.displayName == "Bench Press" })
    }

    private fun largeExerciseCsv(): String =
        buildString {
            appendLine("Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region")
            repeat(30) { index ->
                appendLine("Exercise ${index.toString().padStart(2, '0')},Legs,Machine,Push,Strength,Beginner,Lower Body")
            }
        }
}
