package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkoutHomeTemplateBodyweightTest {
    @Test
    fun launchedTemplatePreservesBodyweightPlannedSetWithoutWeight() = runTest {
        val harness = FoundationHarness()
        val workout = harness.lifecycle.startEmpty(instant(1_000)).successValue()
        val exercise = harness.setLogging.addExercise(workout.id, harness.bodyweightReference, instant(1_100)).successValue()
        harness.setLogging.confirmSet(workout.id, exercise.id, SetKind.BODYWEIGHT, reps = 12, weight = null, position = 0, loggedAt = instant(1_200)).successValue()
        val completed = harness.routines.finishWorkout(workout.id, instant(2_000)).successValue()
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Pull", instant(3_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)

        val launched = holder.launchTemplate(template.id).successValue()
        val set = launched.exercises.single().sets.single()

        assertEquals(SetKind.BODYWEIGHT, set.setKind)
        assertEquals(12, set.reps)
        assertNull(set.weight)
        assertNull(set.loggedAt)
    }
}
