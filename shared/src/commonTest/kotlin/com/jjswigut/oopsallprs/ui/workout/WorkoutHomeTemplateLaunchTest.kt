package com.jjswigut.oopsallprs.ui.workout

import com.jjswigut.oopsallprs.domain.model.FoundationResult
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.FullAccessState
import com.jjswigut.oopsallprs.domain.model.FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT
import com.jjswigut.oopsallprs.domain.usecase.FullAccessUseCases
import com.jjswigut.oopsallprs.testing.setFullAccessForTest
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
    fun templateLaunchUsesTheSameTrialGateAsAnEmptyWorkout() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue().workout
        val template = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        harness.store.setFullAccessForTest(
            FullAccessState(completedFreeWorkouts = FULL_ACCESS_FREE_COMPLETED_WORKOUT_LIMIT)
        ).successValue()
        val holder = WorkoutHomeStateHolder(
            lifecycle = harness.lifecycle,
            routines = harness.routines,
            fullAccess = FullAccessUseCases(harness.store)
        )

        holder.hydrate()
        val result = holder.launchTemplate(template.id)

        assertTrue(result is FoundationResult.Failure)
        assertTrue(holder.state.value.isFullAccessPaywallVisible)
        assertEquals(null, holder.state.value.activeSession)
    }

    @Test
    fun hydrateShowsTemplatesAndLaunchCreatesActiveWorkout() = runTest {
        val harness = FoundationHarness()
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue().workout
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
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue().workout
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
        val completed = harness.routines.finishWorkout(harness.workoutWithLoggedWeightedSet(), instant(2_000)).successValue().workout
        val olderTemplate = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Push", instant(3_000)).successValue()
        val newerTemplate = harness.routines.saveCompletedWorkoutAsRoutine(completed.id, "Legs", instant(6_000)).successValue()
        val launched = harness.lifecycle.startFromRoutine(olderTemplate.id, instant(7_000)).successValue()
        val exercise = launched.exercises.first()
        harness.setLogging.confirmSet(
            launched.id, exercise.id, SetKind.WEIGHTED, 5, WeightKg(100.0), 0, instant(7_500)
        ).successValue()
        harness.routines.finishWorkout(launched.id, instant(8_000)).successValue().workout
        val holder = WorkoutHomeStateHolder(harness.lifecycle, harness.routines)

        holder.hydrate()

        assertEquals(
            listOf(olderTemplate.id, newerTemplate.id),
            holder.state.value.templates.map { it.templateId }
        )
    }
}
