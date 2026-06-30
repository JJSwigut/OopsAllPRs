package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.domain.model.FullAccessEntitlementSnapshot
import com.jjswigut.oopsallprs.domain.model.FullAccessGate
import com.jjswigut.oopsallprs.domain.model.FullAccessGateResult
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreOffer
import com.jjswigut.oopsallprs.domain.model.FullAccessStoreStatus
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.foundationFailure
import com.jjswigut.oopsallprs.domain.model.foundationSuccess
import com.jjswigut.oopsallprs.domain.model.requiredMessage
import com.jjswigut.oopsallprs.domain.repository.FullAccessRepository
import com.jjswigut.oopsallprs.domain.validation.FoundationError
import com.jjswigut.oopsallprs.platform.FullAccessBillingAdapter
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class FullAccessUseCases(
    private val repository: FullAccessRepository,
    private val billing: FullAccessBillingAdapter? = null
) {
    suspend fun loadState(): FullAccessState = repository.loadFullAccess()

    suspend fun offers(): List<FullAccessStoreOffer> =
        when (val result = billing?.loadOffers()) {
            is FoundationResult.Success -> result.value.ifEmpty { fallbackOffers() }
            else -> fallbackOffers()
        }

    suspend fun refreshEntitlements(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> {
        val adapter = billing
            ?: return markStoreUnavailable("Store purchases are unavailable on this build.", now)
        return when (val result = adapter.refreshEntitlements()) {
            is FoundationResult.Failure -> markStoreError(result.error.message, now)
            is FoundationResult.Success -> saveEntitlements(result.value, now)
        }
    }

    suspend fun purchaseLifetimeUnlock(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> {
        val adapter = billing
            ?: return markStoreUnavailable("Store purchases are unavailable on this build.", now)
        return when (val result = adapter.purchaseLifetimeUnlock()) {
            is FoundationResult.Failure -> markStoreError(result.error.message, now)
            is FoundationResult.Success -> saveEntitlements(result.value, now)
        }
    }

    suspend fun restorePurchases(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> {
        val adapter = billing
            ?: return markStoreUnavailable("Restore purchases is unavailable on this build.", now)
        return when (val result = adapter.restorePurchases()) {
            is FoundationResult.Failure -> markStoreError(result.error.message, now)
            is FoundationResult.Success -> saveEntitlements(result.value, now)
        }
    }

    suspend fun checkGate(gate: FullAccessGate): FullAccessGateResult {
        val state = loadState()
        val allowed = when (gate) {
            FullAccessGate.WORKOUT_START -> state.canStartWorkout()
            FullAccessGate.EXPORT,
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

    suspend fun recordCompletedWorkout(now: Instant = Clock.System.now()): FoundationResult<FullAccessState> {
        val current = loadState()
        if (current.hasFullAccess || current.normalizedCompletedFreeWorkouts >= FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT) {
            return foundationSuccess(current)
        }
        val updated = current.copy(
            completedFreeWorkouts = current.normalizedCompletedFreeWorkouts + 1,
            updatedAt = now
        )
        return repository.saveFullAccess(updated)
    }

    private suspend fun saveEntitlements(
        snapshot: FullAccessEntitlementSnapshot,
        now: Instant
    ): FoundationResult<FullAccessState> {
        val current = loadState()
        val updated = current.copy(
            lifetimeUnlocked = snapshot.lifetimeUnlocked,
            storeStatus = snapshot.storeStatus,
            lastError = snapshot.message,
            updatedAt = now
        )
        return repository.saveFullAccess(updated)
    }

    private suspend fun markStoreUnavailable(
        message: String,
        now: Instant
    ): FoundationResult<FullAccessState> {
        val current = loadState()
        val updated = current.copy(
            storeStatus = FullAccessStoreStatus.UNAVAILABLE,
            lastError = message,
            updatedAt = now
        )
        repository.saveFullAccess(updated)
        return foundationFailure(FoundationError.Platform(message))
    }

    private suspend fun markStoreError(
        message: String,
        now: Instant
    ): FoundationResult<FullAccessState> {
        val current = loadState()
        val updated = current.copy(
            storeStatus = FullAccessStoreStatus.ERROR,
            lastError = message,
            updatedAt = now
        )
        repository.saveFullAccess(updated)
        return foundationFailure(FoundationError.Platform(message))
    }

    private fun fallbackOffers(): List<FullAccessStoreOffer> =
        listOf(
            FullAccessStoreOffer(
                title = "Lifetime Unlock",
                priceLabel = "${'$'}14.99",
                termsLabel = "One-time purchase for this store ecosystem."
            )
        )
}
