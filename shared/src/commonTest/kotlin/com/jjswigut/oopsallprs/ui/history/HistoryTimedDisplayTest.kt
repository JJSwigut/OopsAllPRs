package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryTimedDisplayTest {
    @Test
    fun timedSetsDisplayDurationWithoutRepsOrWeight() {
        val summary = timedCompletedWorkout().toSummary()
        val row = summary.exercises.single().setRows.single()

        assertTrue(summary.exercises.single().isBodyweight)
        assertEquals("Set 1: 1:15", row.historyDisplayLabel(WeightUnit.POUNDS))
        assertNull(row.reps)
        assertNull(row.weight)
    }
}

internal fun timedCompletedWorkout(): CompletedWorkout {
    val started = instant(1_000)
    val finished = instant(121_000)
    val timedSet = ExerciseSet(
        id = FoundationId("set-plank"),
        exerciseInstanceId = FoundationId("completed-exercise-plank"),
        position = OrderedPosition(0),
        setKind = SetKind.TIMED,
        weight = null,
        reps = null,
        loggedAt = instant(10_000),
        createdAt = instant(10_000),
        updatedAt = instant(10_000),
        durationMs = 75_000L
    )
    return CompletedWorkout(
        id = FoundationId("completed-timed"),
        sourceActiveWorkoutId = FoundationId("workout-timed"),
        startedAt = started,
        finishedAt = finished,
        durationMs = finished.toEpochMilliseconds() - started.toEpochMilliseconds(),
        routineId = null,
        exercises = listOf(
            CompletedExercise(
                id = FoundationId("completed-exercise-plank"),
                completedWorkoutId = FoundationId("completed-timed"),
                exerciseCatalogId = FoundationId("exercise-plank"),
                displayNameSnapshot = "Plank",
                position = OrderedPosition(0),
                loggedSets = listOf(timedSet)
            )
        ),
        createdAt = finished
    )
}
