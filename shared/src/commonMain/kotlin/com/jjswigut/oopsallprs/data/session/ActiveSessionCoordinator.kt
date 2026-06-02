package com.jjswigut.oopsallprs.data.session

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.repository.SessionRepository
import com.jjswigut.oopsallprs.platform.RestAlertScheduler
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class HydratedSession(
    val state: ActiveSessionState,
    val elapsedMillis: Long,
    val restRemainingMillis: Long
)

class ActiveSessionCoordinator(
    private val sessions: SessionRepository,
    private val notifications: RestAlertScheduler? = null
) {
    suspend fun hydrate(now: Instant = Clock.System.now()): HydratedSession? {
        val state = sessions.load() ?: return null
        val nextState = if (state.restEndsAt != null && state.restEndsAt <= now) {
            val cleared = state.withoutRest(now)
            sessions.save(cleared)
            notifications?.cancel()
            cleared
        } else {
            state
        }
        return HydratedSession(
            state = nextState,
            elapsedMillis = nextState.elapsedMillis(now),
            restRemainingMillis = nextState.restRemainingMillis(now)
        )
    }
}
