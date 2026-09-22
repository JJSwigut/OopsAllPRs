package com.jjswigut.oopsallprs.ui.common

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateLabelsTest {
    @Test
    fun shortDateUsesTheViewerTimezoneAndReadableMonth() {
        val instant = Instant.parse("2024-07-03T00:49:00Z")

        assertEquals("Jul 2", instant.shortDateLabel(TimeZone.of("America/New_York")))
        assertEquals("Jul 3", instant.shortDateLabel(TimeZone.of("Asia/Tokyo")))
    }
}
