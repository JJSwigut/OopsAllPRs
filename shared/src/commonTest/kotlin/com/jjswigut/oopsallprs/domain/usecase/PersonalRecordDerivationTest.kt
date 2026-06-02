package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PersonalRecordDerivationTest {
    @Test
    fun derivesWeightedAndBodyweightRecordsWithSourceReferences() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val weighted = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val bodyweight = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_200)).successValue()
        harness.setLogging.confirmSet(workout.id, weighted.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(1_300))
        harness.setLogging.confirmSet(workout.id, bodyweight.id, SetKind.BODYWEIGHT, 12, null, 0, instant(1_400))
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))

        val records = harness.store.personalRecords()
        assertEquals(4, records.size)
        assertTrue(records.all { it.sourceWorkoutId == completed.id })
    }
}
