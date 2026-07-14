package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressTimedDisplayTest {
    @Test
    fun timeRecordsRenderDurationLabels() {
        val record = progressRecord(
            id = FoundationId("pr-time"),
            exerciseCatalogId = FoundationId("exercise-plank"),
            recordKind = PersonalRecordKind.TIME,
            reps = null,
            weight = null,
            value = 75_000.0,
            sourceSetId = FoundationId("set-plank")
        )

        val row = record.toProgressPrRow("Plank", WeightUnit.POUNDS)

        assertEquals("Best time", row.kindLabel)
        assertEquals("1:15", row.valueLabel)
        assertEquals("Time", row.detailLabel)
    }

    @Test
    fun timeChartUsesTimeMetricAndDurationValues() {
        val point = progressPoint(
            id = FoundationId("point-time"),
            exerciseCatalogId = FoundationId("exercise-plank"),
            metric = ProgressMetric.TIME,
            value = 90_000.0,
            weight = null,
            reps = null,
            sourceSetId = FoundationId("set-plank")
        )
        val state = buildProgressChartState(
            points = listOf(point),
            records = emptyList(),
            exerciseCatalogId = FoundationId("exercise-plank"),
            selectedMetric = null,
            weightUnit = WeightUnit.POUNDS
        )

        assertEquals(ProgressEvidenceMetric.LONGEST_DURATION, state.selectedMetric)
        assertEquals("1:30", state.latestValueLabel)
        assertEquals("Time", state.availableMetrics.single().label())
    }

    @Test
    fun longestDistanceMetricCodeNeverFormatsAsTime() {
        val point = progressPoint(
            id = FoundationId("point-distance"),
            exerciseCatalogId = FoundationId("exercise-run"),
            metric = ProgressMetric.TIME,
            metricCode = ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode,
            value = 5_000.0,
            weight = null,
            reps = null,
            sourceSetId = FoundationId("set-run")
        )
        val state = buildProgressChartState(
            points = listOf(point),
            records = emptyList(),
            exerciseCatalogId = FoundationId("exercise-run"),
            selectedMetric = null,
            weightUnit = WeightUnit.POUNDS
        )

        assertEquals(ProgressEvidenceMetric.LONGEST_DISTANCE, state.selectedMetric)
        assertEquals("5000 m", state.latestValueLabel)
        assertEquals("Longest distance", state.availableMetrics.single().label())
    }
}
