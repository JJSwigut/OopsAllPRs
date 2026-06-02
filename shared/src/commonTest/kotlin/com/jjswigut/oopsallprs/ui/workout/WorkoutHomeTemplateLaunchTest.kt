package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutHomeTemplateLaunchTest {
    @Test
    fun hydrateShowsTemplatesAndLaunchCreatesActiveWorkout() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue()
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)

        holder.hydrate()
        assertEquals(listOf(template.id), holder.state.value.templates.map { it.templateId })

        val launched = holder.launchTemplate(template.id).successValue()
        assertEquals(launched.id, holder.state.value.activeSession?.activeWorkoutId)
        assertTrue(launched.exercises.flatMap { it.sets }.all { it.loggedAt == null })
    }

    @Test
    fun launchTemplateReportsConflictWhenSessionExists() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue()
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)
        holder.hydrate()
        holder.launchTemplate(template.id).successValue()

        val result = holder.launchTemplate(template.id)

        assertTrue(result is FoundationResult.Failure)
        assertEquals(null, holder.state.value.errorMessage)
    }

    @Test
    fun hydrateSortsUsedTemplatesBeforeNewerUnusedTemplates() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue()
        val olderTemplate = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        val newerTemplate = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Legs", instant(6_000)).successValue()
        val launched = harness.lifecycle.startFromRoutine(olderTemplate.id, instant(7_000)).successValue()
        harness.routines.finishWorkout(launched.id, instant(8_000)).successValue()
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)

        holder.hydrate()

        assertEquals(
            listOf(olderTemplate.id, newerTemplate.id),
            holder.state.value.templates.map { it.templateId }
        )
    }
}
