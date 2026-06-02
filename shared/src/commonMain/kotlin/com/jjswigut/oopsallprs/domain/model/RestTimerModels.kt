package com.jjswigut.oopsallprs.domain.model

data class RestConfiguration(
    val durationSeconds: Int = DEFAULT_SECONDS,
    val autoStart: Boolean = true
) {
    init {
        require(durationSeconds >= 0) { "Rest duration must be zero or greater" }
    }

    val isEnabled: Boolean = autoStart && durationSeconds > 0

    companion object {
        const val DEFAULT_SECONDS: Int = 120

        fun default(): RestConfiguration = RestConfiguration()

        fun disabled(): RestConfiguration = RestConfiguration(durationSeconds = 0, autoStart = false)
    }
}

data class ActiveRestTimer(
    val activeWorkoutId: FoundationId,
    val originSetId: FoundationId?,
    val startedAt: kotlinx.datetime.Instant,
    val endsAt: kotlinx.datetime.Instant
) {
    fun remainingMillis(now: kotlinx.datetime.Instant): Long =
        (endsAt.toEpochMilliseconds() - now.toEpochMilliseconds()).coerceAtLeast(0L)
}
