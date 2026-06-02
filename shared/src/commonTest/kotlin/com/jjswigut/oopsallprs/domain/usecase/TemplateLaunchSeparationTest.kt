package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

class TemplateLaunchSeparationTest {
    @Test
    fun launchedTemplateCreatesPlannedSetsWithoutLoggedTimestamps() = runTest {
        val harness = FoundationHarness()
        val workoutId = harness.workoutWithLoggedWeightedSet()
        val completed = harness.routines.finishWorkout(workoutId, instant(2_000)).successValue()
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()

        val active = harness.lifecycle.startFromRoutine(template.id, instant(4_000)).successValue()

        assertTrue(active.exercises.flatMap { it.sets }.isNotEmpty())
        assertTrue(active.exercises.flatMap { it.sets }.all { it.loggedAt == null })
        assertTrue(active.exercises.flatMap { it.sets }.none { set ->
            completed.exercises.flatMap { it.loggedSets }.any { it.id == set.id }
        })
    }
}
