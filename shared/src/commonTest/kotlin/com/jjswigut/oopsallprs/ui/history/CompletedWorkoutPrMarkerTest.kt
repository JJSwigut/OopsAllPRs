package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecord
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.instant
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
        assertEquals("PR 220.5 lb x 5", marker.historyLabel(WeightUnit.POUNDS))
        assertEquals("PR 220.5 lb x 5 • 1970-01-01", marker.historyDetailLabel(WeightUnit.POUNDS))
        assertEquals(
            "Set 1: 5 reps • 220.5 lb • PR 220.5 lb x 5 • 1970-01-01",
            summary.exercises.first().setRows.single().historyDisplayLabel(WeightUnit.POUNDS)
        )
    }
}
