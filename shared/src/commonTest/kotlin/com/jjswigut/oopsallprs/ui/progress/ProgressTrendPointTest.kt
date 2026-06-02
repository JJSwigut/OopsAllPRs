package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressTrendPointTest {
    @Test
    fun selectedExerciseTrendRowsAreChronological() = runTest {
        val harness = FoundationHarness()
        val record = progressRecord()
        val newer = progressPoint(
            id = FoundationId("point-newer"),
            metric = ProgressMetric.VOLUME,
            value = 500.0,
            recordedAtMs = 20_000
        )
        val older = progressPoint(
            id = FoundationId("point-older"),
            metric = ProgressMetric.BEST_SET,
            value = 90.0,
            recordedAtMs = 10_000
        )
        harness.store.replaceRecords(listOf(record), listOf(newer, older)).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()

        holder.selectExercise(record.exerciseCatalogId)

        assertEquals(
            listOf(FoundationId("point-older"), FoundationId("point-newer")),
            holder.state.value.selectedExercise?.trendRows?.map { it.pointId }
        )
    }
}
