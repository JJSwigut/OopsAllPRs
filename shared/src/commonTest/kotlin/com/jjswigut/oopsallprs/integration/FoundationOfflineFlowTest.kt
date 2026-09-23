package com.jjswigut.oopsallprs.integration

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.PersonalRecordDerivationUseCase
import com.jjswigut.oopsallprs.data.export.ExportService
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class FoundationOfflineFlowTest {
    @Test
    fun startLogFinishDeriveAndExportWithoutNetwork() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val weighted = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val bodyweight = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        harness.setLogging.confirmSet(workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_300))
        harness.setLogging.confirmSet(workout.id, bodyweight.id, SetKind.BODYWEIGHT, 12, null, 0, instant(1_400))
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))
        val export = ExportService(harness.store).export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        assertTrue(export.content.contains("BODYWEIGHT"))
        assertTrue(harness.store.personalRecords().isNotEmpty())
    }
}
