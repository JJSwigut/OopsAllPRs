package com.jjswigut.oopsallprs.ui.history

import com.jjswigut.oopsallprs.domain.model.CompletedExercise
import com.jjswigut.oopsallprs.domain.model.CompletedWorkout
import com.jjswigut.oopsallprs.domain.model.ExerciseSet
import com.jjswigut.oopsallprs.domain.model.FoundationId
import com.jjswigut.oopsallprs.domain.model.OrderedPosition
import com.jjswigut.oopsallprs.domain.model.SetKind
import com.jjswigut.oopsallprs.domain.model.WeightKg
import com.jjswigut.oopsallprs.domain.model.WeightUnit
import com.jjswigut.oopsallprs.testing.instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CompletedWorkoutSummaryModelTest {
    @Test
    fun mapsDurationMixedRowsAndCounts() {
        val summary = mixedCompletedWorkout().toSummary()

        assertEquals("1h 1m", summary.durationLabel)
        assertEquals(2, summary.exerciseCount)
        assertEquals(2, summary.setCount)
        assertEquals("Bench Press", summary.exercises.first().displayName)
        assertEquals(WeightKg(100.0), summary.exercises.first().setRows.single().weight)
        assertTrue(summary.exercises.last().isBodyweight)
        assertNull(summary.exercises.last().setRows.single().weight)
    }

    @Test
    fun formatsHistorySetRowsInSelectedUnit() {
        val weightedRow = mixedCompletedWorkout().toSummary()
            .exercises
            .first()
            .setRows
            .single()

        assertEquals("Set 1: 5 reps • 100 kg", weightedRow.historyDisplayLabel(WeightUnit.KILOGRAMS))
        assertEquals("Set 1: 5 reps • 220.5 lb", weightedRow.historyDisplayLabel(WeightUnit.POUNDS))
    }
}

internal fun mixedCompletedWorkout(): CompletedWorkout {
    val started = instant(1_000)
    val finished = instant(3_661_000)
    val weightedSet = ExerciseSet(
        id = FoundationId("set-weighted"),
        exerciseInstanceId = FoundationId("completed-exercise-weighted"),
        position = OrderedPosition(0),
        setKind = SetKind.WEIGHTED,
        weight = WeightKg(100.0),
        reps = 5,
        loggedAt = instant(10_000),
        createdAt = instant(10_000),
        updatedAt = instant(10_000)
    )
    val bodyweightSet = ExerciseSet(
        id = FoundationId("set-bodyweight"),
        exerciseInstanceId = FoundationId("completed-exercise-bodyweight"),
        position = OrderedPosition(0),
        setKind = SetKind.BODYWEIGHT,
        weight = null,
        reps = 12,
        loggedAt = instant(20_000),
        createdAt = instant(20_000),
        updatedAt = instant(20_000)
    )
    return CompletedWorkout(
        id = FoundationId("completed-mixed"),
        sourceActiveWorkoutId = FoundationId("workout-mixed"),
        startedAt = started,
        finishedAt = finished,
        durationMs = finished.toEpochMilliseconds() - started.toEpochMilliseconds(),
        routineId = null,
        exercises = listOf(
            CompletedExercise(
                id = FoundationId("completed-exercise-weighted"),
                completedWorkoutId = FoundationId("completed-mixed"),
                exerciseCatalogId = FoundationId("exercise-bench"),
                displayNameSnapshot = "Bench Press",
                position = OrderedPosition(0),
                loggedSets = listOf(weightedSet)
            ),
            CompletedExercise(
                id = FoundationId("completed-exercise-bodyweight"),
                completedWorkoutId = FoundationId("completed-mixed"),
                exerciseCatalogId = FoundationId("exercise-pullup"),
                displayNameSnapshot = "Pull-Up",
                position = OrderedPosition(1),
                loggedSets = listOf(bodyweightSet)
            )
        ),
        createdAt = finished
    )
}
