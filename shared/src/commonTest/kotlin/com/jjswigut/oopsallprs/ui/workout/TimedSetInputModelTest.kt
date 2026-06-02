package com.jjswigut.oopsallprs.ui.workout

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimedSetInputModelTest {
    @Test
    fun durationInputAcceptsSecondsMinutesAndHours() {
        assertEquals(45_000L, parseDurationInput("45"))
        assertEquals(75_000L, parseDurationInput("1:15"))
        assertEquals(3_675_000L, parseDurationInput("1:01:15"))
        assertNull(parseDurationInput("1:"))
    }

    @Test
    fun durationFormattingAndStepperUseClockLabels() {
        assertEquals("0:45", formatDurationMs(45_000L))
        assertEquals("1:01:15", formatDurationMs(3_675_000L))
        assertEquals(90_000L, nextDurationMs(75_000L, 15_000L))
        assertEquals(0L, nextDurationMs(10_000L, -15_000L))
    }
}
