package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessGate
import com.jjswigut.oopsallprs.domain.model.FullAccessGateResult
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessOfferState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.requiredMessage
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import com.jjswigut.oopsallprs.platform.FullAccessBillingObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FullAccessUseCases(
    private val repository: FullAccessRepository,
    private val billing: FullAccessBillingAdapter? = null
) {
    private val storeOperationMutex = Mutex()
    private val persistenceMutex = Mutex()
    private val observerMutex = Mutex()
    private val observerChannels = MutableStateFlow<Set<SendChannel<Unit>>>(emptySet())
    private val entitlementObserver = object : FullAccessBillingObserver {
        override fun onEntitlementsChanged() {
            observerChannels.value.forEach { it.trySend(Unit) }
        }
    }

    suspend fun loadState(): FullAccessState = persistenceMutex.withLock { repository.loadFullAccess() }

    fun observeEntitlementChanges(): Flow<Unit> = callbackFlow<Unit> {
        try {
            observerMutex.withLock {
                val existing = observerChannels.value
                observerChannels.value = existing + channel
                if (existing.isEmpty()) billing?.setEntitlementObserver(entitlementObserver)
            }
            trySend(Unit)
            awaitClose { }
        } finally {
            // Cleanup must run even when a collector is canceled while another is registering.
            withContext(NonCancellable) {
                observerMutex.withLock {
                    val existing = observerChannels.value
                    if (channel in existing) {
                        val remaining = existing - channel
                        observerChannels.value = remaining
                        if (remaining.isEmpty()) {
                            try {
                                billing?.setEntitlementObserver(null)
                            } catch (_: Exception) {
                                // A late native callback now has no recipients.
                            }
                        }
                    }
                }
            }
        }
    }.buffer(Channel.CONFLATED)

    suspend fun loadLifetimeOffer(): FullAccessOfferState {
        val adapter = billing
            ?: return FullAccessOfferState.Unavailable("Store purchases are unavailable on this build.")
        return try {
            when (val result = withTimeoutOrNull(15_000) { adapter.loadOffers() }) {
                null -> FullAccessOfferState.Unavailable("Loading the store offer timed out. Try again.")
                is FoundationResult.Failure -> FullAccessOfferState.Unavailable(
                    result.error.message.ifBlank { "The store offer is unavailable. Try again." }
                )
                is FoundationResult.Success -> {
                    val offer = result.value.singleOrNull()?.takeIf { it.priceLabel.isNotBlank() }
                    if (offer == null) {
                        FullAccessOfferState.Unavailable("The lifetime offer is unavailable. Try again.")
                    } else {
                        FullAccessOfferState.Available(offer)
                    }
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            FullAccessOfferState.Unavailable(error.message?.takeIf { it.isNotBlank() }
                ?: "The store offer is unavailable. Try again.")
        }
    }

    suspend fun refreshEntitlements(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> =
        storeOperation(now, STORE_NETWORK_TIMEOUT_MS, "Store purchases are unavailable on this build.") {
            it.refreshEntitlements()
        }

    suspend fun purchaseLifetimeUnlock(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> =
        storeOperation(
            now, PURCHASE_TIMEOUT_MS, "Store purchases are unavailable on this build.",
            "The purchase is taking longer than expected and may still complete. Check or restore access before purchasing again."
        ) {
            it.purchaseLifetimeUnlock()
        }

    suspend fun restorePurchases(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> =
        storeOperation(now, STORE_NETWORK_TIMEOUT_MS, "Restore purchases is unavailable on this build.") {
            it.restorePurchases()
        }

    suspend fun checkGate(gate: FullAccessGate): FullAccessGateResult {
        val state = loadState()
        val allowed = when (gate) {
            FullAccessGate.WORKOUT_START -> state.canStartWorkout()
            FullAccessGate.EXPORT -> true
            FullAccessGate.IMPORT,
            FullAccessGate.BACKUP_LINK,
            FullAccessGate.BACKUP_NOW,
            FullAccessGate.SYNC_NOW,
            FullAccessGate.RESTORE_BACKUP -> state.canUsePaidDataTools()
        }
        return FullAccessGateResult(
            allowed = allowed,
            state = state,
            message = if (allowed) null else gate.requiredMessage()
        )
    }

    /** Enables access only for a locally seeded developer demo. */
    internal suspend fun enableDeveloperDemoAccess(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> =
        updateState { current ->
            current.copy(
                lifetimeUnlocked = true,
                storeStatus = FullAccessStoreStatus.NOT_CHECKED,
                lastError = null,
                updatedAt = now
            )
        }

    private suspend fun storeOperation(
        now: Instant,
        timeoutMillis: Long,
        unavailableMessage: String,
        timeoutMessage: String = "Store operation timed out. Try again.",
        operation: suspend (FullAccessBillingAdapter) -> FoundationResult<FullAccessEntitlementSnapshot>
    ): FoundationResult<FullAccessState> = storeOperationMutex.withLock {
        val adapter = billing ?: return@withLock markStoreFailure(
            unavailableMessage, FullAccessStoreStatus.UNAVAILABLE, now
        )
        when (val result = storeCall(timeoutMillis, timeoutMessage) { operation(adapter) }) {
            is FoundationResult.Failure -> markStoreFailure(result.error.message, FullAccessStoreStatus.ERROR, now)
            is FoundationResult.Success -> saveEntitlements(result.value, adapter, now)
        }
    }

    private suspend fun saveEntitlements(
        snapshot: FullAccessEntitlementSnapshot,
        adapter: FullAccessBillingAdapter,
        now: Instant
    ): FoundationResult<FullAccessState> {
        when (val saved = updateState { current ->
            current.copy(
                lifetimeUnlocked = snapshot.lifetimeUnlocked,
                storeStatus = snapshot.storeStatus,
                lastError = snapshot.message,
                updatedAt = now
            )
        }) {
            is FoundationResult.Failure -> return saved
            is FoundationResult.Success -> Unit
        }
        // Native delivery may wait on the store, so it must not hold the persistence lock.
        // The token belongs only to the exact snapshot whose ownership was just saved.
        return when (val delivery = storeCall(STORE_NETWORK_TIMEOUT_MS) {
            adapter.completeEntitlementDelivery(snapshot.deliveryToken)
        }) {
            is FoundationResult.Success -> updateState { it }
            is FoundationResult.Failure -> updateState { current ->
                current.copy(storeStatus = FullAccessStoreStatus.ERROR, lastError = delivery.error.message, updatedAt = now)
            }
        }
    }

    private suspend fun markStoreFailure(
        message: String,
        status: FullAccessStoreStatus,
        now: Instant
    ): FoundationResult<FullAccessState> {
        return when (val saved = updateState { current ->
            current.copy(storeStatus = status, lastError = message, updatedAt = now)
        }) {
            is FoundationResult.Failure -> saved
            is FoundationResult.Success -> foundationFailure(FoundationError.Platform(message))
        }
    }

    private suspend fun updateState(
        transform: (FullAccessState) -> FullAccessState
    ): FoundationResult<FullAccessState> = persistenceMutex.withLock {
        try {
            currentCoroutineContext().ensureActive()
            repository.updateFullAccess(transform)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            foundationFailure(FoundationError.Persistence(error.message ?: "Could not save Full Access state."))
        }
    }

    private suspend fun <T> storeCall(
        timeoutMillis: Long,
        timeoutMessage: String = "Store operation timed out. Try again.",
        operation: suspend () -> FoundationResult<T>
    ): FoundationResult<T> = try {
        withTimeoutOrNull(timeoutMillis) {
            val result = operation()
            currentCoroutineContext().ensureActive()
            result
        } ?: foundationFailure(FoundationError.Platform(timeoutMessage))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Exception) {
        foundationFailure(FoundationError.Platform(error.message ?: "Store operation failed. Try again."))
    }

    private companion object {
        const val STORE_NETWORK_TIMEOUT_MS = 15_000L
        // A purchase includes user interaction and needs a longer watchdog than a read.
        const val PURCHASE_TIMEOUT_MS = 300_000L
    }
}
