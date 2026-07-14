package com.jjswigut.oopsallprs.data.export

import com.jjswigut.oopsallprs.domain.model.ExportType
import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ExportSnapshotTest {
    @Test
    fun workoutExportIncludesLoggedSetsAndCsvHeader() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        harness.routines.finishWorkout(workoutId, instant(2_000))
        val export = ExportService(harness.store).export(ExportType.WORKOUTS, WeightUnit.POUNDS).successValue()
        assertEquals(
            "workout_id,exercise,reps,weight,duration_ms,duration_label,kind,logged_at," +
                "config_id,distance_m,rpe,rir,failure_outcome,load_role",
            export.content.lineSequence().first()
        )
        assertTrue(export.content.contains("Bench Press"))
    }

    @Test
    fun routineExportIncludesTemplateCreatedFromCompletedWorkout() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()

        val export = ExportService(harness.store).export(ExportType.ROUTINES, WeightUnit.POUNDS).successValue()

        assertTrue(export.content.contains("routine_id,name,exercise_count"))
        assertTrue(export.content.contains("Push"))
    }
}
