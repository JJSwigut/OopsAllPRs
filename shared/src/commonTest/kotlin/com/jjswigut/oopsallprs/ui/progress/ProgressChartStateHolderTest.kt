package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProgressChartStateHolderTest {
    @Test
    fun selectingExerciseDefaultsToEstimatedOneRepMaxWhenAvailable() = runTest {
        val harness = FoundationHarness()
        val exerciseId = FoundationId("exercise-bench")
        val record = progressRecord(
            id = FoundationId("pr-e1rm"),
            exerciseCatalogId = exerciseId,
            recordKind = PersonalRecordKind.ESTIMATED_ONE_REP_MAX,
            value = 120.0,
            sourceSetId = FoundationId("set-e1rm")
        )
        val points = listOf(
            progressPoint(id = FoundationId("point-best"), exerciseCatalogId = exerciseId, metric = ProgressMetric.BEST_SET),
            progressPoint(
                id = FoundationId("point-e1rm"),
                exerciseCatalogId = exerciseId,
                metric = ProgressMetric.ESTIMATED_ONE_REP_MAX,
                value = 120.0,
                sourceSetId = FoundationId("set-e1rm")
            ),
            progressPoint(id = FoundationId("point-volume"), exerciseCatalogId = exerciseId, metric = ProgressMetric.VOLUME)
        )
        harness.store.replaceRecords(listOf(record), points).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()
        holder.selectExercise(exerciseId)

        assertEquals(ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX, holder.state.value.selectedChartMetric)
        assertEquals(listOf(FoundationId("point-e1rm")), holder.state.value.selectedExercise?.chart?.points?.map { it.pointId })
    }

    @Test
    fun switchingMetricKeepsExerciseSelectedAndFiltersChartPoints() = runTest {
        val harness = FoundationHarness()
        val exerciseId = FoundationId("exercise-bench")
        val record = progressRecord(exerciseCatalogId = exerciseId)
        val points = listOf(
            progressPoint(id = FoundationId("point-best"), exerciseCatalogId = exerciseId, metric = ProgressMetric.BEST_SET),
            progressPoint(id = FoundationId("point-volume-1"), exerciseCatalogId = exerciseId, metric = ProgressMetric.VOLUME, value = 500.0),
            progressPoint(id = FoundationId("point-volume-2"), exerciseCatalogId = exerciseId, metric = ProgressMetric.VOLUME, value = 550.0)
        )
        harness.store.replaceRecords(listOf(record), points).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)

        holder.refresh()
        holder.selectExercise(exerciseId)
        holder.selectChartMetric(ProgressEvidenceMetric.VOLUME)

        val selected = holder.state.value.selectedExercise
        assertEquals(exerciseId, holder.state.value.selectedExerciseId)
        assertEquals(ProgressEvidenceMetric.VOLUME, holder.state.value.selectedChartMetric)
        assertTrue(selected?.chart?.points.orEmpty().all { it.metric == ProgressEvidenceMetric.VOLUME })
        assertEquals(2, selected?.chart?.points?.size)
    }
}
