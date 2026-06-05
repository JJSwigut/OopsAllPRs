package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import com.jjswigut.oopsallprs.testing.workoutWithLoggedWeightedSet
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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

    @Test
    fun launchedGroupedRoutineCarriesDisplayGroupContextOnly() = runTest {
        val harness = FoundationHarness()
        val groupId = FoundationId("routine-group-1")
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Upper",
            exercises = listOf(
                groupedRoutineExercise("routine-exercise-1", "Bench Press", groupId, 0),
                groupedRoutineExercise("routine-exercise-2", "Seated Row", groupId, 1)
            ),
            now = instant(1_000)
        ).successValue()

        val active = harness.lifecycle.startFromRoutine(routine.id, instant(2_000)).successValue()

        assertEquals(listOf("Superset", "Superset"), active.exercises.map { it.groupContext?.label })
        assertEquals(listOf(groupId, groupId), active.exercises.map { it.groupContext?.groupId })
        assertTrue(active.exercises.flatMap { it.sets }.all { it.loggedAt == null })
    }

    private fun groupedRoutineExercise(id: String, name: String, groupId: FoundationId, position: Int): RoutineExercise =
        RoutineExercise(
            id = FoundationId(id),
            routineId = FoundationId("pending"),
            exerciseCatalogId = FoundationId("exercise-$position"),
            displayNameSnapshot = name,
            position = OrderedPosition(position),
            groupId = groupId,
            groupPosition = OrderedPosition(0),
            plannedSets = listOf(
                RoutineSetTemplate(
                    id = FoundationId("routine-set-$position"),
                    routineExerciseId = FoundationId(id),
                    position = OrderedPosition(0),
                    targetWeight = WeightKg(100.0),
                    targetReps = 5,
                    setKind = SetKind.WEIGHTED
                )
            )
        )
}
