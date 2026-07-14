package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProgressChartModelTest {
    @Test
    fun emptyChartStateHasNoSelectedMetric() {
        val state = buildProgressChartState(
            points = emptyList(),
            records = emptyList(),
            exerciseCatalogId = FoundationId("exercise-bench"),
            selectedMetric = null,
            weightUnit = WeightUnit.POUNDS
        )

        assertEquals(null, state.selectedMetric)
        assertTrue(state.availableMetrics.isEmpty())
        assertTrue(state.points.isEmpty())
        assertEquals("Chart points will appear after completed workouts.", state.emptyMessage)
    }

    @Test
    fun singlePointChartStateIsStableAndPreservesSourceRecord() {
        val record = progressRecord()
        val point = progressPoint(
            id = FoundationId("point-best"),
            metric = ProgressMetric.BEST_SET,
            value = 100.0,
            sourceWorkoutId = record.sourceWorkoutId,
            sourceSetId = record.sourceSetId
        )

        val state = buildProgressChartState(
            points = listOf(point),
            records = listOf(record),
            exerciseCatalogId = record.exerciseCatalogId,
            selectedMetric = null,
            weightUnit = WeightUnit.POUNDS
        )

        assertEquals(ProgressEvidenceMetric.WEIGHT_FOR_REPS, state.selectedMetric)
        assertEquals(1, state.points.size)
        assertEquals(record.id, state.points.single().sourceRecordId)
        assertTrue(state.latestValueLabel.orEmpty().contains("lb"))
        assertTrue(state.latestValueLabel.orEmpty().contains("x 5"))
    }

    @Test
    fun multiPointChartStateSortsChronologicallyThenById() {
        val exerciseId = FoundationId("exercise-bench")
        val later = progressPoint(
            id = FoundationId("point-later"),
            exerciseCatalogId = exerciseId,
            recordedAtMs = 30_000,
            value = 110.0
        )
        val tiedB = progressPoint(
            id = FoundationId("point-b"),
            exerciseCatalogId = exerciseId,
            recordedAtMs = 10_000,
            value = 95.0
        )
        val tiedA = progressPoint(
            id = FoundationId("point-a"),
            exerciseCatalogId = exerciseId,
            recordedAtMs = 10_000,
            value = 90.0
        )

        val state = buildProgressChartState(
            points = listOf(later, tiedB, tiedA),
            records = emptyList(),
            exerciseCatalogId = exerciseId,
            selectedMetric = ProgressEvidenceMetric.WEIGHT_FOR_REPS,
            weightUnit = WeightUnit.KILOGRAMS
        )

        assertEquals(
            listOf(FoundationId("point-a"), FoundationId("point-b"), FoundationId("point-later")),
            state.points.map { it.pointId }
        )
    }

    @Test
    fun selectedMetricFiltersPointsAndBodyweightLabelsStayRepsOnly() {
        val exerciseId = FoundationId("exercise-pullup")
        val weighted = progressPoint(
            id = FoundationId("point-volume"),
            exerciseCatalogId = exerciseId,
            metric = ProgressMetric.VOLUME,
            value = 500.0
        )
        val bodyweight = progressPoint(
            id = FoundationId("point-reps"),
            exerciseCatalogId = exerciseId,
            metric = ProgressMetric.BODYWEIGHT_REPS,
            value = 12.0,
            weight = null,
            reps = 12,
            sourceSetId = FoundationId("set-bodyweight")
        )
        val record = progressRecord(
            id = FoundationId("pr-reps"),
            exerciseCatalogId = exerciseId,
            recordKind = PersonalRecordKind.BODYWEIGHT_REPS,
            reps = 12,
            weight = null,
            value = 12.0,
            sourceSetId = FoundationId("set-bodyweight")
        )

        val state = buildProgressChartState(
            points = listOf(weighted, bodyweight),
            records = listOf(record),
            exerciseCatalogId = exerciseId,
            selectedMetric = ProgressEvidenceMetric.REPS,
            weightUnit = WeightUnit.POUNDS
        )

        assertEquals(ProgressEvidenceMetric.REPS, state.selectedMetric)
        assertEquals(listOf(FoundationId("point-reps")), state.points.map { it.pointId })
        assertEquals("12 reps", state.points.single().valueLabel)
        assertEquals(FoundationId("pr-reps"), assertNotNull(state.points.single().sourceRecordId))
    }
}
