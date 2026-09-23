package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.FinishPostCommitWarning
import com.jjswigut.oopsallprs.domain.model.FinishWorkoutOutcome
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompletedWorkoutFinishWarningTest {
    @Test
    fun navigationRefreshFailurePreservesSavedReceiptAndReportsError() = runTest {
        val store = FoundationHarness().store
        val holder = HistoryStateHolder(object : WorkoutRepository by store {
            override suspend fun completedWorkouts() = error("Read failed")
        })
        val workout = mixedCompletedWorkout()
        holder.presentCompletion(FinishWorkoutOutcome(workout, true))

        holder.refreshForDisplay()

        assertEquals(workout.id, holder.state.value.selectedSummary?.workoutId)
        assertEquals("Couldn't refresh history. Try again.", holder.state.value.errorMessage)
    }

    @Test
    fun navigationRefreshCancellationIsNotReportedAsReadFailure() = runTest {
        val store = FoundationHarness().store
        val holder = HistoryStateHolder(object : WorkoutRepository by store {
            override suspend fun completedWorkouts(): Nothing = throw CancellationException("Canceled")
        })
        assertFailsWith<CancellationException> { holder.refreshForDisplay() }
        assertNull(holder.state.value.errorMessage)
    }

    @Test
    fun committedReceiptCanBePresentedWithoutAnyRepositoryRead() = runTest {
        val store = FoundationHarness().store
        val unavailable = object : WorkoutRepository by store {
            override suspend fun completedWorkouts() = error("Database read unavailable")
            override suspend fun completedWorkout(id: FoundationId) = error("Database read unavailable")
        }
        val holder = HistoryStateHolder(unavailable)
        val workout = mixedCompletedWorkout()

        holder.presentCompletion(FinishWorkoutOutcome(workout, true, listOf(FinishPostCommitWarning.TIMER_CLEANUP_FAILED)))

        assertEquals(workout.id, holder.state.value.selectedSummary?.workoutId)
        assertEquals(2, holder.state.value.selectedSummary?.setCount)
        assertTrue(holder.state.value.completionNotice.orEmpty().startsWith("Workout saved."))
        assertNull(holder.state.value.errorMessage)
    }

    @Test
    fun refreshAndSameSelectionRetainNoticeButAnotherWorkoutDoesNot() = runTest {
        val store = FoundationHarness().store
        val workout = store.finishWorkout(mixedCompletedWorkout()).successValue()
        val another = store.finishWorkout(workout.copy(id = FoundationId("another"))).successValue()
        val holder = HistoryStateHolder(store)
        holder.presentCompletion(FinishWorkoutOutcome(workout, true, listOf(FinishPostCommitWarning.PROGRESS_REFRESH_FAILED)))
        val notice = holder.state.value.completionNotice

        holder.refresh()
        holder.selectWorkout(workout.id)
        assertEquals(notice, holder.state.value.completionNotice)
        holder.selectWorkout(another.id)
        assertNull(holder.state.value.completionNotice)
    }

    @Test
    fun failedRefreshAddsOneNoticeAndBackClearsIt() {
        val holder = HistoryStateHolder(FoundationHarness().store)
        val workout = mixedCompletedWorkout()
        holder.presentCompletion(FinishWorkoutOutcome(workout, true))
        assertEquals("Workout saved.", holder.state.value.completionNotice)
        holder.reportCompletionRefreshFailure(workout.id)
        val notice = holder.state.value.completionNotice
        holder.reportCompletionRefreshFailure(workout.id)
        assertEquals(notice, holder.state.value.completionNotice)
        holder.clearSelection()
        assertNull(holder.state.value.completionNotice)
        holder.reportCompletionRefreshFailure(workout.id)
        assertNull(holder.state.value.completionNotice)
    }
}
