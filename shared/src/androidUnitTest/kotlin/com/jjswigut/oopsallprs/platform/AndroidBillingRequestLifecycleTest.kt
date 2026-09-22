package com.jjswigut.oopsallprs.platform

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidBillingRequestLifecycleTest {
    @Test
    fun relevantSuccessfulUpdateInvalidatesWithoutAnInAppPurchaseCaller() {
        val slot = AndroidBillingRequestSlot<String>()
        var invalidations = 0
        val updates = AndroidBillingPurchaseUpdates(slot) { invalidations++ }

        updates.dispatch(successful = true, relevant = true) { error("No direct caller exists") }

        assertEquals(1, invalidations)
    }

    @Test
    fun unrelatedAndFailedUpdatesDoNotInvalidateEntitlements() {
        val slot = AndroidBillingRequestSlot<String>()
        var invalidations = 0
        val updates = AndroidBillingPurchaseUpdates(slot) { invalidations++ }

        updates.dispatch(successful = true, relevant = false) { error("No direct caller exists") }
        updates.dispatch(successful = false, relevant = true) { error("No direct caller exists") }

        assertEquals(0, invalidations)
    }

    @Test
    fun directResponseIsPreservedAlongsideInvalidation() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        slot.pending = request
        var invalidations = 0
        val updates = AndroidBillingPurchaseUpdates(slot) { invalidations++ }

        updates.dispatch(successful = true, relevant = true) { it.complete("purchase-result") }

        assertEquals("purchase-result", request.await())
        assertEquals(1, invalidations)
        assertNull(slot.pending)
    }

    @Test
    fun unrelatedSuccessfulUpdateLeavesLifetimeWaiterUntilRelevantUpdate() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val lifetimeRequest = CompletableDeferred<String>()
        slot.pending = lifetimeRequest
        var invalidations = 0
        val updates = AndroidBillingPurchaseUpdates(slot) { invalidations++ }

        updates.dispatch(successful = true, relevant = false) { error("Unrelated product consumed the lifetime waiter") }
        assertSame(lifetimeRequest, slot.pending)
        assertFalse(lifetimeRequest.isCompleted)
        assertEquals(0, invalidations)

        updates.dispatch(successful = true, relevant = true) { it.complete("lifetime") }
        assertEquals("lifetime", lifetimeRequest.await())
        assertEquals(1, invalidations)
        assertNull(slot.pending)
    }

    @Test
    fun cancellationDoesNotReturnBeforeOwnedOperationCleanupAllowsRetry() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        val allowCleanup = CompletableDeferred<Unit>()
        var purchaseInProgress = false
        val nativeOperation = async {
            purchaseInProgress = true
            slot.pending = request
            try {
                awaitCancellation()
            } finally {
                withContext(NonCancellable) {
                    allowCleanup.await()
                    slot.clear(request)
                    purchaseInProgress = false
                }
            }
        }
        val caller = launch { awaitOwnedAndroidBillingOperation(nativeOperation) }
        runCurrent()

        caller.cancel()
        runCurrent()
        assertFalse(caller.isCompleted)
        assertTrue(purchaseInProgress)
        assertSame(request, slot.pending)

        allowCleanup.complete(Unit)
        caller.join()
        assertTrue(nativeOperation.isCompleted)
        assertFalse(purchaseInProgress)
        assertNull(slot.pending)
    }

    @Test
    fun canceledNativeAwaitReleasesItsPendingRequestForRetry() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        slot.pending = request
        val waiting = launch { slot.await(request) }
        runCurrent()

        waiting.cancelAndJoin()

        assertNull(slot.pending)
    }

    @Test
    fun timedOutNativeAwaitReleasesItsPendingRequestForRetry() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        slot.pending = request

        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { slot.await(request) }
        }

        assertNull(slot.pending)
    }

    @Test
    fun cancelingOneSharedConnectionWaiterDoesNotCancelAnother() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        slot.pending = request
        val owner = launch { slot.await(request) }
        val waiter = async { slot.await(request) }
        runCurrent()

        owner.cancelAndJoin()

        assertSame(request, slot.pending)
        assertFalse(waiter.isCompleted)
        request.complete("connected")
        assertEquals("connected", waiter.await())
    }

    @Test
    fun staleCompletionCannotClearANewerPendingRequest() {
        val slot = AndroidBillingRequestSlot<String>()
        val oldRequest = CompletableDeferred<String>()
        val retry = CompletableDeferred<String>()
        slot.pending = oldRequest
        slot.pending = retry

        slot.clear(oldRequest)
        oldRequest.complete("late")

        assertSame(retry, slot.pending)
        assertFalse(retry.isCompleted)
    }

    @Test
    fun timeoutReturnsFailureAndRetryIgnoresLateAndDuplicateCallbacks() = runTest {
        lateinit var oldCallback: (FoundationResult<String>) -> Unit
        val timedOut = awaitAndroidBillingCallback<String>(100, "Still awaiting store confirmation.") {
            oldCallback = it
        }
        assertEquals("Still awaiting store confirmation.", assertIs<FoundationResult.Failure>(timedOut).error.message)

        lateinit var retryCallback: (FoundationResult<String>) -> Unit
        val retry = async {
            awaitAndroidBillingCallback<String>(100, "Retry timed out.") { retryCallback = it }
        }
        runCurrent()
        oldCallback(foundationSuccess("stale"))
        assertFalse(retry.isCompleted)
        retryCallback(foundationSuccess("current"))
        retryCallback(foundationSuccess("duplicate"))
        assertEquals(foundationSuccess("current"), retry.await())
    }

    @Test
    fun externalCancellationIsNotConvertedToTimeoutFailure() = runTest {
        lateinit var callback: (FoundationResult<String>) -> Unit
        val waiting = async {
            awaitAndroidBillingCallback<String>(1_000, "Timed out.") { callback = it }
        }
        runCurrent()
        waiting.cancelAndJoin()
        assertFailsWith<CancellationException> { waiting.await() }
        callback(foundationSuccess("late"))
    }

    @Test
    fun cancellationThrownByNativeRegistrationPropagates() = runTest {
        assertFailsWith<CancellationException> {
            awaitAndroidBillingCallback<String>(100, "Timed out.") {
                throw CancellationException("Registration canceled")
            }
        }
    }

    @Test
    fun boundedPurchaseWaitReleasesSlotAndPreservesTimeoutMessage() = runTest {
        val slot = AndroidBillingRequestSlot<FoundationResult<String>>()
        val request = CompletableDeferred<FoundationResult<String>>()
        slot.pending = request
        val message = "The purchase may still complete. Restore purchases to check."
        val result = awaitAndroidBillingResult(120_000, message) { slot.await(request) }
        assertEquals(message, assertIs<FoundationResult.Failure>(result).error.message)
        assertNull(slot.pending)
        assertTrue(request.isCancelled)
    }

    @Test
    fun timedOutConnectionWaiterDoesNotClearAnotherWaiter() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val request = CompletableDeferred<String>()
        slot.pending = request
        val remaining = async { slot.await(request) }
        runCurrent()
        assertFailsWith<TimeoutCancellationException> {
            withTimeout(100) { slot.await(request) }
        }
        assertSame(request, slot.pending)
        request.complete("connected")
        assertEquals("connected", remaining.await())
        assertNull(slot.pending)
    }

    @Test
    fun disposalCancelsPendingAndAlreadyDispatchedRequests() = runTest {
        val slot = AndroidBillingRequestSlot<String>()
        val dispatched = CompletableDeferred<String>()
        slot.pending = dispatched
        val firstWaiter = async { slot.await(dispatched) }
        runCurrent()
        slot.take()
        val pending = CompletableDeferred<String>()
        slot.pending = pending
        val secondWaiter = async { slot.await(pending) }
        runCurrent()

        slot.cancelAll()
        assertFailsWith<CancellationException> { firstWaiter.await() }
        assertFailsWith<CancellationException> { secondWaiter.await() }
        assertNull(slot.pending)
        assertFalse(dispatched.complete("late"))
    }

    @Test
    fun resumeBeforeObserverAttachmentIsDeliveredAndLaterResumesInvalidate() {
        val invalidation = AndroidBillingInvalidation()
        var count = 0
        invalidation.invalidate()
        invalidation.setObserver { count++ }
        assertEquals(1, count)
        invalidation.invalidate()
        assertEquals(2, count)
        invalidation.setObserver(null)
        invalidation.invalidate()
        assertEquals(2, count)
        invalidation.setObserver { count++ }
        assertEquals(3, count)
    }

    @Test
    fun disposedObserverCannotReceiveLateCallbacksOrBeReattached() {
        val invalidation = AndroidBillingInvalidation()
        var count = 0
        invalidation.setObserver { count++ }
        invalidation.dispose()
        invalidation.invalidate()
        invalidation.setObserver { count++ }
        invalidation.invalidate()
        assertEquals(0, count)
    }
}
