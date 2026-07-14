package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressStateHolderTest {
    @Test
    fun dashboardShowsOnlyNondominatedWeightedSetRecords() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 16, WeightKg(25.0), 0, instant(1_200))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 14, WeightKg(25.0), 1, instant(1_300))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 10, WeightKg(30.0), 2, instant(1_400))
        harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()

        val weightedRows = holder.state.value.recentRows.filter { it.kind == PersonalRecordKind.WEIGHT_FOR_REPS }
        assertEquals(setOf("55.1 lb x 16", "66.1 lb x 10"), weightedRows.map { it.valueLabel }.toSet())
    }

    @Test
    fun refreshBuildsEmptyStateAndRecentRowsNewestFirst() = runTest {
        val harness = FoundationHarness()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()

        assertTrue(holder.state.value.recentRows.isEmpty())
        assertEquals("Finish workouts to build PRs here.", holder.state.value.emptyMessage)

        harness.finishWeightedWorkout(loggedAtMs = 1_300, finishedAtMs = 2_000)
        harness.finishBodyweightWorkout(reps = 15, loggedAtMs = 3_300, finishedAtMs = 4_000)
        holder.refresh()

        val state = holder.state.value
        assertEquals(4, state.recentRows.size)
        assertEquals("Pull-Up", state.recentRows.first().exerciseName)
        assertEquals("15 reps", state.recentRows.first().valueLabel)
        assertEquals("Pull-Up", state.latestPr?.exerciseName)
        assertEquals("15 reps", state.latestPr?.valueLabel)
    }

    @Test
    fun groupsRecordsByExerciseAndKeepsSelectionInState() = runTest {
        val harness = FoundationHarness()
        harness.finishWeightedWorkout(loggedAtMs = 1_300, finishedAtMs = 2_000)
        harness.finishBodyweightWorkout(reps = 15, loggedAtMs = 3_300, finishedAtMs = 4_000)
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.selectExercise(harness.weightedReference.exerciseCatalogId)

        val selected = holder.state.value.selectedExercise
        assertEquals("Bench Press", selected?.exerciseName)
        assertEquals(3, selected?.records?.size)
        assertEquals("Bench Press", selected?.latestRecord?.exerciseName)
        assertTrue(selected?.records.orEmpty().all { it.exerciseName == "Bench Press" })

        holder.clearExerciseSelection()

        assertNull(holder.state.value.selectedExercise)
        assertNull(holder.state.value.selectedExerciseId)
    }
}

private suspend fun FoundationHarness.finishWeightedWorkout(
    reps: Int = 5,
    weightKg: Double = 100.0,
    loggedAtMs: Long,
    finishedAtMs: Long
) {
    val workout = lifecycle.startEmpty(instant(loggedAtMs - 300)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(loggedAtMs - 200)).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        SetKind.WEIGHTED,
        reps,
        WeightKg(weightKg),
        0,
        instant(loggedAtMs)
    ).successValue()
    routines.finishWorkout(workout.id, instant(finishedAtMs)).successValue()
}

private suspend fun FoundationHarness.finishBodyweightWorkout(
    reps: Int,
    loggedAtMs: Long,
    finishedAtMs: Long
) {
    val workout = lifecycle.startEmpty(instant(loggedAtMs - 300)).successValue()
    val exercise = setLogging.addExercise(workout.id, bodyweightReference, instant(loggedAtMs - 200)).successValue()
    setLogging.confirmSet(
        workout.id,
        exercise.id,
        SetKind.BODYWEIGHT,
        reps,
        null,
        0,
        instant(loggedAtMs)
    ).successValue()
    routines.finishWorkout(workout.id, instant(finishedAtMs)).successValue()
}
