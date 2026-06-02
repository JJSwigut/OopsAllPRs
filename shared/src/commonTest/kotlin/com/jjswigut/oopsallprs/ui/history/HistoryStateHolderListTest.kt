package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryStateHolderListTest {
    @Test
    fun refreshSortsRowsNewestFirstAndHandlesSelection() = runTest {
        val harness = FoundationHarness()
        val older = harness.store.finishWorkout(mixedCompletedWorkout().copy(id = FoundationId("completed-older"), finishedAt = instant(10_000))).successValue()
        val newer = harness.store.finishWorkout(mixedCompletedWorkout().copy(id = FoundationId("completed-newer"), finishedAt = instant(20_000))).successValue()
        val holder = HistoryStateHolder(harness.store, harness.store)

        holder.refresh()

        assertEquals(listOf(newer.id, older.id), holder.state.value.rows.map { it.workoutId })
        holder.selectWorkout(older.id)
        assertEquals(older.id, holder.state.value.selectedSummary?.workoutId)
        holder.clearSelection()
        assertNull(holder.state.value.selectedSummary)
    }

    @Test
    fun emptyHistoryHasNoRowsOrSelectedSummary() = runTest {
        val holder = HistoryStateHolder(FoundationHarness().store)

        holder.refresh()

        assertEquals(emptyList(), holder.state.value.rows)
        assertNull(holder.state.value.selectedSummary)
    }
}
