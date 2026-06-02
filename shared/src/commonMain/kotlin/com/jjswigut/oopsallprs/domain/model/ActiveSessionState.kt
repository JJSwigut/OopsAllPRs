package com.jjswigut.oopsallprs.domain.model

import kotlinx.datetime.Instant

data class ActiveSessionState(
    val activeWorkoutId: FoundationId?,
    val startedAt: Instant?,
    val restEndsAt: Instant? = null,
    val restStartedAt: Instant? = null,
    val restOriginSetId: FoundationId? = null,
    val lastOpenedRoute: String? = null,
    val updatedAt: Instant
) {
    fun elapsedMillis(now: Instant): Long =
        startedAt?.let { now.toEpochMilliseconds() - it.toEpochMilliseconds() }?.coerceAtLeast(0L) ?: 0L

    fun restRemainingMillis(now: Instant): Long =
        restEndsAt?.let { it.toEpochMilliseconds() - now.toEpochMilliseconds() }?.coerceAtLeast(0L) ?: 0L

    fun withoutRest(now: Instant): ActiveSessionState =
        copy(restEndsAt = null, restStartedAt = null, restOriginSetId = null, updatedAt = now)
}
