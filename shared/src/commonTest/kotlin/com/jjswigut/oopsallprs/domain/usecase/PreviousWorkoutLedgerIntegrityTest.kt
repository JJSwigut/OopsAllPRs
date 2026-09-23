package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.ui.workout.ActiveWorkoutStateHolder
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PreviousWorkoutLedgerIntegrityTest {
    @Test
    fun looking_up_previous_values_for_active_drafts_does_not_mutate_ledger_or_exports() = runTest {
        val harness = FoundationHarness()
        harness.completeLedgerSource()
        val beforeCompleted = harness.store.completedWorkouts()
        val beforeRecords = harness.store.personalRecords()
        val beforePoints = harness.store.progressPoints()
        val beforeExport = harness.store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue().content
        val active = harness.lifecycle.startEmpty(instant(3_000)).successValue()
        val holder = ActiveWorkoutStateHolder(
            setLogging = harness.setLogging,
            lifecycle = harness.lifecycle,
            activeUx = harness.store,
            previousDefaults = harness.previousDefaults
        )
        holder.hydrate(active.id, now = instant(3_100))

        holder.addExercise(active.id, harness.weightedReference).successValue()

        assertEquals(beforeCompleted, harness.store.completedWorkouts())
        assertEquals(beforeRecords, harness.store.personalRecords())
        assertEquals(beforePoints, harness.store.progressPoints())
        assertEquals(beforeExport, harness.store.export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue().content)
    }
}

private suspend fun FoundationHarness.completeLedgerSource() {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, weightedReference, instant(1_100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.WEIGHTED,
        reps = 5,
        weight = WeightKg(100.0),
        position = 0,
        loggedAt = instant(1_200)
    ).successValue()
    routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
}
