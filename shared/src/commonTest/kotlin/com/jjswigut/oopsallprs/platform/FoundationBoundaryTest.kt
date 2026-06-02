package com.jjswigut.oopsallprs.platform

import com.jjswigut.oopsallprs.data.exercise.ExerciseCsvParser
import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoundationBoundaryTest {
    @Test
    fun seedBootstrapParsesBaselineShape() {
        val report = ExerciseCsvParser.parse(SAMPLE_CSV)
        assertEquals(4, report.acceptedRows.size)
        assertTrue(report.acceptedRows.any { it.isBodyweight })
    }

    @Test
    fun platformBoundaryIsRepresentedByExpectTypes() {
        val boundaryTypes = listOf(
            "PlatformDatabaseDriverFactory",
            "LocalSettingsStore",
            "RestNotificationScheduler",
            "FileExportHandoff",
            "HapticFeedback",
            "PlatformClock"
        )
        assertEquals(6, boundaryTypes.size)
    }
}
