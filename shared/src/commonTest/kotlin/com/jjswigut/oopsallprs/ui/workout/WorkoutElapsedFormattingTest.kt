package com.jjswigut.oopsallprs.ui.workout

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutElapsedFormattingTest {
    @Test
    fun activeWorkoutShowsElapsedSecondsImmediately() {
        assertEquals("0:00", formatElapsed(0))
        assertEquals("0:01", formatElapsed(1_000))
        assertEquals("1:01", formatElapsed(61_000))
        assertEquals("1:01:01", formatElapsed(3_661_000))
    }

    @Test
    fun activeWorkoutExplainsThatTimingBeginsWithTheFirstLoggedSet() {
        assertEquals("Starts with first set", workoutElapsedLabel(isTimerStarted = false, elapsedMillis = 0))
        assertEquals("1:01", workoutElapsedLabel(isTimerStarted = true, elapsedMillis = 61_000))
    }

    @Test
    fun exercisesDoNotMakeAnUnstartedTimerLookStuck() {
        assertEquals(
            "The workout timer starts when you log your first set.",
            workoutTimerStartHint(hasExercises = true, isTimerStarted = false)
        )
        assertEquals(null, workoutTimerStartHint(hasExercises = false, isTimerStarted = false))
        assertEquals(null, workoutTimerStartHint(hasExercises = true, isTimerStarted = true))
    }
}
