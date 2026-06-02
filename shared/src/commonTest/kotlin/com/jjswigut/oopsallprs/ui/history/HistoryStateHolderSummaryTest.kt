package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HistoryStateHolderSummaryTest {
    @Test
    fun presentsCompletedWorkoutSummaryFromLocalHistory() = runTest {
        val harness = FoundationHarness()
        val completed = harness.store.finishWorkout(mixedCompletedWorkout()).successValue()
        val holder = HistoryStateHolder(harness.store, harness.store)

        holder.presentCompletedWorkout(completed.id)

        val summary = assertNotNull(holder.state.value.selectedSummary)
        assertEquals(completed.id, summary.workoutId)
        assertEquals(2, summary.exerciseCount)
        assertEquals(2, summary.setCount)
    }
}
