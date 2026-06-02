package com.jjswigut.oopsallprs.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppDestinationTest {
    @Test
    fun topLevelRoutesAreStableAndOrdered() {
        assertEquals(
            listOf("train", "history", "progress", "profile"),
            TopLevelDestination.ordered.map { it.route }
        )
    }

    @Test
    fun unknownRoutesFallBackToTrain() {
        assertEquals(TopLevelDestination.TRAIN, TopLevelDestination.fromRoute(null))
        assertEquals(TopLevelDestination.TRAIN, TopLevelDestination.fromRoute("unknown"))
        assertEquals(
            TopLevelDestination.TRAIN,
            (AppRoute.fromPersisted("unknown", hasActiveWorkout = true) as AppRoute.TopLevel).destination
        )
    }

    @Test
    fun activeWorkoutRouteRequiresAnActiveWorkout() {
        assertTrue(AppRoute.fromPersisted("active-workout", hasActiveWorkout = true).isActiveWorkout)
        assertFalse(AppRoute.fromPersisted("active-workout", hasActiveWorkout = false).isActiveWorkout)
        assertEquals(
            TopLevelDestination.TRAIN,
            (AppRoute.fromPersisted("active-workout", hasActiveWorkout = false) as AppRoute.TopLevel).destination
        )
    }
}
