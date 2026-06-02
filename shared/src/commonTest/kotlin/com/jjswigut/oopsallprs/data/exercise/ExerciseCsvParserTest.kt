package com.jjswigut.oopsallprs.data.exercise

import com.jjswigut.oopsallprs.testing.SAMPLE_CSV
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExerciseCsvParserTest {
    @Test
    fun parsesQuotedFieldsAndBodyweightRows() {
        val report = ExerciseCsvParser.parse(SAMPLE_CSV)
        assertEquals(4, report.acceptedRows.size)
        assertEquals("Cable Row, Seated", report.acceptedRows.last().exerciseName)
        assertTrue(report.acceptedRows.any { it.isBodyweight })
    }

    @Test
    fun reportsMalformedRows() {
        val report = ExerciseCsvParser.parse(
            "Exercise Name,Muscle Group,Equipment,Movement Pattern,Exercise Type,Experience Level,Body Region\nBad,Row"
        )
        assertEquals(1, report.rejectedRows.size)
    }
}
