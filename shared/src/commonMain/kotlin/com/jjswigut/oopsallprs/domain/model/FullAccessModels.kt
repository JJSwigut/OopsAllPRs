package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

const val FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT: Int = FullAccessBillingConfig.FREE_COMPLETED_WORKOUT_LIMIT

enum class FullAccessStatus {
    TRIAL,
    EXPIRED,
    LIFETIME,
    UNAVAILABLE
}

enum class FullAccessGate {
    WORKOUT_START,
    EXPORT,
    BACKUP_LINK,
    BACKUP_NOW,
    SYNC_NOW,
    RESTORE_BACKUP
}

enum class FullAccessStoreStatus {
    NOT_CHECKED,
    AVAILABLE,
    UNAVAILABLE,
    ERROR
}

data class FullAccessState(
    val completedFreeWorkouts: Int = 0,
    val lifetimeUnlocked: Boolean = false,
    val storeStatus: FullAccessStoreStatus = FullAccessStoreStatus.NOT_CHECKED,
    val lastError: String? = null,
    val updatedAt: Instant? = null
) {
    val normalizedCompletedFreeWorkouts: Int
        get() = completedFreeWorkouts.coerceAtLeast(0)

    val hasFullAccess: Boolean
        get() = lifetimeUnlocked

    val remainingFreeWorkouts: Int
        get() = (FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT - normalizedCompletedFreeWorkouts).coerceAtLeast(0)

    val status: FullAccessStatus
        get() = when {
            lifetimeUnlocked -> FullAccessStatus.LIFETIME
            storeStatus == FullAccessStoreStatus.UNAVAILABLE && normalizedCompletedFreeWorkouts >= FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT -> FullAccessStatus.UNAVAILABLE
            remainingFreeWorkouts > 0 -> FullAccessStatus.TRIAL
            else -> FullAccessStatus.EXPIRED
        }

    fun canStartWorkout(): Boolean = hasFullAccess || remainingFreeWorkouts > 0

    fun canUsePaidDataTools(): Boolean = hasFullAccess
}

data class FullAccessStoreOffer(
    val title: String,
    val priceLabel: String,
    val termsLabel: String
)

data class FullAccessEntitlementSnapshot(
    val lifetimeUnlocked: Boolean = false,
    val storeStatus: FullAccessStoreStatus = FullAccessStoreStatus.AVAILABLE,
    val message: String? = null
)

data class FullAccessGateResult(
    val allowed: Boolean,
    val state: FullAccessState,
    val message: String? = null
)

fun FullAccessGate.requiredMessage(): String =
    when (this) {
        FullAccessGate.WORKOUT_START -> "Full Access is required to start another workout."
        FullAccessGate.EXPORT -> "Full Access is required to export your data."
        FullAccessGate.BACKUP_LINK -> "Full Access is required to set up backup."
        FullAccessGate.BACKUP_NOW -> "Full Access is required to back up your data."
        FullAccessGate.SYNC_NOW -> "Full Access is required to sync your backup."
        FullAccessGate.RESTORE_BACKUP -> "Full Access is required to restore from backup."
    }
