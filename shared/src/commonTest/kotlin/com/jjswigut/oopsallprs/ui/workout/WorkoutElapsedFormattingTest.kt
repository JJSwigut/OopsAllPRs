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
}
