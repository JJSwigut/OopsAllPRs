package com.jjswigut.oopsallprs.domain.usecase

import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.RoutineExercise
import com.jjswigut.oopsallprs.domain.model.RoutineSetTemplate
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.testing.FoundationHarness
import com.jjswigut.oopsallprs.testing.instant
import com.jjswigut.oopsallprs.testing.successValue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TimedRoutineLaunchTest {
    @Test
    fun blankTimedRoutineTargetUsesPreviousDurationWithoutMutatingRoutine() = runTest {
        val harness = FoundationHarness()
        harness.completeTimedSource(durationMs = 80_000L)
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Core holds",
            exercises = listOf(
                RoutineExercise(
                    id = FoundationId("routine-exercise-plank"),
                    routineId = FoundationId("pending"),
                    exerciseCatalogId = harness.timedReference.exerciseCatalogId,
                    displayNameSnapshot = harness.timedReference.displayNameSnapshot,
                    position = OrderedPosition(0),
                    plannedSets = listOf(
                        RoutineSetTemplate(
                            id = FoundationId("routine-set-plank"),
                            routineExerciseId = FoundationId("routine-exercise-plank"),
                            position = OrderedPosition(0),
                            targetWeight = null,
                            targetReps = null,
                            targetDurationMs = null,
                            setKind = SetKind.TIMED
                        )
                    )
                )
            ),
            now = instant(3_000)
        ).successValue()

        val active = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        assertEquals(SetKind.TIMED, active.exercises.single().sets.single().setKind)
        assertEquals(80_000L, active.exercises.single().sets.single().durationMs)
        assertNull(harness.routines.listRoutines().single { it.id == routine.id }.exercises.single().plannedSets.single().targetDurationMs)
    }

    @Test
    fun explicitTimedRoutineTargetWinsOverPreviousDuration() = runTest {
        val harness = FoundationHarness()
        harness.completeTimedSource(durationMs = 80_000L)
        val routine = harness.routines.saveRoutine(
            routineId = null,
            name = "Core holds",
            exercises = listOf(
                RoutineExercise(
                    id = FoundationId("routine-exercise-plank"),
                    routineId = FoundationId("pending"),
                    exerciseCatalogId = harness.timedReference.exerciseCatalogId,
                    displayNameSnapshot = harness.timedReference.displayNameSnapshot,
                    position = OrderedPosition(0),
                    plannedSets = listOf(
                        RoutineSetTemplate(
                            id = FoundationId("routine-set-plank"),
                            routineExerciseId = FoundationId("routine-exercise-plank"),
                            position = OrderedPosition(0),
                            targetWeight = null,
                            targetReps = null,
                            targetDurationMs = 30_000L,
                            setKind = SetKind.TIMED
                        )
                    )
                )
            ),
            now = instant(3_000)
        ).successValue()

        val active = harness.lifecycle.startFromRoutine(routine.id, instant(4_000)).successValue()

        assertEquals(30_000L, active.exercises.single().sets.single().durationMs)
    }
}

private suspend fun FoundationHarness.completeTimedSource(durationMs: Long) {
    val workout = lifecycle.startEmpty(instant(1_000)).successValue()
    val exercise = setLogging.addExercise(workout.id, timedReference, instant(1_100)).successValue()
    setLogging.confirmSet(
        activeWorkoutId = workout.id,
        exerciseInstanceId = exercise.id,
        setKind = SetKind.TIMED,
        reps = 0,
        weight = null,
        position = 0,
        loggedAt = instant(1_200),
        durationMs = durationMs
    ).successValue()
    routines.finishWorkout(workout.id, instant(2_000)).successValue().workout
}
