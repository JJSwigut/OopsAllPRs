package com.jjswigut.oopsallprs.ui.navigation

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val order: Int
) {
    TRAIN("train", "Train", 0),
    HISTORY("history", "History", 1),
    PROGRESS("progress", "Progress", 2),
    PROFILE("profile", "Profile", 3);

    companion object {
        val ordered: List<TopLevelDestination> = entries.sortedBy { it.order }

        fun fromRoute(route: String?): TopLevelDestination =
            ordered.firstOrNull { it.route == route } ?: TRAIN
    }
}

sealed class AppRoute(
    val persistedValue: String
) {
    data class TopLevel(val destination: TopLevelDestination) : AppRoute(destination.route)

    data object ActiveWorkout : AppRoute(ACTIVE_WORKOUT_ROUTE)

    val topLevel: TopLevelDestination?
        get() = (this as? TopLevel)?.destination

    val isActiveWorkout: Boolean
        get() = this == ActiveWorkout

    companion object {
        const val ACTIVE_WORKOUT_ROUTE = "active-workout"

        fun fromPersisted(value: String?, hasActiveWorkout: Boolean): AppRoute =
            when (value) {
                ACTIVE_WORKOUT_ROUTE -> if (hasActiveWorkout) ActiveWorkout else TopLevel(TopLevelDestination.TRAIN)
                TopLevelDestination.TRAIN.route -> TopLevel(TopLevelDestination.TRAIN)
                TopLevelDestination.HISTORY.route -> TopLevel(TopLevelDestination.HISTORY)
                TopLevelDestination.PROGRESS.route -> TopLevel(TopLevelDestination.PROGRESS)
                TopLevelDestination.PROFILE.route -> TopLevel(TopLevelDestination.PROFILE)
                else -> TopLevel(TopLevelDestination.TRAIN)
            }
    }
}
