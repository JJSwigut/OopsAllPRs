package com.jjswigut.oopsallprs.ui.navigation

enum class NavigationLayoutKind {
    COMPACT,
    MEDIUM,
    EXPANDED
}

data class NavigationLayoutClass(
    val kind: NavigationLayoutKind
) {
    val usesBottomBar: Boolean = kind == NavigationLayoutKind.COMPACT
    val usesNavigationRail: Boolean = kind != NavigationLayoutKind.COMPACT
    val usesTwoPaneActiveWorkout: Boolean = kind == NavigationLayoutKind.EXPANDED

    companion object {
        fun fromWidthDp(widthDp: Float): NavigationLayoutClass =
            when {
                widthDp >= 840f -> NavigationLayoutClass(NavigationLayoutKind.EXPANDED)
                widthDp >= 600f -> NavigationLayoutClass(NavigationLayoutKind.MEDIUM)
                else -> NavigationLayoutClass(NavigationLayoutKind.COMPACT)
            }
    }
}
