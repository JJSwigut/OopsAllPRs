package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PersonalRecordDerivationRecordKindsTest {
    @Test
    fun derivesWeightedBodyweightE1rmAndVolumeRecordsFromCompletedLedger() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val weighted = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val bodyweight = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        val weightedSet = harness.setLogging
            .confirmSet(workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_300))
            .successValue()
        val bodyweightSet = harness.setLogging
            .confirmSet(workout.id, bodyweight.id, SetKind.BODYWEIGHT, 12, null, 0, instant(1_400))
            .successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))

        val records = harness.store.personalRecords()
        val points = harness.store.progressPoints()
        val kinds = records.map { it.recordKind }.toSet()
        assertTrue(kinds.contains(PersonalRecordKind.WEIGHT_FOR_REPS))
        assertTrue(kinds.contains(PersonalRecordKind.BODYWEIGHT_REPS))
        assertTrue(kinds.contains(PersonalRecordKind.ESTIMATED_ONE_REP_MAX))
        assertTrue(kinds.contains(PersonalRecordKind.VOLUME))
        assertTrue(points.map { it.metric }.containsAll(listOf(ProgressMetric.BEST_SET, ProgressMetric.BODYWEIGHT_REPS, ProgressMetric.ESTIMATED_ONE_REP_MAX, ProgressMetric.VOLUME)))

        val bestWeight = assertNotNull(records.firstOrNull { it.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS })
        val bodyweightReps = assertNotNull(records.firstOrNull { it.recordKind == PersonalRecordKind.BODYWEIGHT_REPS })
        val e1rm = assertNotNull(records.firstOrNull { it.recordKind == PersonalRecordKind.ESTIMATED_ONE_REP_MAX })
        val volume = assertNotNull(records.firstOrNull { it.recordKind == PersonalRecordKind.VOLUME })

        assertEquals(weightedSet.id, bestWeight.sourceSetId)
        assertEquals(bodyweightSet.id, bodyweightReps.sourceSetId)
        assertEquals(completed.id, e1rm.sourceWorkoutId)
        assertTrue(abs(e1rm.value - 116.6666) < 0.01)
        assertEquals(500.0, volume.value)
        assertEquals(weightedSet.id, volume.sourceSetId)
    }
}
