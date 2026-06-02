package com.jjswigut.oopsallprs.ui.navigation

import com.jjswigut.oopsallprs.domain.model.ActiveSessionState
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.ds.theme.FitPalette
import com.jjswigut.oopsallprs.ds.theme.FitPalettes
import kotlinx.datetime.Instant

enum class PaletteMode {
    SYSTEM,
    DARK,
    LIGHT;

    fun resolve(systemDark: Boolean = true): FitPalette =
        when (this) {
            SYSTEM -> if (systemDark) FitPalettes.IceDark else FitPalettes.IceLight
            DARK -> FitPalettes.IceDark
            LIGHT -> FitPalettes.IceLight
        }
}

data class ActiveWorkoutResume(
    val activeWorkoutId: FoundationId,
    val startedAt: Instant,
    val elapsedMillis: Long,
    val restRemainingMillis: Long,
    val displayTitle: String = "Active workout",
    val resumeRoute: AppRoute = AppRoute.ActiveWorkout
) {
    companion object {
        fun fromSession(session: ActiveSessionState?, now: Instant): ActiveWorkoutResume? {
            val workoutId = session?.activeWorkoutId ?: return null
            val startedAt = session.startedAt ?: return null
            return ActiveWorkoutResume(
                activeWorkoutId = workoutId,
                startedAt = startedAt,
                elapsedMillis = session.elapsedMillis(now),
                restRemainingMillis = session.restRemainingMillis(now)
            )
        }
    }
}

data class AppShellState(
    val selectedDestination: TopLevelDestination = TopLevelDestination.TRAIN,
    val layoutClass: NavigationLayoutClass = NavigationLayoutClass(NavigationLayoutKind.COMPACT),
    val activeWorkoutResume: ActiveWorkoutResume? = null,
    val isActiveWorkoutPresented: Boolean = false,
    val previousTopLevelDestination: TopLevelDestination = TopLevelDestination.TRAIN,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val paletteMode: PaletteMode = PaletteMode.DARK,
    val errorMessage: String? = null
)

sealed interface NavigationIntent {
    data class Hydrate(val session: ActiveSessionState?, val now: Instant) : NavigationIntent
    data class SelectDestination(val destination: TopLevelDestination) : NavigationIntent
    data object PresentActiveWorkout : NavigationIntent
    data object DismissActiveWorkout : NavigationIntent
    data class SetLayoutClass(val layoutClass: NavigationLayoutClass) : NavigationIntent
    data class SetPreferences(
        val reduceMotion: Boolean,
        val hapticsEnabled: Boolean,
        val paletteMode: PaletteMode
    ) : NavigationIntent
    data class ActiveSessionChanged(val session: ActiveSessionState?, val now: Instant) : NavigationIntent
}
