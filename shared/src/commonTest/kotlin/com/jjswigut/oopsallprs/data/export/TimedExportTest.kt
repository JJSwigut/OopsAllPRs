package com.jjswigut.oopsallprs.data.export

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TimedExportTest {
    @Test
    fun workoutAndPrExportsIncludeDurationColumnsAndLabels() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.timedReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(
            activeWorkoutId = workout.id,
            exerciseInstanceId = exercise.id,
            setKind = SetKind.TIMED,
            reps = 0,
            weight = null,
            position = 0,
            loggedAt = instant(1_200),
            durationMs = 75_000L
        ).successValue()
        harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()

        val workoutExport = ExportService(harness.store).export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        val prExport = ExportService(harness.store).export(ExportType.PERSONAL_RECORDS, WeightUnit.POUNDS).successValue()

        assertTrue(workoutExport.content.contains("duration_ms,duration_label"))
        assertTrue(workoutExport.content.contains("75000,1:15,TIMED"))
        assertTrue(prExport.content.contains("duration_ms,value,value_label"))
        assertTrue(prExport.content.contains("TIME,,,75000,75000.0,1:15"))
    }
}
