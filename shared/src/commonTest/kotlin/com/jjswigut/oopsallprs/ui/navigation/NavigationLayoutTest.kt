package com.jjswigut.oopsallprs.ui.navigation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationLayoutTest {
    @Test
    fun compactWidthUsesBottomBarOnly() {
        val layout = NavigationLayoutClass.fromWidthDp(599f)
        assertTrue(layout.usesBottomBar)
        assertFalse(layout.usesNavigationRail)
        assertFalse(layout.usesTwoPaneActiveWorkout)
    }

    @Test
    fun mediumWidthUsesNavigationRail() {
        val layout = NavigationLayoutClass.fromWidthDp(600f)
        assertFalse(layout.usesBottomBar)
        assertTrue(layout.usesNavigationRail)
        assertFalse(layout.usesTwoPaneActiveWorkout)
    }

    @Test
    fun expandedWidthEnablesTwoPaneActiveWorkoutSignal() {
        val layout = NavigationLayoutClass.fromWidthDp(840f)
        assertFalse(layout.usesBottomBar)
        assertTrue(layout.usesNavigationRail)
        assertTrue(layout.usesTwoPaneActiveWorkout)
    }
}
