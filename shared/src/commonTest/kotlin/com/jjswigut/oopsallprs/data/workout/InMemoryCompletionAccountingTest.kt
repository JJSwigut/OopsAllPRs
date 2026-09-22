package com.jjswigut.oopsallprs.data.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InMemoryCompletionAccountingTest {
    @Test
    fun preCanceledFinishDoesNotCommitOrChargeEvenWhenLockIsUncontended() = runTest {
        val harness = FoundationHarness()
        val activeId = harness.workoutWithLoggedWeightedSet()
        var checked = false
        launch(start = CoroutineStart.UNDISPATCHED) {
            cancel()
            assertFailsWith<CancellationException> { harness.store.finishActiveWorkout(activeId, instant(2_000)) }
            checked = true
        }.join()
        assertTrue(checked)
        assertTrue(harness.store.completedWorkouts().isEmpty())
        assertEquals(activeId, harness.store.currentActiveWorkout()?.id)
        assertEquals(activeId, harness.store.load()?.activeWorkoutId)
        assertEquals(0, harness.store.loadFullAccess().completedFreeWorkouts)
        assertTrue(harness.store.finishActiveWorkout(activeId, instant(3_000)).successValue().newlyCompleted)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
    }

    @Test
    fun preCanceledUpdateDoesNotInvokeTransformOrMutateState() = runTest {
        val harness = FoundationHarness()
        var invoked = false
        var checked = false
        launch(start = CoroutineStart.UNDISPATCHED) {
            cancel()
            assertFailsWith<CancellationException> {
                harness.store.updateFullAccess {
                    invoked = true
                    it.copy(lifetimeUnlocked = true)
                }
            }
            checked = true
        }.join()
        assertTrue(checked)
        assertFalse(invoked)
        assertFalse(harness.store.loadFullAccess().hasFullAccess)
    }

    @Test
    fun transformThatCancelsCallerCannotPersistItsResult() = runTest {
        val harness = FoundationHarness()
        val before = harness.store.loadFullAccess()
        var checked = false
        launch(start = CoroutineStart.UNDISPATCHED) {
            val job = currentCoroutineContext().job
            assertFailsWith<CancellationException> {
                harness.store.updateFullAccess {
                    job.cancel()
                    it.copy(lifetimeUnlocked = true, completedFreeWorkouts = 8)
                }
            }
            checked = true
        }.join()
        assertTrue(checked)
        assertEquals(before, harness.store.loadFullAccess())
        harness.store.updateFullAccess { it.copy(lifetimeUnlocked = true) }.successValue()
        assertTrue(harness.store.loadFullAccess().hasFullAccess)
    }

    @Test
    fun concurrentAndSequentialRetriesReturnOneCompletionAndOneCharge() = runTest {
        val harness = FoundationHarness()
        val activeId = harness.workoutWithLoggedWeightedSet()
        val receipts = List(2) { index ->
            async { harness.store.finishActiveWorkout(activeId, instant(2_000L + index)).successValue() }
        }.awaitAll()

        assertEquals(1, receipts.count { it.newlyCompleted })
        assertEquals(receipts.first().workout, receipts.last().workout)
        val retry = harness.store.finishActiveWorkout(activeId, instant(9_000)).successValue()
        assertFalse(retry.newlyCompleted)
        assertEquals(receipts.first().workout, retry.workout)
        assertEquals(1, harness.store.completedWorkouts().size)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        assertNull(harness.store.load())
        assertNull(harness.store.activeWorkout(activeId))
    }

    @Test
    fun deletedCompletionDoesNotRefundAndRestoredSourceDoesNotChargeAgain() = runTest {
        val harness = FoundationHarness()
        val activeId = harness.workoutWithLoggedWeightedSet()
        val active = requireNotNull(harness.store.activeWorkout(activeId))
        val completed = harness.store.finishActiveWorkout(active.id, instant(2_000)).successValue().workout
        harness.store.deleteCompletedWorkout(completed.id, instant(3_000)).successValue()
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
        assertIs<FoundationResult.Failure>(harness.store.finishActiveWorkout(active.id, instant(4_000)))
        assertTrue(harness.store.completedWorkouts().isEmpty())

        harness.store.createActiveWorkout(active).successValue()
        val restored = harness.store.finishActiveWorkout(active.id, instant(5_000)).successValue()
        assertTrue(restored.newlyCompleted)
        assertEquals(1, harness.store.completedWorkouts().size)
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
    }

    @Test
    fun completionCountSaturatesAtTen() = runTest {
        val harness = FoundationHarness()
        harness.store.setFullAccessForTest(FullAccessState(completedFreeWorkouts = 9)).successValue()
        repeat(2) { index ->
            val activeId = harness.workoutWithLoggedWeightedSet()
            val receipt = harness.store.finishActiveWorkout(activeId, instant(index * 2_000L + 1_000)).successValue()
            assertEquals(1, receipt.workout.exercises.size)
            assertEquals(10, harness.store.loadFullAccess().completedFreeWorkouts)
        }
    }

    @Test
    fun paidCompletionDoesNotConsumeFreeAllowance() = runTest {
        val harness = FoundationHarness()
        harness.store.setFullAccessForTest(FullAccessState(completedFreeWorkouts = 4, lifetimeUnlocked = true)).successValue()
        val activeId = harness.workoutWithLoggedWeightedSet()
        harness.store.finishActiveWorkout(activeId, instant(2_000)).successValue()
        assertEquals(4, harness.store.loadFullAccess().completedFreeWorkouts)
        assertTrue(harness.store.loadFullAccess().hasFullAccess)
    }

    @Test
    fun entitlementTransformsAndCompletionPreserveBothChangesInEitherOrdering() = runTest {
        for (entitlementFirst in listOf(false, true)) {
            val harness = FoundationHarness()
            val activeId = harness.workoutWithLoggedWeightedSet()
            val completion: suspend () -> Unit = {
                harness.store.finishActiveWorkout(activeId, instant(2_000)).successValue()
            }
            val entitlement: suspend () -> Unit = {
                harness.store.updateFullAccess { it.copy(lifetimeUnlocked = true) }.successValue()
            }
            val operations = if (entitlementFirst) listOf(entitlement, completion) else listOf(completion, entitlement)
            operations.map { operation -> async { operation() } }.awaitAll()
            val access = harness.store.loadFullAccess()
            assertTrue(access.hasFullAccess)
            assertEquals(if (entitlementFirst) 0 else 1, access.completedFreeWorkouts)
            assertEquals(1, harness.store.completedWorkouts().size)
        }
    }

    @Test
    fun failedAndCanceledTransformsLeaveStateUnchangedAndReleaseAccountingLock() = runTest {
        val harness = FoundationHarness()
        val before = harness.store.loadFullAccess()
        val failure = harness.store.updateFullAccess { error("Transform failed") }
        assertIs<FoundationError.Persistence>(assertIs<FoundationResult.Failure>(failure).error)
        assertEquals(before, harness.store.loadFullAccess())
        assertFailsWith<CancellationException> {
            harness.store.updateFullAccess { throw CancellationException("Canceled") }
        }
        assertEquals(before, harness.store.loadFullAccess())
        val activeId = harness.workoutWithLoggedWeightedSet()
        harness.store.finishActiveWorkout(activeId, instant(2_000)).successValue()
        assertEquals(1, harness.store.loadFullAccess().completedFreeWorkouts)
    }
}
