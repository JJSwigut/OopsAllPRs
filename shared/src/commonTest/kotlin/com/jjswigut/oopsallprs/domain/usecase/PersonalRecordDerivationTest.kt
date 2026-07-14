package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
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
    fun weightedRecordsKeepOnlyNondominatedWeightAndRepAchievements() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 16, WeightKg(25.0), 0, instant(1_200))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 14, WeightKg(25.0), 1, instant(1_300))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 10, WeightKg(30.0), 2, instant(1_400))
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))

        val weightedRecords = harness.store.personalRecords()
            .filter { it.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS }
        assertEquals(
            setOf(25.0 to 16, 30.0 to 10),
            weightedRecords.map { it.weight!!.value to it.reps!! }.toSet()
        )
        assertEquals(3, harness.store.progressPoints().count { it.metric == ProgressMetric.BEST_SET })
    }

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
