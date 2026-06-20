package com.jjswigut.oopsallprs.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals

class RestDurationRollerTest {

    @Test
    fun rest_duration_formatter_supports_seconds_and_minutes() {
        assertEquals("5s", formatRestDurationSeconds(5))
        assertEquals("45s", formatRestDurationSeconds(45))
        assertEquals("1m", formatRestDurationSeconds(60))
        assertEquals("1:15", formatRestDurationSeconds(75))
        assertEquals("5m", formatRestDurationSeconds(300))
    }
}
