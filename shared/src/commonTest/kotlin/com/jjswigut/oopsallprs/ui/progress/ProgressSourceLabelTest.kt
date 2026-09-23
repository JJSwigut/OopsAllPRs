package com.jjswigut.oopsallprs.ui.progress

import com.jjswigut.oopsallprs.domain.model.EvidenceReading
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.domain.usecase.EvidenceLadderUseCase
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProgressSourceLabelTest {
    @Test
    fun strengthEvidenceShowsEstimateAndOriginalSetInSelectedUnit() = runTest {
        val snapshot = history().snapshot()
        val source = snapshot.overallReadings.single { it.reading == EvidenceReading.CAPABILITY }.evidence.first()

        assertEquals("25.3 lb estimated 1RM \u2022 from 20 lb x 8", source.displayValueLabel(WeightUnit.POUNDS))
        assertEquals("11.5 kg estimated 1RM \u2022 from 9.07 kg x 8", source.displayValueLabel(WeightUnit.KILOGRAMS))
        assertEquals("25.3 lb estimated 1RM \u2022 from 20 lb x 8", source.displayValueLabel(WeightUnit.POUNDS))
    }

    @Test
    fun workEvidenceShowsOriginalSetAndLoadRepTotalInSelectedUnit() = runTest {
        val snapshot = history().snapshot()
        val source = snapshot.overallReadings.single { it.reading == EvidenceReading.WORK_CAPACITY }.evidence.first()

        assertEquals("20 lb x 8 \u2022 160 lb load x reps", source.displayValueLabel(WeightUnit.POUNDS))
        assertEquals("9.07 kg x 8 \u2022 72.6 kg load x reps", source.displayValueLabel(WeightUnit.KILOGRAMS))
    }

    @Test
    fun fractionalLoadIsConvertedBeforeDisplayRounding() = runTest {
        val snapshot = history(WeightKg(1.25)).snapshot()
        val source = snapshot.overallReadings.single { it.reading == EvidenceReading.WORK_CAPACITY }.evidence.first()

        assertEquals("2.76 lb x 8 \u2022 22 lb load x reps", source.displayValueLabel(WeightUnit.POUNDS))
        assertEquals("1.25 kg x 8 \u2022 10 kg load x reps", source.displayValueLabel(WeightUnit.KILOGRAMS))
    }

    @Test
    fun consistencyDoesNotAcquireWeightUnits() = runTest {
        val source = history().snapshot().overallReadings.single { it.reading == EvidenceReading.CONSISTENCY }.evidence.first()

        assertEquals("Completed session", source.displayValueLabel(WeightUnit.POUNDS))
        assertEquals("Completed session", source.displayValueLabel(WeightUnit.KILOGRAMS))
    }

    @Test
    fun unitPreferenceRefreshChangesLabelsWithoutChangingEvidenceOrComparisons() = runTest {
        val harness = history()
        harness.store.setWeightUnit(WeightUnit.POUNDS).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()
        holder.selectReading(EvidenceReading.WORK_CAPACITY)
        val before = requireNotNull(holder.state.value.selectedReading)
        assertEquals("20 lb x 8 \u2022 160 lb load x reps", before.evidence.first().displayValueLabel(holder.state.value.weightUnit))

        harness.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
        holder.refresh()

        val after = requireNotNull(holder.state.value.selectedReading)
        assertEquals(before, after)
        assertEquals("9.07 kg x 8 \u2022 72.6 kg load x reps", after.evidence.first().displayValueLabel(holder.state.value.weightUnit))
        val exercise = holder.state.value.exerciseGroups.single()
        holder.selectExercise(exercise.exerciseCatalogId)
        val source = requireNotNull(holder.state.value.selectedExercise?.capability).evidence.first()
        assertEquals("11.5 kg estimated 1RM \u2022 from 9.07 kg x 8", source.displayValueLabel(holder.state.value.weightUnit))
        harness.store.setWeightUnit(WeightUnit.POUNDS).successValue()
        holder.refresh()
        assertEquals(exercise.exerciseCatalogId, holder.state.value.selectedExercise?.exerciseCatalogId)
        val refreshed = requireNotNull(holder.state.value.selectedExercise?.capability).evidence.first()
        assertEquals(source, refreshed)
        assertEquals("25.3 lb estimated 1RM \u2022 from 20 lb x 8", refreshed.displayValueLabel(holder.state.value.weightUnit))
    }

    @Test
    fun achievementSourceRetainsFractionalLoad() = runTest {
        val harness = history(WeightKg(1.25))
        harness.store.setWeightUnit(WeightUnit.KILOGRAMS).successValue()
        val holder = ProgressStateHolder(harness.store, harness.store, harness.store)
        holder.refresh()
        val record = holder.state.value.personalRecords.first {
            it.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode
        }

        holder.openEvidence(record.id)

        assertEquals("Set 1: 8 reps \u2022 1.25 kg", holder.state.value.selectedEvidence?.sourceSetLabel)
    }

    private suspend fun history(weight: WeightKg = WeightKg.fromDisplay(20.0, WeightUnit.POUNDS)): FoundationHarness {
        val harness = FoundationHarness()
        listOf(0, 7, 14, 21, 28, 35).forEach { day ->
            val time = day * 86_400_000L + 1_000
            val workout = harness.lifecycle.startEmpty(instant(time)).successValue()
            val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(time + 100)).successValue()
            harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 8, weight, 0, instant(time + 200)).successValue()
            harness.routines.finishWorkout(workout.id, instant(time + 1_000)).successValue().workout
        }
        return harness
    }

    private suspend fun FoundationHarness.snapshot() = EvidenceLadderUseCase().project(
        store.completedWorkouts(), store.personalRecords(), store.progressPoints()
    )
}
