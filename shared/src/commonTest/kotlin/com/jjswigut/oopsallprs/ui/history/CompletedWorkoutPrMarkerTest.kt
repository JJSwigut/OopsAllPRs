package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.ui.common.shortDateLabel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompletedWorkoutPrMarkerTest {
    @Test
    fun mapsPersonalRecordsToCompletedSetMarkers() {
        val workout = mixedCompletedWorkout()
        val summary = workout.toSummary(
            listOf(
                PersonalRecord(
                    id = FoundationId("pr-weighted"),
                    exerciseCatalogId = FoundationId("exercise-bench"),
                    recordKind = PersonalRecordKind.WEIGHT_FOR_REPS,
                    reps = 5,
                    weight = WeightKg(100.0),
                    value = 100.0,
                    sourceWorkoutId = workout.id,
                    sourceSetId = FoundationId("set-weighted"),
                    achievedAt = instant(10_000),
                    createdAt = instant(10_000)
                )
            )
        )

        assertEquals(1, summary.prCount)
        val marker = summary.exercises.first().setRows.single().prMarkers.single()
        assertEquals(PersonalRecordKind.WEIGHT_FOR_REPS, marker.kind)
        assertTrue(marker.label.contains("PR"))
        assertEquals("5-rep PR 220.46 lb x 5", marker.historyLabel(WeightUnit.POUNDS))
        val expectedDate = marker.achievedAt.shortDateLabel()
        assertEquals("5-rep PR 220.46 lb x 5 • $expectedDate", marker.historyDetailLabel(WeightUnit.POUNDS))
        assertEquals(
            "Set 1: 5 reps • 220.46 lb • 5-rep PR 220.46 lb x 5 • $expectedDate",
            summary.exercises.first().setRows.single().historyDisplayLabel(WeightUnit.POUNDS)
        )
    }

    @Test
    fun longestDistanceMetricCodeNeverUsesDurationLabel() {
        val workout = mixedCompletedWorkout()
        val summary = workout.toSummary(
            listOf(
                PersonalRecord(
                    id = FoundationId("pr-distance"),
                    exerciseCatalogId = FoundationId("exercise-bench"),
                    recordKind = PersonalRecordKind.TIME,
                    reps = null,
                    weight = null,
                    value = 1_500.0,
                    sourceWorkoutId = workout.id,
                    sourceSetId = FoundationId("set-weighted"),
                    achievedAt = instant(10_000),
                    createdAt = instant(10_000),
                    metricCode = ProgressEvidenceMetric.LONGEST_DISTANCE.wireCode
                )
            )
        )

        val marker = summary.exercises.first().setRows.single().prMarkers.single()
        assertEquals("PR 1500 m", marker.historyLabel(WeightUnit.KILOGRAMS))
        assertTrue("1:30" !in marker.historyLabel(WeightUnit.KILOGRAMS))
    }
}
