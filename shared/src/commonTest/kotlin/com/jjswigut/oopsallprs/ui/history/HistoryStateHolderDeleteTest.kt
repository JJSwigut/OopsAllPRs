package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryStateHolderDeleteTest {
    @Test
    fun deletingSelectedWorkoutClearsDetailAndRebuildsRows() = runTest {
        val harness = FoundationHarness()
        val first = harness.completedWeightedWorkout(weight = 100.0, startedAt = 1_000, finishedAt = 2_000)
        val second = harness.completedWeightedWorkout(weight = 120.0, startedAt = 3_000, finishedAt = 4_000)
        val holder = HistoryStateHolder(harness.store, harness.store, harness.routines)

        holder.refresh()
        holder.selectWorkout(second.id)
        holder.requestDeleteSelectedWorkout()
        holder.confirmDeleteWorkout(instant(5_000)).successValue()

        assertNull(holder.state.value.selectedSummary)
        assertNull(holder.state.value.pendingDeleteSummary)
        assertEquals(listOf(first.id), holder.state.value.rows.map { it.workoutId })
        assertTrue(harness.store.personalRecords().all { it.sourceWorkoutId == first.id })
    }
}

private suspend fun FoundationHarness.completedWeightedWorkout(
    weight: Double,
    startedAt: Long,
    finishedAt: Long
) = run {
    val workout = lifecycle.startEmpty(instant(startedAt)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(startedAt + 100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.WEIGHTED,
        reps = 5,
        weight = WeightKg(weight),
        position = 0,
        loggedAt = instant(startedAt + 200)
    )
    routines.finishWorkout(workout.id, instant(finishedAt)).successValue().workout
}
