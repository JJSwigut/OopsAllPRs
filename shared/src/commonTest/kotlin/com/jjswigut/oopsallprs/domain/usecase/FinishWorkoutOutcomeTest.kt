package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FinishPostCommitWarning
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.ProgressPoint
import com.jjswigut.oopsallprs.domain.model.WorkoutCompletionReceipt
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.repository.ProgressRepository
import com.jjswigut.oopsallprs.domain.repository.WorkoutRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.RestAlertScheduleResult
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FinishWorkoutOutcomeTest {
    @Test
    fun timerFailureReturnsCommittedWorkoutAndStillRepairsProgress() = runTest {
        val harness = FoundationHarness()
        val id = harness.workoutWithLoggedWeightedSet()
        val timer = CompletionTimer { throw IllegalStateException("Unavailable timer") }
        val routines = routines(harness, timer)

        val outcome = routines.finishWorkout(id, instant(2_000)).successValue()

        assertTrue(outcome.newlyCompleted)
        assertEquals(listOf(FinishPostCommitWarning.TIMER_CLEANUP_FAILED), outcome.warnings)
        assertEquals(outcome.workout, harness.store.completedWorkout(outcome.workout.id))
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        assertNull(harness.store.currentActiveWorkout())
        assertNull(harness.store.load())
        assertTrue(harness.store.progressPoints().isNotEmpty())
    }

    @Test
    fun returnedAndThrownProgressFailuresAreExplicitWarningsAndRetryRepairs() = runTest {
        for (throws in listOf(false, true)) {
            val harness = FoundationHarness()
            val id = harness.workoutWithLoggedWeightedSet()
            var failing = true
            val progress = object : ProgressRepository by harness.store {
                override suspend fun replaceRecords(records: List<PersonalRecord>, points: List<ProgressPoint>): FoundationResult<Unit> {
                    if (failing) {
                        if (throws) throw IllegalStateException("Progress unavailable")
                        return foundationFailure(FoundationError.Persistence("Progress unavailable"))
                    }
                    return harness.store.replaceRecords(records, points)
                }
            }
            val timer = CompletionTimer()
            val routines = routines(harness, timer, progress)
            val outcome = routines.finishWorkout(id, instant(2_000)).successValue()
            assertEquals(listOf(FinishPostCommitWarning.PROGRESS_REFRESH_FAILED), outcome.warnings)
            assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)

            failing = false
            val next = harness.lifecycle.startEmpty(instant(3_000)).successValue()
            val retry = routines.finishWorkout(id, instant(4_000)).successValue()
            assertFalse(retry.newlyCompleted)
            assertEquals(outcome.workout, retry.workout)
            assertTrue(retry.warnings.isEmpty())
            assertTrue(harness.store.progressPoints().isNotEmpty())
            assertEquals(1, timer.cancellations)
            assertEquals(next.id, harness.store.load()?.activeWorkoutId)
            assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test
    fun bothPostCommitFailuresAreReportedWithoutChangingCommittedSuccess() = runTest {
        val harness = FoundationHarness()
        val id = harness.workoutWithLoggedWeightedSet()
        val progress = object : ProgressRepository by harness.store {
            override suspend fun replaceRecords(records: List<PersonalRecord>, points: List<ProgressPoint>): FoundationResult<Unit> =
                foundationFailure(FoundationError.Persistence("Progress unavailable"))
        }
        val outcome = routines(harness, CompletionTimer { error("Timer unavailable") }, progress)
            .finishWorkout(id, instant(2_000)).successValue()

        assertEquals(FinishPostCommitWarning.entries.toList(), outcome.warnings)
        assertEquals(1, harness.store.completedWorkouts().size)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
    }

    @Test
    fun cancellationDuringProgressPropagatesAndRetryDoesNotRepeatTimerOrCharge() = runTest {
        val harness = FoundationHarness()
        val id = harness.workoutWithLoggedWeightedSet()
        var cancel = true
        val progress = object : ProgressRepository by harness.store {
            override suspend fun replaceRecords(records: List<PersonalRecord>, points: List<ProgressPoint>): FoundationResult<Unit> {
                if (cancel) awaitCancellation()
                return harness.store.replaceRecords(records, points)
            }
        }
        val timer = CompletionTimer()
        val routines = routines(harness, timer, progress)
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { routines.finishWorkout(id, instant(2_000)) }
        }
        val committed = harness.store.completedWorkouts().single()
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        cancel = false
        val retry = routines.finishWorkout(id, instant(3_000)).successValue()
        assertEquals(committed, retry.workout)
        assertFalse(retry.newlyCompleted)
        assertTrue(retry.warnings.isEmpty())
        assertEquals(1, timer.cancellations)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
    }

    @Test
    fun cancellationFromTimerPropagatesAfterCommit() = runTest {
        val harness = FoundationHarness()
        val id = harness.workoutWithLoggedWeightedSet()
        val timer = CompletionTimer { throw CancellationException("Canceled") }
        val routines = routines(harness, timer)
        assertFailsWith<CancellationException> { routines.finishWorkout(id, instant(2_000)) }
        assertEquals(1, harness.store.completedWorkouts().size)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        assertFalse(routines.finishWorkout(id, instant(3_000)).successValue().newlyCompleted)
        assertEquals(1, timer.cancellations)
    }

    @Test
    fun repositoryFailureIsNotReportedAsSavedAndDoesNotRunPostCommitWork() = runTest {
        val harness = FoundationHarness()
        val id = harness.workoutWithLoggedWeightedSet()
        val failure = FoundationError.Persistence("Disk full")
        val repository = object : WorkoutRepository by harness.store {
            override suspend fun finishActiveWorkout(id: FoundationId, finishedAt: Instant): FoundationResult<WorkoutCompletionReceipt> =
                foundationFailure(failure)
        }
        val timer = CompletionTimer()
        val routines = RoutineUseCases(repository, harness.store, restNotifications = timer)

        assertEquals(failure, assertIs<FoundationResult.Failure>(routines.finishWorkout(id, instant(2_000))).error)
        assertEquals(0, timer.cancellations)
        assertTrue(harness.store.completedWorkouts().isEmpty())
        assertEquals(id, harness.store.currentActiveWorkout()?.id)
        assertEquals(0, harness.store.loadFullAccess().completedFreeWorkouts)
    }

    private fun routines(
        harness: FoundationHarness,
        timer: CompletionTimer,
        progress: ProgressRepository = harness.store
    ) = RoutineUseCases(
        harness.store,
        harness.store,
        personalRecords = PersonalRecordDerivationUseCase(progress, harness.store),
        restNotifications = timer
    )
}

private class CompletionTimer(private val onCancel: () -> Unit = {}) : RestAlertScheduler {
    var cancellations = 0
    override fun schedule(restEndsAt: Instant, soundEnabled: Boolean, persistentSurfaceEnabled: Boolean): RestAlertScheduleResult =
        RestAlertScheduleResult.SCHEDULED

    override fun cancel() {
        cancellations++
        onCancel()
    }
}
