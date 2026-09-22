package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.PersonalRecordKind
import com.jjswigut.oopsallprs.domain.model.ProgressMetric
import com.jjswigut.oopsallprs.domain.model.ProgressEvidenceMetric
import com.jjswigut.oopsallprs.domain.model.ProgressDerivationVersions
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class PersonalRecordDerivationTest {
    @Test
    fun rebuildingKeepsAchievementIdentityStableFromItsSourceSet() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(
            workout.id, exercise.id, SetKind.WEIGHTED, 8, WeightKg(25.0), 0, instant(1_200)
        ).successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
        val derivation = PersonalRecordDerivationUseCase(harness.store)

        val first = derivation.snapshotFrom(listOf(completed))
        val second = derivation.snapshotFrom(listOf(completed))

        assertEquals(first.records.map { it.id }, second.records.map { it.id })
        assertEquals(first.records.map { it.createdAt }, second.records.map { it.createdAt })
        assertTrue(first.records.all { it.derivationVersion == ProgressDerivationVersions.CURRENT })
        assertNotEquals(ProgressDerivationVersions.CONFIGURATION_CAPTURE, ProgressDerivationVersions.CURRENT)
    }

    @Test
    fun estimatedOneRepMaxEvidenceOnlyUsesWeightedSetsFromOneToTwelveReps() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        val qualified = harness.setLogging.confirmSet(
            workout.id, exercise.id, SetKind.WEIGHTED, 12, WeightKg(25.0), 0, instant(1_200)
        ).successValue()
        val excluded = harness.setLogging.confirmSet(
            workout.id, exercise.id, SetKind.WEIGHTED, 13, WeightKg(25.0), 1, instant(1_300)
        ).successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout

        val snapshot = PersonalRecordDerivationUseCase(harness.store).snapshotFrom(listOf(completed))
        val estimates = snapshot.points.filter {
            it.metricCode == ProgressEvidenceMetric.ESTIMATED_ONE_REP_MAX.wireCode
        }

        assertEquals(listOf(qualified.id), estimates.map { it.sourceSetId })
        assertTrue(snapshot.points.any {
            it.sourceSetId == excluded.id && it.metricCode == ProgressEvidenceMetric.WEIGHT_FOR_REPS.wireCode
        })
    }

    @Test
    fun weightedAchievementsKeepChronologicalBestForEachExactRepSeries() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.weightedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 16, WeightKg(25.0), 0, instant(1_200))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 14, WeightKg(25.0), 1, instant(1_300))
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.WEIGHTED, 10, WeightKg(30.0), 2, instant(1_400))
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout

        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))

        val weightedRecords = harness.store.personalRecords()
            .filter { it.recordKind == PersonalRecordKind.WEIGHT_FOR_REPS }
        assertEquals(
            setOf(25.0 to 16, 25.0 to 14, 30.0 to 10),
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
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue().workout

        PersonalRecordDerivationUseCase(harness.store).rebuildFrom(listOf(completed))

        val records = harness.store.personalRecords()
        assertEquals(4, records.size)
        assertTrue(records.all { it.sourceWorkoutId == completed.id })
    }
}
