package com.jjswigut.oopsallprs.platform

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

// Slot ownership and waiter bookkeeping are confined to the billing main dispatcher.
internal class AndroidBillingRequestSlot<T> {
    var pending: CompletableDeferred<T>? = null
    private val waiters = mutableMapOf<CompletableDeferred<T>, Int>()

    fun take(): CompletableDeferred<T>? = pending.also { pending = null }

    fun clear(request: CompletableDeferred<T>) {
        if (pending === request) pending = null
    }

    suspend fun await(request: CompletableDeferred<T>): T {
        waiters[request] = (waiters[request] ?: 0) + 1
        try {
            return request.await()
        } finally {
            val remaining = waiters.getValue(request) - 1
            if (remaining == 0) {
                waiters.remove(request)
                clear(request)
                request.cancel()
            } else {
                waiters[request] = remaining
            }
        }
    }

    fun cancelAll() {
        take()?.cancel()
        waiters.keys.toList().forEach { it.cancel() }
    }
}

internal class AndroidBillingPurchaseUpdates<T>(
    private val requests: AndroidBillingRequestSlot<T>,
    private val onEntitlementsChanged: () -> Unit
) {
    fun dispatch(
        successful: Boolean,
        relevant: Boolean,
        deliver: (CompletableDeferred<T>) -> Unit
    ) {
        if (successful && !relevant) return
        val request = requests.take()
        if (successful && relevant) onEntitlementsChanged()
        if (request != null) deliver(request)
    }
}

internal class AndroidBillingInvalidation {
    private var observer: (() -> Unit)? = null
    private var dirty = false
    private var disposed = false

    fun setObserver(observer: (() -> Unit)?) {
        if (disposed) return
        this.observer = observer
        if (dirty && observer != null) invalidate()
    }

    fun invalidate() {
        if (disposed) return
        val listener = observer
        dirty = listener == null
        listener?.invoke()
    }

    fun dispose() {
        disposed = true
        observer = null
        dirty = false
    }
}

internal suspend fun <T> awaitOwnedAndroidBillingOperation(operation: Deferred<T>): T {
    try {
        return operation.await()
    } finally {
        withContext(NonCancellable) { operation.cancelAndJoin() }
    }
}

internal suspend fun <T> awaitAndroidBillingResult(
    timeoutMillis: Long,
    timeoutMessage: String,
    block: suspend () -> FoundationResult<T>
): FoundationResult<T> = withTimeoutOrNull(timeoutMillis) { block() }
    ?: foundationFailure(FoundationError.Platform(timeoutMessage))

internal suspend fun <T> awaitAndroidBillingCallback(
    timeoutMillis: Long,
    timeoutMessage: String,
    register: (complete: (FoundationResult<T>) -> Unit) -> Unit
): FoundationResult<T> = awaitAndroidBillingResult(timeoutMillis, timeoutMessage) {
    val result = CompletableDeferred<FoundationResult<T>>()
    try {
        register { result.complete(it) }
        result.await()
    } finally {
        // Native callbacks cannot be unregistered; late/duplicate delivery is harmless.
        result.cancel()
    }
}
